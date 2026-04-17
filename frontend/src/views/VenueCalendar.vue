<template>
  <div class="calendar-page page-enter">
    <header class="page-header" v-if="venueInfo">
      <div class="header-left">
        <button class="back-btn" @click="$router.back()">← 返回場地列表</button>
        <h1>{{ venueInfo.name }}</h1>
        <div class="venue-meta">
          <span>👥 容納人數: {{ venueInfo.capacity }} 人</span>
          <span class="status-legend">
            <span class="legend-dot my-approved"></span> 我的預約 (已通過)
            <span class="legend-dot my-pending"></span> 我的預約 (審核中)
            <span class="legend-dot others"></span> 已佔用時段
          </span>
        </div>
      </div>
    </header>

    <div v-if="loading" class="loading-state">載入中，請稍候...</div>

    <div class="calendar-container" :class="{ 'is-loading': isFetchingEvents }">
      <FullCalendar ref="calendarRef" :options="calendarOptions" />
    </div>
  </div>

  <DayScheduleModal
    :visible="isDayModalVisible"
    :selectedDate="selectedDate"
    :dayOfWeek="selectedDayOfWeek"
    :venueName="venueInfo?.name || ''"
    :bookings="selectedDayBookings"
    @close="closeDayModal"
    @create="openCreateModalFromDay"
    @edit-booking="openEditModal"
  />

  <BookingModal
    v-model:visible="isModalVisible"
    :mode="modalMode"
    :initialData="modalInitialData"
    :venueInfo="venueInfo"
    @success="handleModalSuccess"
  />
</template>

<script setup>
import BookingModal from "@/components/booking/BookingModal.vue";
import DayScheduleModal from "@/components/booking/DayScheduleModal.vue";
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
// 簡化 FullCalendar 引入，移除 timeGrid
import FullCalendar from "@fullcalendar/vue3";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";

import { fetchVenueDetail } from "@/api/venue";
import { fetchCalendarMonth } from "@/api/booking"; // 現在只需要月曆 API
import {
  convertSlotsToTimeRange,
  getEventColorConfig,
} from "@/utils/dateHelper";
import { useToast } from "@/utils/useToast.js";

const { warning } = useToast();

const route = useRoute();
const venueId = route.params.venueId;

const venueInfo = ref(null);
const loading = ref(true);
const isFetchingEvents = ref(false);
const calendarRef = ref(null);
const events = ref([]);
const monthlyBookings = ref([]);
const isModalVisible = ref(false);
const isDayModalVisible = ref(false);
const modalMode = ref("create");
const modalInitialData = ref({});
const selectedDate = ref("");
const selectedDayOfWeek = ref("");

const selectedDayBookings = computed(() => {
  if (!selectedDate.value) return [];

  return monthlyBookings.value
    .filter((booking) => booking.bookingDate === selectedDate.value && booking.status !== 0)
    .sort((a, b) => Math.min(...a.slots) - Math.min(...b.slots))
    .map((booking) => {
      const parsedContact = parseContactInfo(booking.contactInfo);

      return {
        id: booking.id,
        purpose: booking.purpose || "",
        contactName: parsedContact.name || "預約人",
        participantCount: booking.pCount || 0,
        timeRange: formatSlotsAsTimeRange(booking.slots),
        statusText: getStatusText(booking.status),
        statusClass: getStatusClass(booking.status),
        isEditable: booking.status === 1,
        originalData: booking,
      };
    });
});

// 將不連續的 slots 分組 (例如 [8,9,14,15] 變成 [[8,9], [14,15]])
const groupContiguousSlots = (slots) => {
  if (!slots || slots.length === 0) return [];
  const sorted = [...slots].sort((a, b) => a - b);
  const groups = [];
  let currentGroup = [sorted[0]];

  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i] === sorted[i - 1] + 1) {
      currentGroup.push(sorted[i]);
    } else {
      groups.push(currentGroup);
      currentGroup = [sorted[i]];
    }
  }
  groups.push(currentGroup);
  return groups;
};

const parseContactInfo = (contactInfo) => {
  if (!contactInfo) return { name: "", phone: "", email: "" };

  try {
    return JSON.parse(contactInfo);
  } catch (error) {
    console.error("聯絡人資訊解析失敗:", error);
    return { name: "", phone: "", email: "" };
  }
};

const padZero = (num) => num.toString().padStart(2, "0");

const formatSlotsAsTimeRange = (slots) => {
  if (!slots || slots.length === 0) return "";

  const sortedSlots = [...slots].sort((a, b) => a - b);
  const start = sortedSlots[0];
  const end = sortedSlots[sortedSlots.length - 1] + 1;

  return `${padZero(start)}:00 - ${padZero(end)}:00`;
};

const getStatusText = (status) => {
  switch (status) {
    case 1:
      return "審核中";
    case 2:
      return "已核准";
    case 3:
      return "已拒絕";
    default:
      return "已撤回";
  }
};

const getStatusClass = (status) => {
  switch (status) {
    case 1:
      return "is-pending";
    case 2:
      return "is-approved";
    case 3:
      return "is-rejected";
    default:
      return "is-withdrawn";
  }
};

const renderEventContent = (arg) => {
  const purpose = arg.event.extendedProps.originalData?.purpose?.trim();
  const fallbackLabel = arg.event.title;
  const label = purpose || fallbackLabel;

  return {
    html: `
      <div class="calendar-event-content">
        <span class="calendar-event-time">${arg.timeText}</span>
        <span class="calendar-event-purpose">${label}</span>
      </div>
    `,
  };
};

const renderMoreLinkContent = (arg) => {
  return {
    html: `<span class="calendar-more-link-text">還有 ${arg.num} 個</span>`,
  };
};

const formatDateKey = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const getDailyEventCount = (date) => {
  const dateKey = formatDateKey(date);
  return events.value.filter((event) => event.start?.split("T")[0] === dateKey).length;
};

const openDayModal = (dateStr) => {
  selectedDate.value = dateStr;

  const targetDate = new Date(`${dateStr}T00:00:00`);
  selectedDayOfWeek.value = targetDate.toLocaleDateString("zh-TW", {
    weekday: "long",
  });

  isDayModalVisible.value = true;
};

const closeDayModal = () => {
  isDayModalVisible.value = false;
};

const openCreateModal = (dateStr) => {
  modalMode.value = "create";
  modalInitialData.value = { dateStr };
  isModalVisible.value = true;
};

const openCreateModalFromDay = () => {
  closeDayModal();
  openCreateModal(selectedDate.value);
};

const openEditModal = (originalData) => {
  modalMode.value = "edit";

  const mappedEquipmentIds =
    originalData.equipments && venueInfo.value?.equipments
      ? venueInfo.value.equipments
          .filter((eq) => originalData.equipments.includes(eq.name))
          .map((eq) => eq.id)
      : [];

  modalInitialData.value = {
    id: originalData.id,
    dateStr: originalData.bookingDate,
    slots: originalData.slots,
    purpose: originalData.purpose || "",
    participantCount: originalData.pCount || 1,
    contactInfo: parseContactInfo(originalData.contactInfo),
    equipmentIds: mappedEquipmentIds,
  };

  isDayModalVisible.value = false;
  isModalVisible.value = true;
};

const renderDayCellContent = (arg) => {
  const count = getDailyEventCount(arg.date);

  return {
    html: `
      <span class="calendar-day-number">${arg.dayNumberText}</span>
      ${count > 0 ? `<span class="calendar-day-count">${count}</span>` : ""}
    `,
  };
};

// --- FullCalendar 配置 (專注於月視圖) ---
const calendarOptions = ref({
  plugins: [dayGridPlugin, interactionPlugin],
  initialView: "dayGridMonth",
  headerToolbar: {
    left: "",
    center: "prev title next", // 將上個月、標題、下個月放在中間
    right: "", // 移除視圖切換，保持純粹的月曆
  },
  locale: "zh-tw",
  firstDay: 1,
  height: "auto",
  dayMaxEvents: 3, // 每日最多顯示 3 筆活動，其餘收合到更多連結
  displayEventEnd: true, // 顯示結束時間
  dayCellContent: renderDayCellContent,
  eventContent: renderEventContent,
  moreLinkContent: renderMoreLinkContent,
  eventTimeFormat: {
    // 強制顯示 24 小時制 (例如 08:00)
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  },
  events: events,

  datesSet: async (arg) => {
    await loadEvents(arg.view);
  },

  dateClick: (info) => {
    const dateStr = info.dateStr.split("T")[0];
    openDayModal(dateStr);
  },

  eventClick: (info) => {
    const isMine = info.event.extendedProps.isMine;
    const originalData = info.event.extendedProps.originalData;

    if (isMine && originalData) {
      openEditModal(originalData);
    } else if (!isMine) {
      warning("該時段已被其他人預約，請選擇其他時段。");
    }
  },
});

// --- 核心資料載入邏輯 (純粹解析 bookings 陣列) ---
const loadEvents = async (view) => {
  isFetchingEvents.value = true;
  events.value = [];
  monthlyBookings.value = [];

  try {
    const currentStart = view.currentStart;
    const startObj = new Date(currentStart);
    startObj.setDate(startObj.getDate() + 15);
    const year = startObj.getFullYear();
    const month = startObj.getMonth() + 1;

    const apiData = await fetchCalendarMonth(venueId, year, month);
    const newEvents = [];

    // 新版 API 會在 apiData.bookings 中回傳該月所有的詳細預約
    if (apiData && apiData.bookings) {
      monthlyBookings.value = apiData.bookings;
      apiData.bookings.forEach((booking) => {
        // 狀態 0(已撤回) 與 3(已拒絕) 視為未佔用，不顯示在日曆上
        if (booking.status !== 1 && booking.status !== 2) return;

        // 由於目前 API 沒有明確提供 userId 對比，我們先用 status 判斷：
        // 1(審核中) 必定是自己的預約；2(已通過) 暫時當作已佔用防呆處理
        const isMine = booking.status === 1;

        // 將 slots 分組並轉為具體的起訖時間
        const groups = groupContiguousSlots(booking.slots);
        groups.forEach((group) => {
          const timeRange = convertSlotsToTimeRange(booking.bookingDate, group);
          if (timeRange) {
            newEvents.push({
              title: isMine ? "審核中" : "已佔用",
              start: timeRange.start,
              end: timeRange.end,
              display: "block", // 關鍵：讓事件在月曆上呈現時間色塊，而不是小圓點
              extendedProps: {
                isMine,
                originalData: booking,
              },
              ...getEventColorConfig(booking.status, isMine),
            });
          }
        });
      });
    }

    events.value = newEvents;
  } catch (error) {
    console.error("取得日曆資料失敗:", error);
  } finally {
    isFetchingEvents.value = false;
  }
};

const handleModalSuccess = async () => {
  // 1. 確保彈窗被關閉
  isModalVisible.value = false;
  isDayModalVisible.value = false;

  // 2. 手動重新觸發資料載入，讓月曆刷新
  if (calendarRef.value) {
    const view = calendarRef.value.getApi().view;
    await loadEvents(view); // 直接呼叫我們的 loadEvents 更新 events 陣列
  }
};

onMounted(async () => {
  try {
    venueInfo.value = await fetchVenueDetail(venueId);
  } catch (error) {
    console.error("取得場地資訊失敗");
  } finally {
    loading.value = false;
  }
});
</script>

<style lang="scss" scoped>
/* 樣式部分保持不變，與之前一致 */
.calendar-page {
  .page-header {
    margin-bottom: 2rem;
    padding-bottom: 1.25rem;
    border-bottom: 1px solid var(--line);

    .back-btn {
      margin-bottom: 0.5rem;
    }

    h1 {
      margin: 0 0 0.5rem 0;
    }

    .venue-meta {
      display: flex;
      gap: 2rem;
      color: var(--muted);
      font-size: var(--text-base);
      align-items: center;
    }
  }

  .status-legend {
    display: flex;
    gap: 1rem;
    align-items: center;

    .legend-dot {
      display: inline-block;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      margin-right: 4px;

      &.my-approved {
        background-color: var(--status-occupied);
      }

      &.my-pending {
        background-color: var(--status-pending);
      }

      &.others {
        background-color: var(--status-approved);
      }
    }
  }

  .calendar-container {
    background: var(--card);
    padding: 1.5rem;
    border-radius: var(--radius-lg);
    border: 1px solid rgba(var(--blue-900-rgb), 0.08);
    box-shadow: var(--shadow-soft);
    transition: opacity 0.3s;
    overflow-x: auto;

    &.is-loading {
      opacity: 0.5;
      pointer-events: none;
    }
  }

  :deep(.fc) {
    min-width: 720px;

    /* --- 新增：優化頂部切換列的排版 --- */
    .fc-toolbar.fc-header-toolbar {
      justify-content: center;
      /* 強制整個 Toolbar 置中 */
      margin-bottom: 1.5rem;
    }

    .fc-toolbar-chunk {
      display: flex;
      align-items: center;
      gap: 1rem;
      /* 讓左右箭頭跟中間的標題保持一點呼吸空間 */
    }

    .fc-toolbar-title {
      font-size: 1.5rem;
      color: var(--ink);
      font-weight: bold;
    }

    /* --------------------------------- */

    .fc-button-primary {
      font-size: 0.75rem;
      color: var(--ink);
      background-color: var(--surface-muted);
      border: 1px solid rgba(var(--blue-900-rgb), 0.08);
      border-radius: 999px;

      &:hover {
        transition: all 0.3s;
        background-color: var(--blue-900);
        color: #ffffff;
      }

      &:disabled {
        background-color: var(--accent-soft);
        border-color: var(--accent-soft);
      }
    }

    .fc-daygrid-day {
      cursor: pointer;
      transition: background-color 0.2s ease;

      &:hover {
        background-color: rgba(var(--blue-900-rgb), 0.03);
      }
    }

    .fc-event {
      cursor: pointer;
      border-radius: 8px;
      padding: 2px 4px;
      font-size: 0.85em;
      transition:
        filter 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease;

      &:hover {
        filter: brightness(0.94);
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
        transform: translateY(-1px);
      }
    }

    .fc-daygrid-more-link {
      display: inline-flex;
      align-items: center;
      margin-top: 0.35rem;
      color: var(--ink);
      font-size: 1.05rem;
      font-weight: 700;
      text-decoration: none;

      &:hover {
        color: var(--accent);
        text-decoration: none;
      }
    }

    .calendar-more-link-text {
      display: inline-block;
      line-height: 1.2;
    }

    .fc-daygrid-day-top {
      position: relative;
      display: flex;
      justify-content: flex-start;
      align-items: center;
      padding: 0.25rem 0.35rem;
      min-height: 2rem;
    }

    .fc-daygrid-day-number {
      width: 100%;
      padding: 0;
      text-align: left;
      line-height: 1;
      text-decoration: none;
    }

    .calendar-day-number {
      width: 100%;
      display: block;
      font-size: 1.15rem;
      font-weight: 700;
      color: var(--ink);
      line-height: 1;
      text-align: left;
    }

    .calendar-day-count {
      position: absolute;
      top: 50%;
      right: 0.35rem;
      transform: translateY(-50%);
      min-width: 1.6rem;
      height: 1.6rem;
      padding: 0 0.45rem;
      border-radius: 999px;
      background-color: var(--accent);
      color: #ffffff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
      font-weight: 700;
      line-height: 1;
    }

    .calendar-event-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
      line-height: 1.2;
    }

    .calendar-event-purpose,
    .calendar-event-time {
      display: block;
      white-space: normal;
      word-break: break-word;
    }
  }

  @media (max-width: 768px) {
    .page-header {
      .venue-meta {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.75rem;
      }
    }

    .calendar-container {
      padding: 0.75rem;
      border-radius: var(--radius);
    }

    :deep(.fc) {
      min-width: 680px;

      .fc-toolbar.fc-header-toolbar {
        margin-bottom: 1rem;
      }

      .fc-toolbar-title {
        font-size: 1.15rem;
      }

      .fc-toolbar-chunk {
        gap: 0.5rem;
      }

      .fc-col-header-cell-cushion {
        padding: 0.35rem 0.15rem;
        font-size: 0.85rem;
      }

      .fc-daygrid-day-top {
        min-height: 1.75rem;
        padding: 0.2rem 0.25rem;
      }

      .calendar-day-number {
        font-size: 1rem;
      }

      .calendar-day-count {
        right: 0.25rem;
        min-width: 1.4rem;
        height: 1.4rem;
        padding: 0 0.35rem;
        font-size: 0.75rem;
      }

      .fc-event {
        padding: 2px 3px;
        font-size: 0.75rem;
      }

      .calendar-event-content {
        gap: 1px;
      }
    }
  }
}
</style>
