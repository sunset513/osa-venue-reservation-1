<template>
  <div class="selector-page page-enter">
    <header class="page-header">
      <button class="back-btn" @click="$router.push('/')">← {{ t("pages.venueSelector.backToUnits") }}</button>
      <h1>{{ t("pages.venueSelector.title") }}</h1>
      <p>{{ t("pages.venueSelector.description") }}</p>
    </header>

    <div v-if="loading" class="loading-state">{{ t("common.loading") }}</div>

    <div v-else class="selector-sections">
      <section class="selector-section">
        <div class="card-grid">
          <div
            v-for="venue in venues"
            :key="venue.id"
            class="select-card venue-card"
            @click="selectVenue(venue.id)"
          >
            <h3>{{ formatVenueDisplayName(venue.name, locale) }}</h3>
            <div class="venue-info">
              <span>👥 {{ t("pages.venueSelector.capacity", { count: venue.capacity }) }}</span>
            </div>
            <p class="description">{{ venue.description }}</p>
            <div class="card-footer">
              <span class="card-footer-icon" aria-hidden="true">
                <Calendar :size="16" />
              </span>
              <span>{{ t("pages.venueSelector.enterReservation") }}</span>
              <span class="card-footer-icon" aria-hidden="true">
                <ArrowRight :size="16" />
              </span>
            </div>
          </div>
        </div>
      </section>

      <section class="selector-section equipment-section">
        <div class="equipment-section-heading">
          <p class="section-kicker">{{ t("pages.venueSelector.or") }}</p>
          <h2 class="selector-section-title">{{ t("pages.venueSelector.standaloneTitle") }}</h2>
        </div>

        <div class="card-grid equipment-card-grid">
          <div class="select-card venue-card equipment-entry-card" @click="openEquipmentBorrowForm">
            <h3>{{ t("pages.venueSelector.equipmentRequest") }}</h3>
            <div class="venue-info">
              <span>{{ t("pages.venueSelector.notLinkedToVenue") }}</span>
            </div>
            <p class="description">{{ t("pages.venueSelector.equipmentDescription") }}</p>
            <div class="card-footer">
              <span class="card-footer-icon" aria-hidden="true">
                <Wrench :size="16" />
              </span>
              <span>{{ t("pages.venueSelector.enterEquipment") }}</span>
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
import { useI18n } from "vue-i18n";
import { ArrowRight, Calendar, Wrench } from "lucide-vue-next";
import { fetchVenuesByUnit } from "@/api/venue";
import { formatVenueDisplayName } from "@/utils/venueLabels";

const props = defineProps(["unitId"]);
const router = useRouter();
const { locale, t } = useI18n();
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
