<template>
  <div class="review-page page-enter">
    <header class="workbench-header">
      <div class="header-copy">
        <span class="mode-pill">
          <ShieldCheck :size="16" aria-hidden="true" />
          審核者模式
        </span>
        <p class="header-eyebrow">Review Workbench</p>
        <h1>審核工作台</h1>
        <p>以預約申請為中心處理場地借用，檢視狀態、比對時段，並從同一處完成核准或退回。</p>
      </div>

      <button
        class="btn btn-secondary route-booking-btn"
        type="button"
        :disabled="!selectedVenueId"
        @click="navigateToVenueBooking()"
      >
        <ArrowRight :size="17" aria-hidden="true" />
        <span>前往一般預約流程</span>
      </button>
    </header>

    <div v-if="pageLoading" class="loading-state">載入場地與審核資料中...</div>

    <div v-else class="workbench-layout">
      <aside class="control-panel card">
        <section class="panel-section">
          <label for="review-venue">審核場地</label>
          <select id="review-venue" v-model.number="selectedVenueId" @change="handleFilterChange">
            <option v-for="venue in venues" :key="venue.id" :value="venue.id">
              {{ venue.name }}
            </option>
          </select>
        </section>

        <section class="panel-section status-filter-section" aria-label="申請狀態篩選">
          <span class="section-label">申請狀態</span>
          <div class="status-filter-list">
            <button
              v-for="option in statusFilterOptions"
              :key="option.key"
              class="status-filter-card"
              :class="[option.className, { 'is-active': selectedStatus === option.statusValue }]"
              type="button"
              :aria-pressed="selectedStatus === option.statusValue"
              @click="selectStatusFilter(option.statusValue)"
            >
              <span class="status-filter-icon">
                <component :is="option.icon" :size="19" aria-hidden="true" />
              </span>
              <span class="status-filter-copy">
                <strong>{{ option.label }}</strong>
                <span>{{ option.helper }}</span>
              </span>
              <span class="status-filter-count">{{ option.value }}</span>
            </button>
          </div>
        </section>

      </aside>

      <section class="calendar-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">目前場地</p>
            <h2>{{ selectedVenueName }}</h2>
          </div>
          <div class="panel-heading-actions">
            <div class="view-toggle" role="group" aria-label="切換預約申請檢視">
              <button
                class="view-toggle-btn"
                :class="{ 'is-active': activeViewMode === 'calendar' }"
                type="button"
                :aria-pressed="activeViewMode === 'calendar'"
                @click="activeViewMode = 'calendar'"
              >
                <CalendarDays :size="16" aria-hidden="true" />
                <span>月曆</span>
              </button>
              <button
                class="view-toggle-btn"
                :class="{ 'is-active': activeViewMode === 'list' }"
                type="button"
                :aria-pressed="activeViewMode === 'list'"
                @click="activeViewMode = 'list'"
              >
                <List :size="16" aria-hidden="true" />
                <span>列表</span>
              </button>
            </div>
            <label class="quick-status-filter" for="review-status-quick">
              <span>目前篩選</span>
              <select id="review-status-quick" v-model="selectedStatus" :disabled="isFetchingEvents" @change="handleFilterChange">
                <option value="">全部申請</option>
                <option value="1">審核中</option>
                <option value="2">已通過</option>
                <option value="3">已拒絕</option>
              </select>
            </label>
          </div>
        </div>

        <div v-show="activeViewMode === 'calendar'" class="calendar-shell" :class="{ 'is-loading': isFetchingEvents }">
          <FullCalendar ref="calendarRef" :options="calendarOptions" />
        </div>

        <div v-show="activeViewMode === 'list'" class="list-shell" :class="{ 'is-loading': isFetchingEvents }">
          <div v-if="reviewListBookings.length === 0" class="list-empty-state">
            目前篩選條件下沒有預約申請。
          </div>

          <div v-else class="case-list">
            <button
              v-for="booking in reviewListBookings"
              :key="booking.id"
              class="case-row"
              type="button"
              @click="openBookingDetail(booking.id)"
            >
              <div class="case-main">
                <div class="case-title-line">
                  <span class="status-badge" :class="booking.statusClass">{{ booking.statusText }}</span>
                  <strong>{{ booking.purpose || "未填寫用途" }}</strong>
                </div>
                <div class="case-meta">
                  <span>申請編號 #{{ booking.id }}</span>
                  <span>{{ booking.contactName }}</span>
                  <span>{{ booking.participantCount }} 人</span>
                </div>
              </div>

              <div class="case-schedule">
                <strong>{{ booking.bookingDate }}</strong>
                <span>{{ booking.timeRange || "未提供時段" }}</span>
              </div>
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>

  <ReviewDayScheduleModal
    :visible="isDayModalVisible"
    :selectedDate="selectedDate"
    :dayOfWeek="selectedDayOfWeek"
    :bookings="selectedDayBookings"
    @close="closeDayModal"
    @open-detail="openBookingDetail"
    @create-booking="navigateToVenueBooking(selectedDate)"
  />

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
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import FullCalendar from "@fullcalendar/vue3";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import {
  ArrowRight,
  BadgeCheck,
  CalendarDays,
  ClipboardList,
  Clock3,
  List,
  ShieldCheck,
  XCircle,
} from "lucide-vue-next";

import ReviewBookingModal from "@/components/review/ReviewBookingModal.vue";
import ReviewDayScheduleModal from "@/components/review/ReviewDayScheduleModal.vue";
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
  getReviewEventColorConfig,
  groupContiguousSlots,
} from "@/utils/dateHelper";
import { formatDateKey, getDailyEventCount, renderMoreLinkContent } from "@/utils/calendarDisplay";
import { getBookingStatusMeta, parseContactInfo } from "@/utils/bookingMeta";
import { useToast } from "@/utils/useToast";

const { success, error } = useToast();
const router = useRouter();

const calendarRef = ref(null);
const venues = ref([]);
const selectedVenueId = ref(1);
const selectedStatus = ref("");
const pageLoading = ref(true);
const isFetchingEvents = ref(false);
const activeViewMode = ref("calendar");
const events = ref([]);
const monthlyBookings = ref([]);
const allMonthlyBookings = ref([]);
const isDayModalVisible = ref(false);
const selectedDate = ref("");
const selectedDayOfWeek = ref("");

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
    ? "顯示全部申請"
    : `目前篩選：${getBookingStatusMeta(Number(selectedStatus.value)).text}`;
});

const statusCounts = computed(() => {
  return allMonthlyBookings.value.reduce(
    (counts, booking) => {
      counts.all += 1;

      if (booking.status === 1) counts.pending += 1;
      if (booking.status === 2) counts.approved += 1;
      if (booking.status === 3) counts.rejected += 1;

      return counts;
    },
    { all: 0, pending: 0, approved: 0, rejected: 0 },
  );
});

const statusFilterOptions = computed(() => [
  {
    key: "all",
    label: "全部申請",
    helper: "目前月份所有申請",
    value: statusCounts.value.all,
    className: "is-all",
    icon: ClipboardList,
    statusValue: "",
  },
  {
    key: "pending",
    label: "審核中",
    helper: "等待處理的申請",
    value: statusCounts.value.pending,
    className: "is-pending",
    icon: Clock3,
    statusValue: "1",
  },
  {
    key: "approved",
    label: "已通過",
    helper: "已核准使用",
    value: statusCounts.value.approved,
    className: "is-approved",
    icon: BadgeCheck,
    statusValue: "2",
  },
  {
    key: "rejected",
    label: "已拒絕",
    helper: "未核准申請",
    value: statusCounts.value.rejected,
    className: "is-rejected",
    icon: XCircle,
    statusValue: "3",
  },
]);

const selectedDayBookings = computed(() => {
  if (!selectedDate.value) return [];

  return monthlyBookings.value
    .filter((booking) => booking.bookingDate === selectedDate.value)
    .sort((a, b) => Math.min(...(a.slots || [Infinity])) - Math.min(...(b.slots || [Infinity])))
    .map((booking) => {
      const parsedContact = parseContactInfo(booking.contactInfo);
      const statusMeta = getBookingStatusMeta(booking.status);

      return {
        id: booking.id,
        purpose: booking.purpose || "",
        venueName: booking.venueName || selectedVenueName.value || "未提供場地",
        contactName: parsedContact.name || "申請人",
        participantCount: booking.pCount || 0,
        timeRange: formatSlotGroupsAsTimeRange(booking.slots),
        statusText: statusMeta.text,
        statusClass: statusMeta.className,
      };
    });
});

const reviewListBookings = computed(() => {
  return [...monthlyBookings.value]
    .sort((a, b) => {
      const dateComparison = String(a.bookingDate || "").localeCompare(String(b.bookingDate || ""));

      if (dateComparison !== 0) return dateComparison;

      return Math.min(...(a.slots || [Infinity])) - Math.min(...(b.slots || [Infinity]));
    })
    .map((booking) => {
      const parsedContact = parseContactInfo(booking.contactInfo);
      const statusMeta = getBookingStatusMeta(booking.status);

      return {
        id: booking.id,
        bookingDate: booking.bookingDate,
        purpose: booking.purpose || "",
        contactName: parsedContact.name || "申請人",
        participantCount: booking.pCount || 0,
        timeRange: formatSlotGroupsAsTimeRange(booking.slots),
        statusText: statusMeta.text,
        statusClass: statusMeta.className,
      };
    });
});

const renderEventContent = (arg) => {
  const wrapper = document.createElement("div");
  wrapper.className = "calendar-event-content";

  const status = document.createElement("span");
  status.className = `calendar-event-status ${arg.event.extendedProps.statusClass}`;
  status.textContent = arg.event.extendedProps.statusLabel;

  const time = document.createElement("span");
  time.className = "calendar-event-time";
  time.textContent = arg.event.extendedProps.timeLabel;

  const purpose = document.createElement("span");
  purpose.className = "calendar-event-purpose";
  purpose.textContent = arg.event.extendedProps.purposeLabel;

  wrapper.append(status, time, purpose);

  return {
    domNodes: [wrapper],
  };
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
  moreLinkContent: (arg) => renderMoreLinkContent(arg, "筆"),
  moreLinkClick: () => {},
  events,
  datesSet: async (arg) => {
    if (!selectedVenueId.value) return;
    await loadEvents(arg.view);
  },
  dateClick: (info) => {
    const dateStr = info.dateStr.split("T")[0];
    openDayModal(dateStr);
  },
  eventClick: async (info) => {
    const bookingId = info.event.extendedProps.bookingId;
    await openBookingDetail(bookingId);
  },
});

const refreshCalendarLayout = async () => {
  await nextTick();
  calendarRef.value?.getApi()?.updateSize();
};

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
    const statusMeta = getBookingStatusMeta(booking.status);

    slotGroups.forEach((group) => {
      const timeRange = convertSlotsToTimeRange(booking.bookingDate, group);

      if (!timeRange) return;

      mappedEvents.push({
        title: `${statusMeta.text}｜${booking.purpose || "未填寫用途"}`,
        start: timeRange.start,
        end: timeRange.end,
        display: "block",
        extendedProps: {
          bookingId: booking.id,
          booking,
          statusLabel: statusMeta.text,
          statusClass: statusMeta.className,
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
    const baseQuery = {
      venueId: selectedVenueId.value,
      startDate: monthRange.startDate,
      endDate: monthRange.endDate,
    };

    const allBookings = await fetchPendingReviews(baseQuery);
    const filteredBookings =
      selectedStatus.value === ""
        ? allBookings
        : await fetchPendingReviews({ ...baseQuery, status: Number(selectedStatus.value) });

    allMonthlyBookings.value = allBookings;
    monthlyBookings.value = filteredBookings;
    events.value = mapBookingsToEvents(filteredBookings);
  } catch (loadError) {
    allMonthlyBookings.value = [];
    monthlyBookings.value = [];
    events.value = [];
    error(loadError.message || "取得審核月曆失敗");
  } finally {
    isFetchingEvents.value = false;
    pageLoading.value = false;

    if (activeViewMode.value === "calendar") {
      void refreshCalendarLayout();
    }
  }
};

const reloadCurrentView = async () => {
  const view = calendarRef.value?.getApi().view;

  if (view) {
    await loadEvents(view);
  }
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

const closeTransientUi = () => {
  isDayModalVisible.value = false;
  isDetailModalVisible.value = false;
  detailLoading.value = false;
  detailProcessing.value = false;
  selectedBookingId.value = null;
  selectedBookingDetail.value = null;
  selectedDate.value = "";
  selectedDayOfWeek.value = "";
};

const handleFilterChange = async () => {
  closeTransientUi();
  await reloadCurrentView();
};

const selectStatusFilter = async (statusValue) => {
  if (selectedStatus.value === statusValue) return;

  selectedStatus.value = statusValue;
  await handleFilterChange();
};

watch(activeViewMode, (nextMode) => {
  if (nextMode === "calendar") {
    void refreshCalendarLayout();
  }
});

const navigateToVenueBooking = (dateStr) => {
  if (!selectedVenueId.value) return;

  const query = dateStr
    ? {
        create: "1",
        date: dateStr,
      }
    : {};

  router.push({
    name: "VenueCalendar",
    params: { venueId: String(selectedVenueId.value) },
    query,
  });
};

const openBookingDetail = async (bookingId) => {
  isDayModalVisible.value = false;
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
  closeTransientUi();
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
    success(`申請狀態已更新為${getBookingStatusMeta(status).text}`);
    closeDetailModal();
    await reloadCurrentView();
  } catch (updateError) {
    error(updateError.message || "更新申請狀態失敗");
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
  --review-panel: #ffffff;
  --review-line: #d7dde5;
  --review-ink: #202936;
  --review-muted: #5f6b7a;

  .workbench-header {
    margin-bottom: 1.25rem;
    padding: 1.35rem 1.45rem;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
    border: 1px solid var(--review-line);
    border-radius: var(--radius);
    background: linear-gradient(180deg, #ffffff 0%, #f6f8fb 100%);
    box-shadow: var(--shadow-soft);
  }

  .header-copy {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.45rem;

    h1,
    p {
      margin: 0;
    }

    h1 {
      color: var(--review-ink);
    }

    p:last-child {
      max-width: 42rem;
      color: var(--review-muted);
    }
  }

  .header-eyebrow,
  .panel-kicker {
    margin: 0;
    color: #5b6675;
    font-size: var(--text-sm);
    font-weight: 800;
    text-transform: uppercase;
  }

  .mode-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    min-height: 2rem;
    padding: 0.4rem 0.75rem;
    border: 1px solid rgba(32, 41, 54, 0.12);
    border-radius: 999px;
    background: #202936;
    color: #ffffff;
    font-size: var(--text-sm);
    font-weight: 800;
    line-height: 1;
  }

  .route-booking-btn {
    flex-shrink: 0;
  }

  .workbench-layout {
    display: grid;
    grid-template-columns: minmax(290px, 340px) minmax(0, 1fr);
    gap: 1.25rem;
    align-items: start;
  }

  .control-panel {
    position: sticky;
    top: calc(var(--header-height) + 1rem);
    padding: 1.15rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
    background: #f9fafb;
    border-color: var(--review-line);
  }

  .panel-section,
  .venue-summary {
    display: flex;
    flex-direction: column;
    gap: 0.45rem;
  }

  .panel-section {
    h2 {
      color: var(--review-ink);
      font-size: var(--text-xl);
    }

    label,
    .section-label {
      color: var(--review-muted);
      font-size: var(--text-sm);
      font-weight: 800;
    }

    select {
      min-height: 3rem;
      padding: 0.75rem 0.95rem;
      border: 1px solid var(--review-line);
      border-radius: var(--radius-sm);
      background: #ffffff;
      color: var(--review-ink);
    }
  }

  .status-filter-section {
    gap: 0.65rem;
  }

  .status-filter-list {
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
  }

  .status-filter-card {
    width: 100%;
    min-height: 5.2rem;
    padding: 0.8rem 0.75rem;
    border: 1px solid var(--review-line);
    border-left: 5px solid #7a8796;
    border-radius: var(--radius-sm);
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 0.75rem;
    background: #ffffff;
    color: var(--review-ink);
    text-align: left;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      border-color 0.2s ease,
      box-shadow 0.2s ease,
      transform 0.2s ease;

    &:hover,
    &:focus-visible {
      background: #f7f8fa;
      border-color: #bfc8d4;
      box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
      outline: none;
      transform: translateY(-1px);
    }

    &.is-active {
      background: #eef1f5;
      border-color: #aeb9c7;
      box-shadow: inset 0 0 0 1px rgba(32, 41, 54, 0.08);
    }

    &.is-pending {
      border-left-color: var(--status-pending);
    }

    &.is-approved {
      border-left-color: var(--status-approved);
    }

    &.is-rejected {
      border-left-color: var(--status-rejected);
    }
  }

  .status-filter-icon {
    width: 2.4rem;
    height: 2.4rem;
    border-radius: 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #eef1f5;
    color: var(--review-ink);
  }

  .status-filter-card.is-active .status-filter-icon {
    background: #ffffff;
  }

  .status-filter-copy {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.18rem;

    strong {
      color: var(--review-ink);
      font-size: var(--text-base);
      line-height: 1.3;
    }

    span {
      color: var(--review-muted);
      font-size: var(--text-sm);
      line-height: 1.35;
    }
  }

  .status-filter-count {
    min-width: 2.3rem;
    height: 2.3rem;
    padding: 0 0.55rem;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #d9e2ee;
    color: var(--review-ink);
    font-size: var(--text-lg);
    font-weight: 800;
  }

  .venue-summary {
    padding: 0.9rem;
    border: 1px solid var(--review-line);
    border-radius: var(--radius-sm);
    background: #ffffff;

    strong {
      color: var(--review-ink);
      font-size: var(--text-lg);
      line-height: 1.3;
    }
  }

  .summary-label,
  .summary-subtle {
    color: var(--review-muted);
    font-size: var(--text-sm);
  }

  .booking-entry-btn {
    width: 100%;
  }

  .calendar-panel {
    min-width: 0;
  }

  .panel-heading {
    margin-bottom: 0.8rem;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;

    h2 {
      margin: 0;
      color: var(--review-ink);
    }
  }

  .panel-heading-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.75rem;
  }

  .view-toggle {
    min-height: 2.5rem;
    padding: 0.2rem;
    display: inline-grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.2rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    background: #eef1f5;
  }

  .view-toggle-btn {
    min-width: 5rem;
    min-height: 2.05rem;
    padding: 0.35rem 0.75rem;
    border: 0;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;
    background: transparent;
    color: var(--review-muted);
    font-size: var(--text-sm);
    font-weight: 800;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      color 0.2s ease,
      box-shadow 0.2s ease;

    &.is-active {
      background: #ffffff;
      color: var(--review-ink);
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
    }
  }

  .quick-status-filter {
    flex-shrink: 0;
    min-height: 2.7rem;
    padding: 0.25rem 0.35rem 0.25rem 0.8rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    background: #eef1f5;
    color: var(--review-muted);
    font-size: var(--text-sm);
    font-weight: 800;

    select {
      min-height: 2.05rem;
      padding: 0.25rem 2rem 0.25rem 0.7rem;
      border: 0;
      border-radius: 999px;
      background: #ffffff;
      color: var(--review-ink);
      font-size: var(--text-base);
      font-weight: 800;
      cursor: pointer;

      &:disabled {
        cursor: progress;
      }
    }
  }

  .calendar-shell,
  .list-shell {
    background: var(--review-panel);
    padding: 1.2rem;
    border-radius: var(--radius);
    border: 1px solid var(--review-line);
    box-shadow: var(--shadow-soft);
    overflow-x: auto;
    transition: opacity 0.3s ease;

    &.is-loading {
      opacity: 0.55;
      pointer-events: none;
    }
  }

  .list-shell {
    overflow: hidden;
  }

  .list-empty-state {
    min-height: 18rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed var(--review-line);
    border-radius: var(--radius-sm);
    background: #f7f8fa;
    color: var(--review-muted);
    font-weight: 700;
  }

  .case-list {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--review-line);
    border-radius: var(--radius-sm);
    overflow: hidden;
    background: #ffffff;
  }

  .case-row {
    width: 100%;
    padding: 1rem 1.1rem;
    border: 0;
    border-bottom: 1px solid var(--review-line);
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(12rem, auto);
    gap: 1rem;
    align-items: center;
    background: #ffffff;
    color: inherit;
    text-align: left;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      box-shadow 0.2s ease;

    &:last-child {
      border-bottom: 0;
    }

    &:hover,
    &:focus-visible {
      background: #f7f8fa;
      box-shadow: inset 4px 0 0 #202936;
      outline: none;
    }
  }

  .case-main,
  .case-schedule {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.45rem;
  }

  .case-title-line {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    min-width: 0;

    strong {
      min-width: 0;
      color: var(--review-ink);
      font-size: var(--text-base);
      overflow-wrap: anywhere;
    }
  }

  .case-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.45rem 0.8rem;
    color: var(--review-muted);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .case-schedule {
    align-items: flex-end;

    strong {
      color: var(--review-ink);
    }

    span {
      color: var(--review-muted);
      font-size: var(--text-sm);
      font-weight: 700;
      text-align: right;
    }
  }

  .status-badge {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 1.75rem;
    padding: 0.28rem 0.65rem;
    border-radius: 999px;
    color: #ffffff;
    font-size: var(--text-sm);
    font-weight: 800;
    line-height: 1;

    &.is-pending {
      background: var(--status-pending);
      color: #2d3436;
    }

    &.is-approved {
      background: var(--status-approved);
    }

    &.is-rejected {
      background: var(--status-rejected);
    }

    &.is-withdrawn {
      background: var(--status-occupied);
    }
  }

  :deep(.fc) {
    min-width: 760px;

    .fc-toolbar.fc-header-toolbar {
      justify-content: center;
      margin-bottom: 1.25rem;
    }

    .fc-toolbar-chunk {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .fc-toolbar-title {
      color: var(--review-ink);
      font-size: 1.35rem;
      font-weight: 800;
    }

    .fc-button-primary {
      font-size: 0.8rem;
      color: var(--review-ink);
      background-color: #eef1f5;
      border: 1px solid var(--review-line);
      border-radius: 999px;

      &:hover,
      &:focus {
        background-color: #202936;
        color: #ffffff;
      }
    }

    .fc-scrollgrid,
    .fc-theme-standard td,
    .fc-theme-standard th {
      border-color: var(--review-line);
    }

    .fc-col-header-cell {
      background: #f4f6f8;
      color: #5b6675;
      font-size: var(--text-sm);
    }

    .fc-daygrid-day {
      cursor: pointer;
      background: #ffffff;
      transition: background-color 0.2s ease;

      &:hover {
        background-color: #f7f8fa;
      }
    }

    .fc-event {
      cursor: pointer;
      border-radius: 8px;
      padding: 4px 5px;
      font-size: 0.78rem;
      transition:
        filter 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease;

      &:hover {
        filter: brightness(0.98);
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.14);
        transform: translateY(-1px);
      }
    }

    .fc-daygrid-more-link {
      display: inline-flex;
      align-items: center;
      margin-top: 0.35rem;
      color: var(--review-ink);
      cursor: default;
      font-size: 0.95rem;
      font-weight: 800;
      text-decoration: none;
      pointer-events: none;
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
      color: var(--review-ink);
      font-size: 1.05rem;
      font-weight: 800;
      line-height: 1;
      text-align: left;
    }

    .calendar-day-count {
      position: absolute;
      top: 50%;
      right: 0.35rem;
      transform: translateY(-50%);
      min-width: 1.55rem;
      height: 1.55rem;
      padding: 0 0.45rem;
      border-radius: 999px;
      background-color: #202936;
      color: #ffffff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: 800;
      line-height: 1;
      z-index: 0;
    }

    .calendar-event-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
      line-height: 1.2;
    }

    .calendar-event-status {
      width: max-content;
      max-width: 100%;
      padding: 0.16rem 0.36rem;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.78);
      color: #17202c;
      font-size: 0.7rem;
      font-weight: 800;
    }

    .calendar-event-time {
      font-weight: 800;
    }

    .calendar-event-purpose,
    .calendar-event-time {
      display: block;
      white-space: normal;
      word-break: break-word;
    }
  }

  @media (max-width: 1024px) {
    .workbench-layout {
      grid-template-columns: 1fr;
    }

    .control-panel {
      position: static;
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      align-items: end;
    }

    .panel-section:first-child,
    .status-filter-section {
      grid-column: 1 / -1;
    }
  }

  @media (max-width: 760px) {
    .workbench-header,
    .panel-heading {
      align-items: stretch;
      flex-direction: column;
    }

    .panel-heading-actions {
      align-items: stretch;
      flex-direction: column;
    }

    .view-toggle {
      width: 100%;
    }

    .quick-status-filter {
      width: 100%;

      select {
        flex: 1;
      }
    }

    .control-panel {
      grid-template-columns: 1fr;
    }

    .calendar-shell,
    .list-shell {
      padding: 0.75rem;
      border-radius: var(--radius-sm);
    }

    .case-row {
      grid-template-columns: 1fr;
    }

    .case-schedule {
      align-items: flex-start;

      span {
        text-align: left;
      }
    }
  }
}
</style>
