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

    <div v-else class="equipment-content">
      <section class="summary-grid" aria-label="設備總覽">
        <article class="summary-card">
          <span>總設備數</span>
          <strong>{{ equipmentSummary.total }}</strong>
        </article>
        <article class="summary-card">
          <span>使用中</span>
          <strong>{{ equipmentSummary.inUse }}</strong>
        </article>
        <article class="summary-card">
          <span>閒置</span>
          <strong>{{ equipmentSummary.idle }}</strong>
        </article>
        <article class="summary-card">
          <span>場地數</span>
          <strong>{{ equipmentSummary.venues }}</strong>
        </article>
      </section>

      <div class="equipment-sections">
        <section
          v-for="group in venueEquipmentGroups"
          :key="group.venueName"
          class="venue-equipment-section"
        >
        <div class="section-heading">
          <button
            type="button"
            class="section-title-button"
            :aria-expanded="!isVenueCollapsed(group)"
            @click="toggleVenueCollapse(group)"
          >
            <ChevronDown
              :size="18"
              aria-hidden="true"
              class="collapse-icon"
              :class="{ 'is-collapsed': isVenueCollapsed(group) }"
            />
            <div>
            <p class="section-eyebrow">Venue</p>
            <h2>{{ group.venueName }}</h2>
          </div>
          </button>
          <span class="section-count-pill">
            使用中 {{ getGroupUsageCount(group) }} / 總數 {{ group.equipmentList.length }}
          </span>
          <button
            type="button"
            class="btn btn-secondary compact-btn"
            @click="startCreate(group)"
          >
            <Plus :size="16" aria-hidden="true" />
            新增設備
          </button>
        </div>

        <div v-if="!isVenueCollapsed(group)" class="equipment-table-wrap">
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
                        class="btn btn-primary edit-action-btn"
                        :disabled="savingId === equipment.equipmentId"
                        @click="saveEdit(equipment)"
                      >
                        儲存
                      </button>
                      <button type="button" class="btn btn-secondary edit-action-btn" @click="cancelEdit">
                        取消
                      </button>
                    </template>
                    <template v-else>
                      <RouterLink
                        class="history-url-link"
                        :to="borrowHistoryRoute(equipment)"
                        title="前往此設備的借用紀錄"
                      >
                        <History :size="14" aria-hidden="true" />
                        <span>查看借用紀錄</span>
                      </RouterLink>
                      <div class="more-menu-wrap">
                        <button
                          type="button"
                          class="icon-action"
                          :aria-expanded="openActionMenuKey === getEquipmentActionKey(group, equipment)"
                          aria-label="更多操作"
                          @click="toggleActionMenu(group, equipment)"
                        >
                          <MoreHorizontal :size="18" aria-hidden="true" />
                        </button>
                        <div
                          v-if="openActionMenuKey === getEquipmentActionKey(group, equipment)"
                          class="more-menu"
                        >
                          <button type="button" class="menu-item" @click="startEdit(equipment)">
                            <Pencil :size="15" aria-hidden="true" />
                            編輯
                          </button>
                          <button
                            type="button"
                            class="menu-item is-danger"
                            :disabled="savingId === equipment.equipmentId"
                            @click="requestDeleteEquipment(equipment)"
                          >
                            <Trash2 :size="15" aria-hidden="true" />
                            刪除
                          </button>
                        </div>
                      </div>
                    </template>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!isVenueCollapsed(group)" class="equipment-card-list">
          <div v-if="group.equipmentList.length === 0" class="empty-row card-empty">
            這個場地目前沒有設備資料。
          </div>

          <article
            v-for="equipment in group.equipmentList"
            :key="`card-${equipment.equipmentId}`"
            class="equipment-card"
            :class="{ 'is-editing': editingId === equipment.equipmentId }"
          >
            <div class="card-main">
              <input
                v-if="editingId === equipment.equipmentId"
                v-model.trim="editForm.equipmentName"
                class="table-input"
                type="text"
              />
              <strong v-else>{{ equipment.equipmentName }}</strong>
              <span
                class="status-pill"
                :class="getEquipmentStatusMeta(equipment.isInUse).className"
              >
                {{ getEquipmentStatusMeta(equipment.isInUse).text }}
              </span>
            </div>

            <label class="card-field">
              <span>數量</span>
              <input
                v-if="editingId === equipment.equipmentId"
                v-model.number="editForm.quantity"
                class="table-input is-number"
                type="number"
                min="1"
              />
              <strong v-else>{{ equipment.quantity }}</strong>
            </label>

            <div class="action-group card-actions">
              <template v-if="editingId === equipment.equipmentId">
                <button
                  type="button"
                  class="btn btn-primary edit-action-btn"
                  :disabled="savingId === equipment.equipmentId"
                  @click="saveEdit(equipment)"
                >
                  儲存
                </button>
                <button type="button" class="btn btn-secondary edit-action-btn" @click="cancelEdit">
                  取消
                </button>
              </template>
              <template v-else>
                <RouterLink
                  class="history-url-link"
                  :to="borrowHistoryRoute(equipment)"
                  title="前往此設備的借用紀錄"
                >
                  <History :size="14" aria-hidden="true" />
                  <span>查看借用紀錄</span>
                </RouterLink>
                <div class="more-menu-wrap">
                  <button
                    type="button"
                    class="icon-action"
                    :aria-expanded="openActionMenuKey === getEquipmentActionKey(group, equipment)"
                    aria-label="更多操作"
                    @click="toggleActionMenu(group, equipment)"
                  >
                    <MoreHorizontal :size="18" aria-hidden="true" />
                  </button>
                  <div
                    v-if="openActionMenuKey === getEquipmentActionKey(group, equipment)"
                    class="more-menu"
                  >
                    <button type="button" class="menu-item" @click="startEdit(equipment)">
                      <Pencil :size="15" aria-hidden="true" />
                      編輯
                    </button>
                    <button
                      type="button"
                      class="menu-item is-danger"
                      :disabled="savingId === equipment.equipmentId"
                      @click="requestDeleteEquipment(equipment)"
                    >
                      <Trash2 :size="15" aria-hidden="true" />
                      刪除
                    </button>
                  </div>
                </div>
              </template>
            </div>
          </article>
        </div>
        </section>
      </div>
    </div>

    <div v-if="isCreateModalVisible" class="modal-overlay delete-modal-overlay" @click.self="cancelCreate">
      <div class="confirm-dialog create-equipment-dialog">
        <div class="confirm-heading">
          <span class="confirm-icon is-create">
            <Plus :size="18" aria-hidden="true" />
          </span>
          <h3>新增設備</h3>
        </div>
        <div class="create-form-grid">
          <label class="modal-field">
            <span>設備名稱</span>
            <input
              v-model.trim="createForm.equipmentName"
              type="text"
              placeholder="輸入設備名稱"
            />
          </label>
          <label class="modal-field">
            <span>場地</span>
            <select v-model.number="createForm.venueId">
              <option :value="null" disabled>選擇場地</option>
              <option
                v-for="venue in createVenueOptions"
                :key="venue.id"
                :value="venue.id"
              >
                {{ venue.name }}
              </option>
            </select>
          </label>
          <label class="modal-field">
            <span>數量</span>
            <input
              v-model.number="createForm.quantity"
              type="number"
              min="1"
            />
          </label>
        </div>
        <div class="confirm-actions">
          <button class="btn btn-secondary" type="button" :disabled="savingId === 'create'" @click="cancelCreate">
            取消
          </button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="savingId === 'create'"
            @click="saveCreate"
          >
            <span class="btn-icon">
              <Plus :size="16" aria-hidden="true" />
            </span>
            {{ savingId === "create" ? "新增中..." : "新增設備" }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteTarget" class="modal-overlay delete-modal-overlay" @click.self="cancelDelete">
      <div class="confirm-dialog delete-confirm-dialog">
        <div class="confirm-heading">
          <span class="confirm-icon">
            <Trash2 :size="18" aria-hidden="true" />
          </span>
          <h3>刪除設備</h3>
        </div>
        <p>
          確定要刪除「{{ deleteTarget.equipmentName }}」嗎？這個操作會移除此設備與場地的關聯。
        </p>
        <div class="confirm-actions">
          <button class="btn btn-secondary" type="button" :disabled="isDeleteProcessing" @click="cancelDelete">
            取消
          </button>
          <button
            class="btn btn-danger"
            type="button"
            :disabled="isDeleteProcessing"
            @click="confirmDelete"
          >
            <span class="btn-icon">
              <Trash2 :size="16" aria-hidden="true" />
            </span>
            {{ isDeleteProcessing ? "刪除中..." : "確認刪除" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ChevronDown, History, MoreHorizontal, Pencil, Plus, Trash2 } from "lucide-vue-next";
import { useRouter } from "vue-router";
import {
  createEquipment,
  deleteEquipment,
  fetchEquipmentGroups,
  updateEquipment,
} from "@/api/equipment";
import { fetchAllUnits, fetchVenuesByUnit } from "@/api/venue";
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
const venues = ref([]);
const editingId = ref(null);
const savingId = ref(null);
const openActionMenuKey = ref(null);
const deleteTarget = ref(null);
const isDeleteProcessing = ref(false);
const collapsedVenueNames = ref(new Set());
const isCreateModalVisible = ref(false);

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

const resolveGroupVenueId = (group) => {
  return group.venueId || group.equipmentList[0]?.venueId || null;
};

const venueEquipmentGroups = computed(() => {
  const groupsByVenueName = new Map(
    equipmentGroups.value.map((group) => [group.venueName, group]),
  );

  const mergedGroups = venues.value.map((venue) => {
    const group = groupsByVenueName.get(venue.name);
    return {
      venueId: venue.id,
      venueName: venue.name,
      equipmentList: group?.equipmentList || [],
    };
  });

  equipmentGroups.value.forEach((group) => {
    if (!mergedGroups.some((mergedGroup) => mergedGroup.venueName === group.venueName)) {
      mergedGroups.push({
        venueId: resolveGroupVenueId(group),
        venueName: group.venueName,
        equipmentList: group.equipmentList,
      });
    }
  });

  return mergedGroups;
});

const allEquipment = computed(() => {
  return venueEquipmentGroups.value.flatMap((group) => group.equipmentList);
});

const equipmentSummary = computed(() => {
  const total = allEquipment.value.length;
  const inUse = allEquipment.value.filter((equipment) => equipment.isInUse).length;

  return {
    total,
    inUse,
    idle: total - inUse,
    venues: venueEquipmentGroups.value.length,
  };
});

const createVenueOptions = computed(() => {
  return venueEquipmentGroups.value
    .filter((group) => resolveGroupVenueId(group))
    .map((group) => ({
      id: resolveGroupVenueId(group),
      name: group.venueName,
    }));
});

const getGroupUsageCount = (group) => {
  return group.equipmentList.filter((equipment) => equipment.isInUse).length;
};

const isVenueCollapsed = (group) => {
  return collapsedVenueNames.value.has(group.venueName);
};

const toggleVenueCollapse = (group) => {
  const nextCollapsedVenueNames = new Set(collapsedVenueNames.value);

  if (nextCollapsedVenueNames.has(group.venueName)) {
    nextCollapsedVenueNames.delete(group.venueName);
  } else {
    nextCollapsedVenueNames.add(group.venueName);
  }

  collapsedVenueNames.value = nextCollapsedVenueNames;
};

const loadEquipmentGroups = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    const [groups, units] = await Promise.all([
      fetchEquipmentGroups(),
      fetchAllUnits(),
    ]);
    const venuesByUnit = await Promise.all(
      (Array.isArray(units) ? units : []).map((unit) => fetchVenuesByUnit(unit.id)),
    );

    equipmentGroups.value = normalizeEquipmentGroups(groups);
    venues.value = venuesByUnit.flat();
  } catch (error) {
    console.error("載入設備資料失敗:", error);
    loadError.value = error.message || "請稍後再試一次。";
  } finally {
    loading.value = false;
  }
};

const startEdit = (equipment) => {
  cancelCreate();
  openActionMenuKey.value = null;
  editingId.value = equipment.equipmentId;
  editForm.value = {
    equipmentName: equipment.equipmentName,
    venueId: equipment.venueId,
    quantity: equipment.quantity,
  };
};

const cancelEdit = () => {
  openActionMenuKey.value = null;
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
  openActionMenuKey.value = null;
  const venueId = resolveGroupVenueId(group);
  createForm.value = {
    equipmentName: "",
    venueId,
    quantity: 1,
  };
  isCreateModalVisible.value = true;
};

const cancelCreate = () => {
  openActionMenuKey.value = null;
  isCreateModalVisible.value = false;
  createForm.value = {
    equipmentName: "",
    venueId: null,
    quantity: 1,
  };
};

const saveCreate = async () => {
  const payload = buildEquipmentPayload(createForm.value);

  if (!payload.equipmentName || !payload.venueId) {
    toast.warning("請填寫設備名稱並選擇場地");
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

const borrowHistoryRoute = (equipment) => {
  return {
    name: "EquipmentBorrowHistory",
    query: {
      equipmentId: equipment.equipmentId,
      equipmentName: equipment.equipmentName,
      venueId: equipment.venueId,
      venueName: equipment.venueName,
    },
  };
};

const getEquipmentActionKey = (group, equipment) => {
  return `${resolveGroupVenueId(group) || group.venueName}-${equipment.equipmentId}`;
};

const toggleActionMenu = (group, equipment) => {
  const actionKey = getEquipmentActionKey(group, equipment);
  openActionMenuKey.value = openActionMenuKey.value === actionKey ? null : actionKey;
};

const requestDeleteEquipment = (equipment) => {
  openActionMenuKey.value = null;
  deleteTarget.value = equipment;
};

const cancelDelete = () => {
  if (isDeleteProcessing.value) return;
  deleteTarget.value = null;
};

const confirmDelete = async () => {
  if (!deleteTarget.value) return;

  const equipment = deleteTarget.value;
  savingId.value = equipment.equipmentId;
  isDeleteProcessing.value = true;

  try {
    await deleteEquipment(equipment.equipmentId);
    toast.success("設備已刪除");
    deleteTarget.value = null;
    await loadEquipmentGroups();
  } catch (error) {
    console.error("刪除設備失敗:", error);
    toast.error(error.message || "設備刪除失敗");
  } finally {
    savingId.value = null;
    isDeleteProcessing.value = false;
  }
};

onMounted(() => {
  loadEquipmentGroups();
});
</script>

<style lang="scss" scoped>
.equipment-page {
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
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

.equipment-content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.9rem;
}

.summary-card {
  display: flex;
  min-height: 6rem;
  flex-direction: column;
  justify-content: center;
  gap: 0.35rem;
  padding: 1rem 1.1rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.1);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-soft);

  span {
    color: var(--muted);
    font-size: var(--text-sm);
    font-weight: 800;
  }

  strong {
    color: var(--ink);
    font-size: var(--text-2xl);
    line-height: 1;
  }
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

.section-title-button {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 0.65rem;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;

  h2 {
    margin: 0;
  }
}

.collapse-icon {
  flex: 0 0 auto;
  color: var(--accent);
  transition: transform 0.2s ease;

  &.is-collapsed {
    transform: rotate(-90deg);
  }
}

.section-count-pill {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  margin-left: auto;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  white-space: nowrap;
}

.compact-btn {
  min-height: 2.35rem;
  padding: 0.55rem 0.9rem;
  font-size: var(--text-sm);
}

.equipment-table-wrap {
  overflow: visible;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--card);
}

.equipment-card-list {
  display: none;
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

.history-url-link {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  max-width: 9rem;
  min-height: 2rem;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  text-decoration: underline;
  text-underline-offset: 0.18rem;

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &:hover {
    color: var(--ink);
  }
}

.delete-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background: rgba(15, 23, 42, 0.5);
}

.confirm-dialog {
  width: min(100%, 420px);
  padding: 1.25rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  border-radius: var(--radius);
  background: #ffffff;
  box-shadow: var(--shadow);

  p {
    margin: 0;
    color: var(--muted-strong);
    font-size: var(--text-sm);
    line-height: 1.6;
  }
}

.delete-confirm-dialog {
  width: min(100%, 380px);
}

.confirm-heading {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  margin-bottom: 0.65rem;

  h3 {
    margin: 0;
    color: var(--ink);
    font-size: 1.05rem;
  }
}

.confirm-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 999px;
  background: rgba(196, 69, 69, 0.1);
  color: var(--danger);

  &.is-create {
    background: rgba(39, 94, 168, 0.1);
    color: var(--accent);
  }
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}

.create-equipment-dialog {
  width: min(100%, 460px);
}

.create-form-grid {
  display: grid;
  gap: 0.9rem;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;

  span {
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 800;
  }

  input,
  select {
    width: 100%;
    min-height: 2.5rem;
    padding: 0.45rem 0.7rem;
    border: 1px solid var(--line-strong);
    border-radius: 8px;
    background: #ffffff;
    color: var(--text);
    font-size: var(--text-base);
  }
}

.btn-danger {
  background: var(--danger);
  color: #ffffff;
  border-color: var(--danger);

  &:hover:not(:disabled) {
    box-shadow: 0 8px 18px rgba(196, 69, 69, 0.22);
  }
}

.more-menu-wrap {
  position: relative;
  display: inline-flex;
}

.history-url-link + .more-menu-wrap {
  margin-left: 0.45rem;
}

.icon-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.15rem;
  height: 2.15rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
  color: var(--muted-strong);
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

  &:hover,
  &[aria-expanded="true"] {
    color: var(--accent);
    border-color: rgba(var(--blue-900-rgb), 0.22);
    transform: translateY(-1px);
  }
}

.more-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 0.35rem);
  z-index: 20;
  display: flex;
  min-width: 8rem;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.35rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--shadow-soft);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  width: 100%;
  min-height: 2.15rem;
  padding: 0.45rem 0.6rem;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text);
  font-size: var(--text-sm);
  font-weight: 800;
  text-align: left;
  cursor: pointer;

  &:hover {
    background: var(--surface-muted);
    color: var(--accent);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.58;
  }

  &.is-danger {
    color: var(--danger);

    &:hover {
      background: rgba(196, 69, 69, 0.1);
    }
  }
}

.edit-action-btn {
  min-height: 2.2rem;
  padding: 0.45rem 0.9rem;
  font-size: var(--text-sm);
  font-weight: 800;
}

.row-action {
  min-width: 3.75rem;
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: var(--text-sm);
  font-weight: 800;
  text-decoration: none;
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
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-card {
    min-height: 5.25rem;
    padding: 0.9rem;
  }

  .equipment-table-wrap {
    display: none;
  }

  .equipment-card-list {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
  }

  .equipment-card {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
    padding: 0.95rem;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--card);

    &.is-editing {
      background: var(--accent-soft);
      border-color: rgba(var(--blue-900-rgb), 0.18);
    }
  }

  .card-main {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.8rem;

    strong {
      min-width: 0;
      color: var(--ink);
      line-height: 1.45;
      overflow-wrap: anywhere;
    }

    .table-input {
      flex: 1;
      min-width: 0;
    }
  }

  .card-field {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    color: var(--muted-strong);
    font-size: var(--text-sm);
    font-weight: 800;

    strong {
      color: var(--text);
      font-size: var(--text-base);
    }
  }

  .card-actions {
    align-items: stretch;

    .row-action {
      flex: 1 1 5rem;
    }

    .history-url-link {
      flex: 1 1 calc(100% - 3rem);
      max-width: 100%;
    }

    .more-menu-wrap {
      margin-left: auto;
    }
  }

  .card-empty {
    padding: 1rem;
    border: 1px dashed var(--line-strong);
    border-radius: var(--radius-sm);
    background: var(--surface-muted);
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .section-count-pill {
    margin-left: 0;
  }

  .venue-equipment-section {
    padding: 1rem;
  }
}
</style>
