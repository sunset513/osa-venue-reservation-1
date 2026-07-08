<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container" role="dialog" aria-modal="true" aria-labelledby="equipment-edit-title">
      <header class="modal-header">
        <h2 id="equipment-edit-title">修改設備借用</h2>
        <button class="close-btn" type="button" aria-label="關閉" :disabled="isSubmitting" @click="closeModal">
          <X :size="20" aria-hidden="true" />
        </button>
      </header>

      <form class="edit-form" @submit.prevent="handleSubmit">
        <div class="form-layout">
          <section class="form-panel">
            <div class="form-section-title">借用時間</div>
            <label class="form-group">
              <span>借用日期 <b class="required">*</b></span>
              <input v-model="form.borrowDate" type="date" required />
            </label>

            <div class="form-group">
              <span>借用時段 <b class="required">*</b></span>
              <div class="slots-grid" role="group" aria-label="借用時段">
                <label
                  v-for="slot in slotOptions"
                  :key="slot.value"
                  class="slot-checkbox"
                  :class="{ 'is-selected': form.slots.includes(slot.value) }"
                  role="checkbox"
                  tabindex="0"
                  :aria-checked="form.slots.includes(slot.value)"
                  @click.prevent="handleSlotClick(slot.value)"
                  @keydown.enter.prevent="handleSlotClick(slot.value)"
                  @keydown.space.prevent="handleSlotClick(slot.value)"
                >
                  <input
                    class="slot-input"
                    type="checkbox"
                    :value="slot.value"
                    :checked="form.slots.includes(slot.value)"
                    tabindex="-1"
                    aria-hidden="true"
                  />
                  <span class="slot-index">{{ slot.label }}</span>
                  <span class="slot-time">{{ slot.timeRange }}</span>
                </label>
              </div>
              <small v-if="formErrors.slots" class="error-text">請至少選擇一個借用時段</small>
            </div>
          </section>

          <section class="form-panel form-panel-details">
            <div class="form-section-title">借用內容</div>
            <label class="form-group">
              <span>借用用途 <b class="required">*</b></span>
              <input v-model.trim="form.purpose" type="text" required />
            </label>

            <div class="equipment-list-heading">設備品項 <b class="required">*</b></div>
            <div v-if="isEquipmentLoading" class="equipment-helper">載入設備資料中...</div>
            <p v-else class="equipment-list-helper">
              綁定特定場地的設備需從對應場地的場地借用流程一併申請，無法在此視窗單獨修改。
            </p>
            <div v-if="!isEquipmentLoading" class="equipment-list">
              <label
                v-for="equipment in equipmentSelectionOptions"
                :key="equipment.id"
                class="equipment-option"
                :class="{
                  'is-selected': isEquipmentSelected(equipment.id),
                  'is-disabled': isStandaloneRestricted(equipment),
                }"
              >
                <input
                  type="checkbox"
                  :disabled="isStandaloneRestricted(equipment)"
                  :checked="isEquipmentSelected(equipment.id)"
                  @change="toggleEquipment(equipment.id, $event.target.checked)"
                />
                <div class="equipment-copy">
                  <strong>{{ equipment.name }}</strong>
                  <span v-if="getEquipmentBoundVenueText(equipment)" class="equipment-bound-venues">
                    {{ getEquipmentBoundVenueText(equipment) }}
                  </span>
                </div>
                <span>總數 {{ equipment.totalQuantity }}</span>
                <input
                  v-if="isEquipmentSelected(equipment.id) && !isStandaloneRestricted(equipment)"
                  v-model.number="equipmentQuantities[equipment.id]"
                  type="number"
                  min="1"
                  :max="Math.max(equipment.totalQuantity || 1, 1)"
                  aria-label="設備借用數量"
                />
              </label>
            </div>

            <div class="form-section-title">聯絡資訊</div>
            <div class="form-row">
              <label class="form-group">
                <span>姓名 <b class="required">*</b></span>
                <input v-model.trim="form.contactInfo.name" type="text" required />
              </label>
              <label class="form-group">
                <span>電話 <b class="required">*</b></span>
                <input v-model.trim="form.contactInfo.phone" type="tel" required />
              </label>
            </div>
            <label class="form-group">
              <span>Email <b class="required">*</b></span>
              <input v-model.trim="form.contactInfo.email" type="email" required />
            </label>
          </section>
        </div>

        <footer class="modal-footer">
          <button class="btn btn-secondary" type="button" :disabled="isSubmitting" @click="closeModal">
            <span class="btn-icon" aria-hidden="true">
              <X :size="16" />
            </span>
            取消
          </button>
          <button class="btn btn-primary" type="submit" :disabled="isSubmitting || isEquipmentLoading">
            <Save v-if="!isSubmitting" :size="16" aria-hidden="true" />
            <span>{{ isSubmitting ? "儲存中..." : "儲存修改" }}</span>
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { Save, X } from "lucide-vue-next";
import { listEquipments, updateEquipmentBooking } from "@/api/equipment";
import {
  buildEquipmentBookingUpdatePayload,
  canBorrowEquipmentStandalone,
  formatEquipmentBoundVenueText,
  normalizeEquipmentMasters,
} from "@/utils/equipment";
import { useToast } from "@/utils/useToast.js";

const props = defineProps({
  visible: Boolean,
  booking: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["update:visible", "success"]);
const toast = useToast();

const isSubmitting = ref(false);
const isEquipmentLoading = ref(false);
const equipmentOptions = ref([]);
const equipmentQuantities = reactive({});
const slotRangeAnchor = ref(null);
const formErrors = reactive({ slots: false });

const form = reactive({
  borrowDate: "",
  slots: [],
  purpose: "",
  contactInfo: {
    name: "",
    phone: "",
    email: "",
  },
  equipmentItems: [],
});

const slotOptions = Array.from({ length: 24 }, (_, hour) => ({
  value: hour,
  label: String(hour),
  timeRange: `${String(hour).padStart(2, "0")}:00 - ${String(hour + 1).padStart(2, "0")}:00`,
}));

const selectedEquipmentIds = computed(() => {
  return new Set(form.equipmentItems.map((item) => item.equipmentId));
});

const equipmentSelectionOptions = computed(() => {
  const optionMap = new Map();

  equipmentOptions.value.forEach((equipment) => {
    if (equipment?.id) {
      optionMap.set(equipment.id, { ...equipment });
    }
  });

  form.equipmentItems.forEach((item) => {
    if (!item?.equipmentId || optionMap.has(item.equipmentId)) return;

    optionMap.set(item.equipmentId, {
      id: item.equipmentId,
      name: item.equipmentName || `Equipment ${item.equipmentId}`,
      totalQuantity: item.quantity || 1,
    });
  });

  return Array.from(optionMap.values());
});

const isEquipmentSelected = (equipmentId) => selectedEquipmentIds.value.has(equipmentId);

const isStandaloneRestricted = (equipment) => !canBorrowEquipmentStandalone(equipment);

const getEquipmentBoundVenueText = (equipment) => formatEquipmentBoundVenueText(equipment);

const resetEquipmentSelection = () => {
  form.equipmentItems = [];
  Object.keys(equipmentQuantities).forEach((key) => {
    delete equipmentQuantities[key];
  });
};

const applyBooking = () => {
  const booking = props.booking || {};
  const slots = Array.isArray(booking.slots)
    ? [...new Set(booking.slots.map(Number).filter(Number.isFinite))].sort((a, b) => a - b)
    : [];

  form.borrowDate = booking.borrowDate || "";
  form.slots = slots;
  form.purpose = booking.purpose || "";
  form.contactInfo = {
    name: booking.contact?.name || "",
    phone: booking.contact?.phone || "",
    email: booking.contact?.email || "",
  };

  resetEquipmentSelection();
  (Array.isArray(booking.items) ? booking.items : []).forEach((item) => {
    if (!item?.equipmentId) return;
    const quantity = item.quantity || 1;
    form.equipmentItems.push({
      equipmentId: item.equipmentId,
      equipmentName: item.equipmentName,
      quantity,
    });
    equipmentQuantities[item.equipmentId] = quantity;
  });

  slotRangeAnchor.value = slots[0] ?? null;
  formErrors.slots = false;
};

const loadEquipmentOptions = async () => {
  isEquipmentLoading.value = true;

  try {
    equipmentOptions.value = normalizeEquipmentMasters(await listEquipments());
  } catch (error) {
    equipmentOptions.value = [];
    toast.warning(error.message || "載入設備資料失敗");
  } finally {
    isEquipmentLoading.value = false;
  }
};

const buildSlotRange = (start, end) => {
  const minSlot = Math.min(start, end);
  const maxSlot = Math.max(start, end);
  return Array.from({ length: maxSlot - minSlot + 1 }, (_, index) => minSlot + index);
};

const handleSlotClick = (slot) => {
  const selectedSlots = [...new Set(form.slots)].sort((a, b) => a - b);

  if (selectedSlots.length === 0) {
    slotRangeAnchor.value = slot;
    form.slots = [slot];
    formErrors.slots = false;
    return;
  }

  const currentStart = selectedSlots[0];
  const currentEnd = selectedSlots[selectedSlots.length - 1];

  if (selectedSlots.length >= 2 && slot >= currentStart && slot <= currentEnd) {
    slotRangeAnchor.value = slot;
    form.slots = [slot];
  } else if (slot < currentStart) {
    slotRangeAnchor.value = currentEnd;
    form.slots = buildSlotRange(slot, currentEnd);
  } else if (slot > currentEnd) {
    slotRangeAnchor.value = currentStart;
    form.slots = buildSlotRange(currentStart, slot);
  } else {
    slotRangeAnchor.value = slot;
    form.slots = [slot];
  }

  formErrors.slots = false;
};

const toggleEquipment = (equipmentId, checked) => {
  const equipment = equipmentSelectionOptions.value.find((option) => option.id === equipmentId);
  if (equipment && isStandaloneRestricted(equipment)) {
    return;
  }

  if (checked) {
    if (!isEquipmentSelected(equipmentId)) {
      form.equipmentItems.push({ equipmentId, quantity: 1 });
    }
    equipmentQuantities[equipmentId] = equipmentQuantities[equipmentId] || 1;
    return;
  }

  form.equipmentItems = form.equipmentItems.filter((item) => item.equipmentId !== equipmentId);
  delete equipmentQuantities[equipmentId];
};

const closeModal = () => {
  if (isSubmitting.value) return;
  emit("update:visible", false);
};

const handleSubmit = async () => {
  if (isSubmitting.value || !props.booking?.id) return;

  if (form.slots.length === 0) {
    formErrors.slots = true;
    toast.warning("請至少選擇一個借用時段");
    return;
  }

  const payload = buildEquipmentBookingUpdatePayload({
    borrowDate: form.borrowDate,
    slots: form.slots,
    purpose: form.purpose,
    contactInfo: form.contactInfo,
    relatedVenueBookingId: null,
    equipmentItems: form.equipmentItems
      .map((item) => ({
        equipmentId: item.equipmentId,
        quantity: equipmentQuantities[item.equipmentId] || item.quantity,
      }))
      .filter((item) => {
        const equipment = equipmentSelectionOptions.value.find((option) => option.id === item.equipmentId);
        return Boolean(equipment) && canBorrowEquipmentStandalone(equipment);
      }),
  });

  if (payload.items.length === 0) {
    toast.warning("請至少選擇一項設備");
    return;
  }

  isSubmitting.value = true;

  try {
    await updateEquipmentBooking(props.booking.id, payload);
    toast.success("設備借用已更新");
    emit("success");
    emit("update:visible", false);
  } catch (error) {
    toast.error(error.message || "設備借用更新失敗");
  } finally {
    isSubmitting.value = false;
  }
};

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) return;

    applyBooking();
    await loadEquipmentOptions();
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
  width: min(100%, 980px);
  max-height: 90vh;
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
  border-bottom: 1px solid var(--line);
}

.modal-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: var(--text-xl);
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

.edit-form {
  overflow-y: auto;
  background: #fbfcfe;
}

.form-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.95fr) minmax(340px, 1.05fr);
  gap: 1.5rem;
  padding: 1.4rem;
}

.form-panel {
  min-width: 0;
}

.form-panel-details {
  padding-left: 1.5rem;
  border-left: 1px solid var(--line);
}

.form-section-title {
  margin: 0 0 0.8rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--line);
  color: var(--ink);
  font-weight: 800;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex: 1;
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
input[type="date"],
input[type="tel"],
input[type="email"],
input[type="number"] {
  min-height: 2.45rem;
  padding: 0.55rem 0.75rem;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: #ffffff;
}

.slots-grid {
  display: flex;
  flex-direction: column;
  gap: 0.32rem;
  max-height: min(58vh, 540px);
  overflow-y: auto;
  padding: 0.42rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #f3f7fb;
}

.slot-checkbox {
  position: relative;
  display: grid;
  grid-template-columns: minmax(2.55rem, 0.28fr) minmax(0, 1fr);
  align-items: center;
  min-height: 2.55rem;
  margin: 0;
  padding: 0 0.7rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.18);
  border-radius: 8px;
  background: #ffffff;
  color: var(--ink);
  cursor: pointer;
  user-select: none;
}

.slot-checkbox.is-selected {
  border-color: rgba(var(--blue-900-rgb), 0.36);
  background: var(--accent-soft);
  color: var(--accent-hover);
}

.slot-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.slot-index,
.slot-time {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
  line-height: 1.2;
  font-weight: 700;
  text-align: center;
}

.slot-index {
  color: var(--accent);
}

.equipment-list,
.equipment-readonly-list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  margin-bottom: 1rem;
}

.equipment-list-heading {
  margin: 0 0 0.6rem;
  color: var(--ink);
  font-size: var(--text-sm);
  font-weight: 700;
}

.equipment-list-helper {
  margin: 0 0 0.75rem;
  color: var(--muted);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.equipment-option {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-height: 2.35rem;
  padding: 0.55rem 0.65rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
  color: var(--text);
  font-size: var(--text-sm);
  flex-wrap: wrap;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.equipment-option:hover,
.equipment-option:focus-within {
  border-color: rgba(var(--blue-900-rgb), 0.24);
  background: #f8fbff;
  box-shadow: 0 0 0 3px rgba(var(--blue-900-rgb), 0.08);
  transform: translateY(-1px);
}

.equipment-option.is-selected {
  border-color: rgba(var(--blue-900-rgb), 0.36);
  background: var(--accent-soft);
  box-shadow: 0 6px 16px rgba(var(--blue-900-rgb), 0.08);
}

.equipment-option.is-disabled {
  background: #f6f8fb;
  color: var(--muted);
  cursor: not-allowed;
  opacity: 0.8;
}

.equipment-option.is-disabled input[type="checkbox"] {
  cursor: not-allowed;
}

.equipment-option strong {
  color: var(--ink);
}

.equipment-option input[type="checkbox"] {
  flex-shrink: 0;
  margin: 0;
  cursor: pointer;
  accent-color: var(--accent);
}

.equipment-option span,
.equipment-helper {
  color: var(--muted);
}

.equipment-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 0.2rem;
}

.equipment-bound-venues {
  color: var(--muted-strong);
  font-size: 0.8rem;
  line-height: 1.4;
}

.equipment-option input[type="number"] {
  width: 5rem;
  margin-left: auto;
}

.error-text {
  color: var(--danger);
  font-size: var(--text-sm);
}

.modal-footer {
  justify-content: flex-end;
  border-top: 1px solid var(--line);
  border-bottom: 0;
}

.modal-footer .btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

@media (max-width: 820px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .form-panel-details {
    padding-top: 1.25rem;
    padding-left: 0;
    border-top: 1px solid var(--line);
    border-left: 0;
  }

  .form-row,
  .modal-footer {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .equipment-option input[type="number"] {
    width: 100%;
    margin-left: 0;
  }
}
</style>
