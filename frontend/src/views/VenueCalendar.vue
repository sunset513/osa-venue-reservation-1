<template>
  <div class="calendar-page">
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
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
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
const router = useRouter();
const venueId = route.params.venueId;

const venueInfo = ref(null);
const loading = ref(true);
const isFetchingEvents = ref(false);
const calendarRef = ref(null);
const events = ref([]);
const isModalVisible = ref(false);
const modalMode = ref("create");
const modalInitialData = ref({});

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
  dayMaxEvents: true, // 若一天太多預約，顯示「+X 更多」，避免格子無限拉長
  displayEventEnd: true, // 顯示結束時間
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
    modalMode.value = "create";
    modalInitialData.value = { dateStr: dateStr };
    isModalVisible.value = true;
  },

  eventClick: (info) => {
    const isMine = info.event.extendedProps.isMine;
    const originalData = info.event.extendedProps.originalData;

    if (isMine && originalData) {
      modalMode.value = "edit";

      // 處理聯絡人資訊解析
      let parsedContact = { name: "", phone: "", email: "" };
      if (originalData.contactInfo) {
        try {
          parsedContact = JSON.parse(originalData.contactInfo);
        } catch (e) {
          console.error("聯絡人資訊解析失敗:", e);
        }
      }

      // 將後端回傳的「設備名稱陣列」轉換為前端表單需要的「設備 ID 陣列」
      let mappedEquipmentIds = [];
      if (originalData.equipments && venueInfo.value?.equipments) {
        mappedEquipmentIds = venueInfo.value.equipments
          .filter((eq) => originalData.equipments.includes(eq.name))
          .map((eq) => eq.id);
      }

      modalInitialData.value = {
        id: originalData.id,
        dateStr: info.event.startStr.split("T")[0],
        slots: originalData.slots,
        purpose: originalData.purpose || "",
        participantCount: originalData.pCount || 1,
        contactInfo: parsedContact,
        equipmentIds: mappedEquipmentIds, // ✨ 將轉換好的 ID 傳給 Modal
      };

      isModalVisible.value = true;
    } else if (!isMine) {
      warning("該時段已被其他人預約，請選擇其他時段。");
    }
  },
});

// --- 核心資料載入邏輯 (純粹解析 bookings 陣列) ---
const loadEvents = async (view) => {
  isFetchingEvents.value = true;
  events.value = [];

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
    padding-bottom: 1rem;
    border-bottom: 1px solid #eee;

    .back-btn {
      background: none;
      border: none;
      color: #0984e3;
      cursor: pointer;
      margin-bottom: 0.5rem;
      padding: 0;
      &:hover {
        text-decoration: underline;
      }
    }
    h1 {
      color: #2d3436;
      margin: 0 0 0.5rem 0;
    }

    .venue-meta {
      display: flex;
      gap: 2rem;
      color: #636e72;
      font-size: 0.9rem;
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
        background-color: #0984e3;
      }
      &.my-pending {
        background-color: #fdcb6e;
      }
      &.others {
        background-color: #b2bec3;
      }
    }
  }

  .calendar-container {
    background: white;
    padding: 1.5rem;
    border-radius: 12px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02);
    transition: opacity 0.3s;

    &.is-loading {
      opacity: 0.5;
      pointer-events: none;
    }
  }

  :deep(.fc) {
    /* --- 新增：優化頂部切換列的排版 --- */
    .fc-toolbar.fc-header-toolbar {
      justify-content: center; /* 強制整個 Toolbar 置中 */
      margin-bottom: 1.5rem;
    }

    .fc-toolbar-chunk {
      display: flex;
      align-items: center;
      gap: 1rem; /* 讓左右箭頭跟中間的標題保持一點呼吸空間 */
    }

    .fc-toolbar-title {
      font-size: 1.5rem;
      color: #2d3436;
      font-weight: bold;
    }
    /* --------------------------------- */

    .fc-button-primary {
      font-size: 0.6rem;
      color: #2d3436;
      background-color: #dbe0e4;
      &:hover {
        transition: all 0.3s;
        background-color: #1e2225;
        color: white;
      }
      &:disabled {
        background-color: #74b9ff;
        border-color: #74b9ff;
      }
    }
    .fc-event {
      cursor: pointer;
      border-radius: 4px;
      padding: 2px 4px;
      font-size: 0.85em;
    }
  }
}
</style>
