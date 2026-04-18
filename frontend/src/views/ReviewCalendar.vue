<template>
  <div class="review-page page-enter">
    <header class="page-header">
      <div class="header-main">
        <div>
          <p class="header-eyebrow">Admin Review</p>
          <h1>審核介面</h1>
          <p>用月曆快速比對同月申請，直接查看時段、用途與目前審核狀態。</p>
        </div>

        <div class="legend-group">
          <span class="legend-item">
            <span class="legend-dot is-withdrawn"></span>
            已撤回
          </span>
          <span class="legend-item">
            <span class="legend-dot is-pending"></span>
            審核中
          </span>
          <span class="legend-item">
            <span class="legend-dot is-approved"></span>
            已通過
          </span>
          <span class="legend-item">
            <span class="legend-dot is-rejected"></span>
            已拒絕
          </span>
        </div>
      </div>

      <section class="filter-panel card">
        <div class="filter-field">
          <label for="review-venue">場地</label>
          <select id="review-venue" v-model.number="selectedVenueId" @change="handleFilterChange">
            <option v-for="venue in venues" :key="venue.id" :value="venue.id">
              {{ venue.name }}
            </option>
          </select>
        </div>

        <div class="filter-field">
          <label for="review-status">申請狀態</label>
          <select id="review-status" v-model="selectedStatus" @change="handleFilterChange">
            <option value="">全部狀態</option>
            <option value="0">已撤回</option>
            <option value="1">審核中</option>
            <option value="2">已通過</option>
            <option value="3">已拒絕</option>
          </select>
        </div>

        <div class="filter-summary">
          <span class="summary-label">目前場地</span>
          <strong>{{ selectedVenueName }}</strong>
          <span class="summary-subtle">{{ selectedStatusLabel }}</span>
        </div>
      </section>
    </header>

    <div v-if="pageLoading" class="loading-state">載入場地與審核資料中...</div>

    <div v-else class="calendar-shell" :class="{ 'is-loading': isFetchingEvents }">
      <FullCalendar ref="calendarRef" :options="calendarOptions" />
    </div>
  </div>

  <ReviewBookingModal
    :visible="isDetailModalVisible"
    :booking="selectedBookingDetail"
    :loading="detailLoading"
    :processing="detailProcessing"
    @close="closeDetailModal"
    @approve="handleApprove"
    @update-status="handleStatusUpdate"
  />
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import FullCalendar from "@fullcalendar/vue3";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";

import ReviewBookingModal from "@/components/review/ReviewBookingModal.vue";
import { fetchVenuesByUnit } from "@/api/venue";
import {
  approveReviewBooking,
  fetchPendingReviews,
  fetchReviewBookingDetail,
  updateReviewBookingStatus,
} from "@/api/review";
import {
  convertSlotsToTimeRange,
  formatSlotsAsTimeRange,
  formatSlotGroupsAsTimeRange,
  getBookingStatusMeta,
  getReviewEventColorConfig,
  groupContiguousSlots,
} from "@/utils/dateHelper";
import { useToast } from "@/utils/useToast";

const { success, error } = useToast();

const calendarRef = ref(null);
const venues = ref([]);
const selectedVenueId = ref(1);
const selectedStatus = ref("");
const pageLoading = ref(true);
const isFetchingEvents = ref(false);
const events = ref([]);
const monthlyBookings = ref([]);

const isDetailModalVisible = ref(false);
const detailLoading = ref(false);
const detailProcessing = ref(false);
const selectedBookingId = ref(null);
const selectedBookingDetail = ref(null);

const selectedVenueName = computed(() => {
  return venues.value.find((venue) => venue.id === selectedVenueId.value)?.name || "未選擇場地";
});

const selectedStatusLabel = computed(() => {
  return selectedStatus.value === ""
    ? "顯示全部狀態"
    : `目前篩選：${getBookingStatusMeta(Number(selectedStatus.value)).text}`;
});

const renderEventContent = (arg) => {
  const wrapper = document.createElement("div");
  wrapper.className = "calendar-event-content";

  const time = document.createElement("span");
  time.className = "calendar-event-time";
  time.textContent = arg.event.extendedProps.timeLabel;

  const purpose = document.createElement("span");
  purpose.className = "calendar-event-purpose";
  purpose.textContent = arg.event.extendedProps.purposeLabel;

  wrapper.append(time, purpose);

  return {
    domNodes: [wrapper],
  };
};

const renderMoreLinkContent = (arg) => {
  return {
    html: `<span class="calendar-more-link-text">還有 ${arg.num} 筆</span>`,
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

const renderDayCellContent = (arg) => {
  const count = getDailyEventCount(arg.date);

  return {
    html: `
      <span class="calendar-day-number">${arg.dayNumberText}</span>
      ${count > 0 ? `<span class="calendar-day-count">${count}</span>` : ""}
    `,
  };
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
  eventTimeFormat: {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  },
  dayCellContent: renderDayCellContent,
  eventContent: renderEventContent,
  moreLinkContent: renderMoreLinkContent,
  events,
  datesSet: async (arg) => {
    if (!selectedVenueId.value) return;
    await loadEvents(arg.view);
  },
  eventClick: async (info) => {
    const bookingId = info.event.extendedProps.bookingId;
    await openBookingDetail(bookingId);
  },
});

const getMonthRangeFromView = (view) => {
  const anchor = new Date(view.currentStart);
  anchor.setDate(anchor.getDate() + 15);

  const year = anchor.getFullYear();
  const month = anchor.getMonth();
  const startDate = new Date(year, month, 1);
  const endDate = new Date(year, month + 1, 0);

  return {
    startDate: formatDateKey(startDate),
    endDate: formatDateKey(endDate),
  };
};

const mapBookingsToEvents = (bookings) => {
  const mappedEvents = [];

  bookings.forEach((booking) => {
    const slotGroups = groupContiguousSlots(booking.slots);

    slotGroups.forEach((group) => {
      const timeRange = convertSlotsToTimeRange(booking.bookingDate, group);

      if (!timeRange) return;

      mappedEvents.push({
        title: booking.purpose || "未填寫用途",
        start: timeRange.start,
        end: timeRange.end,
        display: "block",
        extendedProps: {
          bookingId: booking.id,
          booking,
          timeLabel: formatSlotsAsTimeRange(group),
          purposeLabel: booking.purpose?.trim() || "未填寫用途",
          fullTimeLabel: formatSlotGroupsAsTimeRange(booking.slots),
        },
        ...getReviewEventColorConfig(booking.status),
      });
    });
  });

  return mappedEvents;
};

const loadEvents = async (view) => {
  isFetchingEvents.value = true;

  try {
    const monthRange = getMonthRangeFromView(view);

    const bookings = await fetchPendingReviews({
      venueId: selectedVenueId.value,
      startDate: monthRange.startDate,
      endDate: monthRange.endDate,
      status: selectedStatus.value === "" ? undefined : Number(selectedStatus.value),
    });

    monthlyBookings.value = bookings;
    events.value = mapBookingsToEvents(bookings);
  } catch (loadError) {
    monthlyBookings.value = [];
    events.value = [];
    error(loadError.message || "取得審核月曆失敗");
  } finally {
    isFetchingEvents.value = false;
    pageLoading.value = false;
  }
};

const reloadCurrentView = async () => {
  const view = calendarRef.value?.getApi().view;

  if (view) {
    await loadEvents(view);
  }
};

const handleFilterChange = async () => {
  await reloadCurrentView();
};

const openBookingDetail = async (bookingId) => {
  selectedBookingId.value = bookingId;
  selectedBookingDetail.value = null;
  detailLoading.value = true;
  isDetailModalVisible.value = true;

  try {
    selectedBookingDetail.value = await fetchReviewBookingDetail(bookingId);
  } catch (detailError) {
    error(detailError.message || "取得申請詳情失敗");
    closeDetailModal();
  } finally {
    detailLoading.value = false;
  }
};

const closeDetailModal = () => {
  isDetailModalVisible.value = false;
  detailLoading.value = false;
  detailProcessing.value = false;
  selectedBookingId.value = null;
  selectedBookingDetail.value = null;
};

const handleApprove = async () => {
  if (!selectedBookingId.value) return;

  detailProcessing.value = true;

  try {
    await approveReviewBooking(selectedBookingId.value);
    success("申請已通過");
    closeDetailModal();
    await reloadCurrentView();
  } catch (approveError) {
    error(approveError.message || "通過申請失敗");
  } finally {
    detailProcessing.value = false;
  }
};

const handleStatusUpdate = async (status) => {
  if (!selectedBookingId.value) return;

  detailProcessing.value = true;

  try {
    await updateReviewBookingStatus(selectedBookingId.value, status);
    success(`狀態已更新為${getBookingStatusMeta(status).text}`);
    closeDetailModal();
    await reloadCurrentView();
  } catch (updateError) {
    error(updateError.message || "更新狀態失敗");
  } finally {
    detailProcessing.value = false;
  }
};

onMounted(async () => {
  try {
    const fetchedVenues = await fetchVenuesByUnit(1);
    venues.value = fetchedVenues;
    pageLoading.value = false;

    if (!fetchedVenues.some((venue) => venue.id === selectedVenueId.value)) {
      selectedVenueId.value = fetchedVenues[0]?.id || null;
    }

    if (fetchedVenues.length === 0) {
      pageLoading.value = false;
      error("目前沒有可供審核的場地");
    }
  } catch (venueError) {
    error(venueError.message || "取得場地清單失敗");
    pageLoading.value = false;
  }
});
</script>

<style lang="scss" scoped>
.review-page {
  .header-main {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
  }

  .header-eyebrow {
    margin: 0 0 0.35rem;
    color: var(--accent);
    font-size: var(--text-sm);
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
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

    &.is-withdrawn {
      background: #7b8794;
    }

    &.is-pending {
      background: #f4d37b;
    }

    &.is-approved {
      background: #2f8f63;
    }

    &.is-rejected {
      background: #ca5656;
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

  .calendar-shell {
    background: var(--card);
    padding: 1.5rem;
    border-radius: var(--radius-lg);
    border: 1px solid rgba(var(--blue-900-rgb), 0.08);
    box-shadow: var(--shadow-soft);
    overflow-x: auto;
    transition: opacity 0.3s ease;

    &.is-loading {
      opacity: 0.5;
      pointer-events: none;
    }
  }

  :deep(.fc) {
    min-width: 780px;

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
      font-weight: 800;
    }

    .fc-button-primary {
      font-size: 0.8rem;
      color: var(--ink);
      background-color: #f2f6fb;
      border: 1px solid rgba(var(--blue-900-rgb), 0.08);
      border-radius: 999px;

      &:hover,
      &:focus {
        background-color: var(--blue-900);
        color: #ffffff;
      }
    }

    .fc-daygrid-day {
      transition: background-color 0.2s ease;

      &:hover {
        background-color: rgba(var(--blue-900-rgb), 0.03);
      }
    }

    .fc-event {
      cursor: pointer;
      border-radius: 10px;
      padding: 3px 5px;
      font-size: 0.84rem;
      transition:
        filter 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease;

      &:hover {
        filter: brightness(0.96);
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.15);
        transform: translateY(-1px);
      }
    }

    .fc-daygrid-more-link {
      display: inline-flex;
      align-items: center;
      margin-top: 0.35rem;
      color: var(--ink);
      font-size: 1rem;
      font-weight: 700;
      text-decoration: none;

      &:hover {
        color: var(--accent);
        text-decoration: none;
      }
    }

    .fc-popover,
    .fc-more-popover {
      z-index: 20 !important;
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
      display: block;
      width: 100%;
      color: var(--ink);
      font-size: 1.15rem;
      font-weight: 700;
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

    .calendar-event-time {
      font-weight: 700;
    }

    .calendar-event-purpose,
    .calendar-event-time {
      display: block;
      white-space: normal;
      word-break: break-word;
    }
  }

  @media (max-width: 900px) {
    .header-main {
      flex-direction: column;
      align-items: flex-start;
    }

    .legend-group {
      justify-content: flex-start;
    }

    .filter-panel {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 768px) {
    .calendar-shell {
      padding: 0.8rem;
      border-radius: var(--radius);
    }

    :deep(.fc) {
      min-width: 680px;

      .fc-toolbar-title {
        font-size: 1.15rem;
      }

      .fc-toolbar-chunk {
        gap: 0.5rem;
      }

      .fc-event {
        font-size: 0.75rem;
      }
    }
  }
}
</style>
