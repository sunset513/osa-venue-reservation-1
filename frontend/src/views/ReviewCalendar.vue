<template>
  <div class="review-page page-enter">
    <header class="workbench-header">
      <div class="header-copy">
        <span class="mode-pill">
          <ShieldCheck :size="16" aria-hidden="true" />
          審核者模式
        </span>
        <p class="header-eyebrow">Review Workbench</p>
        <h1 class="page-title">
          <ClipboardCheck :size="28" aria-hidden="true" class="page-title-icon" />
          <span>審核工作台</span>
        </h1>
        <p>以預約申請為中心處理場地借用，檢視狀態、比對時段，並從同一處完成通過或退回。</p>
      </div>

      <div class="header-actions">
        <div class="review-mode-toggle" role="group" aria-label="切換審核類型">
          <button
            class="view-toggle-btn badge-toggle-btn"
            :class="{ 'is-active': activeReviewMode === 'venue' }"
            type="button"
            @click="activeReviewMode = 'venue'"
          >
            <Building2 :size="16" aria-hidden="true" />
            <span>場地預約</span>
            <span v-if="venuePendingCount > 0" class="pending-badge">
              {{ venuePendingCount }}
            </span>
          </button>
          <button
            class="view-toggle-btn badge-toggle-btn"
            :class="{ 'is-active': activeReviewMode === 'equipment' }"
            type="button"
            @click="activeReviewMode = 'equipment'"
          >
            <Wrench :size="16" aria-hidden="true" />
            <span>設備借用</span>
            <span
              v-if="equipmentPendingCount > 0"
              class="pending-badge pending-badge--dot"
              aria-hidden="true"
            ></span>
          </button>
        </div>

        <button
          v-if="isReviewer"
          class="btn admin-equipment-btn"
          type="button"
          @click="navigateToEquipmentStatus"
        >
          <Wrench :size="17" aria-hidden="true" />
          <span>設備狀態管理</span>
        </button>

        <button
          class="btn btn-secondary route-booking-btn"
          type="button"
          :disabled="!canNavigateToVenueBooking"
          @click="navigateToVenueBooking()"
        >
          <ArrowRight :size="17" aria-hidden="true" />
          <span>{{ bookingRouteLabel }}</span>
        </button>
      </div>
    </header>

    <div v-if="pageLoading" class="loading-state">載入場地與審核資料中...</div>

    <div v-else-if="activeReviewMode === 'venue'" class="workbench-layout">
      <aside class="control-panel card">
        <section class="panel-section">
          <label for="review-venue">審核場地</label>
          <select id="review-venue" v-model="selectedVenueId" @change="handleFilterChange">
            <option :value="ALL_VENUES_VALUE">全部場地</option>
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
                <option value="1">待審核</option>
                <option value="2">已通過</option>
                <option value="3">已拒絕</option>
              </select>
            </label>
          </div>
        </div>

        <div
          v-show="activeViewMode === 'calendar'"
          class="calendar-shell"
          :class="{ 'is-loading': isFetchingEvents }"
          @keydown.esc="closeMonthPicker"
        >
          <div v-if="isMonthPickerOpen" class="month-picker-popover" role="dialog" aria-label="選擇月份">
            <label for="review-month-picker">選擇月份</label>
            <input
              id="review-month-picker"
              ref="monthPickerRef"
              v-model="monthPickerValue"
              type="month"
              @keyup.enter="goToSelectedMonth"
            />
            <button class="month-picker-action is-primary" type="button" @click="goToSelectedMonth">
              套用
            </button>
            <button class="month-picker-action" type="button" @click="closeMonthPicker">
              取消
            </button>
          </div>
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
                  <span class="status-pill" :class="booking.statusClass">{{ booking.statusText }}</span>
                  <strong>{{ booking.purpose || "未填寫用途" }}</strong>
                </div>
                <div class="case-meta">
                  <span class="case-id-pill">場地預約編號 #{{ booking.id }}</span>
                  <span>{{ booking.venueName }}</span>
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

    <div v-else class="workbench-layout">
      <aside class="control-panel card">
        <section class="panel-section status-filter-section equipment-status-filter-section" aria-label="設備申請狀態篩選">
          <span class="section-label">申請狀態</span>
          <div class="status-filter-list">
            <button
              v-for="option in equipmentStatusFilterOptions"
              :key="option.key"
              class="status-filter-card"
              :class="[option.className, { 'is-active': equipmentSelectedStatus === option.statusValue }]"
              type="button"
              :aria-pressed="equipmentSelectedStatus === option.statusValue"
              @click="selectEquipmentStatusFilter(option.statusValue)"
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

      <section class="calendar-panel standalone-equipment-panel card">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">設備借用</p>
            <h2>設備審核清單</h2>
            <p class="panel-note">通過關聯場地預約申請時，系統也會一併通過該筆設備借用申請。</p>
          </div>
          <button class="btn btn-secondary" type="button" @click="loadEquipmentReviews">
            <span class="btn-icon">
              <RefreshCw :size="16" aria-hidden="true" />
            </span>
            <span>重新整理</span>
          </button>
        </div>

        <div v-if="equipmentReviewLoading" class="loading-state">載入設備申請中...</div>
        <div v-else-if="filteredEquipmentReviewItems.length === 0" class="list-empty-state">
          目前沒有設備借用申請。
        </div>
        <div v-else class="case-list">
          <button
            v-for="equipmentBooking in filteredEquipmentReviewItems"
            :key="equipmentBooking.id"
            class="case-row"
            type="button"
            @click="openEquipmentReviewTarget(equipmentBooking)"
          >
            <div class="case-main">
              <div class="case-title-line">
                <span class="status-pill" :class="getReviewEquipmentStatusMeta(equipmentBooking.status).className">
                  {{ getReviewEquipmentStatusMeta(equipmentBooking.status).text }}
                </span>
                <strong>{{ equipmentBooking.itemSummary }}</strong>
              </div>
              <div class="case-meta">
                <span class="case-type-pill" :class="getEquipmentBookingTypeMeta(equipmentBooking).className">
                  {{ getEquipmentBookingTypeMeta(equipmentBooking).text }}
                </span>
                <span class="case-id-pill">設備借用編號 #{{ equipmentBooking.id }}</span>
                <template v-if="equipmentBooking.relatedVenueBookingId">
                  <span class="case-id-pill">場地預約編號 #{{ equipmentBooking.relatedVenueBookingId }}</span>
                  <strong class="case-meta-strong">{{ equipmentBooking.relatedVenueBookingTitle || "未填寫用途" }}</strong>
                  <span>{{ equipmentBooking.relatedVenueName || "未提供場地" }}</span>
                </template>
                <span v-else>單獨借用設備</span>
                <span>{{ equipmentBooking.contact.name || equipmentBooking.userId || "未提供申請人" }}</span>
              </div>
            </div>
            <div class="case-schedule">
              <strong>{{ formatEquipmentBorrowDateMeta(equipmentBooking.borrowDate) }}</strong>
              <span>{{ equipmentBooking.timeRange || "未提供時段" }}</span>
            </div>
          </button>
        </div>
      </section>
    </div>
  </div>

  <ReviewDayScheduleModal
    :visible="isDayModalVisible"
    :selectedDate="selectedDate"
    :dayOfWeek="selectedDayOfWeek"
    :bookings="selectedDayBookings"
    :can-create-booking="canNavigateToVenueBooking"
    @close="closeDayModal"
    @open-detail="openBookingDetail"
    @create-booking="navigateToVenueBooking(selectedDate)"
  />

  <ReviewBookingModal
    :visible="isDetailModalVisible"
    :booking="selectedBookingDetail"
    :loading="detailLoading"
    :processing="detailProcessing"
    :equipment-bookings="selectedEquipmentBookings"
    :equipment-loading="equipmentDetailLoading"
    @close="closeDetailModal"
    @approve="handleApprove"
    @update-status="handleStatusUpdate"
  />

  <ReviewEquipmentModal
    :visible="isEquipmentDetailModalVisible"
    :booking="selectedEquipmentBookingDetail"
    :processing="equipmentProcessingId !== null"
    @close="closeEquipmentDetailModal"
    @update-status="handleEquipmentDetailStatusUpdate"
  />
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import FullCalendar from "@fullcalendar/vue3";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import {
  ArrowRight,
  BadgeCheck,
  Building2,
  CalendarDays,
  Check,
  ClipboardCheck,
  ClipboardList,
  Clock3,
  List,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  Wrench,
  XCircle,
} from "lucide-vue-next";

import ReviewBookingModal from "@/components/review/ReviewBookingModal.vue";
import ReviewDayScheduleModal from "@/components/review/ReviewDayScheduleModal.vue";
import ReviewEquipmentModal from "@/components/review/ReviewEquipmentModal.vue";
import { fetchVenuesByUnit } from "@/api/venue";
import {
  approveReviewBooking,
  fetchPendingReviews,
  fetchReviewBookingDetail,
  updateReviewBookingStatus,
} from "@/api/review";
import {
  getEquipmentReviewsByVenueBooking,
  queryEquipmentReviews,
  updateEquipmentReviewStatus,
} from "@/api/equipment";
import {
  convertSlotsToTimeRange,
  formatSlotsAsTimeRange,
  formatSlotGroupsAsTimeRange,
  getReviewEventColorConfig,
  groupContiguousSlots,
} from "@/utils/dateHelper";
import { formatDateKey, getDailyEventCount, renderMoreLinkContent } from "@/utils/calendarDisplay";
import { getBookingStatusMeta, parseContactInfo } from "@/utils/bookingMeta";
import {
  getEquipmentBookingStatusMeta,
  getEquipmentReviewOpenTarget,
  normalizeEquipmentBooking,
  normalizeEquipmentBookingPage,
} from "@/utils/equipment";
import { useAuthSessionStore } from "@/stores/authSession";
import { useToast } from "@/utils/useToast";

const { success, error } = useToast();
const router = useRouter();
const authSession = useAuthSessionStore();
const ALL_VENUES_VALUE = "all";
const REVIEW_UNIT_ID = "1";

const calendarRef = ref(null);
const venues = ref([]);
const selectedVenueId = ref(ALL_VENUES_VALUE);
const selectedStatus = ref("1");
const pageLoading = ref(true);
const isFetchingEvents = ref(false);
const activeViewMode = ref("list");
const activeReviewMode = ref("venue");
const equipmentSelectedStatus = ref("1");
const isMonthPickerOpen = ref(false);
const monthPickerValue = ref("");
const monthPickerRef = ref(null);
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
const selectedEquipmentBookings = ref([]);
const equipmentDetailLoading = ref(false);
const equipmentProcessingId = ref(null);
const equipmentPendingCount = ref(0);
const equipmentReviewLoading = ref(false);
const equipmentReviewPage = ref(normalizeEquipmentBookingPage());
const isEquipmentDetailModalVisible = ref(false);
const selectedEquipmentBookingDetail = ref(null);

const isReviewer = computed(() => authSession.isReviewer);
const isAllVenuesSelected = computed(() => selectedVenueId.value === ALL_VENUES_VALUE);
const canNavigateToVenueBooking = computed(() => {
  if (isAllVenuesSelected.value) return venues.value.length > 0;

  return Boolean(selectedVenueId.value);
});

const selectedVenueName = computed(() => {
  if (isAllVenuesSelected.value) return "全部場地";

  return (
    venues.value.find((venue) => String(venue.id) === String(selectedVenueId.value))?.name ||
    "未選擇場地"
  );
});

const bookingRouteLabel = computed(() => {
  if (isAllVenuesSelected.value) return "前往場地選擇頁";

  return `前往「${selectedVenueName.value}」預約`;
});

let calendarTitleElement = null;

const formatEquipmentBorrowDateMeta = (value) => {
  if (!value) return "未提供日期";

  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(date);
};

const formatMonthPickerValue = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return "";

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");

  return `${year}-${month}`;
};

const getCalendarMonthPickerValue = () => {
  const currentStart = calendarRef.value?.getApi()?.view?.currentStart;

  return formatMonthPickerValue(currentStart ? new Date(currentStart) : new Date());
};

const closeMonthPicker = () => {
  isMonthPickerOpen.value = false;
};

const handleMonthPickerOutsideClick = (event) => {
  if (!isMonthPickerOpen.value) return;

  const target = event.target;
  const pickerElement = monthPickerRef.value?.closest(".month-picker-popover");

  if (pickerElement?.contains(target) || calendarTitleElement?.contains(target)) return;

  closeMonthPicker();
};

const openMonthPicker = async () => {
  monthPickerValue.value = getCalendarMonthPickerValue();
  isMonthPickerOpen.value = true;

  await nextTick();
  monthPickerRef.value?.focus();
};

const goToSelectedMonth = () => {
  if (!monthPickerValue.value) return;

  const targetDate = new Date(`${monthPickerValue.value}-01T00:00:00`);

  if (Number.isNaN(targetDate.getTime())) return;

  closeTransientUi();
  calendarRef.value?.getApi()?.gotoDate(targetDate);
  closeMonthPicker();
};

const clearCalendarTitleInteraction = () => {
  if (!calendarTitleElement) return;

  calendarTitleElement.onclick = null;
  calendarTitleElement.onkeydown = null;
  calendarTitleElement = null;
};

const enhanceCalendarTitleInteraction = async () => {
  await nextTick();

  const titleElement = calendarRef.value?.$el?.querySelector(".fc-toolbar-title");

  if (!titleElement || titleElement === calendarTitleElement) return;

  clearCalendarTitleInteraction();

  titleElement.setAttribute("role", "button");
  titleElement.setAttribute("tabindex", "0");
  titleElement.setAttribute("title", "選擇月份");
  titleElement.classList.add("is-month-picker-trigger");
  titleElement.onclick = openMonthPicker;
  titleElement.onkeydown = (event) => {
    if (event.key !== "Enter" && event.key !== " ") return;

    event.preventDefault();
    void openMonthPicker();
  };

  calendarTitleElement = titleElement;
};

const getReviewStatusText = (status) => {
  const statusMeta = getBookingStatusMeta(status);
  return statusMeta.text === "審核中" ? "待審核" : statusMeta.text;
};

const selectedStatusLabel = computed(() => {
  return selectedStatus.value === ""
    ? "顯示全部申請"
    : `目前篩選：${getReviewStatusText(Number(selectedStatus.value))}`;
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

const venuePendingCount = computed(() => statusCounts.value.pending);

const statusFilterOptions = computed(() => [
  {
    key: "all",
    label: "全部申請",
    helper: "目前畫面所有申請",
    value: statusCounts.value.all,
    className: "is-all",
    icon: ClipboardList,
    statusValue: "",
  },
  {
    key: "pending",
    label: "待審核",
    helper: "等待處理的申請",
    value: statusCounts.value.pending,
    className: "is-pending",
    icon: Clock3,
    statusValue: "1",
  },
  {
    key: "approved",
    label: "已通過",
    helper: "已通過申請",
    value: statusCounts.value.approved,
    className: "is-approved",
    icon: BadgeCheck,
    statusValue: "2",
  },
  {
    key: "rejected",
    label: "已拒絕",
    helper: "未通過申請",
    value: statusCounts.value.rejected,
    className: "is-rejected",
    icon: XCircle,
    statusValue: "3",
  },
]);

const equipmentStatusCounts = computed(() => {
  return equipmentReviewPage.value.items.reduce(
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

const equipmentStatusFilterOptions = computed(() => [
  {
    key: "all",
    label: "全部申請",
    helper: "目前畫面所有設備申請",
    value: equipmentStatusCounts.value.all,
    className: "is-all",
    icon: ClipboardList,
    statusValue: "",
  },
  {
    key: "pending",
    label: "待審核",
    helper: "等待處理的設備申請",
    value: equipmentStatusCounts.value.pending,
    className: "is-pending",
    icon: Clock3,
    statusValue: "1",
  },
  {
    key: "approved",
    label: "已通過",
    helper: "已通過的設備申請",
    value: equipmentStatusCounts.value.approved,
    className: "is-approved",
    icon: BadgeCheck,
    statusValue: "2",
  },
  {
    key: "rejected",
    label: "已拒絕",
    helper: "未通過的設備申請",
    value: equipmentStatusCounts.value.rejected,
    className: "is-rejected",
    icon: XCircle,
    statusValue: "3",
  },
]);

const getReviewEquipmentStatusMeta = (status) => {
  const meta = getEquipmentBookingStatusMeta(status);
  return {
    ...meta,
    text: meta.text === "審核中" ? "待審核" : meta.text,
  };
};

const getEquipmentBookingTypeMeta = (equipmentBooking) => {
  if (equipmentBooking?.relatedVenueBookingId) {
    return {
      text: "關聯場地",
      className: "is-venue-linked",
    };
  }

  return {
    text: "單獨借用設備",
    className: "is-standalone",
  };
};

const filteredEquipmentReviewItems = computed(() => {
  return equipmentSelectedStatus.value === ""
    ? equipmentReviewPage.value.items
    : equipmentReviewPage.value.items.filter((booking) => booking.status === Number(equipmentSelectedStatus.value));
});

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
        statusText: getReviewStatusText(booking.status),
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
        venueName: booking.venueName || selectedVenueName.value || "未提供場地",
        purpose: booking.purpose || "",
        contactName: parsedContact.name || "申請人",
        participantCount: booking.pCount || 0,
        timeRange: formatSlotGroupsAsTimeRange(booking.slots),
        statusText: getReviewStatusText(booking.status),
        statusClass: statusMeta.className,
      };
    });
});

const getEquipmentReviewActions = (equipmentBooking) => {
  // Reviewers can now correct an equipment decision after approval or rejection.
  // The UI exposes only review-owned states here; user withdrawal remains a
  // borrower-side action and is intentionally not offered from this workbench.
  switch (equipmentBooking?.status) {
    case 1:
      return [
        { key: "reject", label: "拒絕申請", icon: XCircle, buttonClass: "btn-danger", status: 3 },
        { key: "approve", label: "通過申請", icon: Check, buttonClass: "btn-primary", status: 2 },
      ];
    case 2:
      return [
        { key: "reject-approved", label: "改為拒絕", icon: XCircle, buttonClass: "btn-danger", status: 3 },
      ];
    case 3:
      return [
        { key: "pending-rejected", label: "改為待審核", icon: RotateCcw, buttonClass: "btn-secondary-alt", status: 1 },
        { key: "approve-rejected", label: "改為通過", icon: Check, buttonClass: "btn-primary", status: 2 },
      ];
    default:
      return [];
  }
};

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
  firstDay: 0,
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
  await enhanceCalendarTitleInteraction();
};

const getVisibleDateRangeFromView = (view) => {
  const startDate = view.activeStart || view.currentStart;
  const endDate = view.activeEnd || view.currentEnd;

  if (!startDate || !endDate) {
    const fallbackStart = new Date(view.currentStart);
    const fallbackEnd = new Date(fallbackStart);
    fallbackEnd.setMonth(fallbackEnd.getMonth() + 1);
    fallbackEnd.setDate(fallbackEnd.getDate() - 1);

    return {
      startDate: formatDateKey(fallbackStart),
      endDate: formatDateKey(fallbackEnd),
    };
  }

  const endInclusive = new Date(endDate);
  endInclusive.setDate(endInclusive.getDate() - 1);

  return {
    startDate: formatDateKey(startDate),
    endDate: formatDateKey(endInclusive),
  };
};

const mapBookingsToEvents = (bookings) => {
  const mappedEvents = [];

  bookings.forEach((booking) => {
    const slotGroups = groupContiguousSlots(booking.slots);
    const statusMeta = getBookingStatusMeta(booking.status);
    const purposeLabel = booking.purpose?.trim() || "未填寫用途";
    const displayPurpose = isAllVenuesSelected.value
      ? `${booking.venueName || "未提供場地"}｜${purposeLabel}`
      : purposeLabel;

    slotGroups.forEach((group) => {
      const timeRange = convertSlotsToTimeRange(booking.bookingDate, group);

      if (!timeRange) return;

      mappedEvents.push({
        title: `${getReviewStatusText(booking.status)}｜${booking.purpose || "未填寫用途"}`,
        start: timeRange.start,
        end: timeRange.end,
        display: "block",
        extendedProps: {
          bookingId: booking.id,
          booking,
          statusLabel: getReviewStatusText(booking.status),
          statusClass: statusMeta.className,
          timeLabel: formatSlotsAsTimeRange(group),
          purposeLabel: displayPurpose,
          fullTimeLabel: formatSlotGroupsAsTimeRange(booking.slots),
        },
        ...getReviewEventColorConfig(booking.status),
      });
    });
  });

  return mappedEvents;
};

const fetchReviewsForVenue = (venueId, query) => {
  return fetchPendingReviews({
    ...query,
    venueId,
  });
};

const fetchReviewsForSelectedVenue = async (query) => {
  if (!isAllVenuesSelected.value) {
    return fetchReviewsForVenue(selectedVenueId.value, query);
  }

  const bookingsByVenue = await Promise.all(
    venues.value.map((venue) => fetchReviewsForVenue(venue.id, query)),
  );
  const seenBookingIds = new Set();

  return bookingsByVenue.flat().filter((booking) => {
    if (seenBookingIds.has(booking.id)) return false;

    seenBookingIds.add(booking.id);
    return true;
  });
};

const loadEvents = async (view) => {
  isFetchingEvents.value = true;

  try {
    const visibleDateRange = getVisibleDateRangeFromView(view);
    const baseQuery = {
      startDate: visibleDateRange.startDate,
      endDate: visibleDateRange.endDate,
    };

    const allBookings = await fetchReviewsForSelectedVenue(baseQuery);
    const filteredBookings =
      selectedStatus.value === ""
        ? allBookings
        : allBookings.filter((booking) => booking.status === Number(selectedStatus.value));

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

const enrichEquipmentReviewItemsWithVenueBookingName = async (bookings) => {
  const relatedVenueBookingIds = [...new Set(
    bookings.map((booking) => booking.relatedVenueBookingId).filter(Boolean),
  )];

  if (relatedVenueBookingIds.length === 0) return bookings;

  const bookingTitles = Object.fromEntries(
    await Promise.all(
      relatedVenueBookingIds.map(async (bookingId) => {
        try {
          const bookingDetail = await fetchReviewBookingDetail(bookingId);
          return [bookingId, bookingDetail?.purpose?.trim() || "未填寫用途"];
        } catch (loadError) {
          console.error(`取得場地預約 ${bookingId} 名稱失敗:`, loadError);
          return [bookingId, null];
        }
      }),
    ),
  );

  return bookings.map((booking) => {
    if (!booking.relatedVenueBookingId) return booking;

    const bookingTitle = bookingTitles[booking.relatedVenueBookingId] || null;

    return {
      ...booking,
      relatedVenueBookingTitle: bookingTitle,
    };
  });
};

const loadEquipmentPendingCount = async () => {
  try {
    const pendingPage = await queryEquipmentReviews({
      statusList: [1],
      pageNo: 1,
      pageSize: 1,
    });
    equipmentPendingCount.value = Number(pendingPage?.total) || 0;
  } catch (countError) {
    console.error("取得設備待審數失敗:", countError);
    equipmentPendingCount.value = 0;
  }
};

const loadEquipmentReviews = async () => {
  equipmentReviewLoading.value = true;

  try {
    // The equipment tab is a reviewer-facing list for every equipment request,
    // including standalone requests and requests linked to venue bookings.
    const equipmentReviewPageData = normalizeEquipmentBookingPage(
      await queryEquipmentReviews({
        pageNo: 1,
        pageSize: 100,
      }),
    );
    const enrichedItems = await enrichEquipmentReviewItemsWithVenueBookingName(equipmentReviewPageData.items);
    equipmentReviewPage.value = {
      ...equipmentReviewPageData,
      items: enrichedItems,
      data: enrichedItems,
    };
    await loadEquipmentPendingCount();
  } catch (standaloneError) {
    equipmentReviewPage.value = normalizeEquipmentBookingPage();
    error(standaloneError.message || "取得設備申請失敗");
  } finally {
    equipmentReviewLoading.value = false;
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
  equipmentDetailLoading.value = false;
  selectedBookingId.value = null;
  selectedBookingDetail.value = null;
  selectedEquipmentBookings.value = [];
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

const selectEquipmentStatusFilter = (statusValue) => {
  if (equipmentSelectedStatus.value === statusValue) return;

  equipmentSelectedStatus.value = statusValue;
};

watch(activeViewMode, (nextMode) => {
  if (nextMode === "calendar") {
    void refreshCalendarLayout();
  } else {
    closeMonthPicker();
  }
});

watch(activeReviewMode, (nextMode) => {
  // The two review modes have different data sources. Loading the equipment
  // review list lazily keeps the venue calendar path unchanged for reviewers
  // who only need to handle normal venue bookings.
  if (nextMode === "equipment") {
    closeTransientUi();
    void loadEquipmentReviews();
  } else {
    void reloadCurrentView();
    void loadEquipmentPendingCount();
  }
});

const navigateToVenueBooking = (dateStr) => {
  if (!canNavigateToVenueBooking.value) return;

  if (isAllVenuesSelected.value) {
    router.push({
      name: "VenueSelector",
      params: { unitId: REVIEW_UNIT_ID },
    });
    return;
  }

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

const navigateToEquipmentStatus = () => {
  if (!isReviewer.value) return;

  router.push({ name: "EquipmentStatus" });
};

const openBookingDetail = async (bookingId) => {
  isDayModalVisible.value = false;
  selectedBookingId.value = bookingId;
  selectedBookingDetail.value = null;
  selectedEquipmentBookings.value = [];
  detailLoading.value = true;
  equipmentDetailLoading.value = true;
  isDetailModalVisible.value = true;

  try {
    const [bookingDetail, equipmentBookings] = await Promise.all([
      fetchReviewBookingDetail(bookingId),
      getEquipmentReviewsByVenueBooking(bookingId),
    ]);
    selectedBookingDetail.value = bookingDetail;
    selectedEquipmentBookings.value = Array.isArray(equipmentBookings)
      ? equipmentBookings.map(normalizeEquipmentBooking)
      : [];
  } catch (detailError) {
    error(detailError.message || "取得申請詳情失敗");
    closeDetailModal();
  } finally {
    detailLoading.value = false;
    equipmentDetailLoading.value = false;
  }
};

const closeDetailModal = () => {
  closeTransientUi();
};

const refreshAfterVenueReviewUpdate = async () => {
  if (activeReviewMode.value === "equipment") {
    await refreshEquipmentReviewState();
    return;
  }

  await reloadCurrentView();
};

const handleApprove = async () => {
  if (!selectedBookingId.value) return;

  detailProcessing.value = true;
  let venueStatusUpdated = false;

  try {
    await approveReviewBooking(selectedBookingId.value);
    venueStatusUpdated = true;
    await syncLinkedEquipmentReviewStatus(2);
    success("申請已通過");
    closeDetailModal();
    await refreshAfterVenueReviewUpdate();
  } catch (approveError) {
    if (venueStatusUpdated) {
      error(approveError.message || "場地已通過，但關聯設備同步失敗，畫面已重新載入");
      closeDetailModal();
      await refreshAfterVenueReviewUpdate();
    } else {
      error(approveError.message || "通過申請失敗");
    }
  } finally {
    detailProcessing.value = false;
  }
};

const handleStatusUpdate = async (status) => {
  if (!selectedBookingId.value) return;

  detailProcessing.value = true;
  let venueStatusUpdated = false;

  try {
    await updateReviewBookingStatus(selectedBookingId.value, status);
    venueStatusUpdated = true;
    await syncLinkedEquipmentReviewStatus(status);
    success(`申請狀態已更新為${getReviewStatusText(status)}`);
    closeDetailModal();
    await refreshAfterVenueReviewUpdate();
  } catch (updateError) {
    if (venueStatusUpdated) {
      error(updateError.message || "場地狀態已更新，但關聯設備同步失敗，畫面已重新載入");
      closeDetailModal();
      await refreshAfterVenueReviewUpdate();
    } else {
      error(updateError.message || "更新申請狀態失敗");
    }
  } finally {
    detailProcessing.value = false;
  }
};

const isVenueEquipmentSyncStatus = (status) => {
  return [1, 2, 3].includes(Number(status));
};

const isReviewableEquipmentStatus = (status) => {
  return [1, 2, 3].includes(Number(status));
};

const syncLinkedEquipmentReviewStatus = async (status) => {
  if (!selectedBookingId.value || !isVenueEquipmentSyncStatus(status)) return;

  const equipmentBookings = await getEquipmentReviewsByVenueBooking(selectedBookingId.value);
  const normalizedBookings = Array.isArray(equipmentBookings)
    ? equipmentBookings.map(normalizeEquipmentBooking)
    : [];

  selectedEquipmentBookings.value = normalizedBookings;

  const targetStatus = Number(status);
  const syncTargets = normalizedBookings.filter((equipmentBooking) => {
    return (
      equipmentBooking.id &&
      isReviewableEquipmentStatus(equipmentBooking.status) &&
      Number(equipmentBooking.status) !== targetStatus
    );
  });

  await Promise.all(
    syncTargets.map((equipmentBooking) => updateEquipmentReviewStatus(equipmentBooking.id, targetStatus)),
  );
};

const refreshEquipmentReviewState = async () => {
  // Refresh only the data source that is currently visible. This avoids forcing
  // a full venue calendar reload when the reviewer is processing standalone
  // equipment requests, while still keeping modal equipment details current.
  await loadEquipmentPendingCount();

  if (activeReviewMode.value === "equipment") {
    await loadEquipmentReviews();
    return;
  }

  if (selectedBookingId.value && isDetailModalVisible.value) {
    const equipmentBookings = await getEquipmentReviewsByVenueBooking(selectedBookingId.value);
    selectedEquipmentBookings.value = Array.isArray(equipmentBookings)
      ? equipmentBookings.map(normalizeEquipmentBooking)
      : [];
  }
};

const openEquipmentDetail = (id) => {
  const booking = equipmentReviewPage.value.items.find((b) => b.id === id);
  if (booking) {
    selectedEquipmentBookingDetail.value = booking;
    isEquipmentDetailModalVisible.value = true;
  }
};

const openEquipmentReviewTarget = async (equipmentBooking) => {
  const target = getEquipmentReviewOpenTarget(equipmentBooking);
  if (!target) return;

  closeEquipmentDetailModal();

  if (target.type === "venue") {
    await openBookingDetail(target.id);
    return;
  }

  openEquipmentDetail(target.id);
};

const closeEquipmentDetailModal = () => {
  isEquipmentDetailModalVisible.value = false;
  selectedEquipmentBookingDetail.value = null;
};

const handleEquipmentDetailStatusUpdate = async (id, status) => {
  await handleEquipmentStatusUpdate(id, status);
  if (selectedEquipmentBookingDetail.value?.id === id) {
    const updated = equipmentReviewPage.value.items.find((b) => b.id === id);
    if (updated) {
      selectedEquipmentBookingDetail.value = updated;
    }
  }
};

const handleEquipmentStatusUpdate = async (equipmentBookingId, status) => {
  if (!equipmentBookingId) return;

  equipmentProcessingId.value = equipmentBookingId;

  try {
    await updateEquipmentReviewStatus(equipmentBookingId, status);
    success(`設備申請狀態已更新為${getEquipmentBookingStatusMeta(status).text}`);
    await refreshEquipmentReviewState();
  } catch (updateError) {
    error(updateError.message || "更新設備申請狀態失敗");
  } finally {
    equipmentProcessingId.value = null;
  }
};

onMounted(async () => {
  document.addEventListener("click", handleMonthPickerOutsideClick);

  try {
    const fetchedVenues = await fetchVenuesByUnit(1);
    venues.value = fetchedVenues;
    pageLoading.value = false;

    if (selectedVenueId.value !== ALL_VENUES_VALUE && !fetchedVenues.some((venue) => venue.id === selectedVenueId.value)) {
      selectedVenueId.value = fetchedVenues[0]?.id || null;
    }

    if (fetchedVenues.length === 0) {
      pageLoading.value = false;
      error("目前沒有可供審核的場地");
    }
    await loadEquipmentPendingCount();
  } catch (venueError) {
    error(venueError.message || "取得場地清單失敗");
    pageLoading.value = false;
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleMonthPickerOutsideClick);
  clearCalendarTitleInteraction();
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
    align-items: flex-start;
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
    flex: 1 1 auto;

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

  .header-actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0.85rem;
    margin-left: auto;
    align-self: stretch;
  }

  .page-title {
    display: inline-flex;
    align-items: center;
    gap: 0.7rem;
  }

  .page-title-icon {
    flex-shrink: 0;
    color: var(--accent);
  }

  .btn-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1rem;
    height: 1rem;
    flex-shrink: 0;
  }

  .btn-danger {
    background: var(--danger);
    color: #ffffff;
  }

  .btn-danger:hover:not(:disabled) {
    box-shadow: 0 8px 18px rgba(196, 69, 69, 0.22);
  }

  .btn-secondary-alt {
    background: #f3f6fb;
    border-color: rgba(var(--blue-900-rgb), 0.12);
    color: var(--accent);
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
    margin-top: auto;
  }

  .admin-equipment-btn {
    flex-shrink: 0;
    border: 1px solid rgba(36, 63, 107, 0.16);
    background: linear-gradient(135deg, rgba(243, 247, 252, 0.98), rgba(232, 240, 250, 0.96));
    color: #243f6b;
    box-shadow: 0 10px 22px rgba(36, 63, 107, 0.12);

    &:hover:not(:disabled) {
      border-color: rgba(36, 63, 107, 0.28);
      box-shadow: 0 12px 26px rgba(36, 63, 107, 0.18);
      transform: translateY(-1px);
    }
  }

  .review-mode-toggle {
    display: inline-flex;
    align-self: flex-start;
    gap: 0.4rem;
    padding: 0.25rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.78);
  }

  .badge-toggle-btn {
    position: relative;
  }

  .pending-badge {
    position: absolute;
    top: -0.45rem;
    right: -0.45rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 1.25rem;
    height: 1.25rem;
    padding: 0 0.3rem;
    border-radius: 999px;
    background: var(--danger);
    color: #ffffff;
    font-size: 0.72rem;
    font-weight: 900;
  }

  .pending-badge--dot {
    min-width: 1.05rem;
    height: 1.05rem;
    padding: 0;
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

  .panel-note {
    margin: 0.35rem 0 0;
    color: var(--review-muted);
    font-size: var(--text-sm);
    line-height: 1.45;
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

  .calendar-shell {
    position: relative;
  }

  .list-shell {
    overflow: hidden;
  }

  .month-picker-popover {
    position: absolute;
    top: 4.1rem;
    left: 50%;
    z-index: 20;
    transform: translateX(-50%);
    padding: 0.65rem;
    border: 1px solid var(--review-line);
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    gap: 0.45rem;
    background: #ffffff;
    box-shadow: var(--shadow);

    label {
      color: var(--review-muted);
      font-size: var(--text-sm);
      font-weight: 800;
      white-space: nowrap;
    }

    input {
      width: 9.75rem;
      min-height: 2.35rem;
      padding: 0.35rem 0.55rem;
      border: 1px solid var(--review-line);
      border-radius: 8px;
      background: #ffffff;
      color: var(--review-ink);
      font-weight: 800;
    }
  }

  .month-picker-action {
    min-height: 2.35rem;
    padding: 0.35rem 0.75rem;
    border: 1px solid var(--review-line);
    border-radius: 999px;
    background: #ffffff;
    color: var(--review-ink);
    font-size: var(--text-sm);
    font-weight: 800;
    white-space: nowrap;
    cursor: pointer;

    &:hover {
      background: var(--surface-muted);
    }

    &.is-primary {
      border-color: var(--accent);
      background: var(--accent);
      color: #ffffff;

      &:hover {
        border-color: var(--accent-hover);
        background: var(--accent-hover);
      }
    }
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

  .standalone-equipment-panel {
    padding: 1.15rem;
    background: #f9fafb;
    border-color: var(--review-line);
  }

  .equipment-status-filter-section {
    margin-bottom: 0;
  }

  .equipment-case-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    border: 0;
    background: transparent;
    overflow: visible;
  }

  .equipment-case-row {
    grid-template-columns: minmax(0, 1.8fr) minmax(10.5rem, 12rem) minmax(7.25rem, 8.5rem);
    gap: 1rem;
    align-items: start;
    padding: 1rem 1.1rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.12);
    border-radius: calc(var(--radius-sm) + 4px);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 252, 0.98));
    box-shadow: 0 12px 28px rgba(20, 35, 58, 0.06);
    cursor: default;

    &:hover,
    &:focus-visible {
      background: linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(247, 250, 252, 1));
      box-shadow: 0 16px 34px rgba(20, 35, 58, 0.08);
    }
  }

  .equipment-case-main {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas:
      "title meta"
      "facts facts"
      "purpose purpose"
      "contact contact";
    gap: 0.7rem 1rem;
  }

  .equipment-title-line {
    grid-area: title;
    align-items: flex-start;
    margin: 0;

    strong {
      font-size: 1.08rem;
      line-height: 1.35;
    }
  }

  .equipment-card-meta {
    grid-area: meta;
    justify-self: end;
    align-self: start;
  }

  .equipment-card-id {
    display: inline-flex;
    align-items: center;
    min-height: 1.9rem;
    padding: 0.2rem 0.65rem;
    border-radius: 999px;
    background: rgba(var(--blue-900-rgb), 0.06);
    color: var(--review-muted);
    font-size: var(--text-xs);
    font-weight: 800;
    white-space: nowrap;
  }

  .equipment-key-facts {
    grid-area: facts;
    display: grid;
    gap: 0.55rem;
  }

  .equipment-fact {
    min-width: 0;
    display: grid;
    grid-template-columns: 5.25rem minmax(0, 1fr);
    column-gap: 0.85rem;
    row-gap: 0.2rem;
    align-items: start;

    strong,
    .equipment-fact-value {
      color: var(--review-ink);
      font-size: 0.98rem;
      font-weight: 700;
      line-height: 1.35;
      overflow-wrap: anywhere;
    }
  }

  .equipment-fact-full {
    padding: 0;
  }

  .equipment-fact-label,
  .equipment-schedule-label {
    color: var(--review-muted);
    font-size: var(--text-xs);
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
  }

  .equipment-fact-label {
    grid-column: 1;
    padding-top: 0.18rem;
  }

  .equipment-fact > strong,
  .equipment-fact > .equipment-fact-value,
  .equipment-fact > .equipment-context-chip,
  .equipment-fact > .equipment-fact-value-stack {
    grid-column: 2;
  }

  .equipment-fact-value-stack {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.28rem;
    min-width: 0;
  }

  .equipment-context-chip {
    width: fit-content;
    min-height: 1.7rem;
    padding: 0.2rem 0.55rem;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: var(--text-xs);
    font-weight: 800;
    line-height: 1;

    &.is-linked {
      background: rgba(var(--blue-900-rgb), 0.1);
      color: var(--accent);
    }

    &.is-standalone {
      background: rgba(97, 117, 138, 0.12);
      color: #4d5e71;
    }
  }

  .equipment-fact-value-group {
    grid-column: 2;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .equipment-purpose {
    grid-area: purpose;
    margin: 0;
  }

  .equipment-contact-list {
    grid-area: contact;
    display: grid;
    gap: 0.45rem;
  }

  .equipment-contact-row {
    display: grid;
    grid-template-columns: 5.25rem minmax(0, 1fr);
    column-gap: 0.85rem;
    align-items: start;
  }

  .equipment-secondary-meta {
    display: none;
  }

  .equipment-schedule {
    min-width: 0;
    padding: 0.85rem 0.9rem;
    border: 1px solid rgba(var(--blue-900-rgb), 0.16);
    border-radius: var(--radius-sm);
    background:
      linear-gradient(135deg, rgba(39, 94, 168, 0.14), rgba(255, 255, 255, 0.96) 52%),
      linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(39, 94, 168, 0.08));
    box-shadow: 0 8px 20px rgba(39, 94, 168, 0.08);
    align-self: start;
    align-items: flex-start;
    justify-content: flex-start;
    gap: 0.5rem;
    text-align: left;

    strong {
      line-height: 1.3;
    }
  }

  .equipment-schedule-header {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 0.45rem;
    width: 100%;
    text-align: left;
  }

  .equipment-schedule-badge {
    width: fit-content;
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    min-height: 1.95rem;
    padding: 0.3rem 0.65rem;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.92);
    color: var(--accent);
    font-size: var(--text-sm);
    font-weight: 800;
    letter-spacing: 0.02em;
  }

  .equipment-schedule-date {
    display: block;
    color: var(--review-ink);
    font-size: 1.05rem;
    font-weight: 800;
    letter-spacing: 0.01em;
    width: 100%;
    text-align: left;
  }

  .equipment-time-pill {
    width: fit-content;
    max-width: 100%;
    min-height: 2rem;
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.3rem 0.7rem;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.9);
    color: var(--accent);
    font-size: var(--text-sm);
    font-weight: 800;
    line-height: 1.3;
    text-align: left;
    justify-content: flex-start;
  }

  .equipment-review-row-actions {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    gap: 0.55rem;

    .btn {
      width: 100%;
      justify-content: center;
      min-height: 2.5rem;
      padding-inline: 0.85rem;
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

  .case-meta-strong {
    color: var(--review-ink);
    font-weight: 800;
  }

  .case-id-pill {
    width: fit-content;
    min-height: 1.75rem;
    padding: 0.18rem 0.6rem;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: rgba(var(--blue-900-rgb), 0.08);
    color: var(--review-ink);
    font-size: var(--text-xs);
    font-weight: 800;
    line-height: 1.2;
    white-space: nowrap;
  }

  .case-type-pill {
    width: fit-content;
    min-height: 1.75rem;
    padding: 0.18rem 0.6rem;
    border: 1px solid transparent;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: var(--text-xs);
    font-weight: 800;
    line-height: 1.2;
    white-space: nowrap;

    &.is-standalone {
      border-color: rgba(var(--blue-900-rgb), 0.12);
      background: #ffffff;
      color: var(--review-ink);
    }

    &.is-venue-linked {
      border-color: rgba(var(--blue-900-rgb), 0.18);
      background: var(--accent-soft);
      color: var(--accent);
    }
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

      &.is-month-picker-trigger {
        padding: 0.25rem 0.55rem;
        border-radius: 999px;
        cursor: pointer;
        transition:
          background-color 0.2s ease,
          box-shadow 0.2s ease;

        &:hover,
        &:focus-visible {
          background: #eef1f5;
          box-shadow: inset 0 0 0 1px var(--review-line);
          outline: none;
        }
      }
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
      background-color: var(--accent);
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

    .equipment-case-row {
      grid-template-columns: minmax(0, 1fr) minmax(9.5rem, 11rem);
    }

    .equipment-review-row-actions {
      grid-column: 1 / -1;
      flex-direction: row;
      justify-content: flex-start;

      .btn {
        width: auto;
        min-width: 7.5rem;
      }
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

    .header-actions {
      align-items: stretch;
      margin-left: 0;
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

    .equipment-case-main {
      grid-template-columns: 1fr;
      grid-template-areas:
        "title"
        "meta"
        "facts"
        "purpose"
        "contact";
    }

    .equipment-card-meta {
      justify-self: start;
    }

    .case-schedule {
      align-items: flex-start;

      span {
        text-align: left;
      }
    }

    .equipment-schedule {
      min-width: 0;
    }

    .equipment-review-row-actions {
      justify-content: flex-start;
      flex-wrap: wrap;

      .btn {
        width: 100%;
      }
    }
  }
}

</style>
