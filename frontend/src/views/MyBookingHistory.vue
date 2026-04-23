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
      <section class="history-hero card">
        <header class="page-header history-header">
          <button class="back-btn" @click="router.push('/')">
            ← {{ BACK_TO_UNIT_SELECTOR_LABEL }}
          </button>
          <p class="hero-eyebrow">Booking Archive</p>
          <h1>我的預約歷史紀錄</h1>
          <p>集中查看所有預約申請、借用時段與目前審核狀態。</p>
        </header>

        <section class="filter-panel">
          <div class="filter-field">
            <label for="booking-keyword">關鍵字搜尋</label>
            <input
              id="booking-keyword"
              v-model.trim="keywordFilter"
              type="text"
              placeholder="搜尋場地名稱或使用用途"
            />
          </div>

          <div class="filter-field">
            <label for="booking-status">申請狀態</label>
            <select id="booking-status" v-model="statusFilter">
              <option value="">全部狀態</option>
              <option value="1">審核中</option>
              <option value="2">已通過</option>
              <option value="3">已拒絕</option>
              <option value="0">已撤回</option>
            </select>
          </div>

          <div class="filter-summary">
            <span class="summary-label">篩選結果</span>
            <strong>{{ filteredHistoryItems.length }} 筆</strong>
            <button
              v-if="hasActiveFilters"
              type="button"
              class="clear-filter-btn"
              @click="clearFilters"
            >
              清除篩選
            </button>
          </div>
        </section>

        <section class="summary-grid">
          <article class="summary-card card">
            <span>全部申請</span>
            <strong>{{ filteredHistoryItems.length }}</strong>
          </article>
          <article class="summary-card card">
            <span>審核中</span>
            <strong>{{ statusCounts.pending }}</strong>
          </article>
          <article class="summary-card card">
            <span>已通過</span>
            <strong>{{ statusCounts.approved }}</strong>
          </article>
          <article class="summary-card card">
            <span>已拒絕 / 已撤回</span>
            <strong>{{ statusCounts.closed }}</strong>
          </article>
        </section>
      </section>

      <div v-if="filteredHistoryItems.length === 0" class="empty-state history-feedback">
        <h3>目前沒有符合條件的預約紀錄</h3>
        <p>可以調整篩選條件，或清除篩選後查看全部預約。</p>
        <button type="button" class="btn btn-secondary retry-btn" @click="clearFilters">
          清除篩選
        </button>
      </div>

      <section v-else class="history-records">
        <header class="records-header">
          <div>
            <p class="records-eyebrow">Application Timeline</p>
            <h2>申請歷史紀錄</h2>
            <p>逐筆查看你曾送出的申請內容與目前進度。</p>
          </div>
          <span class="records-count">{{ filteredHistoryItems.length }} 筆紀錄</span>
        </header>

        <div class="history-list">
          <article
            v-for="booking in filteredHistoryItems"
            :key="booking.id"
            class="history-card card"
          >
            <div class="history-main">
              <div class="history-overview">
                <div class="overview-row">
                  <div>
                    <p class="eyebrow">{{ booking.bookingDateLabel }}</p>
                    <h3>{{ booking.venueName }}</h3>
                  </div>
                  <span class="status-pill" :class="booking.statusClass">
                    {{ booking.statusText }}
                  </span>
                </div>

                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">借用時段</span>
                    <strong>{{ booking.timeRange }}</strong>
                  </div>
                  <div class="info-item">
                    <span class="info-label">使用用途</span>
                    <strong>{{ booking.purpose }}</strong>
                  </div>
                  <div class="info-item">
                    <span class="info-label">申請時間</span>
                    <strong>{{ booking.createdAtLabel }}</strong>
                  </div>
                  <div class="info-item">
                    <span class="info-label">預估人數</span>
                    <strong>{{ booking.pCount }} 人</strong>
                  </div>
                </div>
              </div>

              <button
                type="button"
                class="btn btn-secondary detail-toggle"
                @click="toggleExpanded(booking.id)"
              >
                {{ expandedBookingId === booking.id ? "收起詳情" : "查看詳情" }}
              </button>
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
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMyBookings } from "@/api/booking";
import { getBookingStatusMeta, parseContactInfo } from "@/utils/bookingMeta";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import {
  BACK_TO_UNIT_SELECTOR_LABEL,
  GO_TO_UNIT_SELECTOR_LABEL,
} from "@/utils/navigationLabels";
import { normalizeVenueDisplayName } from "@/utils/venueLabels";

const router = useRouter();
const loading = ref(true);
const loadError = ref("");
const bookings = ref([]);
const expandedBookingId = ref(null);
const keywordFilter = ref("");
const statusFilter = ref("");

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
        contact: parseContactInfo(booking.contactInfo),
        equipments: Array.isArray(booking.equipments) ? booking.equipments : [],
      };
    });
});

const filteredHistoryItems = computed(() => {
  const keyword = keywordFilter.value.trim().toLowerCase();

  return historyItems.value.filter((booking) => {
    const matchesStatus =
      statusFilter.value === "" || String(booking.status) === statusFilter.value;

    const matchesKeyword =
      keyword === "" ||
      booking.venueName.toLowerCase().includes(keyword) ||
      booking.purpose.toLowerCase().includes(keyword);

    return matchesStatus && matchesKeyword;
  });
});

const hasActiveFilters = computed(() => {
  return keywordFilter.value.trim() !== "" || statusFilter.value !== "";
});

const statusCounts = computed(() => {
  return filteredHistoryItems.value.reduce(
    (counts, booking) => {
      if (booking.status === 1) {
        counts.pending += 1;
      } else if (booking.status === 2) {
        counts.approved += 1;
      } else {
        counts.closed += 1;
      }

      return counts;
    },
    {
      pending: 0,
      approved: 0,
      closed: 0,
    },
  );
});

const toggleExpanded = (bookingId) => {
  expandedBookingId.value =
    expandedBookingId.value === bookingId ? null : bookingId;
};

const clearFilters = () => {
  keywordFilter.value = "";
  statusFilter.value = "";
};

const loadBookings = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    bookings.value = await fetchMyBookings();
  } catch (error) {
    console.error("載入個人預約清單失敗:", error);
    loadError.value = error.message || "請稍後再試一次。";
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await loadBookings();
});
</script>

<style lang="scss" scoped>
.history-page {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.history-hero {
  padding: 1.5rem;
  background:
    linear-gradient(180deg, rgba(232, 240, 250, 0.95), rgba(255, 255, 255, 0.98)),
    radial-gradient(circle at top right, rgba(var(--blue-900-rgb), 0.1), transparent 38%);
  border: 1px solid rgba(var(--blue-900-rgb), 0.12);
}

.history-header {
  margin-bottom: 1.25rem;
}

.hero-eyebrow,
.records-eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
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
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(220px, 0.9fr) minmax(180px, 0.7fr);
  gap: 1rem;
  padding: 1.1rem 1.2rem;
  margin-bottom: 1.1rem;
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  backdrop-filter: blur(8px);
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

.filter-summary {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 0.35rem;

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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.history-records {
  padding: 1.4rem;
  border-radius: calc(var(--radius-lg) + 2px);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  box-shadow: var(--shadow-soft);
}

.records-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.2rem;

  h2 {
    margin: 0.2rem 0 0.35rem;
    color: var(--ink);
  }

  p:last-child {
    margin: 0;
    color: var(--muted);
  }
}

.records-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.45rem 0.85rem;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 700;
  white-space: nowrap;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.history-card {
  padding: 1.35rem;
}

.history-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
}

.history-overview {
  flex: 1;
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

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
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

.info-label,
.detail-title {
  display: block;
  margin-bottom: 0.3rem;
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.detail-toggle {
  min-width: 8.5rem;
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
  .history-hero,
  .history-records {
    padding: 1.15rem;
  }

  .filter-panel {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .history-main {
    flex-direction: column;
  }

  .records-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-toggle {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .summary-grid,
  .info-grid,
  .history-details {
    grid-template-columns: 1fr;
  }

  .filter-panel {
    padding: 1rem;
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
}
</style>
