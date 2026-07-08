<template>
  <div class="equipment-history-page page-enter">
    <header class="page-header equipment-history-header">
      <button class="back-btn" type="button" @click="router.push({ name: 'EquipmentStatus' })">
        返回設備狀態
      </button>
      <p class="hero-eyebrow">Equipment Records</p>
      <h1>我的設備借用記錄</h1>
      <p>查看設備借用申請狀態，待審核的獨立設備借用可在此修改。</p>
    </header>

    <section class="history-toolbar">
      <div>
        <span class="toolbar-label">{{ activeFilterLabel }}</span>
        <strong>{{ borrowPage.total }}</strong>
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
        <span v-if="activeFilters.equipmentName" class="filter-chip">
          設備：{{ activeFilters.equipmentName }}
        </span>
      </div>
      <button type="button" class="page-btn filter-clear-btn" @click="clearFilters">
        清除篩選
      </button>
    </section>

    <div v-if="loading" class="loading-state">載入設備借用記錄中...</div>

    <div v-else-if="loadError" class="empty-state history-feedback">
      <h3>無法載入設備借用記錄</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadBorrowHistory">重新載入</button>
    </div>

    <section v-else class="borrow-record-section">
      <div v-if="borrowPage.items.length === 0" class="empty-state history-feedback">
        <h3>目前沒有設備借用記錄</h3>
        <p>送出設備借用申請後，就可以在這裡追蹤狀態。</p>
      </div>

      <div v-else class="borrow-table-wrap">
        <table class="borrow-table">
          <thead>
            <tr>
              <th>借用編號</th>
              <th>設備</th>
              <th>關聯場地預約編號</th>
              <th>借用日期</th>
              <th>借用時段</th>
              <th>狀態</th>
              <th>用途</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in borrowPage.items" :key="record.id">
              <td>#{{ record.id }}</td>
              <td>
                <strong>{{ record.itemSummary }}</strong>
                <p v-if="record.relatedVenueName" class="row-subtle">
                  關聯場地：{{ record.relatedVenueName }}
                </p>
                <p v-else class="row-subtle">獨立設備借用</p>
              </td>
              <td>
                <span v-if="record.relatedVenueBookingId" class="venue-booking-id">
                  #{{ record.relatedVenueBookingId }}
                </span>
                <span v-else class="row-subtle">獨立借用</span>
              </td>
              <td>{{ formatDateLabel(record.borrowDate) }}</td>
              <td><span class="time-chip">{{ record.timeRange }}</span></td>
              <td>
                <span class="status-pill" :class="getEquipmentBookingStatusMeta(record.status).className">
                  {{ getEquipmentBookingStatusMeta(record.status).text }}
                </span>
              </td>
              <td>{{ record.purpose }}</td>
              <td>
                <button
                  v-if="getEditTarget(record) === 'equipment'"
                  type="button"
                  class="page-btn action-btn"
                  @click="openEquipmentEditModal(record)"
                >
                  修改
                </button>
                <button
                  v-else-if="getEditTarget(record) === 'venue'"
                  type="button"
                  class="page-btn action-btn"
                  @click="goToVenueBookingEdit(record)"
                >
                  至場地預約修改
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="pagination-bar">
        <button
          type="button"
          class="page-btn"
          :disabled="borrowPage.pageNo <= 1 || loading"
          @click="goToPage(borrowPage.pageNo - 1)"
        >
          上一頁
        </button>
        <span class="page-indicator">第 {{ borrowPage.pageNo }} / {{ displayTotalPages }} 頁</span>
        <button
          type="button"
          class="page-btn"
          :disabled="borrowPage.pageNo >= displayTotalPages || loading"
          @click="goToPage(borrowPage.pageNo + 1)"
        >
          下一頁
        </button>
      </footer>
    </section>

    <EquipmentBookingEditModal
      v-model:visible="isEditModalVisible"
      :booking="editingRecord"
      @success="handleEquipmentEditSuccess"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import EquipmentBookingEditModal from "@/components/equipment/EquipmentBookingEditModal.vue";
import { queryMyEquipmentBookings } from "@/api/equipment";
import {
  getEquipmentBookingEditTarget,
  getEquipmentBookingStatusMeta,
  normalizeEquipmentBookingPage,
} from "@/utils/equipment";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const loadError = ref("");
const pageNo = ref(1);
const pageSize = ref(10);
const borrowPage = ref(normalizeEquipmentBookingPage());
const editingRecord = ref(null);
const isEditModalVisible = ref(false);

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
  equipmentName: firstQueryValue(route.query.equipmentName) || "",
}));

const hasActiveFilters = computed(() => Boolean(activeFilters.value.equipmentId));

const activeFilterLabel = computed(() => (hasActiveFilters.value ? "篩選結果" : "全部記錄"));

const formatDateLabel = (value) => {
  if (!value) return "未提供日期";

  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(date);
};

const buildQueryPayload = () => {
  const payload = {
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  };

  if (activeFilters.value.equipmentId) {
    payload.equipmentId = activeFilters.value.equipmentId;
  }

  return payload;
};

const loadBorrowHistory = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    borrowPage.value = normalizeEquipmentBookingPage(
      await queryMyEquipmentBookings(buildQueryPayload()),
    );
  } catch (error) {
    console.error("載入設備借用記錄失敗:", error);
    loadError.value = error.message || "請稍後再試，或聯絡系統管理員。";
  } finally {
    loading.value = false;
  }
};

const clearFilters = async () => {
  pageNo.value = 1;
  await router.replace({ name: "EquipmentBorrowHistory" });
};

const goToPage = async (targetPage) => {
  pageNo.value = Math.min(Math.max(targetPage, 1), displayTotalPages.value);
  await loadBorrowHistory();
};

const getEditTarget = (record) => getEquipmentBookingEditTarget(record);

const openEquipmentEditModal = (record) => {
  if (getEditTarget(record) !== "equipment") return;

  editingRecord.value = record;
  isEditModalVisible.value = true;
};

const goToVenueBookingEdit = async (record) => {
  if (getEditTarget(record) !== "venue" || !record.relatedVenueBookingId) return;

  await router.push({
    name: "MyBookingHistory",
    query: { editBookingId: record.relatedVenueBookingId },
  });
};

const handleEquipmentEditSuccess = async () => {
  editingRecord.value = null;
  await loadBorrowHistory();
};

watch(pageSize, async () => {
  pageNo.value = 1;
  await loadBorrowHistory();
});

watch(
  () => route.query,
  async () => {
    pageNo.value = 1;
    await loadBorrowHistory();
  },
);

watch(isEditModalVisible, (visible) => {
  if (!visible) {
    editingRecord.value = null;
  }
});

onMounted(loadBorrowHistory);
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
  min-width: 1080px;
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

.row-subtle {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: var(--text-sm);
}

.venue-booking-id {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: rgba(var(--blue-900-rgb), 0.06);
  color: var(--ink);
  font-size: var(--text-sm);
  font-weight: 800;
  white-space: nowrap;
}

.time-chip {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: var(--text-sm);
  font-weight: 800;
  white-space: nowrap;
  background: var(--accent-soft);
  color: var(--accent);
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

.action-btn {
  white-space: nowrap;
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
