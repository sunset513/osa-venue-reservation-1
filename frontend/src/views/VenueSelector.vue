<template>
  <div class="selector-page">
    <header class="page-header">
      <button class="back-btn" @click="$router.push('/')">← 返回單位</button>
      <h1>選擇場地</h1>
      <p>點擊下方場地以查看預約狀況</p>
    </header>

    <div v-if="loading" class="loading-state">載入中...</div>

    <div v-else class="card-grid">
      <div
        v-for="venue in venues"
        :key="venue.id"
        class="select-card venue-card"
        @click="selectVenue(venue.id)"
      >
        <h3>{{ venue.name }}</h3>
        <div class="venue-info">
          <span>👥 容納人數: {{ venue.capacity }}</span>
        </div>
        <p class="description">{{ venue.description }}</p>
        <div class="card-footer">進入預約系統 →</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchVenuesByUnit } from "@/api/venue";

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
</script>

<style lang="scss" scoped>
@use "@/assets/styles/selector-common.scss"; // 建議將卡片樣式抽離共用
</style>
