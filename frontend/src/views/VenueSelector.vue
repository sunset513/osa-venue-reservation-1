<template>
  <div class="selector-page page-enter">
    <header class="page-header">
      <button class="back-btn" @click="$router.push('/')">← 返回單位</button>
      <h1>選擇場地</h1>
      <p>點擊下方場地以查看預約狀況</p>
    </header>

    <div v-if="loading" class="loading-state">載入中...</div>

    <div v-else class="selector-sections">
      <section class="selector-section">
        <div class="card-grid">
          <div
            v-for="venue in venues"
            :key="venue.id"
            class="select-card venue-card"
            @click="selectVenue(venue.id)"
          >
            <h3>{{ normalizeVenueDisplayName(venue.name) }}</h3>
            <div class="venue-info">
              <span>👥 容納人數: {{ venue.capacity }}</span>
            </div>
            <p class="description">{{ venue.description }}</p>
            <div class="card-footer">
              <span class="card-footer-icon" aria-hidden="true">
                <Calendar :size="16" />
              </span>
              <span>進入預約系統</span>
              <span class="card-footer-icon" aria-hidden="true">
                <ArrowRight :size="16" />
              </span>
            </div>
          </div>
        </div>
      </section>

      <section class="selector-section equipment-section">
        <div class="equipment-section-heading">
          <p class="section-kicker">或是</p>
          <h2 class="selector-section-title">單獨借用設備</h2>
        </div>

        <div class="card-grid equipment-card-grid">
          <div class="select-card venue-card equipment-entry-card" @click="openEquipmentBorrowForm">
            <h3>設備借用申請</h3>
            <div class="venue-info">
              <span>不綁定場地預約</span>
            </div>
            <p class="description">若只需要借用設備，可直接填寫設備借用申請。</p>
            <div class="card-footer">
              <span class="card-footer-icon" aria-hidden="true">
                <Wrench :size="16" />
              </span>
              <span>進入設備借用</span>
              <span class="card-footer-icon" aria-hidden="true">
                <ArrowRight :size="16" />
              </span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ArrowRight, Calendar, Wrench } from "lucide-vue-next";
import { fetchVenuesByUnit } from "@/api/venue";
import { normalizeVenueDisplayName } from "@/utils/venueLabels";

const props = defineProps(["unitId"]);
const router = useRouter();
const venues = ref([]);
const loading = ref(true);

onMounted(async () => {
  try {
    venues.value = await fetchVenuesByUnit(props.unitId);
  } finally {
    loading.value = false;
  }
});

const selectVenue = (id) => {
  router.push(`/venue/${id}`);
};

const openEquipmentBorrowForm = () => {
  // Route standalone equipment borrowing away from a venue-specific calendar,
  // because these requests intentionally submit without relatedVenueBookingId.
  router.push({ name: "EquipmentBorrowForm" });
};
</script>

<style lang="scss" scoped>
@use "@/assets/styles/selector-common.scss"; // 建議將卡片樣式抽離共用

.selector-sections {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.selector-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.equipment-section {
  padding-top: 0.5rem;
}

.equipment-section-heading {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  align-items: center;
  text-align: center;
}

.section-kicker {
  margin: 0;
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.selector-section-title {
  color: var(--ink);
}

.card-footer {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.card-footer-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.equipment-card-grid {
  grid-template-columns: minmax(280px, 420px);
}

@media (max-width: 560px) {
  .selector-sections {
    gap: 2rem;
  }

  .equipment-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
