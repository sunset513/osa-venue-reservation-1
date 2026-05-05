<template>
  <div class="equipment-history-page page-enter">
    <header class="page-header equipment-history-header">
      <button class="back-btn" type="button" @click="router.push({ name: 'EquipmentStatus' })">
        ← 返回設備狀態管理
      </button>
      <p class="hero-eyebrow">Equipment Records</p>
      <h1>設備借用記錄</h1>
      <p>查看已通過預約中的設備借用紀錄，包含借用場地、日期、時段與用途。</p>
    </header>

    <section class="history-toolbar">
      <div>
        <span class="toolbar-label">{{ activeFilterLabel }}</span>
        <strong>{{ borrowPage.totalCount }}</strong>
      </div>
      <label class="page-size-control">
        每頁
        <select v-model.number="pageSize">
          <option :value="10">10 筆</option>
          <option :value="20">20 筆</option>
          <option :value="50">50 筆</option>
        </select>
      </label>
    </section>

    <section v-if="hasActiveFilters" class="active-filter-bar">
      <div class="filter-chips">
        <span v-if="activeFilters.venueName" class="filter-chip">
          場地：{{ activeFilters.venueName }}
        </span>
        <span v-if="activeFilters.equipmentName" class="filter-chip">
          設備：{{ activeFilters.equipmentName }}
        </span>
      </div>
      <button type="button" class="page-btn filter-clear-btn" @click="clearFilters">
        全部記錄
      </button>
    </section>

    <div v-if="loading" class="loading-state">載入設備借用記錄中...</div>

    <div v-else-if="loadError" class="empty-state history-feedback">
      <h3>目前無法載入借用記錄</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadBorrowHistory">
        重新載入
      </button>
    </div>

    <section v-else class="borrow-record-section">
      <div v-if="borrowPage.data.length === 0" class="empty-state history-feedback">
        <h3>目前沒有設備借用記錄</h3>
        <p>已通過並包含設備的預約會顯示在這裡。</p>
      </div>

      <div v-else class="borrow-table-wrap">
        <table class="borrow-table">
          <thead>
            <tr>
              <th>場地</th>
              <th>設備名稱</th>
              <th>借用日期</th>
              <th>借用時段</th>
              <th>用途</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in borrowPage.data" :key="recordKey(record)">
              <td>
                <strong>{{ record.venueName }}</strong>
              </td>
              <td>{{ record.equipmentName }}</td>
              <td>{{ formatDateLabel(record.borrowDate) }}</td>
              <td>
                <span class="time-chip">{{ record.timeSlots }}</span>
              </td>
              <td>{{ record.purpose }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="pagination-bar">
        <button
          type="button"
          class="page-btn"
          :disabled="borrowPage.currentPage <= 1 || loading"
          @click="goToPage(borrowPage.currentPage - 1)"
        >
          上一頁
        </button>
        <span class="page-indicator">
          第 {{ borrowPage.currentPage }} / {{ displayTotalPages }} 頁
        </span>
        <button
          type="button"
          class="page-btn"
          :disabled="borrowPage.currentPage >= displayTotalPages || loading"
          @click="goToPage(borrowPage.currentPage + 1)"
        >
          下一頁
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchEquipmentBorrowHistory } from "@/api/equipment";
import { normalizeEquipmentBorrowPage } from "@/utils/equipment";

const route = useRoute();
const router = useRouter();

const FILTER_FETCH_PAGE_SIZE = 100;

const loading = ref(true);
const loadError = ref("");
const pageNum = ref(1);
const pageSize = ref(10);
const borrowPage = ref(normalizeEquipmentBorrowPage());

const displayTotalPages = computed(() => Math.max(borrowPage.value.totalPages, 1));

const firstQueryValue = (value) => {
  if (Array.isArray(value)) return value[0];
  return value;
};

const toQueryNumber = (value) => {
  const numberValue = Number(firstQueryValue(value));
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined;
};

const activeFilters = computed(() => ({
  equipmentId: toQueryNumber(route.query.equipmentId),
  venueId: toQueryNumber(route.query.venueId),
  equipmentName: firstQueryValue(route.query.equipmentName) || "",
  venueName: firstQueryValue(route.query.venueName) || "",
}));

const hasActiveFilters = computed(() => Boolean(activeFilters.value.equipmentId || activeFilters.value.venueId));

const activeFilterLabel = computed(() => (hasActiveFilters.value ? "篩選筆數" : "總筆數"));

const formatDateLabel = (value) => {
  if (!value) return "未提供日期";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(date);
};

const recordKey = (record) => {
  return [
    record.venueId,
    record.equipmentId,
    record.borrowDate,
    record.timeSlots,
    record.purpose,
  ].join("-");
};

const recordMatchesFilters = (record) => {
  const { equipmentId, venueId } = activeFilters.value;
  const matchesEquipment = !equipmentId || Number(record.equipmentId) === equipmentId;
  const matchesVenue = !venueId || Number(record.venueId) === venueId;

  return matchesEquipment && matchesVenue;
};

const buildLocalPage = (records) => {
  const totalCount = records.length;
  const totalPages = Math.ceil(totalCount / pageSize.value);
  const currentPage = Math.min(pageNum.value, Math.max(totalPages, 1));
  const startIndex = (currentPage - 1) * pageSize.value;

  pageNum.value = currentPage;

  return {
    totalCount,
    totalPages,
    currentPage,
    pageSize: pageSize.value,
    data: records.slice(startIndex, startIndex + pageSize.value),
  };
};

const loadFilteredBorrowHistory = async () => {
  const firstPage = normalizeEquipmentBorrowPage(
    await fetchEquipmentBorrowHistory({
      pageNum: 1,
      pageSize: FILTER_FETCH_PAGE_SIZE,
    }),
  );

  const remainingPageNumbers = Array.from(
    { length: Math.max(firstPage.totalPages - 1, 0) },
    (_, index) => index + 2,
  );

  const remainingPages = await Promise.all(
    remainingPageNumbers.map((targetPage) =>
      fetchEquipmentBorrowHistory({
        pageNum: targetPage,
        pageSize: FILTER_FETCH_PAGE_SIZE,
      }).then(normalizeEquipmentBorrowPage),
    ),
  );

  const records = [firstPage, ...remainingPages]
    .flatMap((page) => page.data)
    .filter(recordMatchesFilters);

  borrowPage.value = buildLocalPage(records);
};

const loadBorrowHistory = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    if (hasActiveFilters.value) {
      await loadFilteredBorrowHistory();
      return;
    }

    const result = await fetchEquipmentBorrowHistory({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    borrowPage.value = normalizeEquipmentBorrowPage(result);
  } catch (error) {
    console.error("載入設備借用記錄失敗:", error);
    loadError.value = error.message || "請稍後再試一次。";
  } finally {
    loading.value = false;
  }
};

const clearFilters = async () => {
  pageNum.value = 1;
  await router.replace({ name: "EquipmentBorrowHistory" });
};

const goToPage = async (targetPage) => {
  pageNum.value = Math.min(Math.max(targetPage, 1), displayTotalPages.value);
  await loadBorrowHistory();
};

watch(pageSize, async () => {
  pageNum.value = 1;
  await loadBorrowHistory();
});

watch(
  () => route.query,
  async () => {
    pageNum.value = 1;
    await loadBorrowHistory();
  },
);

onMounted(() => {
  loadBorrowHistory();
});
</script>

<style lang="scss" scoped>
.equipment-history-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.equipment-history-header {
  margin-bottom: 0;
}

.hero-eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  text-transform: uppercase;
}

.history-toolbar,
.pagination-bar,
.active-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.2rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.1);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-soft);
}

.toolbar-label {
  display: block;
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.history-toolbar strong {
  color: var(--ink);
  font-size: var(--text-2xl);
}

.filter-chips {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
}

.filter-clear-btn {
  white-space: nowrap;
}

.page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  color: var(--muted-strong);
  font-weight: 700;

  select {
    min-height: 2.35rem;
    padding: 0.35rem 0.7rem;
    border: 1px solid var(--line-strong);
    border-radius: 8px;
    background: #ffffff;
  }
}

.history-feedback {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
  text-align: center;
}

.borrow-record-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.borrow-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-soft);
}

.borrow-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;

  th,
  td {
    padding: 1rem 1.15rem;
    text-align: left;
    border-bottom: 1px solid rgba(var(--blue-900-rgb), 0.08);
    vertical-align: middle;
  }

  th {
    color: var(--ink);
    font-weight: 800;
    background: var(--surface-muted);
  }

  tr:last-child td {
    border-bottom: 0;
  }
}

.time-chip {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  white-space: nowrap;
}

.page-btn {
  min-width: 5rem;
  min-height: 2.35rem;
  padding: 0.45rem 0.8rem;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: #ffffff;
  color: var(--accent);
  font-weight: 800;
  cursor: pointer;

  &:disabled {
    color: var(--muted);
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.page-indicator {
  color: var(--muted-strong);
  font-weight: 800;
}

@media (max-width: 720px) {
  .history-toolbar,
  .pagination-bar,
  .active-filter-bar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
