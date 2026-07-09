<template>
  <div class="history-page page-enter">
    <div v-if="loading" class="loading-state">載入預約歷史紀錄中...</div>

    <div v-else-if="loadError" class="empty-state history-feedback">
      <h3>目前無法載入預約紀錄</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary retry-btn" @click="loadBookings">
        重新載入
      </button>
    </div>

    <div v-else-if="historyItems.length === 0" class="empty-state history-feedback">
      <h3>還沒有任何預約紀錄</h3>
      <p>你建立的預約申請會顯示在這裡，之後可以回來追蹤審核狀態。</p>
      <button type="button" class="btn btn-primary explore-btn" @click="router.push('/')">
        {{ GO_TO_UNIT_SELECTOR_LABEL }}
      </button>
    </div>

    <template v-else>
      <header class="page-header history-page-header">
        <button class="back-btn" @click="router.push('/')">
          ← {{ BACK_TO_UNIT_SELECTOR_LABEL }}
        </button>
        <p class="hero-eyebrow">Booking Archive</p>
        <h1 class="page-title">
          <History :size="28" aria-hidden="true" class="page-title-icon" />
          <span>我的預約歷史紀錄</span>
        </h1>
        <p>集中查看所有預約申請、借用時段與目前審核狀態。</p>
      </header>

      <section class="history-layout">
        <section class="history-hero card">
          <section class="filter-panel">
            <div class="filter-toolbar">
              <div class="filter-field">
                <label for="booking-keyword">關鍵字搜尋</label>
                <input
                  id="booking-keyword"
                  v-model.trim="keywordFilter"
                  type="text"
                  placeholder="搜尋場地名稱或用途"
                />
              </div>

              <div class="filter-field">
                <label for="booking-venue">場地</label>
                <select id="booking-venue" v-model="venueFilter">
                  <option value="">全部場地</option>
                  <option
                    v-for="venueName in venueOptions"
                    :key="venueName"
                    :value="venueName"
                  >
                    {{ venueName }}
                  </option>
                </select>
              </div>

              <div ref="dateRangePickerRef" class="date-range-picker">
                <label for="booking-date-range-trigger">借用日期</label>
                <button
                  id="booking-date-range-trigger"
                  type="button"
                  class="date-range-trigger"
                  :class="{ 'is-open': datePickerOpen }"
                  :aria-expanded="datePickerOpen"
                  aria-controls="booking-date-range-popover"
                  @click="toggleDatePicker"
                >
                  <span class="date-range-segment" :class="{ 'has-value': startDateFilter }">
                    <span class="date-range-label">借用日期起</span>
                    <strong>{{ formatDatePickerLabel(startDateFilter) }}</strong>
                  </span>
                  <span class="date-range-segment" :class="{ 'has-value': endDateFilter }">
                    <span class="date-range-label">借用日期迄</span>
                    <strong>{{ formatDatePickerLabel(endDateFilter) }}</strong>
                  </span>
                  <ChevronDown :size="20" class="date-range-chevron" aria-hidden="true" />
                </button>

                <button
                  v-if="startDateFilter || endDateFilter"
                  type="button"
                  class="date-range-clear"
                  aria-label="清除借用日期篩選"
                  title="清除借用日期篩選"
                  @click.stop="clearDateRange"
                >
                  清除
                </button>

                <Teleport to="body">
                  <div
                    v-if="datePickerOpen"
                    id="booking-date-range-popover"
                    ref="dateRangePopoverRef"
                    class="date-range-popover"
                  >
                    <div class="calendar-selection-footer">
                      <div class="calendar-selection-summary" aria-live="polite">
                        <span class="calendar-selection-label">已選日期</span>
                        <strong>{{ selectedCalendarRangeLabel }}</strong>
                        <span class="calendar-selection-hint">{{ selectedCalendarRangeHint }}</span>
                      </div>
                      <div class="calendar-manual-inputs">
                        <label class="calendar-manual-field">
                          <span>起始日期</span>
                          <input
                            type="date"
                            :value="startDateFilter"
                            @change="updateManualStartDate($event.target.value)"
                          />
                        </label>
                        <label class="calendar-manual-field">
                          <span>結束日期</span>
                          <input
                            type="date"
                            :value="endDateFilter"
                            @change="updateManualEndDate($event.target.value)"
                          />
                        </label>
                      </div>
                    </div>
                  </div>
                </Teleport>
              </div>

              <div class="filter-summary">
                <span class="summary-label">篩選結果</span>
                <strong>{{ nonStatusFilteredHistoryItems.length }} 筆</strong>
                <button
                  v-if="hasActiveFilters"
                  type="button"
                  class="clear-filter-btn"
                  @click="clearFilters"
                >
                  清除篩選
                </button>
              </div>
            </div>
          </section>

          <section class="summary-grid">
            <article class="summary-card card">
              <span class="summary-card-label">全部申請</span>
              <strong>{{ keywordFilteredHistoryItems.length }}</strong>
            </article>
            <article class="summary-card card">
              <span class="summary-card-label">
                <Clock3 :size="18" aria-hidden="true" />
                審核中
              </span>
              <strong>{{ statusCounts.pending }}</strong>
            </article>
            <article class="summary-card card">
              <span class="summary-card-label">
                <CheckCircle2 :size="18" aria-hidden="true" />
                已通過
              </span>
              <strong>{{ statusCounts.approved }}</strong>
            </article>
            <article class="summary-card card">
              <span class="summary-card-label">
                <Ban :size="18" aria-hidden="true" />
                已被拒絕
              </span>
              <strong>{{ statusCounts.rejected }}</strong>
            </article>
            <article class="summary-card card">
              <span class="summary-card-label">
                <RotateCcw :size="18" aria-hidden="true" />
                已撤回
              </span>
              <strong>{{ statusCounts.withdrawn }}</strong>
            </article>
          </section>
        </section>

        <section class="history-records">
          <div class="filter-tabs record-tabs" role="tablist" aria-label="申請狀態篩選">
            <button
              v-for="tab in statusTabs"
              :key="tab.value"
              type="button"
              class="filter-tab"
              :class="{ 'is-active': statusFilter === tab.value }"
              role="tab"
              :aria-selected="statusFilter === tab.value"
              @click="statusFilter = tab.value"
            >
              <component
                :is="tab.icon"
                v-if="tab.icon"
                :size="18"
                class="filter-tab-icon"
                aria-hidden="true"
              />
              {{ tab.label }}
            </button>
          </div>

          <div v-if="filteredHistoryItems.length === 0" class="empty-state history-feedback record-empty-state">
            <h3>目前沒有符合條件的預約紀錄</h3>
            <p>可以調整篩選條件，或清除篩選後查看全部預約。</p>
            <button type="button" class="btn btn-secondary retry-btn" @click="clearFilters">
              清除篩選
            </button>
          </div>

          <div v-else class="history-list">
            <article
              v-for="booking in paginatedHistoryItems"
              :key="booking.id"
              class="history-card card"
            >
              <div class="history-main">
                <div class="history-overview">
                  <div class="overview-row">
                    <div>
                      <h3>
                        <template
                          v-for="(segment, index) in getHighlightedSegments(booking.purpose)"
                          :key="`${booking.id}-purpose-${index}`"
                        >
                          <span
                            :class="{ 'keyword-highlight': segment.isMatch }"
                          >
                            {{ segment.text }}
                          </span>
                        </template>
                      </h3>
                    </div>
                    <span class="status-pill" :class="booking.statusClass">
                      <component
                        :is="booking.statusIcon"
                        :size="15"
                        class="status-pill-icon"
                        aria-hidden="true"
                      />
                      {{ booking.statusText }}
                    </span>
                  </div>

                  <div class="info-grid">
                    <div class="info-item is-time-focus">
                      <div class="time-focus-header">
                        <span class="time-focus-badge">
                          <Clock3 :size="16" aria-hidden="true" />
                          借用時段
                        </span>
                      </div>
                      <strong class="time-range-emphasis">{{ booking.timeRange }}</strong>
                      <div class="time-focus-meta">
                        <span class="time-focus-meta-label">日期</span>
                        <span class="time-focus-meta-value">{{ booking.bookingDateLabel }}</span>
                      </div>
                    </div>
                    <div class="info-item is-side-info">
                      <span class="info-label">場地</span>
                      <strong>
                        <template
                          v-for="(segment, index) in getHighlightedSegments(booking.venueName)"
                          :key="`${booking.id}-venue-${index}`"
                        >
                          <span
                            :class="{ 'keyword-highlight': segment.isMatch }"
                          >
                            {{ segment.text }}
                          </span>
                        </template>
                      </strong>
                    </div>
                    <div class="info-item is-side-info">
                      <span class="info-label">預估人數</span>
                      <strong>{{ booking.pCount }} 人</strong>
                    </div>
                  </div>
                </div>

                <div class="history-actions">
                  <button
                    v-if="booking.canEdit"
                    type="button"
                    class="btn btn-primary edit-booking-btn"
                    :disabled="editingBookingId === booking.id"
                    @click="openEditModal(booking)"
                  >
                    <Pencil :size="16" aria-hidden="true" />
                    <span>{{ editingBookingId === booking.id ? "載入中..." : "修改預約" }}</span>
                  </button>
                  <button
                    type="button"
                    class="btn btn-secondary detail-toggle"
                    :aria-label="expandedBookingId === booking.id ? '收起詳情' : '查看詳情'"
                    :title="expandedBookingId === booking.id ? '收起詳情' : '查看詳情'"
                    @click="toggleExpanded(booking.id)"
                  >
                    <component
                      :is="expandedBookingId === booking.id ? ChevronUp : ChevronDown"
                      :size="15"
                      aria-hidden="true"
                    />
                  </button>
                </div>
              </div>

              <div v-if="expandedBookingId === booking.id" class="history-details">
                <div class="detail-block">
                  <span class="detail-title">聯絡人資訊</span>
                  <p>{{ booking.contact.name || "未提供姓名" }}</p>
                  <p>{{ booking.contact.phone || "未提供電話" }}</p>
                  <p>{{ booking.contact.email || "未提供 Email" }}</p>
                </div>

                <div class="detail-block">
                  <span class="detail-title">借用設備</span>
                  <p v-if="booking.equipments.length">{{ booking.equipments.join("、") }}</p>
                  <p v-else>未借用額外設備</p>
                </div>
              </div>
            </article>
          </div>

          <nav
            v-if="paginationTotalPages > 1"
            class="pagination-bar"
            aria-label="預約紀錄分頁"
          >
            <p class="pagination-summary">
              第 {{ paginationStartIndex }} - {{ paginationEndIndex }} 筆，共 {{ filteredHistoryItems.length }} 筆
            </p>
            <div class="pagination-controls">
              <button
                type="button"
                class="pagination-btn"
                :disabled="!canGoPreviousPage"
                aria-label="上一頁"
                title="上一頁"
                @click="goToPreviousPage"
              >
                <ChevronLeft :size="17" aria-hidden="true" />
              </button>
              <button
                v-for="pageNo in visiblePageNumbers"
                :key="pageNo"
                type="button"
                class="pagination-page"
                :class="{ 'is-active': currentPage === pageNo }"
                :aria-current="currentPage === pageNo ? 'page' : undefined"
                @click="setCurrentPage(pageNo)"
              >
                {{ pageNo }}
              </button>
              <button
                type="button"
                class="pagination-btn"
                :disabled="!canGoNextPage"
                aria-label="下一頁"
                title="下一頁"
                @click="goToNextPage"
              >
                <ChevronRight :size="17" aria-hidden="true" />
              </button>
            </div>
          </nav>
        </section>
      </section>
    </template>
    <BookingModal
      v-model:visible="isModalVisible"
      :mode="modalMode"
      :initial-data="modalInitialData"
      :venue-info="selectedVenueInfo"
      :is-withdrawing="isWithdrawing"
      @success="handleModalSuccess"
      @withdraw-booking="handleWithdrawBooking"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  Ban,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Clock3,
  History,
  Pencil,
  RotateCcw,
} from "lucide-vue-next";
import { useRoute, useRouter } from "vue-router";
import BookingModal from "@/components/booking/BookingModal.vue";
import { queryMyBookings, withdrawBooking } from "@/api/booking";
import { queryMyEquipmentBookings } from "@/api/equipment";
import { fetchAllUnits, fetchVenuesByUnit } from "@/api/venue";
import { getBookingStatusMeta, parseContactInfo } from "@/utils/bookingMeta";
import { buildBookingQueryPayload, normalizeBookingPage } from "@/utils/bookingQuery";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { normalizeEquipmentBookingPage } from "@/utils/equipment";
import {
  BACK_TO_UNIT_SELECTOR_LABEL,
  GO_TO_UNIT_SELECTOR_LABEL,
} from "@/utils/navigationLabels";
import { useToast } from "@/utils/useToast.js";
import { normalizeVenueDisplayName } from "@/utils/venueLabels";

const route = useRoute();
const router = useRouter();
const { error, warning } = useToast();
const loading = ref(true);
const loadError = ref("");
const bookings = ref([]);
const expandedBookingId = ref(null);
const editingBookingId = ref(null);
const isModalVisible = ref(false);
const modalMode = ref("edit");
const modalInitialData = ref({});
const selectedVenueInfo = ref(null);
const isWithdrawing = ref(false);
const venueLookup = ref(null);
const keywordFilter = ref("");
const venueFilter = ref("");
const statusFilter = ref("");
const startDateFilter = ref("");
const endDateFilter = ref("");
const currentPage = ref(1);
const datePickerOpen = ref(false);
const dateRangePickerRef = ref(null);
const dateRangePopoverRef = ref(null);
const handledEditBookingId = ref(null);

const BOOKING_PAGE_SIZE = 10;
const WEEKDAY_LABELS = ["日", "一", "二", "三", "四", "五", "六"];

const statusTabs = [
  { value: "", label: "全部", icon: null },
  { value: "1", label: "審核中", icon: Clock3 },
  { value: "2", label: "已通過", icon: CheckCircle2 },
  { value: "3", label: "已被拒絕", icon: Ban },
  { value: "0", label: "已撤回", icon: RotateCcw },
];

const getStatusIcon = (status) => {
  switch (status) {
    case 1:
      return Clock3;
    case 2:
      return CheckCircle2;
    case 3:
      return Ban;
    default:
      return RotateCcw;
  }
};

const escapeRegExp = (value) => {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
};

const getHighlightedSegments = (value) => {
  const keyword = keywordFilter.value.trim();

  if (!keyword || !value) {
    return [{ text: value, isMatch: false }];
  }

  const pattern = new RegExp(`(${escapeRegExp(keyword)})`, "ig");
  const parts = value.split(pattern).filter(Boolean);

  if (parts.length === 0) {
    return [{ text: value, isMatch: false }];
  }

  return parts.map((part) => ({
    text: part,
    isMatch: part.toLowerCase() === keyword.toLowerCase(),
  }));
};

const formatDateLabel = (value) => {
  if (!value) return "未提供日期";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(date);
};

const formatDateTimeLabel = (value) => {
  if (!value) return "未提供申請時間";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value.replace("T", " ");
  }

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
};

const parseDateString = (value) => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || "")) return null;

  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(year, month - 1, day);

  if (
    date.getFullYear() !== year
    || date.getMonth() !== month - 1
    || date.getDate() !== day
  ) {
    return null;
  }

  return date;
};

const getBookingStartDate = (booking) => {
  const bookingDate = parseDateString(booking?.bookingDate || "");

  if (!bookingDate) return null;

  const normalizedSlots = Array.isArray(booking?.slots)
    ? booking.slots
      .map((slot) => Number(slot))
      .filter((slot) => Number.isInteger(slot) && slot >= 0 && slot <= 23)
      .sort((left, right) => left - right)
    : [];

  if (normalizedSlots.length === 0) {
    return bookingDate;
  }

  bookingDate.setHours(normalizedSlots[0], 0, 0, 0);
  return bookingDate;
};

const canEditBooking = (booking) => {
  if (Number(booking?.status) !== 1) return false;

  const bookingStartDate = getBookingStartDate(booking);
  if (!bookingStartDate) return false;

  return bookingStartDate.getTime() > Date.now();
};

const formatDatePickerLabel = (value) => {
  const date = parseDateString(value);

  if (!date) return "選擇日期";

  const dateLabel = new Intl.DateTimeFormat("zh-TW", {
    month: "numeric",
    day: "numeric",
  }).format(date);
  const weekdayLabel = WEEKDAY_LABELS[date.getDay()];

  return `${dateLabel}（${weekdayLabel}）`;
};

const selectedCalendarRangeLabel = computed(() => {
  if (!startDateFilter.value && !endDateFilter.value) {
    return "尚未選擇日期";
  }

  if (!startDateFilter.value && endDateFilter.value) {
    return `截至 ${formatDatePickerLabel(endDateFilter.value)}`;
  }

  if (startDateFilter.value && endDateFilter.value) {
    if (startDateFilter.value === endDateFilter.value) {
      return formatDatePickerLabel(startDateFilter.value);
    }

    return `${formatDatePickerLabel(startDateFilter.value)} - ${formatDatePickerLabel(endDateFilter.value)}`;
  }

  return `自 ${formatDatePickerLabel(startDateFilter.value)} 起`;
});

const selectedCalendarRangeHint = computed(() => {
  if (!startDateFilter.value && !endDateFilter.value) {
    return "請先選擇起始日期";
  }

  if (!startDateFilter.value && endDateFilter.value) {
    return "可再輸入起始日期";
  }

  if (startDateFilter.value && !endDateFilter.value) {
    return "再選擇結束日期，或再次選取同一天";
  }

  return "日期範圍已套用";
});

const updateManualStartDate = (dateString) => {
  const normalizedDate = parseDateString(dateString) ? dateString : "";
  startDateFilter.value = normalizedDate;

  if (normalizedDate && endDateFilter.value && endDateFilter.value < normalizedDate) {
    endDateFilter.value = "";
  }

};

const updateManualEndDate = (dateString) => {
  const normalizedDate = parseDateString(dateString) ? dateString : "";
  endDateFilter.value = normalizedDate;

  if (normalizedDate && startDateFilter.value && normalizedDate < startDateFilter.value) {
    startDateFilter.value = "";
  }
};

const openDatePicker = () => {
  datePickerOpen.value = true;
};

const toggleDatePicker = () => {
  if (datePickerOpen.value) {
    datePickerOpen.value = false;
    return;
  }

  openDatePicker();
};

const clearDateRange = () => {
  startDateFilter.value = "";
  endDateFilter.value = "";
};

const handleDocumentClick = (event) => {
  if (!datePickerOpen.value || !dateRangePickerRef.value) return;

  const clickedTrigger = dateRangePickerRef.value.contains(event.target);
  const clickedPopover = dateRangePopoverRef.value?.contains(event.target);

  if (!clickedTrigger && !clickedPopover) {
    datePickerOpen.value = false;
  }
};

const historyItems = computed(() => {
  return [...bookings.value]
    .sort((left, right) => {
      const rightTime = new Date(right.createdAt || right.bookingDate || 0).getTime();
      const leftTime = new Date(left.createdAt || left.bookingDate || 0).getTime();
      return rightTime - leftTime;
    })
    .map((booking) => {
      const statusMeta = getBookingStatusMeta(booking.status);

      return {
        ...booking,
        venueName: normalizeVenueDisplayName(booking.venueName) || "未提供場地名稱",
        purpose: booking.purpose || "未填寫用途",
        pCount: booking.pCount || 0,
        timeRange: formatSlotGroupsAsTimeRange(booking.slots) || "未提供時段",
        bookingDateLabel: formatDateLabel(booking.bookingDate),
        createdAtLabel: formatDateTimeLabel(booking.createdAt),
        statusText: statusMeta.text,
        statusClass: statusMeta.className,
        statusIcon: getStatusIcon(booking.status),
        contact: parseContactInfo(booking.contactInfo),
        equipments: Array.isArray(booking.equipments) ? booking.equipments : [],
        canEdit: canEditBooking(booking),
        canWithdraw: Number(booking.status) === 1,
      };
    });
});

const venueOptions = computed(() => {
  return [...new Set(historyItems.value.map((booking) => booking.venueName))]
    .filter(Boolean)
    .sort((left, right) => left.localeCompare(right, "zh-Hant"));
});

const keywordFilteredHistoryItems = computed(() => {
  const keyword = keywordFilter.value.trim().toLowerCase();

  return historyItems.value.filter((booking) => {
    return (
      keyword === ""
      || booking.venueName.toLowerCase().includes(keyword)
      || booking.purpose.toLowerCase().includes(keyword)
    );
  });
});

const nonTabFilteredHistoryItems = computed(() => {
  return keywordFilteredHistoryItems.value.filter((booking) => {
    return venueFilter.value === "" || booking.venueName === venueFilter.value;
  });
});

const nonStatusFilteredHistoryItems = computed(() => {
  return nonTabFilteredHistoryItems.value.filter((booking) => {
    const bookingDate = booking.bookingDate || "";
    const matchesStartDate =
      startDateFilter.value === "" || bookingDate >= startDateFilter.value;
    const matchesEndDate =
      endDateFilter.value === "" || bookingDate <= endDateFilter.value;

    return matchesStartDate && matchesEndDate;
  });
});

const filteredHistoryItems = computed(() => {
  return nonStatusFilteredHistoryItems.value.filter((booking) => {
    return (
      statusFilter.value === ""
      || String(booking.status) === statusFilter.value
    );
  });
});

const paginationTotalPages = computed(() => {
  return Math.ceil(filteredHistoryItems.value.length / BOOKING_PAGE_SIZE);
});

const paginatedHistoryItems = computed(() => {
  const startIndex = (currentPage.value - 1) * BOOKING_PAGE_SIZE;
  return filteredHistoryItems.value.slice(startIndex, startIndex + BOOKING_PAGE_SIZE);
});

const paginationStartIndex = computed(() => {
  if (filteredHistoryItems.value.length === 0) return 0;
  return (currentPage.value - 1) * BOOKING_PAGE_SIZE + 1;
});

const paginationEndIndex = computed(() => {
  return Math.min(currentPage.value * BOOKING_PAGE_SIZE, filteredHistoryItems.value.length);
});

const canGoPreviousPage = computed(() => currentPage.value > 1);
const canGoNextPage = computed(() => currentPage.value < paginationTotalPages.value);

const visiblePageNumbers = computed(() => {
  const totalPages = paginationTotalPages.value;
  const maxVisiblePages = 5;
  const visibleCount = Math.min(totalPages, maxVisiblePages);
  let startPage = currentPage.value - Math.floor(visibleCount / 2);

  if (startPage < 1) {
    startPage = 1;
  }

  if (startPage + visibleCount - 1 > totalPages) {
    startPage = totalPages - visibleCount + 1;
  }

  return Array.from({ length: visibleCount }, (_, index) => startPage + index);
});

const hasActiveFilters = computed(() => {
  return (
    keywordFilter.value.trim() !== ""
    || venueFilter.value !== ""
    || startDateFilter.value !== ""
    || endDateFilter.value !== ""
    || statusFilter.value !== ""
  );
});

const statusCounts = computed(() => {
  return nonStatusFilteredHistoryItems.value.reduce(
    (counts, booking) => {
      if (booking.status === 1) {
        counts.pending += 1;
      } else if (booking.status === 2) {
        counts.approved += 1;
      } else if (booking.status === 3) {
        counts.rejected += 1;
      } else {
        counts.withdrawn += 1;
      }

      return counts;
    },
    {
      pending: 0,
      approved: 0,
      rejected: 0,
      withdrawn: 0,
    },
  );
});

const toggleExpanded = (bookingId) => {
  expandedBookingId.value =
    expandedBookingId.value === bookingId ? null : bookingId;
};

const setCurrentPage = (pageNo) => {
  currentPage.value = Math.min(
    Math.max(pageNo, 1),
    Math.max(paginationTotalPages.value, 1),
  );
  expandedBookingId.value = null;
};

const goToPreviousPage = () => {
  setCurrentPage(currentPage.value - 1);
};

const goToNextPage = () => {
  setCurrentPage(currentPage.value + 1);
};

const clearFilters = () => {
  keywordFilter.value = "";
  venueFilter.value = "";
  statusFilter.value = "";
  clearDateRange();
  datePickerOpen.value = false;
};

const getVenueLookupKey = (venueName) => {
  return normalizeVenueDisplayName(venueName || "").trim();
};

const loadVenueLookup = async () => {
  if (venueLookup.value) return venueLookup.value;

  const lookup = new Map();
  const units = await fetchAllUnits();
  const venueGroups = await Promise.all(
    units.map((unit) => fetchVenuesByUnit(unit.id)),
  );

  venueGroups.flat().forEach((venue) => {
    const key = getVenueLookupKey(venue.name);
    if (key && !lookup.has(key)) {
      lookup.set(key, venue);
    }
  });

  venueLookup.value = lookup;
  return lookup;
};

const resolveVenueInfo = async (booking, linkedEquipmentBooking = null) => {
  const directVenueId = Number(booking.venueId ?? linkedEquipmentBooking?.relatedVenueId);

  if (Number.isFinite(directVenueId) && directVenueId > 0) {
    return {
      id: directVenueId,
      name: booking.venueName,
    };
  }

  const lookup = await loadVenueLookup();
  const matchedVenue = lookup.get(getVenueLookupKey(booking.venueName));

  if (!matchedVenue?.id) {
    throw new Error("無法判斷此預約的場地 ID，請從場地月曆頁修改。");
  }

  return {
    id: matchedVenue.id,
    name: matchedVenue.name || booking.venueName,
  };
};

const fetchLinkedEquipmentBooking = async (bookingId) => {
  const equipmentPage = normalizeEquipmentBookingPage(
    await queryMyEquipmentBookings({
      relatedVenueBookingId: bookingId,
      pageNo: 1,
      pageSize: 10,
    }),
  );

  if (equipmentPage.items.length > 1) {
    throw new Error("此場地預約關聯了多筆設備申請，暫時無法在此頁直接修改。");
  }

  return equipmentPage.items[0] || null;
};

const openEditModal = async (booking) => {
  if (!booking?.id || editingBookingId.value) return;

  editingBookingId.value = booking.id;

  try {
    const linkedEquipmentBooking = await fetchLinkedEquipmentBooking(booking.id);
    const venueInfo = await resolveVenueInfo(booking, linkedEquipmentBooking);
    const equipmentReadonly = Boolean(
      linkedEquipmentBooking && Number(linkedEquipmentBooking.status) !== 1,
    );

    selectedVenueInfo.value = venueInfo;

    modalMode.value = "edit";
    modalInitialData.value = {
      id: booking.id,
      dateStr: booking.bookingDate,
      slots: Array.isArray(booking.slots) ? [...booking.slots] : [],
      purpose: booking.purpose || "",
      participantCount: booking.pCount || 1,
      contactInfo: { ...booking.contact },
      canWithdraw: booking.canWithdraw === true,
      linkedEquipmentBooking,
      equipmentReadonly,
      equipmentReadonlyMessage: equipmentReadonly
        ? "此筆設備申請目前不是審核中，本次只會更新場地預約。"
        : "",
    };
    isModalVisible.value = true;
  } catch (openError) {
    error(openError.message || "載入可修改的設備資料失敗。");
  } finally {
    editingBookingId.value = null;
  }
};

const getRequestedEditBookingId = () => {
  const rawValue = Array.isArray(route.query.editBookingId)
    ? route.query.editBookingId[0]
    : route.query.editBookingId;
  const bookingId = Number(rawValue);

  return Number.isFinite(bookingId) && bookingId > 0 ? bookingId : null;
};

const openRequestedEditBooking = async () => {
  const bookingId = getRequestedEditBookingId();

  if (!bookingId || handledEditBookingId.value === bookingId) return;

  const booking = historyItems.value.find((item) => Number(item.id) === bookingId);
  handledEditBookingId.value = bookingId;

  if (!booking) {
    warning("找不到要修改的場地預約。");
    return;
  }

  if (!booking.canEdit) {
    warning("這筆場地預約目前無法修改。只有待審核且尚未開始的預約可以自行修改；若預約已開始、已過期、已通過、退回或撤回，請重新送出申請或聯絡管理單位協助。");
    return;
  }

  await openEditModal(booking);
};

const handleModalSuccess = async () => {
  isModalVisible.value = false;
  await loadBookings();
};

const handleWithdrawBooking = async (bookingId) => {
  if (!bookingId) return;

  isWithdrawing.value = true;

  try {
    await withdrawBooking(bookingId);
    isModalVisible.value = false;
    await loadBookings();
  } catch (withdrawError) {
    warning(withdrawError.message || "撤回預約失敗。");
  } finally {
    isWithdrawing.value = false;
  }
};

const fetchAllBookings = async () => {
  const pageSize = 100;
  const firstPage = normalizeBookingPage(
    await queryMyBookings(buildBookingQueryPayload({ pageNo: 1, pageSize })),
  );
  const allBookings = [...firstPage.items];

  for (let pageNo = 2; pageNo <= firstPage.totalPages; pageNo += 1) {
    const nextPage = normalizeBookingPage(
      await queryMyBookings(buildBookingQueryPayload({ pageNo, pageSize })),
    );
    allBookings.push(...nextPage.items);
  }

  return allBookings;
};

const loadBookings = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    bookings.value = await fetchAllBookings();
  } catch (error) {
    console.error("載入個人預約清單失敗:", error);
    loadError.value = error.message || "請稍後再試一次。";
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  document.addEventListener("click", handleDocumentClick);
  await loadBookings();
  await openRequestedEditBooking();
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleDocumentClick);
});

watch([keywordFilter, venueFilter, statusFilter, startDateFilter, endDateFilter], () => {
  setCurrentPage(1);
});

watch(
  () => route.query.editBookingId,
  async () => {
    handledEditBookingId.value = null;
    if (!loading.value) {
      await openRequestedEditBooking();
    }
  },
);
</script>

<style lang="scss" scoped>
.history-page {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.history-layout {
  display: grid;
  grid-template-columns: minmax(320px, 360px) minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
  min-width: 0;
}

.history-layout > * {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.history-hero {
  position: sticky;
  top: calc(var(--header-height) + 2.25rem);
  padding: 1.5rem;
  width: 100%;
  max-width: 100%;
  background:
    linear-gradient(180deg, rgba(232, 240, 250, 0.95), rgba(255, 255, 255, 0.98)),
    radial-gradient(circle at top right, rgba(var(--blue-900-rgb), 0.1), transparent 38%);
  border: 1px solid rgba(var(--blue-900-rgb), 0.12);
}

.history-page-header {
  margin-bottom: 0;
}

.hero-eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
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

.history-feedback {
  text-align: center;

  h3 {
    margin-bottom: 0.5rem;
    color: var(--ink);
  }

  p {
    margin: 0;
    color: var(--muted);
  }
}

.retry-btn,
.explore-btn {
  margin-top: 1.25rem;
}

.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  margin-bottom: 1.1rem;
}

.filter-tabs {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0 0 0.25rem;
  border-bottom: 1px solid rgba(var(--blue-900-rgb), 0.1);
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  position: relative;
  padding: 0.6rem 0.25rem 0.9rem;
  border: 0;
  background: transparent;
  color: var(--ink);
  font-size: clamp(1.1rem, 1.2vw, 1.35rem);
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s ease;

  &::after {
    content: "";
    position: absolute;
    left: 0;
    right: 0;
    bottom: -0.26rem;
    height: 4px;
    border-radius: 999px;
    background: transparent;
    transition: background-color 0.2s ease;
  }

  &:hover {
    color: var(--accent);
  }

  &.is-active {
    color: var(--accent);
  }

  &.is-active::after {
    background: var(--accent);
  }
}

.filter-tab-icon {
  flex: 0 0 auto;
}

.filter-toolbar {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  align-items: stretch;
  padding: 1rem 1.2rem;
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  backdrop-filter: blur(8px);
}

.record-tabs {
  margin: -0.15rem -0.1rem 1.1rem;
  padding: 0 0.1rem 0.25rem;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;

  label {
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  input,
  select {
    min-height: 2.85rem;
    padding: 0.75rem 0.95rem;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: #ffffff;
    color: var(--ink);
  }
}

.date-range-picker {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;

  label {
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 700;
  }
}

.date-range-trigger {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  align-items: stretch;
  width: 100%;
  min-height: 4.35rem;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: #ffffff;
  color: var(--ink);
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;

  &:hover,
  &.is-open {
    border-color: rgba(var(--blue-900-rgb), 0.28);
    box-shadow: 0 10px 24px rgba(39, 94, 168, 0.08);
  }
}

.date-range-segment {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 0.75rem 0.9rem;

  & + & {
    border-left: 1px solid rgba(var(--blue-900-rgb), 0.1);
  }

  strong {
    margin-top: 0.3rem;
    color: var(--muted-strong);
    font-size: var(--text-base);
    line-height: 1.2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &.has-value strong {
    color: var(--ink);
  }
}

.date-range-label {
  color: var(--muted);
  font-size: var(--text-xs);
  font-weight: 800;
}

.date-range-chevron {
  align-self: center;
  margin-right: 0.85rem;
  color: var(--accent);
  pointer-events: none;
}

.date-range-clear {
  align-self: flex-start;
  padding: 0;
  border: 0;
  background: none;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  cursor: pointer;

  &:hover {
    color: var(--accent-hover);
    text-decoration: underline;
  }
}

.date-range-popover {
  position: fixed;
  top: 50%;
  left: 50%;
  z-index: 80;
  width: min(520px, calc(100vw - 2rem));
  max-height: calc(100vh - 2rem);
  padding: 1.2rem;
  overflow-y: auto;
  overscroll-behavior: contain;
  border: 1px solid rgba(var(--blue-900-rgb), 0.12);
  border-radius: var(--radius);
  background: #ffffff;
  box-shadow: 0 22px 55px rgba(20, 35, 58, 0.16);
  transform: translate(-50%, -50%);
}

.calendar-selection-footer {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.calendar-selection-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.35rem 0.85rem;
  align-items: center;

  strong {
    min-width: 0;
    color: var(--ink);
    font-size: var(--text-base);
    font-weight: 900;
    line-height: 1.25;
  }
}

.calendar-selection-hint {
  grid-column: 2;
  color: var(--muted);
  font-size: var(--text-xs);
  font-weight: 700;
  line-height: 1.35;
}

.calendar-selection-label {
  align-self: flex-start;
  padding-top: 0.1rem;
  color: var(--accent);
  font-size: var(--text-xs);
  font-weight: 900;
  white-space: nowrap;
}

.calendar-manual-inputs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.calendar-manual-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;

  span {
    color: var(--muted-strong);
    font-size: var(--text-xs);
    font-weight: 800;
  }

  input {
    width: 100%;
    min-height: 2.55rem;
    padding: 0.55rem 0.7rem;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: #ffffff;
    color: var(--ink);
    font: inherit;
    font-size: var(--text-sm);
    font-weight: 700;

    &:focus {
      outline: none;
      border-color: rgba(var(--blue-900-rgb), 0.34);
      box-shadow: 0 0 0 3px rgba(70, 99, 242, 0.12);
    }
  }
}

.filter-summary {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 0.35rem;
  min-width: 0;

  strong {
    color: var(--ink);
    font-size: var(--text-xl);
    line-height: 1.2;
  }
}

.summary-label {
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.clear-filter-btn {
  padding: 0;
  border: 0;
  background: none;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 700;
  cursor: pointer;

  &:hover {
    color: var(--accent-hover);
    text-decoration: underline;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.summary-card {
  padding: 1.1rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  background: rgba(255, 255, 255, 0.88);
  border-color: rgba(var(--blue-900-rgb), 0.06);

  span {
    color: var(--muted);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  strong {
    color: var(--ink);
    font-size: var(--text-2xl);
    line-height: 1;
  }
}

.summary-card-label {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.summary-card:first-child {
  grid-column: 1 / -1;
}

.history-records {
  padding: 1.4rem;
  width: 100%;
  max-width: 100%;
  border-radius: calc(var(--radius-lg) + 2px);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  box-shadow: var(--shadow-soft);
}

.record-empty-state {
  margin-top: 0.25rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1.1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(var(--blue-900-rgb), 0.08);
}

.pagination-summary {
  margin: 0;
  color: var(--muted-strong);
  font-size: var(--text-sm);
  font-weight: 700;
}

.pagination-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.pagination-btn,
.pagination-page {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.25rem;
  height: 2.25rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.12);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.88);
  color: var(--ink);
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;

  &:hover:not(:disabled):not(.is-active) {
    border-color: rgba(var(--blue-900-rgb), 0.22);
    background: rgba(var(--blue-900-rgb), 0.06);
    color: var(--accent);
    transform: translateY(-1px);
  }

  &:disabled {
    color: var(--muted);
    cursor: not-allowed;
    opacity: 0.48;
  }
}

.pagination-page {
  padding: 0 0.75rem;
  font-size: var(--text-sm);

  &.is-active {
    border-color: var(--accent);
    background: var(--accent);
    color: #ffffff;
    cursor: default;
  }
}

.history-card {
  padding: 1.35rem;
}

.history-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  min-width: 0;
}

.history-overview {
  flex: 1;
  min-width: 0;
}

.overview-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;

  h3 {
    margin-top: 0.15rem;
    color: var(--ink);
  }
}

.keyword-highlight {
  padding: 0 0.18em;
  border-radius: 0.3em;
  background: rgba(255, 216, 102, 0.65);
  box-decoration-break: clone;
  -webkit-box-decoration-break: clone;
}

.eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  letter-spacing: 0.04em;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.38rem;
  min-width: 5.4rem;
  padding: 0.45rem 0.85rem;
  border-radius: 999px;
  font-size: var(--text-sm);
  font-weight: 700;

  &.is-pending {
    background: rgba(214, 165, 54, 0.14);
    color: #9b6b00;
  }

  &.is-approved {
    background: rgba(46, 139, 87, 0.14);
    color: #1f6a43;
  }

  &.is-rejected {
    background: rgba(196, 69, 69, 0.14);
    color: #9a2d2d;
  }

  &.is-withdrawn {
    background: rgba(98, 108, 118, 0.12);
    color: #475467;
  }
}

.status-pill-icon {
  flex: 0 0 auto;
}

.info-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(220px, 1fr);
  gap: 0.85rem;
  align-items: stretch;
}

.info-item {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(var(--blue-900-rgb), 0.03));
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);

  strong {
    display: block;
    color: var(--ink);
    line-height: 1.45;
  }
}

.info-item.is-time-focus {
  grid-column: 1;
  grid-row: 1 / span 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 1rem 1.1rem 1.05rem;
  background:
    linear-gradient(135deg, rgba(39, 94, 168, 0.14), rgba(255, 255, 255, 0.96) 52%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(39, 94, 168, 0.08));
  border-color: rgba(39, 94, 168, 0.22);
  box-shadow: 0 12px 28px rgba(39, 94, 168, 0.08);
}

.time-focus-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.55rem;
}

.time-focus-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  letter-spacing: 0.02em;
}

.time-range-emphasis {
  display: block;
  color: var(--ink);
  font-size: clamp(1.15rem, 1.4vw, 1.45rem);
  font-weight: 800;
  line-height: 1.45;
  letter-spacing: 0.01em;
  word-break: break-word;
}

.time-focus-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.8rem;
  color: var(--muted-strong);
  flex-wrap: wrap;
}

.time-focus-meta-label {
  font-size: var(--text-sm);
  font-weight: 700;
}

.time-focus-meta-value {
  font-size: var(--text-sm);
  font-weight: 600;
}

.info-item.is-side-info {
  grid-column: 2;
}

.info-label,
.detail-title {
  display: block;
  margin-bottom: 0.3rem;
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.detail-toggle {
  min-width: auto;
  min-height: 2.3rem;
  padding: 0.55rem 0.95rem;
  font-size: var(--text-sm);
  gap: 0.4rem;
}

.history-actions {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  align-items: stretch;
  gap: 0.55rem;
}

.edit-booking-btn {
  min-height: 2.3rem;
  padding: 0.55rem 0.95rem;
  font-size: var(--text-sm);
  white-space: nowrap;
}

.history-details {
  margin-top: 1rem;
  padding-top: 1rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  border-top: 1px solid var(--line);
}

.detail-block {
  padding: 1rem;
  border-radius: var(--radius-sm);
  background: rgba(var(--blue-900-rgb), 0.03);

  p {
    margin: 0.2rem 0 0;
    color: var(--ink);
    line-height: 1.55;
  }
}

@media (max-width: 900px) {
  .history-layout {
    grid-template-columns: 1fr;
    gap: 1rem;
    width: 100%;
  }

  .history-hero,
  .history-records {
    padding: 1.15rem;
  }

  .history-hero {
    position: static;
  }

  .filter-toolbar {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .history-main {
    flex-direction: column;
    align-items: stretch;
  }

  .history-overview {
    width: 100%;
  }

  .history-actions,
  .detail-toggle,
  .edit-booking-btn {
    width: 100%;
  }

  .pagination-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .date-range-popover {
    width: min(100%, calc(100vw - 2rem));
  }
}

@media (max-width: 640px) {
  .history-layout,
  .history-hero,
  .history-records {
    width: 100%;
    max-width: 100%;
  }

  .summary-grid,
  .info-grid,
  .history-details {
    grid-template-columns: 1fr;
  }

  .summary-card:first-child {
    grid-column: auto;
  }

  .filter-panel {
    gap: 0.85rem;
  }

  .filter-tabs {
    gap: 0.85rem;
  }

  .record-tabs {
    margin: 0 0 1rem;
    padding: 0 0 0.25rem;
  }

  .info-item.is-time-focus,
  .info-item.is-side-info {
    grid-column: auto;
    grid-row: auto;
  }

  .filter-tab {
    font-size: 1rem;
  }

  .filter-toolbar {
    padding: 1rem;
  }

  .date-range-trigger {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  .date-range-chevron {
    position: absolute;
    right: 0;
    top: 50%;
    margin-right: 0.65rem;
    transform: translateY(-50%);
  }

  .date-range-segment {
    padding-right: 1.8rem;
  }

  .date-range-popover {
    top: calc(var(--header-height) + 18px + max(0.75rem, env(safe-area-inset-top)));
    bottom: 0.75rem;
    left: 50%;
    width: calc(100vw - 1rem);
    max-height: calc(100dvh - var(--header-height) - 18px - 1.5rem);
    padding: 1rem;
    transform: translateX(-50%);
  }

  .calendar-selection-summary {
    grid-template-columns: 1fr;
  }

  .calendar-selection-hint {
    grid-column: 1;
  }

  .calendar-selection-label {
    padding-top: 0;
  }

  .calendar-manual-inputs {
    grid-template-columns: 1fr;
  }

  .history-records {
    padding: 1rem;
  }

  .history-card {
    padding: 1rem;
  }

  .overview-row {
    flex-direction: column;
  }

  .time-focus-header {
    align-items: flex-start;
  }

  .pagination-controls {
    width: 100%;
    justify-content: space-between;
    gap: 0.35rem;
  }

  .pagination-btn,
  .pagination-page {
    min-width: 2rem;
    height: 2rem;
  }
}
</style>
