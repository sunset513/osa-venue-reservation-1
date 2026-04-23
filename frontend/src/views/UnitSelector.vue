<template>
  <div class="selector-page page-enter">
    <header class="page-header">
      <h1>{{ UNIT_SELECTOR_TITLE }}</h1>
      <p>{{ UNIT_SELECTOR_DESCRIPTION }}</p>
    </header>

    <div v-if="loading" class="loading-state">載入中...</div>

    <div v-else class="card-grid">
      <div
        v-for="unit in units"
        :key="unit.id"
        class="select-card"
        :class="{ 'is-disabled': unit.disabled }"
        @click="selectUnit(unit)"
      >
        <span v-if="unit.disabled" class="dev-badge">待開發</span>

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
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { Building2 } from "lucide-vue-next";
import { fetchAllUnits } from "@/api/venue";
import {
  UNIT_SELECTOR_DESCRIPTION,
  UNIT_SELECTOR_TITLE,
} from "@/utils/navigationLabels";

const router = useRouter();
const units = ref([]);
const loading = ref(true);

onMounted(async () => {
  try {
    // 1. 取得後端真實資料 (學務處本部)
    const fetchedUnits = await fetchAllUnits();

    // 將真實資料標記為可用
    const activeUnits = fetchedUnits.map((u) => ({ ...u, disabled: false }));

    // 2. 建立佔位單位
    const placeholderUnits = [
      { id: "dev-1", name: "住宿服務組", code: "HSD", disabled: true },
      { id: "dev-2", name: "課外活動組", code: "EAD", disabled: true },
    ];

    // 3. 合併陣列
    units.value = [...activeUnits, ...placeholderUnits];
  } finally {
    loading.value = false;
  }
});

// 修改傳入參數為整個 unit 物件，方便判斷
const selectUnit = (unit) => {
  // 如果是待開發單位，直接 return 阻擋跳轉
  if (unit.disabled) return;
  router.push(`/unit/${unit.id}`);
};
</script>

<style lang="scss" scoped>
// 將 @import 改為 @use 解決警告
@use "@/assets/styles/selector-common.scss";

/* 針對待開發單位的專屬樣式 */
.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
  position: relative;
  background-color: rgba(var(--blue-900-rgb), 0.02); /* 稍微深一點的背景色 */

  /* 覆蓋共用樣式的 hover 效果，防止浮動和變色 */
  &:hover {
    transform: none;
    box-shadow: var(--shadow-soft);
    border-color: rgba(var(--blue-900-rgb), 0.08);
  }
}

/* 右上角待開發標籤 */
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
</style>
