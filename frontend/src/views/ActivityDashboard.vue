<template>
  <div class="activity-dashboard page-enter">
    <header class="dashboard-header">
      <button class="back-btn" type="button" @click="router.push('/')">
        <ArrowLeft :size="16" />
        返回首頁
      </button>

      <div class="dashboard-title-row">
        <div>
          <p class="eyebrow">即時場地使用狀態</p>
          <h1>活動資訊</h1>
        </div>
        <button
          class="btn btn-secondary refresh-btn"
          type="button"
          :disabled="loading || refreshing"
          @click="loadDashboardData"
        >
          <RefreshCw :size="18" :class="{ 'is-spinning': refreshing }" />
          重新整理
        </button>
      </div>

      <div class="time-panel">
        <div class="time-block">
          <span class="time-label">目前時間</span>
          <strong>{{ currentTimeLabel }}</strong>
        </div>
        <div class="time-block">
          <span class="time-label">使用中場地</span>
          <strong>{{ activeBookings.length }}</strong>
        </div>
        <div class="time-block">
          <span class="time-label">最近更新</span>
          <strong>{{ lastUpdatedLabel }}</strong>
        </div>
      </div>
    </header>

    <div v-if="loading" class="loading-state">載入活動資訊中...</div>

    <section v-else-if="loadError" class="empty-state error-state" aria-live="polite">
      <AlertCircle :size="26" />
      <div>
        <h2>目前無法取得活動資訊</h2>
        <p>{{ loadError }}</p>
      </div>
    </section>

    <section v-else-if="activeBookings.length === 0" class="empty-state dashboard-empty" aria-live="polite">
      <CalendarClock :size="30" />
      <div>
        <h2>目前沒有進行中的場地活動</h2>
        <p>系統會持續更新當前時段的已核准預約。</p>
      </div>
    </section>

    <section v-else class="live-activity-grid" aria-label="目前進行中的活動">
      <article v-for="booking in activeBookings" :key="booking.key" class="activity-card">
        <div class="card-accent" aria-hidden="true"></div>
        <div class="activity-card-body">
          <div class="activity-card-top">
            <span class="time-range">{{ booking.timeRange }}</span>
            <span class="status-badge">進行中</span>
          </div>

          <h2>{{ booking.purpose }}</h2>

          <div class="activity-meta">
            <span>
              <MapPin :size="17" />
              {{ booking.venueName }}
            </span>
            <span v-if="booking.unitName">
              <Building2 :size="17" />
              {{ booking.unitName }}
            </span>
            <span v-if="booking.participantCount">
              <Users :size="17" />
              {{ booking.participantCount }} 人
            </span>
          </div>

          <div v-if="booking.equipments.length" class="equipment-row">
            <span v-for="equipment in booking.equipments" :key="`${booking.key}-${equipment}`">
              {{ equipment }}
            </span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarClock,
  MapPin,
  RefreshCw,
  Users,
} from "lucide-vue-next";
import { fetchCalendarMonth } from "@/api/booking";
import { fetchAllUnits, fetchVenuesByUnit } from "@/api/venue";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { normalizeVenueDisplayName } from "@/utils/venueLabels";

const REFRESH_INTERVAL_MS = 60_000;
const CLOCK_INTERVAL_MS = 1_000;

const router = useRouter();
const now = ref(new Date());
const rawBookings = ref([]);
const loading = ref(true);
const refreshing = ref(false);
const loadError = ref("");
const lastUpdatedAt = ref(null);

let clockTimer = null;
let refreshTimer = null;
let requestToken = 0;

const toDateKey = (date) => date.toLocaleDateString("sv-SE");

const formatDateTime = (date) => {
  if (!date) return "尚未更新";

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(date);
};

const currentDateKey = computed(() => toDateKey(now.value));
const currentSlot = computed(() => now.value.getHours());
const currentTimeLabel = computed(() => formatDateTime(now.value));
const lastUpdatedLabel = computed(() => formatDateTime(lastUpdatedAt.value));

const activeBookings = computed(() => {
  return rawBookings.value
    .filter((booking) => {
      const slots = Array.isArray(booking.slots) ? booking.slots.map(Number) : [];

      return (
        Number(booking.status) === 2 &&
        booking.bookingDate === currentDateKey.value &&
        slots.includes(currentSlot.value)
      );
    })
    .map((booking) => {
      const slots = Array.isArray(booking.slots) ? booking.slots.map(Number) : [];
      const venueName = normalizeVenueDisplayName(booking.venueName || booking.fallbackVenueName);
      const fallbackKey = [
        booking.venueId,
        booking.bookingDate,
        booking.purpose,
        slots.join("-"),
      ].join("-");

      return {
        ...booking,
        key: booking.id || fallbackKey,
        slots,
        venueName: venueName || "未提供場地",
        purpose: booking.purpose || "未填寫用途",
        participantCount: Number(booking.pCount) || 0,
        timeRange: formatSlotGroupsAsTimeRange(slots) || "未提供時段",
        equipments: Array.isArray(booking.equipments) ? booking.equipments.filter(Boolean) : [],
      };
    })
    .sort((left, right) => {
      const leftStart = Math.min(...left.slots);
      const rightStart = Math.min(...right.slots);

      if (leftStart !== rightStart) return leftStart - rightStart;

      return left.venueName.localeCompare(right.venueName, "zh-Hant");
    });
});

const collectVenueBookings = async ({ venue, unit, year, month }) => {
  const monthData = await fetchCalendarMonth(venue.id, year, month);
  const bookings = monthData?.bookings || [];

  return bookings.map((booking) => ({
    ...booking,
    venueId: venue.id,
    unitName: unit.name,
    fallbackVenueName: venue.name,
  }));
};

const loadDashboardData = async () => {
  const token = ++requestToken;
  const queryDate = new Date();
  const year = queryDate.getFullYear();
  const month = queryDate.getMonth() + 1;

  loadError.value = "";

  if (lastUpdatedAt.value) {
    refreshing.value = true;
  } else {
    loading.value = true;
  }

  try {
    const unitsResponse = await fetchAllUnits();
    const units = Array.isArray(unitsResponse) ? unitsResponse : [];
    const venueGroups = await Promise.all(
      units.map(async (unit) => {
        const venuesResponse = await fetchVenuesByUnit(unit.id);
        const venues = Array.isArray(venuesResponse) ? venuesResponse : [];

        return venues.map((venue) => ({ venue, unit }));
      }),
    );

    const venueEntries = venueGroups.flat();
    const bookingGroups = await Promise.all(
      venueEntries.map(({ venue, unit }) => collectVenueBookings({ venue, unit, year, month })),
    );

    if (token !== requestToken) return;

    const seenKeys = new Set();
    rawBookings.value = bookingGroups.flat().filter((booking) => {
      const key = booking.id || `${booking.venueId}-${booking.bookingDate}-${booking.purpose}`;

      if (seenKeys.has(key)) return false;

      seenKeys.add(key);
      return true;
    });
    lastUpdatedAt.value = new Date();
  } catch (error) {
    if (token !== requestToken) return;

    console.error("取得活動資訊失敗:", error);
    rawBookings.value = [];
    loadError.value = error.message || "請稍後再試，或確認後端服務是否正常。";
  } finally {
    if (token === requestToken) {
      loading.value = false;
      refreshing.value = false;
    }
  }
};

onMounted(() => {
  loadDashboardData();

  clockTimer = window.setInterval(() => {
    now.value = new Date();
  }, CLOCK_INTERVAL_MS);

  refreshTimer = window.setInterval(() => {
    loadDashboardData();
  }, REFRESH_INTERVAL_MS);
});

onBeforeUnmount(() => {
  window.clearInterval(clockTimer);
  window.clearInterval(refreshTimer);
  requestToken += 1;
});
</script>

<style lang="scss" scoped>
.activity-dashboard {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.dashboard-header {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.dashboard-title-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.eyebrow {
  margin: 0 0 0.25rem;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
}

.refresh-btn {
  flex: 0 0 auto;
}

.is-spinning {
  animation: spin 0.8s linear infinite;
}

.time-panel {
  display: grid;
  grid-template-columns: minmax(260px, 1.35fr) repeat(2, minmax(160px, 0.65fr));
  gap: 0.9rem;
}

.time-block {
  min-height: 5.25rem;
  padding: 1rem 1.1rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.1);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: var(--shadow-soft);

  strong {
    display: block;
    margin-top: 0.35rem;
    color: var(--ink);
    font-size: var(--text-xl);
    line-height: var(--leading-tight);
  }
}

.time-label {
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.live-activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.activity-card {
  position: relative;
  min-height: 18rem;
  overflow: hidden;
  border: 1px solid rgba(46, 139, 87, 0.22);
  border-radius: var(--radius);
  background:
    linear-gradient(135deg, rgba(46, 139, 87, 0.15), rgba(255, 255, 255, 0.96) 52%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(46, 139, 87, 0.08));
  box-shadow: 0 12px 28px rgba(46, 139, 87, 0.08);
}

.card-accent {
  position: absolute;
  inset: 0 auto 0 0;
  width: 0.45rem;
  background: #2e8b57;
}

.activity-card-body {
  display: flex;
  min-height: 18rem;
  flex-direction: column;
  gap: 1rem;
  padding: 1.35rem 1.35rem 1.25rem 1.7rem;
}

.activity-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.time-range {
  color: var(--muted-strong);
  font-size: var(--text-lg);
  font-weight: 800;
}

.status-badge {
  flex: 0 0 auto;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  background: rgba(46, 139, 87, 0.13);
  color: #247047;
  font-size: var(--text-sm);
  font-weight: 800;
}

.activity-card h2 {
  color: var(--ink);
  font-size: var(--text-2xl);
  line-height: 1.35;
}

.activity-meta {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  color: var(--muted-strong);
  font-weight: 700;

  span {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
  }

  svg {
    flex: 0 0 auto;
    color: var(--accent);
  }
}

.equipment-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: auto;

  span {
    padding: 0.25rem 0.55rem;
    border-radius: 999px;
    background: var(--surface-muted);
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 700;
  }
}

.dashboard-empty,
.error-state {
  display: flex;
  align-items: flex-start;
  gap: 1rem;

  svg {
    flex: 0 0 auto;
    color: var(--accent);
  }

  h2 {
    font-size: var(--text-xl);
  }

  p {
    margin: 0.3rem 0 0;
    color: var(--muted);
  }
}

.error-state svg {
  color: var(--danger);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 760px) {
  .dashboard-title-row {
    align-items: stretch;
    flex-direction: column;
  }

  .time-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .live-activity-grid {
    grid-template-columns: 1fr;
  }

  .activity-card,
  .activity-card-body {
    min-height: 0;
  }

  .activity-card h2 {
    font-size: var(--text-xl);
  }
}
</style>
