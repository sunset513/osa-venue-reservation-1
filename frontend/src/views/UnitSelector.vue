<template>
  <div class="selector-page page-enter">
    <header class="page-header">
      <h1>{{ UNIT_SELECTOR_TITLE }}</h1>
      <p>{{ UNIT_SELECTOR_DESCRIPTION }}</p>
    </header>

    <section class="dashboard-entry" aria-labelledby="dashboard-entry-title">
      <div class="dashboard-entry-icon">
        <Activity :size="28" />
      </div>
      <div class="dashboard-entry-copy">
        <h2 id="dashboard-entry-title">活動看板</h2>
        <p>快速查看近期活動與場地借用整體概況。</p>
      </div>
      <button class="btn btn-primary dashboard-entry-action" type="button" @click="goToActivityDashboard">
        前往看板
        <ArrowRight :size="18" />
      </button>
    </section>

    <section
      v-if="isReviewer"
      class="dashboard-entry review-entry"
      aria-labelledby="review-entry-title"
    >
      <div class="dashboard-entry-icon review-entry-icon">
        <ClipboardCheck :size="28" />
      </div>
      <div class="dashboard-entry-copy">
        <h2 id="review-entry-title">場地審核</h2>
        <p>進入 reviewer 專用的審核頁面，查看並處理場地借用申請。</p>
      </div>
      <button class="btn btn-primary dashboard-entry-action" type="button" @click="goToReviewPage">
        前往審核
        <ArrowRight :size="18" />
      </button>
    </section>

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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Activity, ArrowRight, Building2, ClipboardCheck } from "lucide-vue-next";
import { fetchAllUnits } from "@/api/venue";
import { useAuthSessionStore } from "@/stores/authSession";
import {
  UNIT_SELECTOR_DESCRIPTION,
  UNIT_SELECTOR_TITLE,
} from "@/utils/navigationLabels";

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

.dashboard-entry {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1.2rem 1.35rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.1);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-soft);
}

.dashboard-entry-icon {
  width: 3.25rem;
  height: 3.25rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 50%;
  background: rgba(46, 139, 87, 0.12);
  color: #247047;
}

.dashboard-entry-copy {
  flex: 1 1 auto;
  min-width: 0;
  text-align: left;

  h2 {
    color: var(--ink);
    font-size: var(--text-xl);
  }

  p {
    margin: 0.2rem 0 0;
    color: var(--muted);
  }
}

.dashboard-entry-action {
  flex: 0 0 auto;
}

.review-entry {
  border-color: rgba(36, 63, 107, 0.16);
  background: linear-gradient(135deg, rgba(243, 247, 252, 0.98), rgba(232, 240, 250, 0.96));
}

.review-entry-icon {
  background: rgba(36, 63, 107, 0.12);
  color: #243f6b;
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
  .dashboard-entry {
    align-items: stretch;
    flex-direction: column;
    text-align: left;
  }

  .dashboard-entry-action {
    width: 100%;
  }
}
</style>
