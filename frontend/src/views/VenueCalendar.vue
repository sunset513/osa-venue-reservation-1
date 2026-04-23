<template>
  <div class="calendar-page page-enter">
    <header v-if="venueInfo" class="page-header">
      <div class="header-toolbar">
        <div class="toolbar-left">
          <button class="back-btn" @click="goBackToVenueList">← 返回場地列表</button>
          <div class="toolbar-title">
            <span class="toolbar-label">目前場地</span>
            <h2>{{ venueInfo.name }}</h2>
            <div class="venue-meta">
              <span>👥 容納人數: {{ venueInfo.capacity }} 人</span>
            </div>
          </div>
        </div>

        <div class="toolbar-actions">
          <button
            class="toggle-info-btn"
            type="button"
            :aria-expanded="String(!isInfoCollapsed)"
            @click="toggleInfoPanel"
          >
            <ChevronDown v-if="isInfoCollapsed" :size="16" aria-hidden="true" />
            <ChevronUp v-else :size="16" aria-hidden="true" />
            {{ isInfoCollapsed ? "展開資訊" : "收合資訊" }}
          </button>
        </div>
      </div>

      <div v-show="!isInfoCollapsed" class="header-content">
        <div class="header-main">
          <div class="header-left">

            <p class="header-description">用月曆查看當月借用狀況，並快速切換同單位其他場地。</p>
   
          </div>

          <div class="legend-group">
            <span class="legend-item">
              <span class="legend-dot my-approved"></span>
              我的預約 (已通過)
            </span>
            <span class="legend-item">
              <span class="legend-dot my-pending"></span>
              審核中
            </span>
            <span class="legend-item">
              <span class="legend-dot others"></span>
              已佔用時段
            </span>
          </div>
        </div>

        <section class="filter-panel card">
          <div class="filter-field filter-field-main">
            <label for="venue-switcher">場地</label>
            <select
              id="venue-switcher"
              v-model.number="selectedVenueId"
              :disabled="loading || isSwitchingVenue"
              @change="handleVenueChange"
            >
              <option v-for="venue in switchableVenues" :key="venue.id" :value="venue.id">
                {{ venue.name }}
              </option>
            </select>
          </div>

          <div class="filter-summary">
            <span class="summary-label">目前場地</span>
            <strong>{{ selectedVenueName }}</strong>
            <span class="summary-subtle">可快速切換同單位其他場地</span>
          </div>
        </section>
      </div>
    </header>

    <div v-if="loading" class="loading-state">{{ loadingMessage }}</div>

    <div class="calendar-container" :class="{ 'is-loading': loading || isFetchingEvents }">
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
import { fetchCalendarMonth } from "@/api/booking";
import { fetchVenueDetail, fetchVenuesByUnit } from "@/api/venue";
import {
  convertSlotsToTimeRange,
  formatSlotsAsTimeRange,
  getEventColorConfig,
  groupContiguousSlots,
} from "@/utils/dateHelper";
import { getDailyEventCount, renderMoreLinkContent } from "@/utils/calendarDisplay";
import { getBookingStatusMeta, parseContactInfo } from "@/utils/bookingMeta";
import { useToast } from "@/utils/useToast.js";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import FullCalendar from "@fullcalendar/vue3";
import { ChevronDown, ChevronUp } from "lucide-vue-next";
import { computed, nextTick, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

const { error, warning } = useToast();

const route = useRoute();
const router = useRouter();

const venueInfo = ref(null);
const venues = ref([]);
const selectedVenueId = ref(null);
const loading = ref(true);
const isFetchingEvents = ref(false);
const isSwitchingVenue = ref(false);
const isInfoCollapsed = ref(false);
const calendarRef = ref(null);
const events = ref([]);
const monthlyBookings = ref([]);
const isModalVisible = ref(false);
const isDayModalVisible = ref(false);
const modalMode = ref("create");
const modalInitialData = ref({});
const selectedDate = ref("");
const selectedDayOfWeek = ref("");
const suppressDatesSetLoad = ref(false);

let venueSyncToken = 0;
let eventsRequestToken = 0;

const currentRouteVenueId = computed(() => {
  const parsedVenueId = Number(route.params.venueId);
  return Number.isFinite(parsedVenueId) ? parsedVenueId : null;
});

const loadingMessage = computed(() => {
  return isSwitchingVenue.value ? "切換場地中，請稍候..." : "載入中，請稍候...";
});

const switchableVenues = computed(() => {
  if (!venueInfo.value) return venues.value;

  const currentVenueExists = venues.value.some((venue) => venue.id === venueInfo.value.id);

  if (currentVenueExists) {
    return venues.value;
  }

  return [venueInfo.value, ...venues.value];
});

const selectedVenueName = computed(() => {
  return switchableVenues.value.find((venue) => venue.id === selectedVenueId.value)?.name || venueInfo.value?.name || "未選擇場地";
});

const selectedDayBookings = computed(() => {
  if (!selectedDate.value) return [];

  return monthlyBookings.value
    .filter((booking) => booking.bookingDate === selectedDate.value && booking.status !== 0)
    .sort((a, b) => Math.min(...a.slots) - Math.min(...b.slots))
    .map((booking) => {
      const parsedContact = parseContactInfo(booking.contactInfo);
      const statusMeta = getBookingStatusMeta(booking.status);

      return {
        id: booking.id,
        purpose: booking.purpose || "",
        contactName: parsedContact.name || "預約人",
        participantCount: booking.pCount || 0,
        timeRange: formatSlotsAsTimeRange(booking.slots),
        statusText: statusMeta.text,
        statusClass: statusMeta.className,
        isEditable: booking.status === 1,
        originalData: booking,
      };
    });
});

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

const resetVenueState = () => {
  events.value = [];
  monthlyBookings.value = [];
  selectedDate.value = "";
  selectedDayOfWeek.value = "";
};

const closeTransientUi = () => {
  isModalVisible.value = false;
  isDayModalVisible.value = false;
  modalInitialData.value = {};
  selectedDate.value = "";
  selectedDayOfWeek.value = "";
};

const goBackToVenueList = () => {
  const unitId = venueInfo.value?.unitId;

  if (unitId) {
    router.push({ name: "VenueSelector", params: { unitId: String(unitId) } });
    return;
  }

  router.push("/");
};

const toggleInfoPanel = () => {
  isInfoCollapsed.value = !isInfoCollapsed.value;
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
  const count = getDailyEventCount(events.value, arg.date);

  return {
    html: `
      <span class="calendar-day-number">${arg.dayNumberText}</span>
      ${count > 0 ? `<span class="calendar-day-count">${count}</span>` : ""}
    `,
  };
};

const loadEvents = async (view, targetVenueId = venueInfo.value?.id) => {
  if (!view || !targetVenueId) return;

  const requestToken = ++eventsRequestToken;
  isFetchingEvents.value = true;
  events.value = [];
  monthlyBookings.value = [];

  try {
    const currentStart = view.currentStart;
    const startObj = new Date(currentStart);
    startObj.setDate(startObj.getDate() + 15);
    const year = startObj.getFullYear();
    const month = startObj.getMonth() + 1;

    const apiData = await fetchCalendarMonth(targetVenueId, year, month);

    if (requestToken !== eventsRequestToken) return;

    const newEvents = [];

    if (apiData?.bookings) {
      monthlyBookings.value = apiData.bookings;

      apiData.bookings.forEach((booking) => {
        if (booking.status !== 1 && booking.status !== 2) return;

        const isMine = booking.status === 1;
        const groups = groupContiguousSlots(booking.slots);

        groups.forEach((group) => {
          const timeRange = convertSlotsToTimeRange(booking.bookingDate, group);

          if (!timeRange) return;

          newEvents.push({
            title: isMine ? "審核中" : "已佔用",
            start: timeRange.start,
            end: timeRange.end,
            display: "block",
            extendedProps: {
              isMine,
              originalData: booking,
            },
            ...getEventColorConfig(booking.status, isMine),
          });
        });
      });
    }

    events.value = newEvents;
  } catch (loadError) {
    if (requestToken !== eventsRequestToken) return;

    console.error("取得日曆資料失敗:", loadError);
    events.value = [];
    monthlyBookings.value = [];
    error(loadError.message || "取得場地月曆失敗");
  } finally {
    if (requestToken === eventsRequestToken) {
      isFetchingEvents.value = false;
    }
  }
};

const reloadCurrentView = async (targetVenueId = venueInfo.value?.id) => {
  const view = calendarRef.value?.getApi().view;

  if (view) {
    await loadEvents(view, targetVenueId);
  }
};

const syncVenueContext = async (targetVenueId) => {
  if (!targetVenueId) return;

  const syncToken = ++venueSyncToken;
  suppressDatesSetLoad.value = true;
  loading.value = true;
  isSwitchingVenue.value = true;
  closeTransientUi();
  resetVenueState();
  venueInfo.value = null;
  venues.value = [];
  selectedVenueId.value = Number(targetVenueId);

  try {
    const fetchedVenue = await fetchVenueDetail(targetVenueId);

    if (syncToken !== venueSyncToken) return;

    venueInfo.value = fetchedVenue;
    selectedVenueId.value = fetchedVenue.id;

    try {
      venues.value = await fetchVenuesByUnit(fetchedVenue.unitId);
    } catch (venueListError) {
      if (syncToken !== venueSyncToken) return;

      console.error("取得同單位場地清單失敗:", venueListError);
      venues.value = [];
      error(venueListError.message || "取得同單位場地清單失敗");
    }

    if (syncToken !== venueSyncToken) return;

    await nextTick();
    suppressDatesSetLoad.value = false;
    await reloadCurrentView(fetchedVenue.id);
  } catch (syncError) {
    if (syncToken !== venueSyncToken) return;

    console.error("取得場地資訊失敗:", syncError);
    venueInfo.value = null;
    venues.value = [];
    resetVenueState();
    error(syncError.message || "取得場地資訊失敗");
  } finally {
    if (syncToken === venueSyncToken) {
      suppressDatesSetLoad.value = false;
      loading.value = false;
      isSwitchingVenue.value = false;
    }
  }
};

const handleVenueChange = async () => {
  if (!selectedVenueId.value || selectedVenueId.value === currentRouteVenueId.value) {
    return;
  }

  await router.replace({
    name: "VenueCalendar",
    params: { venueId: String(selectedVenueId.value) },
  });
};

const handleModalSuccess = async () => {
  isModalVisible.value = false;
  isDayModalVisible.value = false;
  await reloadCurrentView();
};

const calendarOptions = ref({
  plugins: [dayGridPlugin, interactionPlugin],
  initialView: "dayGridMonth",
  headerToolbar: {
    left: "",
    center: "prev title next",
    right: "",
  },
  locale: "zh-tw",
  firstDay: 1,
  height: "auto",
  dayMaxEvents: 3,
  displayEventEnd: true,
  dayCellContent: renderDayCellContent,
  eventContent: renderEventContent,
  moreLinkContent: (arg) => renderMoreLinkContent(arg, "個"),
  moreLinkClick: () => {},
  eventTimeFormat: {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  },
  events,
  datesSet: async (arg) => {
    if (suppressDatesSetLoad.value || !venueInfo.value?.id) return;
    await loadEvents(arg.view, venueInfo.value.id);
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

watch(
  () => route.params.venueId,
  async (nextVenueId) => {
    const normalizedVenueId = Number(nextVenueId);

    if (!Number.isFinite(normalizedVenueId)) return;

    await syncVenueContext(normalizedVenueId);
  },
  { immediate: true },
);
</script>

<style lang="scss" scoped>
.calendar-page {
  .page-header {
    margin-bottom: 2rem;
    padding-bottom: 1.25rem;
    border-bottom: 1px solid var(--line);
  }

  .header-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
  }

  .toolbar-left,
  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 0.85rem;
  }

  .toolbar-title {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;

    strong {
      color: var(--ink);
      font-size: 1rem;
    }
  }

  .toolbar-label {
    color: var(--muted);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .toggle-info-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    min-height: 2.75rem;
    padding: 0.7rem 1rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.08);
    border-radius: 999px;
    background: #f2f6fb;
    color: var(--ink);
    font-size: var(--text-sm);
    font-weight: 700;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      color 0.2s ease,
      transform 0.2s ease;

    &:hover {
      background: var(--blue-900);
      color: #ffffff;
      transform: translateY(-1px);
    }
  }

  .header-content {
    margin-top: 1.25rem;
  }

  .header-main {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
  }

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .back-btn {
    align-self: flex-start;
  }

  h1 {
    margin: 0;
  }

  .header-eyebrow {
    margin: 0;
    color: var(--accent);
    font-size: var(--text-sm);
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .header-description {
    margin: 0;
    color: var(--muted);
  }

  .venue-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem 1rem;
    color: var(--muted);
    font-size: var(--text-base);
    align-items: center;
  }

  .legend-group {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 0.75rem;
  }

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    min-height: 2.2rem;
    padding: 0.45rem 0.85rem;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.78);
    border: 1px solid rgba(var(--blue-900-rgb), 0.08);
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .legend-dot {
    width: 0.75rem;
    height: 0.75rem;
    border-radius: 50%;
  }

  .legend-dot {
    &.my-approved {
      background-color: var(--status-info);
    }

    &.my-pending {
      background-color: var(--status-pending);
    }

    &.others {
      background-color: var(--status-approved);
    }
  }

  .filter-panel {
    margin-top: 1.5rem;
    padding: 1.1rem 1.2rem;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1rem;
  }

  .filter-field,
  .filter-summary {
    display: flex;
    flex-direction: column;
    gap: 0.45rem;
  }

  .filter-field-main {
    grid-column: span 2;
  }

  .filter-field {
    label {
      color: var(--muted);
      font-size: var(--text-sm);
      font-weight: 700;
    }

    select {
      min-height: 3rem;
      padding: 0.75rem 0.95rem;
      border: 1px solid var(--line);
      border-radius: var(--radius);
      background: #ffffff;
      color: var(--ink);

      &:disabled {
        opacity: 0.7;
        cursor: wait;
      }
    }
  }

  .filter-summary {
    justify-content: center;
    padding: 0.9rem 1rem;
    border-radius: var(--radius);
    background: linear-gradient(180deg, rgba(21, 58, 99, 0.04) 0%, rgba(21, 58, 99, 0.01) 100%);
  }

  .summary-label,
  .summary-subtle {
    color: var(--muted);
    font-size: var(--text-sm);
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

    .fc-toolbar.fc-header-toolbar {
      justify-content: center;
      margin-bottom: 1.5rem;
    }

    .fc-toolbar-chunk {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .fc-toolbar-title {
      font-size: 1.5rem;
      color: var(--ink);
      font-weight: bold;
    }

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
      cursor: default;
      font-size: 1.05rem;
      font-weight: 700;
      text-decoration: none;
      pointer-events: none;

      &:hover {
        color: var(--ink);
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
      z-index: 0;
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
    .header-toolbar,
    .header-main {
      flex-direction: column;
      align-items: flex-start;
    }

    .toolbar-left,
    .toolbar-actions {
      width: 100%;
    }

    .toolbar-left {
      flex-direction: column;
      align-items: flex-start;
    }

    .toggle-info-btn {
      width: 100%;
    }

    .legend-group {
      justify-content: flex-start;
    }

    .venue-meta {
      flex-direction: column;
      align-items: flex-start;
      gap: 0.75rem;
    }

    .filter-panel {
      grid-template-columns: 1fr;
    }

    .filter-field-main {
      grid-column: auto;
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
