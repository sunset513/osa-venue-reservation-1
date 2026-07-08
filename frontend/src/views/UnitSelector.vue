<template>
  <div class="selector-page page-enter">
    <header class="home-hero">
      <p class="home-hero-kicker">NCU OSA Venue</p>
      <h1>場地租借管理系統</h1>
      <p>{{ UNIT_SELECTOR_DESCRIPTION }}</p>
    </header>

    <section class="home-section" aria-labelledby="unit-section-title">
      <div class="section-heading">
        <h2 id="unit-section-title">管理單位</h2>
      </div>

      <div v-if="loading" class="loading-state">載入中...</div>

      <div v-else class="card-grid">
        <div
          v-for="unit in units"
          :key="unit.id"
          class="select-card"
          :class="{ 'is-disabled': unit.disabled }"
          @click="selectUnit(unit)"
        >
          <span v-if="unit.disabled" class="dev-badge">開發中</span>

          <div class="card-icon">
            <Building2 :size="32" />
          </div>
          <h3>{{ unit.name }}</h3>
          <span class="unit-code">{{ unit.code }}</span>
        </div>
      </div>
    </section>

    <section class="home-section" aria-labelledby="quick-entry-title">
      <div class="section-heading">
        <h2 id="quick-entry-title">快速入口</h2>
      </div>

      <div class="quick-entry-grid" :class="{ 'is-reviewer': isReviewer }">
        <button class="quick-entry-card" type="button" @click="goToActivityDashboard">
          <span class="quick-entry-icon quick-entry-icon--activity">
            <Activity :size="24" />
          </span>
          <span class="quick-entry-copy">
            <strong>今日活動看板</strong>
            <small>查看今日已核准且尚未結束的場地活動</small>
          </span>
          <ArrowRight :size="18" />
        </button>

        <button
          v-if="isReviewer"
          class="quick-entry-card quick-entry-card--review"
          type="button"
          @click="goToReviewPage"
        >
          <span class="quick-entry-icon quick-entry-icon--review">
            <ClipboardCheck :size="24" />
          </span>
          <span class="quick-entry-copy">
            <strong>審核工作台</strong>
            <small>處理場地借用申請與審核作業</small>
          </span>
          <ArrowRight :size="18" />
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Activity, ArrowRight, Building2, ClipboardCheck } from "lucide-vue-next";
import { fetchAllUnits } from "@/api/venue";
import { useAuthSessionStore } from "@/stores/authSession";
import { UNIT_SELECTOR_DESCRIPTION } from "@/utils/navigationLabels";

const router = useRouter();
const authSession = useAuthSessionStore();
const units = ref([]);
const loading = ref(true);

const isReviewer = computed(() => authSession.isReviewer);

onMounted(async () => {
  try {
    const fetchedUnits = await fetchAllUnits();
    const activeUnits = fetchedUnits.map((unit) => ({ ...unit, disabled: false }));
    const placeholderUnits = [
      { id: "dev-1", name: "學生宿舍場地", code: "HSD", disabled: true },
      { id: "dev-2", name: "課外活動場地", code: "EAD", disabled: true },
    ];

    units.value = [...activeUnits, ...placeholderUnits];
  } finally {
    loading.value = false;
  }
});

const selectUnit = (unit) => {
  if (unit.disabled) return;
  router.push(`/unit/${unit.id}`);
};

const goToActivityDashboard = () => {
  router.push({ name: "ActivityDashboard" });
};

const goToReviewPage = () => {
  router.push({ name: "ReviewCalendar" });
};
</script>

<style lang="scss" scoped>
@use "@/assets/styles/selector-common.scss";

.selector-page {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.home-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.95rem;
  padding: 2.2rem 0 1rem;
  text-align: center;
}

.home-hero-kicker {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-base);
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.home-hero h1 {
  font-size: clamp(2.4rem, 4vw, 3.3rem);
  line-height: 1.15;
}

.home-hero p:last-child {
  margin: 0;
  max-width: 42rem;
  color: var(--muted);
  font-size: clamp(1.05rem, 2vw, 1.3rem);
  line-height: 1.6;
}

.home-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.section-heading h2 {
  font-size: var(--text-2xl);
  color: var(--ink);
}

.quick-entry-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1rem;
}

.quick-entry-grid.is-reviewer {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quick-entry-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
  padding: 1.25rem 1.35rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.1);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: var(--shadow-soft);
  color: var(--ink);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow);
    border-color: rgba(var(--blue-900-rgb), 0.18);
  }
}

.quick-entry-card--review {
  background: linear-gradient(135deg, rgba(243, 247, 252, 0.98), rgba(232, 240, 250, 0.96));
}

.quick-entry-icon {
  width: 3rem;
  height: 3rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 50%;
}

.quick-entry-icon--activity {
  background: rgba(46, 139, 87, 0.12);
  color: #247047;
}

.quick-entry-icon--review {
  background: rgba(36, 63, 107, 0.12);
  color: #243f6b;
}

.quick-entry-copy {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
  gap: 0.2rem;

  strong {
    font-size: var(--text-lg);
  }

  small {
    color: var(--muted);
    font-size: var(--text-sm);
    line-height: 1.5;
  }
}

.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
  position: relative;
  background-color: rgba(var(--blue-900-rgb), 0.02);

  &:hover {
    transform: none;
    box-shadow: var(--shadow-soft);
    border-color: rgba(var(--blue-900-rgb), 0.08);
  }
}

.dev-badge {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background-color: rgba(214, 165, 54, 0.18);
  color: var(--danger);
  font-size: var(--text-xs);
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-weight: 700;
}

@media (max-width: 680px) {
  .selector-page {
    gap: 1.5rem;
  }

  .home-hero {
    align-items: flex-start;
    text-align: left;
    padding: 1.2rem 0 0.5rem;
  }

  .quick-entry-grid,
  .quick-entry-grid.is-reviewer {
    grid-template-columns: 1fr;
  }

  .quick-entry-card {
    align-items: flex-start;
  }

  .quick-entry-copy strong {
    font-size: var(--text-base);
  }
}
</style>
