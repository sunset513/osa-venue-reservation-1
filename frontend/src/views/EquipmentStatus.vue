<template>
  <div class="equipment-page page-enter">
    <header class="page-header equipment-header">
      <button class="back-btn" type="button" @click="router.push('/')">
        <ArrowLeft :size="16" aria-hidden="true" />
        {{ BACK_TO_UNIT_SELECTOR_LABEL }}
      </button>
      <p class="hero-eyebrow">Equipment Status</p>
      <h1>設備狀態管理</h1>
      <p>查看指定日期與時段的設備借用狀態，並快速前往借用紀錄、編輯設備資料或刪除設備。</p>
    </header>

    <section class="status-toolbar card">
      <label>
        查詢日期
        <input v-model="queryDate" type="date" />
      </label>
      <label>
        查詢時段
        <select v-model.number="queryHour">
          <option v-for="hour in hourOptions" :key="hour" :value="hour">
            {{ String(hour).padStart(2, "0") }}:00
          </option>
        </select>
      </label>
      <button type="button" class="btn btn-secondary" @click="loadStatuses">
        <span class="btn-icon" aria-hidden="true">
          <RefreshCw :size="16" />
        </span>
        <span>重新查詢</span>
      </button>
      <button type="button" class="btn btn-secondary admin-create-btn" @click="openCreateModal">
        <span class="btn-icon" aria-hidden="true">
          <PackagePlus :size="16" />
        </span>
        <span>創建設備</span>
      </button>
      <button
        type="button"
        class="btn btn-secondary route-borrow-link"
        @click="router.push({ name: 'EquipmentBorrowForm' })"
      >
        <span class="btn-icon" aria-hidden="true">
          <ArrowRight :size="16" />
        </span>
        <span>前往設備借用</span>
      </button>
    </section>

    <div v-if="loading" class="loading-state">載入設備狀態中...</div>

    <div v-else-if="loadError" class="empty-state equipment-feedback">
      <h3>目前無法載入設備狀態</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadStatuses">
        <span class="btn-icon" aria-hidden="true">
          <RefreshCw :size="16" />
        </span>
        <span>重新載入</span>
      </button>
    </div>

    <div v-else class="equipment-content">
      <section class="equipment-table-wrap">
        <table class="equipment-table">
          <thead>
            <tr>
              <th>設備名稱</th>
              <th>總數量</th>
              <th>借出數量</th>
              <th>可用數量</th>
              <th>狀態</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="equipment in equipmentStatuses" :key="equipment.equipmentId">
              <tr>
                <td>
                  <button
                    type="button"
                    class="row-title-button"
                    :disabled="equipment.activeBookings.length === 0"
                    @click="toggleExpanded(equipment.equipmentId)"
                  >
                    <strong>{{ equipment.equipmentName }}</strong>
                  </button>
                </td>
                <td>{{ equipment.totalQuantity }}</td>
                <td>{{ equipment.borrowedQuantity }}</td>
                <td>{{ equipment.availableQuantity }}</td>
                <td>
                  <span class="status-pill" :class="getEquipmentStatusMeta(equipment.isInUse).className">
                    {{ getEquipmentStatusMeta(equipment.isInUse).text }}
                  </span>
                </td>
                <td>
                  <div class="action-buttons">
                    <RouterLink class="history-url-link" :to="borrowHistoryRoute(equipment)">
                      <History :size="15" aria-hidden="true" />
                      <span>借用紀錄</span>
                    </RouterLink>
                    <button type="button" class="action-btn" @click="openEditModal(equipment)">
                      <PencilLine :size="15" aria-hidden="true" />
                      <span>編輯</span>
                    </button>
                    <button
                      type="button"
                      class="action-btn action-btn-danger"
                      :disabled="deletingEquipmentId === equipment.equipmentId"
                      @click="handleDeleteEquipment(equipment)"
                    >
                      <Trash2 :size="15" aria-hidden="true" />
                      <span>{{ deletingEquipmentId === equipment.equipmentId ? "刪除中..." : "刪除" }}</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="isExpanded(equipment.equipmentId)" class="active-booking-row">
                <td colspan="6">
                  <div class="active-booking-list">
                    <article
                      v-for="booking in equipment.activeBookings"
                      :key="booking.equipmentBookingId"
                      class="active-booking-card"
                    >
                      <strong>#{{ booking.equipmentBookingId }} {{ booking.purpose }}</strong>
                      <span>{{ booking.borrowDate }} {{ formatSlotGroupsAsTimeRange(booking.slots) }}</span>
                      <span>借用數量 {{ booking.quantity }} / 申請人 {{ booking.userId }}</span>
                      <span v-if="booking.relatedVenueName">關聯場地：{{ booking.relatedVenueName }}</span>
                      <span>{{ booking.contact.name || "未提供姓名" }} / {{ booking.contact.phone || "未提供電話" }}</span>
                    </article>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </section>
    </div>

    <EquipmentMasterEditModal
      v-model:visible="isEditModalVisible"
      :equipment-id="editingEquipmentId"
      :equipment-name="editingEquipmentName"
      @saved="handleEquipmentSaved"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowRight,
  ArrowLeft,
  History,
  PackagePlus,
  PencilLine,
  RefreshCw,
  Trash2,
} from "lucide-vue-next";
import EquipmentMasterEditModal from "@/components/equipment/EquipmentMasterEditModal.vue";
import { deleteEquipment, getEquipmentStatuses } from "@/api/equipment";
import { getEquipmentStatusMeta, normalizeEquipmentStatuses } from "@/utils/equipment";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { BACK_TO_UNIT_SELECTOR_LABEL } from "@/utils/navigationLabels";
import { useToast } from "@/utils/useToast";

const router = useRouter();
const { success, warning } = useToast();

const loading = ref(true);
const loadError = ref("");
const equipmentStatuses = ref([]);
const expandedEquipmentIds = ref(new Set());
const queryDate = ref(new Date().toLocaleDateString("sv-SE"));
const queryHour = ref(new Date().getHours());
const hourOptions = Array.from({ length: 24 }, (_, hour) => hour);
const isEditModalVisible = ref(false);
const editingEquipmentId = ref(null);
const editingEquipmentName = ref("");
const deletingEquipmentId = ref(null);

const isExpanded = (equipmentId) => expandedEquipmentIds.value.has(equipmentId);

const toggleExpanded = (equipmentId) => {
  const nextExpandedIds = new Set(expandedEquipmentIds.value);
  if (nextExpandedIds.has(equipmentId)) {
    nextExpandedIds.delete(equipmentId);
  } else {
    nextExpandedIds.add(equipmentId);
  }
  expandedEquipmentIds.value = nextExpandedIds;
};

const borrowHistoryRoute = (equipment) => ({
  name: "EquipmentBorrowHistory",
  query: {
    equipmentId: equipment.equipmentId,
    equipmentName: equipment.equipmentName,
  },
});

const openCreateModal = () => {
  editingEquipmentId.value = null;
  editingEquipmentName.value = "";
  isEditModalVisible.value = true;
};

const openEditModal = (equipment) => {
  editingEquipmentId.value = equipment.equipmentId;
  editingEquipmentName.value = equipment.equipmentName || "";
  isEditModalVisible.value = true;
};

const handleDeleteEquipment = async (equipment) => {
  const equipmentId = equipment?.equipmentId;
  const equipmentName = equipment?.equipmentName || "這項設備";

  if (!equipmentId || deletingEquipmentId.value === equipmentId) return;

  const confirmed = window.confirm(`確定要刪除「${equipmentName}」嗎？刪除後會從設備清單中移除。`);
  if (!confirmed) return;

  deletingEquipmentId.value = equipmentId;

  try {
    await deleteEquipment(equipmentId);

    if (editingEquipmentId.value === equipmentId) {
      isEditModalVisible.value = false;
      editingEquipmentId.value = null;
      editingEquipmentName.value = "";
    }

    success(`已刪除設備「${equipmentName}」。`);
    await loadStatuses();
  } catch (deleteError) {
    warning(deleteError.message || "刪除設備失敗，若仍有未來借用紀錄，請先處理相關申請。");
  } finally {
    deletingEquipmentId.value = null;
  }
};

const handleEquipmentSaved = async () => {
  await loadStatuses();
};

const loadStatuses = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    equipmentStatuses.value = normalizeEquipmentStatuses(
      await getEquipmentStatuses({
        date: queryDate.value,
        hour: queryHour.value,
      }),
    );
  } catch (error) {
    console.error("Failed to load equipment statuses", error);
    equipmentStatuses.value = [];
    loadError.value = error.message || "Unable to load equipment statuses right now.";
  } finally {
    loading.value = false;
  }
};

onMounted(loadStatuses);
</script>

<style lang="scss" scoped>
.equipment-page {
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.equipment-header {
  margin-bottom: 0;
}

.hero-eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  text-transform: uppercase;
}

.equipment-header p:last-child {
  margin: 0;
  max-width: 52rem;
  color: var(--muted);
  line-height: 1.6;
}

.status-toolbar {
  display: flex;
  align-items: end;
  gap: 0.85rem;
  padding: 1rem;
  flex-wrap: wrap;

  label {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 800;
  }

  input,
  select {
    min-height: 2.35rem;
    padding: 0.4rem 0.65rem;
    border: 1px solid var(--line-strong);
    border-radius: 8px;
    background: #ffffff;
  }
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.admin-create-btn {
  border: 1px solid rgba(36, 63, 107, 0.16);
  background: linear-gradient(135deg, rgba(243, 247, 252, 0.98), rgba(232, 240, 250, 0.96));
  color: #243f6b;
  box-shadow: 0 10px 22px rgba(36, 63, 107, 0.12);

  &:hover:not(:disabled) {
    border-color: rgba(36, 63, 107, 0.28);
    box-shadow: 0 12px 26px rgba(36, 63, 107, 0.18);
    transform: translateY(-1px);
  }
}

.route-borrow-link {
  flex-shrink: 0;
  margin-left: auto;
}

.equipment-content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.equipment-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--card);
}

.equipment-table {
  width: 100%;
  min-width: 840px;
  border-collapse: collapse;

  th,
  td {
    padding: 1rem 1.1rem;
    text-align: left;
    vertical-align: middle;
    border-bottom: 1px solid rgba(var(--blue-900-rgb), 0.08);
  }

  th {
    color: var(--ink);
    font-size: var(--text-base);
    font-weight: 800;
    background: var(--surface-muted);
  }
}

.row-title-button {
  display: inline-flex;
  align-items: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink);
  cursor: pointer;

  &:disabled {
    cursor: default;
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 4.75rem;
  min-height: 2rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  font-size: var(--text-sm);
  font-weight: 800;

  &.is-in-use {
    background: var(--surface-muted);
    color: var(--status-occupied);
  }

  &.is-idle {
    background: rgba(46, 139, 87, 0.12);
    color: var(--status-approved);
  }
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.7rem;
  width: 100%;
}

.history-url-link,
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
}

.history-url-link {
  margin-right: auto;
  text-decoration: underline;
  text-underline-offset: 0.18rem;
}

.action-btn {
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.14);
  border-radius: 999px;
  background: #ffffff;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    color 0.2s ease;

  &:hover:not(:disabled) {
    border-color: rgba(var(--blue-900-rgb), 0.24);
    background: rgba(var(--blue-900-rgb), 0.03);
    box-shadow: 0 6px 16px rgba(var(--blue-900-rgb), 0.08);
  }

  &:disabled {
    opacity: 0.55;
    cursor: wait;
  }
}

.action-btn-danger {
  color: var(--danger);
  border-color: rgba(196, 69, 69, 0.26);
  background: rgba(196, 69, 69, 0.04);

  &:hover:not(:disabled) {
    border-color: rgba(196, 69, 69, 0.42);
    background: rgba(196, 69, 69, 0.08);
    box-shadow: 0 6px 16px rgba(196, 69, 69, 0.12);
  }
}

.active-booking-row td {
  background: #fbfcfe;
}

.active-booking-list {
  display: grid;
  gap: 0.65rem;
}

.active-booking-card {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  padding: 0.75rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
  color: var(--muted-strong);
  font-size: var(--text-sm);

  strong {
    color: var(--ink);
  }
}

.equipment-feedback {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
  text-align: center;
}

@media (max-width: 760px) {
  .status-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .action-buttons {
    flex-direction: column;
    align-items: flex-end;
  }

  .route-borrow-link {
    margin-left: 0;
  }

  .history-url-link {
    margin-right: 0;
  }
}
</style>
