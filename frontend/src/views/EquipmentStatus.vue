<template>
  <div class="equipment-page page-enter">
    <header class="page-header equipment-header">
      <button class="back-btn" type="button" @click="router.push('/')">
        ← {{ BACK_TO_UNIT_SELECTOR_LABEL }}
      </button>
      <p class="hero-eyebrow">Equipment Status</p>
      <h1>設備狀態管理</h1>
      <p>查看各場地設備數量與當前使用狀態，直接在資料列中編輯設備資訊。</p>
    </header>

    <div v-if="loading" class="loading-state">載入設備資料中...</div>

    <div v-else-if="loadError" class="empty-state equipment-feedback">
      <h3>目前無法載入設備資料</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadEquipmentGroups">
        重新載入
      </button>
    </div>

    <div v-else class="equipment-sections">
      <section
        v-for="group in equipmentGroups"
        :key="group.venueName"
        class="venue-equipment-section"
      >
        <div class="section-heading">
          <div>
            <p class="section-eyebrow">Venue</p>
            <h2>{{ group.venueName }}</h2>
          </div>
          <button
            type="button"
            class="btn btn-secondary compact-btn"
            @click="startCreate(group)"
          >
            <Plus :size="16" aria-hidden="true" />
            新增設備
          </button>
        </div>

        <div class="equipment-table-wrap">
          <table class="equipment-table">
            <thead>
              <tr>
                <th>設備名稱</th>
                <th>數量</th>
                <th>使用狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="creatingVenueName === group.venueName" class="is-editing">
                <td>
                  <input
                    v-model.trim="createForm.equipmentName"
                    class="table-input"
                    type="text"
                    placeholder="輸入設備名稱"
                  />
                </td>
                <td>
                  <input
                    v-model.number="createForm.quantity"
                    class="table-input is-number"
                    type="number"
                    min="1"
                  />
                </td>
                <td>
                  <span class="status-pill is-idle">新設備</span>
                </td>
                <td>
                  <div class="action-group">
                    <button
                      type="button"
                      class="row-action is-save"
                      :disabled="savingId === 'create'"
                      @click="saveCreate(group)"
                    >
                      儲存
                    </button>
                    <button type="button" class="row-action is-muted" @click="cancelCreate">
                      取消
                    </button>
                  </div>
                </td>
              </tr>

              <tr v-if="group.equipmentList.length === 0">
                <td colspan="4" class="empty-row">這個場地目前沒有設備資料。</td>
              </tr>

              <tr
                v-for="equipment in group.equipmentList"
                :key="equipment.equipmentId"
                :class="{ 'is-editing': editingId === equipment.equipmentId }"
              >
                <td>
                  <input
                    v-if="editingId === equipment.equipmentId"
                    v-model.trim="editForm.equipmentName"
                    class="table-input"
                    type="text"
                  />
                  <strong v-else>{{ equipment.equipmentName }}</strong>
                </td>
                <td>
                  <input
                    v-if="editingId === equipment.equipmentId"
                    v-model.number="editForm.quantity"
                    class="table-input is-number"
                    type="number"
                    min="1"
                  />
                  <span v-else>{{ equipment.quantity }}</span>
                </td>
                <td>
                  <span
                    class="status-pill"
                    :class="getEquipmentStatusMeta(equipment.isInUse).className"
                  >
                    {{ getEquipmentStatusMeta(equipment.isInUse).text }}
                  </span>
                </td>
                <td>
                  <div class="action-group">
                    <template v-if="editingId === equipment.equipmentId">
                      <button
                        type="button"
                        class="row-action is-save"
                        :disabled="savingId === equipment.equipmentId"
                        @click="saveEdit(equipment)"
                      >
                        儲存
                      </button>
                      <button type="button" class="row-action is-muted" @click="cancelEdit">
                        取消
                      </button>
                    </template>
                    <template v-else>
                      <button
                        type="button"
                        class="row-action is-danger"
                        :disabled="savingId === equipment.equipmentId"
                        @click="removeEquipment(equipment)"
                      >
                        刪除
                      </button>
                      <button
                        type="button"
                        class="row-action is-edit"
                        @click="startEdit(equipment)"
                      >
                        編輯
                      </button>
                    </template>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { Plus } from "lucide-vue-next";
import { useRouter } from "vue-router";
import {
  createEquipment,
  deleteEquipment,
  fetchEquipmentGroups,
  updateEquipment,
} from "@/api/equipment";
import {
  buildEquipmentPayload,
  getEquipmentStatusMeta,
  normalizeEquipmentGroups,
} from "@/utils/equipment";
import { BACK_TO_UNIT_SELECTOR_LABEL } from "@/utils/navigationLabels";
import { useToast } from "@/utils/useToast";

const router = useRouter();
const toast = useToast();

const loading = ref(true);
const loadError = ref("");
const equipmentGroups = ref([]);
const editingId = ref(null);
const savingId = ref(null);
const creatingVenueName = ref("");

const editForm = ref({
  equipmentName: "",
  venueId: null,
  quantity: 1,
});

const createForm = ref({
  equipmentName: "",
  venueId: null,
  quantity: 1,
});

const loadEquipmentGroups = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    const groups = await fetchEquipmentGroups();
    equipmentGroups.value = normalizeEquipmentGroups(groups);
  } catch (error) {
    console.error("載入設備資料失敗:", error);
    loadError.value = error.message || "請稍後再試一次。";
  } finally {
    loading.value = false;
  }
};

const startEdit = (equipment) => {
  cancelCreate();
  editingId.value = equipment.equipmentId;
  editForm.value = {
    equipmentName: equipment.equipmentName,
    venueId: equipment.venueId,
    quantity: equipment.quantity,
  };
};

const cancelEdit = () => {
  editingId.value = null;
  editForm.value = {
    equipmentName: "",
    venueId: null,
    quantity: 1,
  };
};

const saveEdit = async (equipment) => {
  const payload = buildEquipmentPayload(editForm.value);

  if (!payload.equipmentName || !payload.venueId) {
    toast.warning("請填寫設備名稱與場地資訊");
    return;
  }

  savingId.value = equipment.equipmentId;

  try {
    await updateEquipment(equipment.equipmentId, payload);
    toast.success("設備資料已更新");
    cancelEdit();
    await loadEquipmentGroups();
  } catch (error) {
    console.error("更新設備失敗:", error);
    toast.error(error.message || "設備更新失敗");
  } finally {
    savingId.value = null;
  }
};

const startCreate = (group) => {
  cancelEdit();
  const venueId = group.equipmentList[0]?.venueId || null;
  creatingVenueName.value = group.venueName;
  createForm.value = {
    equipmentName: "",
    venueId,
    quantity: 1,
  };
};

const cancelCreate = () => {
  creatingVenueName.value = "";
  createForm.value = {
    equipmentName: "",
    venueId: null,
    quantity: 1,
  };
};

const saveCreate = async () => {
  const payload = buildEquipmentPayload(createForm.value);

  if (!payload.equipmentName || !payload.venueId) {
    toast.warning("請填寫設備名稱；目前無法判斷場地時請先確認該場地已有設備資料");
    return;
  }

  savingId.value = "create";

  try {
    await createEquipment(payload);
    toast.success("設備已新增");
    cancelCreate();
    await loadEquipmentGroups();
  } catch (error) {
    console.error("新增設備失敗:", error);
    toast.error(error.message || "設備新增失敗");
  } finally {
    savingId.value = null;
  }
};

const removeEquipment = async (equipment) => {
  const confirmed = window.confirm(`確定要刪除「${equipment.equipmentName}」嗎？`);
  if (!confirmed) return;

  savingId.value = equipment.equipmentId;

  try {
    await deleteEquipment(equipment.equipmentId);
    toast.success("設備已刪除");
    await loadEquipmentGroups();
  } catch (error) {
    console.error("刪除設備失敗:", error);
    toast.error(error.message || "設備刪除失敗");
  } finally {
    savingId.value = null;
  }
};

onMounted(() => {
  loadEquipmentGroups();
});
</script>

<style lang="scss" scoped>
.equipment-page {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.equipment-header {
  margin-bottom: 0;
}

.hero-eyebrow,
.section-eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  text-transform: uppercase;
}

.equipment-feedback {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
  text-align: center;
}

.equipment-sections {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.venue-equipment-section {
  padding: 1.35rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.12);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-soft);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;

  h2 {
    color: var(--ink);
    font-size: var(--text-xl);
  }
}

.compact-btn {
  min-height: 2.35rem;
  padding: 0.55rem 0.9rem;
  font-size: var(--text-sm);
}

.equipment-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--card);
}

.equipment-table {
  width: 100%;
  min-width: 680px;
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

  td {
    color: var(--text);
    font-size: var(--text-base);
  }

  tr:last-child td {
    border-bottom: 0;
  }

  tr.is-editing td {
    background: var(--accent-soft);
  }
}

.table-input {
  width: min(100%, 15rem);
  min-height: 2.35rem;
  padding: 0.45rem 0.7rem;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: #ffffff;
  color: var(--text);

  &.is-number {
    width: 5.5rem;
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

.action-group {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.row-action {
  min-width: 3.75rem;
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: var(--text-sm);
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;

  &:hover {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.58;
    cursor: not-allowed;
    transform: none;
  }

  &.is-danger {
    background: rgba(196, 69, 69, 0.1);
    color: var(--danger);
    border-color: rgba(196, 69, 69, 0.22);
  }

  &.is-edit,
  &.is-save {
    background: rgba(39, 94, 168, 0.1);
    color: var(--accent);
    border-color: rgba(var(--blue-900-rgb), 0.16);
  }

  &.is-muted {
    background: var(--surface-muted);
    color: var(--muted-strong);
    border-color: var(--line);
  }
}

.empty-row {
  color: var(--muted);
  text-align: center;
}

@media (max-width: 720px) {
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .venue-equipment-section {
    padding: 1rem;
  }
}
</style>
