<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container" role="dialog" aria-modal="true" aria-labelledby="equipment-master-edit-title">
      <header class="modal-header">
        <div class="modal-heading">
          <p class="modal-eyebrow">Equipment Master</p>
          <h2 id="equipment-master-edit-title">編輯設備</h2>
          <p class="modal-subtitle">{{ equipmentTitle }}</p>
        </div>
        <button
          class="close-btn"
          type="button"
          aria-label="關閉編輯設備視窗"
          :disabled="isSaving"
          @click="closeModal"
        >
          <X :size="20" aria-hidden="true" />
        </button>
      </header>

      <div v-if="isLoading" class="modal-state">
        <p>載入設備資料中...</p>
      </div>

      <div v-else-if="loadError" class="modal-state modal-state-error">
        <h3>無法載入設備資料</h3>
        <p>{{ loadError }}</p>
        <div class="modal-state-actions">
          <button class="btn btn-secondary" type="button" @click="initializeModal">重新載入</button>
          <button class="btn btn-primary" type="button" @click="closeModal">先關閉</button>
        </div>
      </div>

      <form v-else class="edit-form" @submit.prevent="handleSubmit">
        <div class="form-layout">
          <section class="form-panel">
            <div class="form-section-title">基本資料</div>

            <label class="form-group">
              <span>設備名稱 <b class="required">*</b></span>
              <input v-model.trim="form.name" type="text" maxlength="100" required />
            </label>

            <label class="form-group">
              <span>總數量 <b class="required">*</b></span>
              <input v-model.number="form.totalQuantity" type="number" min="1" step="1" required />
            </label>

            <label class="form-group">
              <span>設備說明</span>
              <textarea v-model="form.description" rows="4" maxlength="255" />
            </label>

            <label class="form-group">
              <span>借用備註</span>
              <textarea v-model="form.borrowNote" rows="4" maxlength="255" />
            </label>
          </section>

          <section class="form-panel form-panel-details">
            <div class="form-section-title">場地限制</div>

            <label class="toggle-card">
              <input v-model="form.venueRestricted" type="checkbox" />
              <div>
                <strong>僅允許特定場地借用</strong>
                <p>未勾選時，所有場地都可借用此設備。</p>
              </div>
            </label>

            <div v-if="form.venueRestricted" class="venue-rule-section">
              <div class="selection-summary">
                已選擇 {{ selectedVenueCount }} 個場地
              </div>

              <div v-if="venueGroups.length === 0" class="empty-venues">
                目前沒有可設定的場地。
              </div>

              <div v-else class="venue-groups">
                <section v-for="group in venueGroups" :key="group.unitId" class="venue-group">
                  <h3>{{ group.unitName }}</h3>
                  <div class="venue-list">
                    <label
                      v-for="venue in group.venues"
                      :key="venue.id"
                      class="venue-card"
                      :class="{ 'is-selected': isVenueSelected(venue.id) }"
                    >
                      <div class="venue-card-top">
                        <span class="venue-checkbox">
                          <input
                            type="checkbox"
                            :checked="isVenueSelected(venue.id)"
                            @change="toggleVenueRule(venue, $event.target.checked)"
                          />
                        </span>
                        <div class="venue-copy">
                          <strong>{{ venue.name }}</strong>
                          <small v-if="venue.code">{{ venue.code }}</small>
                        </div>
                      </div>

                      <textarea
                        :value="getVenueRuleNote(venue.id)"
                        rows="2"
                        maxlength="255"
                        placeholder="可選填：此設備於該場地的補充規則"
                        :disabled="!isVenueSelected(venue.id)"
                        @input="updateVenueRuleNote(venue.id, $event.target.value)"
                      />
                    </label>
                  </div>
                </section>
              </div>
            </div>
          </section>
        </div>

        <footer class="modal-footer">
          <button class="btn btn-secondary" type="button" :disabled="isSaving" @click="closeModal">
            取消
          </button>
          <button class="btn btn-primary" type="submit" :disabled="isSaving || isLoading">
            <Save v-if="!isSaving" :size="16" aria-hidden="true" />
            <span>{{ isSaving ? "儲存中..." : "儲存變更" }}</span>
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { Save, X } from "lucide-vue-next";
import { getEquipment, updateEquipment } from "@/api/equipment";
import { fetchAllUnits, fetchVenuesByUnit } from "@/api/venue";
import { buildEquipmentMasterPayload, normalizeEquipmentMaster } from "@/utils/equipment";
import { useToast } from "@/utils/useToast.js";

const props = defineProps({
  visible: Boolean,
  equipmentId: {
    type: [Number, String],
    default: null,
  },
  equipmentName: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["update:visible", "saved"]);
const toast = useToast();

const isLoading = ref(false);
const isSaving = ref(false);
const loadError = ref("");
const venueGroups = ref([]);
const hasLoadedVenueCatalog = ref(false);
const currentEquipment = ref(null);

const form = reactive({
  name: "",
  totalQuantity: 1,
  description: "",
  borrowNote: "",
  venueRestricted: false,
  venueRules: [],
});

const equipmentTitle = computed(() => {
  return currentEquipment.value?.name || props.equipmentName || "Equipment";
});

const selectedVenueIds = computed(() => {
  return new Set(
    form.venueRules
      .map((rule) => Number(rule?.venueId))
      .filter((venueId) => Number.isFinite(venueId) && venueId > 0),
  );
});

const selectedVenueCount = computed(() => selectedVenueIds.value.size);

const resetForm = () => {
  form.name = "";
  form.totalQuantity = 1;
  form.description = "";
  form.borrowNote = "";
  form.venueRestricted = false;
  form.venueRules = [];
};

const applyEquipment = (equipment) => {
  currentEquipment.value = equipment;
  form.name = equipment.name || "";
  form.totalQuantity = Number(equipment.totalQuantity) || 1;
  form.description = equipment.description || "";
  form.borrowNote = equipment.borrowNote || "";
  form.venueRestricted = Boolean(equipment.venueRestricted);
  form.venueRules = Array.isArray(equipment.allowedVenues)
    ? equipment.allowedVenues
        .map((venue) => ({
          venueId: Number(venue.venueId),
          ruleNote: venue.ruleNote || "",
        }))
        .filter((venue) => Number.isFinite(venue.venueId) && venue.venueId > 0)
    : [];
};

const loadVenueCatalog = async () => {
  if (hasLoadedVenueCatalog.value) return;

  const unitsResponse = await fetchAllUnits();
  const units = Array.isArray(unitsResponse) ? unitsResponse : [];
  const venueResults = await Promise.all(
    units.map(async (unit) => ({
      unit,
      venues: await fetchVenuesByUnit(unit.id),
    })),
  );

  venueGroups.value = venueResults
    .map(({ unit, venues }) => ({
      unitId: unit.id,
      unitName: unit.name,
      venues: Array.isArray(venues)
        ? venues.map((venue) => ({
            id: Number(venue.id),
            name: venue.name || `Venue ${venue.id}`,
            code: venue.code || "",
          }))
        : [],
    }))
    .filter((group) => group.venues.length > 0);

  hasLoadedVenueCatalog.value = true;
};

const loadEquipmentDetail = async () => {
  const equipment = normalizeEquipmentMaster(await getEquipment(props.equipmentId));
  applyEquipment(equipment);
};

const initializeModal = async () => {
  if (!props.equipmentId) return;

  isLoading.value = true;
  loadError.value = "";

  try {
    await Promise.all([loadVenueCatalog(), loadEquipmentDetail()]);
  } catch (error) {
    loadError.value = error.message || "Failed to load equipment details.";
  } finally {
    isLoading.value = false;
  }
};

const closeModal = () => {
  if (isSaving.value) return;
  emit("update:visible", false);
};

const isVenueSelected = (venueId) => selectedVenueIds.value.has(Number(venueId));

const getVenueRule = (venueId) => {
  return form.venueRules.find((rule) => Number(rule.venueId) === Number(venueId)) || null;
};

const getVenueRuleNote = (venueId) => {
  return getVenueRule(venueId)?.ruleNote || "";
};

const toggleVenueRule = (venue, checked) => {
  const normalizedVenueId = Number(venue?.id);
  if (!Number.isFinite(normalizedVenueId) || normalizedVenueId <= 0) return;

  if (checked) {
    if (!getVenueRule(normalizedVenueId)) {
      form.venueRules = [
        ...form.venueRules,
        {
          venueId: normalizedVenueId,
          ruleNote: "",
        },
      ];
    }
    return;
  }

  form.venueRules = form.venueRules.filter((rule) => Number(rule.venueId) !== normalizedVenueId);
};

const updateVenueRuleNote = (venueId, ruleNote) => {
  const existingRule = getVenueRule(venueId);
  if (!existingRule) return;
  existingRule.ruleNote = ruleNote;
};

const handleSubmit = async () => {
  if (isSaving.value || !props.equipmentId) return;

  if (!form.name.trim()) {
    toast.warning("Please enter an equipment name.");
    return;
  }

  if (!Number.isInteger(Number(form.totalQuantity)) || Number(form.totalQuantity) < 1) {
    toast.warning("Total quantity must be at least 1.");
    return;
  }

  if (form.venueRestricted && selectedVenueCount.value === 0) {
    toast.warning("Select at least one allowed venue when venue restriction is enabled.");
    return;
  }

  isSaving.value = true;

  try {
    await updateEquipment(
      props.equipmentId,
      buildEquipmentMasterPayload({
        name: form.name,
        totalQuantity: form.totalQuantity,
        description: form.description,
        borrowNote: form.borrowNote,
        venueRules: form.venueRestricted ? form.venueRules : [],
      }),
    );

    toast.success("Equipment updated.");
    emit("saved");
    emit("update:visible", false);
  } catch (error) {
    toast.error(error.message || "Failed to update equipment.");
  } finally {
    isSaving.value = false;
  }
};

watch(
  () => [props.visible, props.equipmentId],
  async ([visible, equipmentId]) => {
    if (!visible || !equipmentId) {
      if (!visible) {
        loadError.value = "";
        currentEquipment.value = null;
        resetForm();
      }
      return;
    }

    await initializeModal();
  },
);
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.45);
}

.modal-container {
  display: flex;
  flex-direction: column;
  width: min(100%, 1040px);
  max-height: 92vh;
  overflow: hidden;
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  border-radius: var(--radius-lg);
  background: var(--card);
  box-shadow: var(--shadow);
}

.modal-header,
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.4rem;
}

.modal-header {
  border-bottom: 1px solid var(--line);
}

.modal-heading {
  min-width: 0;
}

.modal-eyebrow {
  margin: 0 0 0.25rem;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  text-transform: uppercase;
}

.modal-heading h2 {
  margin: 0;
  color: var(--ink);
  font-size: var(--text-xl);
}

.modal-subtitle {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: var(--text-sm);
}

.close-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}

.modal-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.9rem;
  min-height: 14rem;
  padding: 1.5rem;
  text-align: center;
}

.modal-state h3,
.modal-state p {
  margin: 0;
}

.modal-state-error h3 {
  color: var(--danger);
}

.modal-state-actions {
  display: flex;
  gap: 0.75rem;
}

.edit-form {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: #fbfcfe;
}

.form-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.95fr) minmax(360px, 1.05fr);
  gap: 1.5rem;
  padding: 1.4rem;
  overflow-y: auto;
}

.form-panel {
  min-width: 0;
}

.form-panel-details {
  padding-left: 1.5rem;
  border-left: 1px solid var(--line);
}

.form-section-title {
  margin: 0 0 0.9rem;
  padding-bottom: 0.55rem;
  border-bottom: 1px solid var(--line);
  color: var(--ink);
  font-weight: 800;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-bottom: 1rem;
  color: var(--ink);
  font-size: var(--text-sm);
  font-weight: 700;
}

.required {
  color: var(--danger);
}

input[type="text"],
input[type="number"],
textarea {
  width: 100%;
  min-height: 2.45rem;
  padding: 0.55rem 0.75rem;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: #ffffff;
  color: var(--ink);
  font: inherit;
}

textarea {
  resize: vertical;
}

.toggle-card {
  display: flex;
  gap: 0.85rem;
  padding: 0.85rem 0.95rem;
  margin-bottom: 1rem;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #ffffff;
  cursor: pointer;
}

.toggle-card strong,
.venue-copy strong {
  color: var(--ink);
}

.toggle-card p {
  margin: 0.3rem 0 0;
  color: var(--muted);
  font-size: var(--text-sm);
}

.venue-rule-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.selection-summary {
  color: var(--muted-strong);
  font-size: var(--text-sm);
  font-weight: 700;
}

.empty-venues {
  padding: 1rem;
  border: 1px dashed var(--line-strong);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--muted);
  text-align: center;
}

.venue-groups {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.venue-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.venue-group h3 {
  margin: 0;
  color: var(--ink);
  font-size: var(--text-base);
}

.venue-list {
  display: grid;
  gap: 0.75rem;
}

.venue-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.85rem 0.9rem;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #ffffff;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.venue-card.is-selected {
  border-color: rgba(var(--blue-900-rgb), 0.28);
  background: var(--accent-soft);
  box-shadow: var(--shadow-soft);
}

.venue-card textarea:disabled {
  background: #f5f7fa;
  color: var(--muted);
  cursor: not-allowed;
}

.venue-card-top {
  display: flex;
  align-items: flex-start;
  gap: 0.7rem;
}

.venue-checkbox {
  padding-top: 0.15rem;
}

.venue-copy {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.venue-copy small {
  color: var(--muted);
  font-size: var(--text-sm);
}

.modal-footer {
  justify-content: flex-end;
  border-top: 1px solid var(--line);
}

.modal-footer .btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

@media (max-width: 860px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .form-panel-details {
    padding-top: 1.25rem;
    padding-left: 0;
    border-top: 1px solid var(--line);
    border-left: 0;
  }
}

@media (max-width: 640px) {
  .modal-footer,
  .modal-state-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }
}
</style>
