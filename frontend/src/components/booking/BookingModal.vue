<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container">
      <header class="modal-header">
        <h2>{{ mode === "create" ? "新增預約申請" : "修改預約申請" }}</h2>
        <button class="close-btn" type="button" @click="closeModal">✕</button>
      </header>

      <div class="modal-body">
        <form class="booking-form" @submit.prevent="handleSubmit">
          <div class="booking-form-layout">
            <section class="booking-form-panel booking-form-panel-times">
              <div class="form-section-title">借用時間</div>

              <div class="form-group">
                <label>預約日期</label>
                <input
                  type="text"
                  :value="formData.bookingDate"
                  disabled
                  class="disabled-input"
                />
              </div>

              <div class="form-group">
                <label>選擇時段 (可複選) <span class="required">*</span></label>
                <div class="slots-grid" role="group">
                  <label
                    v-for="slot in slotOptions"
                    :key="slot.value"
                    class="slot-checkbox"
                    :class="{ 'is-selected': formData.slots.includes(slot.value) }"
                    role="checkbox"
                    tabindex="0"
                    :aria-checked="formData.slots.includes(slot.value)"
                    @click.prevent="handleSlotClick(slot.value)"
                    @keydown.enter.prevent="handleSlotClick(slot.value)"
                    @keydown.space.prevent="handleSlotClick(slot.value)"
                  >
                    <input
                      class="slot-input"
                      type="checkbox"
                      :value="slot.value"
                      :checked="formData.slots.includes(slot.value)"
                      tabindex="-1"
                      aria-hidden="true"
                    />
                    <span class="slot-index">{{ slot.label }}</span>
                    <span class="slot-time">{{ slot.timeRange }}</span>
                  </label>
                </div>
                <small v-if="formErrors.slots" class="error-text">
                  請至少選擇一個時段。
                </small>
              </div>
            </section>

            <section class="booking-form-panel booking-form-panel-details">
              <div class="form-section-title">借用說明</div>

              <div class="form-row">
                <div class="form-group">
                  <label>使用用途 <span class="required">*</span></label>
                  <input
                    v-model="formData.purpose"
                    type="text"
                    placeholder="例如：系學會開會"
                    required
                  />
                </div>
                <div class="form-group">
                  <label>預估人數 <span class="required">*</span></label>
                  <input
                    v-model.number="formData.participantCount"
                    type="number"
                    min="1"
                    required
                  />
                </div>
              </div>

              <div class="form-section-title">聯絡人資訊</div>

              <div class="form-row">
                <div class="form-group">
                  <label>姓名 <span class="required">*</span></label>
                  <input v-model="formData.contactInfo.name" type="text" required />
                </div>
                <div class="form-group">
                  <label>電話 <span class="required">*</span></label>
                  <input v-model="formData.contactInfo.phone" type="tel" required />
                </div>
              </div>

              <div class="form-group">
                <label>電子郵件 <span class="required">*</span></label>
                <input v-model="formData.contactInfo.email" type="email" required />
              </div>

              <div v-if="shouldShowEquipmentSection" class="form-group">
                <div class="form-section-title">需借用器材</div>

                <div v-if="isEquipmentLoading" class="equipment-helper">
                  載入可借用器材中...
                </div>

                <div v-else-if="isEquipmentReadonly" class="equipment-readonly-panel">
                  <p class="equipment-helper">{{ equipmentReadonlyMessage }}</p>
                  <div v-if="readonlyEquipmentItems.length" class="equipment-readonly-list">
                    <div
                      v-for="item in readonlyEquipmentItems"
                      :key="`${item.equipmentId}-${item.id ?? 'readonly'}`"
                      class="equipment-readonly-item"
                    >
                      <strong>{{ item.equipmentName }}</strong>
                      <span>數量 {{ item.quantity }}</span>
                    </div>
                  </div>
                  <p v-else class="equipment-helper">此筆關聯器材申請沒有品項資料。</p>
                </div>

                <div v-else-if="equipmentSelectionOptions.length === 0" class="equipment-helper">
                  此場地目前沒有可修改的器材品項。
                </div>

                <template v-else>
                  <div class="equipment-list-heading">
                    {{ mode === "create" ? "選擇器材" : "修改器材" }}
                  </div>
                  <div class="equipment-list">
                    <label
                      v-for="equipment in equipmentSelectionOptions"
                      :key="equipment.id"
                      class="equipment-option"
                      :class="{ 'is-selected': isEquipmentSelected(equipment.id) }"
                    >
                      <input
                        type="checkbox"
                        :checked="isEquipmentSelected(equipment.id)"
                        :disabled="isEquipmentReadonly"
                        @change="toggleEquipmentSelection(equipment.id, $event.target.checked)"
                      />
                      <strong>{{ equipment.name }}</strong>
                      <span>總數 {{ equipment.totalQuantity }}</span>
                      <input
                        v-if="isEquipmentSelected(equipment.id)"
                        v-model.number="equipmentQuantities[equipment.id]"
                        type="number"
                        min="1"
                        :max="Math.max(equipment.totalQuantity || 1, 1)"
                        :disabled="isEquipmentReadonly"
                        aria-label="器材借用數量"
                      />
                    </label>
                  </div>
                </template>
              </div>
            </section>
          </div>
        </form>
      </div>

      <footer class="modal-footer">
        <button
          v-if="canWithdraw"
          class="btn btn-danger"
          type="button"
          :disabled="isBusy"
          @click="handleWithdraw"
        >
          <span class="btn-icon">
            <Undo2 :size="16" />
          </span>
          <span>撤回申請</span>
        </button>

        <div class="modal-footer-actions">
          <button class="btn btn-secondary" type="button" :disabled="isBusy" @click="closeModal">
            <span class="btn-icon">
              <X :size="16" />
            </span>
            <span>取消</span>
          </button>
          <button class="btn btn-primary" type="button" :disabled="isBusy" @click="handleSubmit">
            <template v-if="!isBusy">
              <span class="btn-icon">
                <Send v-if="mode === 'create'" :size="16" />
                <Save v-else :size="16" />
              </span>
            </template>
            <span>
              {{
                isBusy
                  ? (isWithdrawing ? "撤回中..." : "儲存中...")
                  : mode === "create"
                    ? "送出申請"
                    : "儲存修改"
              }}
            </span>
          </button>
        </div>
      </footer>

      <div
        v-if="isWithdrawConfirmVisible"
        class="confirm-overlay"
        @click.self="closeWithdrawConfirm"
      >
        <div class="confirm-dialog">
          <h3>確認撤回申請？</h3>
          <p>撤回後這筆預約會改成已撤回，且無法再編輯或重新啟用。</p>
          <div class="confirm-actions">
            <button
              class="btn btn-secondary"
              type="button"
              :disabled="isBusy"
              @click="closeWithdrawConfirm"
            >
              取消
            </button>
            <button
              class="btn btn-danger"
              type="button"
              :disabled="isBusy"
              @click="confirmWithdraw"
            >
              <span class="btn-icon">
                <Undo2 :size="16" />
              </span>
              確認撤回
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { Save, Send, Undo2, X } from "lucide-vue-next";
import { createBooking, createBookingWithEquipments, updateBooking } from "@/api/booking";
import { listEquipments, updateEquipmentBooking } from "@/api/equipment";
import {
  buildEquipmentBookingItems,
  isEquipmentAllowedForVenue,
  normalizeEquipmentMasters,
} from "@/utils/equipment";
import { useToast } from "@/utils/useToast.js";

const { success, error: showError, warning } = useToast();

const props = defineProps({
  visible: Boolean,
  mode: { type: String, default: "create" },
  initialData: {
    type: Object,
    default: () => ({}),
  },
  venueInfo: {
    type: Object,
    default: null,
  },
  isWithdrawing: { type: Boolean, default: false },
});

const emit = defineEmits(["update:visible", "success", "withdraw-booking"]);

const isSubmitting = ref(false);
const isWithdrawConfirmVisible = ref(false);
const slotRangeAnchor = ref(null);
const formErrors = reactive({ slots: false });
const equipmentOptions = ref([]);
const isEquipmentLoading = ref(false);
const equipmentQuantities = reactive({});

const formData = reactive({
  venueId: null,
  bookingDate: "",
  slots: [],
  purpose: "",
  participantCount: 1,
  contactInfo: {
    name: "測試生",
    email: "student@ncu.edu.tw",
    phone: "0912345678",
  },
  equipmentItems: [],
});

const isBusy = computed(() => isSubmitting.value || props.isWithdrawing);
const canWithdraw = computed(() => props.mode === "edit" && props.initialData?.canWithdraw === true);
const linkedEquipmentBooking = computed(() => props.initialData?.linkedEquipmentBooking ?? null);
const shouldShowEquipmentSection = computed(() => {
  return props.mode === "create" || Boolean(linkedEquipmentBooking.value);
});
const isEquipmentReadonly = computed(() => {
  return props.mode === "edit" && props.initialData?.equipmentReadonly === true;
});
const canEditLinkedEquipment = computed(() => {
  return props.mode === "edit" && Boolean(linkedEquipmentBooking.value?.id) && !isEquipmentReadonly.value;
});
const equipmentReadonlyMessage = computed(() => {
  return props.initialData?.equipmentReadonlyMessage || "本次只會更新場地預約。";
});

const selectedEquipmentIds = computed(() => {
  return new Set(formData.equipmentItems.map((item) => item.equipmentId));
});

const readonlyEquipmentItems = computed(() => {
  return Array.isArray(linkedEquipmentBooking.value?.items) ? linkedEquipmentBooking.value.items : [];
});

const equipmentSelectionOptions = computed(() => {
  const optionMap = new Map();

  equipmentOptions.value.forEach((equipment) => {
    if (!equipment?.id) return;
    optionMap.set(equipment.id, { ...equipment });
  });

  readonlyEquipmentItems.value.forEach((item) => {
    if (!item?.equipmentId || optionMap.has(item.equipmentId)) return;

    optionMap.set(item.equipmentId, {
      id: item.equipmentId,
      name: item.equipmentName || `Equipment ${item.equipmentId}`,
      totalQuantity: item.quantity || 1,
    });
  });

  return Array.from(optionMap.values());
});

const isEquipmentSelected = (equipmentId) => {
  return selectedEquipmentIds.value.has(equipmentId);
};

const resetEquipmentSelection = () => {
  formData.equipmentItems = [];
  Object.keys(equipmentQuantities).forEach((key) => {
    delete equipmentQuantities[key];
  });
};

const applyInitialEquipmentSelection = (items = []) => {
  resetEquipmentSelection();

  items.forEach((item) => {
    if (!item?.equipmentId) return;

    formData.equipmentItems.push({
      equipmentId: item.equipmentId,
      quantity: item.quantity || 1,
    });
    equipmentQuantities[item.equipmentId] = item.quantity || 1;
  });
};

const toggleEquipmentSelection = (equipmentId, checked) => {
  if (checked) {
    if (!isEquipmentSelected(equipmentId)) {
      formData.equipmentItems.push({ equipmentId, quantity: 1 });
    }
    equipmentQuantities[equipmentId] = equipmentQuantities[equipmentId] || 1;
    return;
  }

  formData.equipmentItems = formData.equipmentItems.filter((item) => item.equipmentId !== equipmentId);
  delete equipmentQuantities[equipmentId];
};

const loadEquipmentOptions = async () => {
  if (!props.venueInfo?.id || !shouldShowEquipmentSection.value || isEquipmentReadonly.value) {
    equipmentOptions.value = [];
    return;
  }

  isEquipmentLoading.value = true;

  try {
    const masters = normalizeEquipmentMasters(await listEquipments());
    equipmentOptions.value = masters.filter((equipment) =>
      isEquipmentAllowedForVenue(equipment, props.venueInfo.id),
    );
  } catch (loadError) {
    console.error("Failed to load equipment options:", loadError);
    equipmentOptions.value = [];
  } finally {
    isEquipmentLoading.value = false;
  }
};

watch(
  () => props.visible,
  (visible) => {
    isWithdrawConfirmVisible.value = false;

    if (!visible || !props.initialData) return;

    const initialSlots = Array.isArray(props.initialData.slots) ? [...props.initialData.slots] : [];
    const sortedInitialSlots = [...new Set(initialSlots)].sort((a, b) => a - b);
    const initialEquipmentItems = Array.isArray(props.initialData.linkedEquipmentBooking?.items)
      ? props.initialData.linkedEquipmentBooking.items
      : [];

    Object.assign(formData, {
      venueId: props.venueInfo?.id ?? null,
      bookingDate: props.initialData.dateStr || "",
      slots: sortedInitialSlots,
      purpose: props.initialData.purpose || "",
      participantCount: props.initialData.participantCount || 1,
      contactInfo: props.initialData.contactInfo?.name
        ? { ...props.initialData.contactInfo }
        : {
            name: "測試生",
            email: "student@ncu.edu.tw",
            phone: "0912345678",
          },
      equipmentItems: [],
    });

    applyInitialEquipmentSelection(initialEquipmentItems);
    slotRangeAnchor.value = sortedInitialSlots[0] ?? null;
    formErrors.slots = false;
    void loadEquipmentOptions();
  },
);

watch(
  () => props.venueInfo?.id,
  () => {
    if (props.visible) {
      formData.venueId = props.venueInfo?.id ?? null;
      void loadEquipmentOptions();
    }
  },
);

const padZero = (num) => num.toString().padStart(2, "0");

const slotOptions = Array.from({ length: 24 }, (_, index) => ({
  value: index,
  label: String(index),
  timeRange: `${padZero(index)}:00 - ${padZero(index + 1)}:00`,
}));

const buildSlotRange = (start, end) => {
  const minSlot = Math.min(start, end);
  const maxSlot = Math.max(start, end);
  return Array.from({ length: maxSlot - minSlot + 1 }, (_, index) => minSlot + index);
};

const handleSlotClick = (slot) => {
  const selectedSlots = [...new Set(formData.slots)].sort((a, b) => a - b);

  if (selectedSlots.length === 0) {
    slotRangeAnchor.value = slot;
    formData.slots = [slot];
    formErrors.slots = false;
    return;
  }

  const currentStart = selectedSlots[0];
  const currentEnd = selectedSlots[selectedSlots.length - 1];

  if (selectedSlots.length >= 2 && slot >= currentStart && slot <= currentEnd) {
    slotRangeAnchor.value = slot;
    formData.slots = [slot];
  } else if (slot < currentStart) {
    slotRangeAnchor.value = currentEnd;
    formData.slots = buildSlotRange(slot, currentEnd);
  } else if (slot > currentEnd) {
    slotRangeAnchor.value = currentStart;
    formData.slots = buildSlotRange(currentStart, slot);
  } else {
    slotRangeAnchor.value = slot;
    formData.slots = [slot];
  }

  formErrors.slots = false;
};

const closeModal = () => {
  if (isBusy.value) return;
  if (isWithdrawConfirmVisible.value) {
    isWithdrawConfirmVisible.value = false;
    return;
  }
  emit("update:visible", false);
};

const handleWithdraw = () => {
  if (isBusy.value || !canWithdraw.value || !props.initialData?.id) return;
  isWithdrawConfirmVisible.value = true;
};

const closeWithdrawConfirm = () => {
  if (isBusy.value) return;
  isWithdrawConfirmVisible.value = false;
};

const confirmWithdraw = () => {
  if (isBusy.value || !canWithdraw.value || !props.initialData?.id) return;
  isWithdrawConfirmVisible.value = false;
  emit("withdraw-booking", props.initialData.id);
};

const emitModalSuccess = (payload) => {
  emit("success", payload);
  emit("update:visible", false);
};

const handleSubmit = async () => {
  if (isBusy.value) return;

  if (formData.slots.length === 0) {
    formErrors.slots = true;
    return;
  }
  formErrors.slots = false;

  if (!formData.purpose || !formData.contactInfo.name || !formData.contactInfo.phone) {
    warning("請填寫所有必填欄位。");
    return;
  }

  if (canEditLinkedEquipment.value && formData.equipmentItems.length === 0) {
    warning("請至少保留一項器材品項。");
    return;
  }

  isSubmitting.value = true;

  try {
    const bookingPayload = {
      venueId: formData.venueId,
      bookingDate: formData.bookingDate,
      slots: [...formData.slots],
      purpose: formData.purpose,
      participantCount: formData.participantCount,
      contactInfo: { ...formData.contactInfo },
    };

    const equipmentItems = buildEquipmentBookingItems(
      formData.equipmentItems.map((item) => ({
        equipmentId: item.equipmentId,
        quantity: equipmentQuantities[item.equipmentId] || item.quantity,
      })),
    );

    if (props.mode === "create") {
      if (equipmentItems.length > 0) {
        await createBookingWithEquipments({
          booking: bookingPayload,
          equipmentItems,
        });
      } else {
        await createBooking(bookingPayload);
      }

      success("申請已成功送出。");
      emitModalSuccess({
        venueUpdated: true,
        equipmentUpdated: equipmentItems.length > 0 ? true : null,
        partial: false,
      });
      return;
    }

    await updateBooking(props.initialData.id, bookingPayload);

    if (canEditLinkedEquipment.value && linkedEquipmentBooking.value?.id) {
      try {
        await updateEquipmentBooking(linkedEquipmentBooking.value.id, {
          borrowDate: formData.bookingDate,
          slots: [...formData.slots],
          purpose: formData.purpose,
          contactInfo: { ...formData.contactInfo },
          relatedVenueBookingId: props.initialData.id,
          items: equipmentItems,
        });

        success("場地與器材預約已成功修改。");
        emitModalSuccess({
          venueUpdated: true,
          equipmentUpdated: true,
          partial: false,
        });
        return;
      } catch (equipmentError) {
        warning(equipmentError.message || "場地已更新，但器材更新失敗。");
        emitModalSuccess({
          venueUpdated: true,
          equipmentUpdated: false,
          partial: true,
        });
        return;
      }
    }

    if (isEquipmentReadonly.value) {
      success("場地預約已成功修改，器材申請未變更。");
    } else {
      success("預約已成功修改。");
    }

    emitModalSuccess({
      venueUpdated: true,
      equipmentUpdated: null,
      partial: false,
    });
  } catch (submitError) {
    showError(submitError.message || "場地更新失敗，請確認時段是否衝突。");
  } finally {
    isSubmitting.value = false;
  }
};
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
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1040px;
  max-height: 90vh;
  overflow: hidden;
  background: var(--card);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--line);

  h2 {
    margin: 0;
    color: var(--ink);
    font-size: var(--text-xl);
  }
}

.close-btn {
  border: 0;
  background: none;
  color: var(--muted);
  font-size: 1.5rem;
  cursor: pointer;
}

.modal-body {
  overflow-y: auto;
  padding: 1.5rem 1.75rem;
  background: #fbfcfe;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--line);
}

.modal-footer-actions {
  display: flex;
  gap: 1rem;
  margin-left: auto;
}

.btn-icon {
  display: inline-flex;
  width: 1rem;
  height: 1rem;
  align-items: center;
  justify-content: center;
}

.btn-danger {
  background: #fff5f5;
  border-color: rgba(185, 28, 28, 0.18);
  color: #b91c1c;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
  color: #ffffff;
}

.confirm-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background: rgba(15, 23, 42, 0.5);
}

.confirm-dialog {
  width: min(100%, 360px);
  padding: 1.25rem;
  background: #ffffff;
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  border-radius: var(--radius);
  box-shadow: var(--shadow);

  h3 {
    margin: 0 0 0.6rem;
    color: var(--ink);
    font-size: 1.05rem;
  }

  p {
    margin: 0;
    color: var(--muted-strong);
    font-size: var(--text-sm);
    line-height: 1.6;
  }
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}

.booking-form-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(360px, 1.05fr);
  gap: 1.75rem;
  align-items: start;
}

.booking-form-panel {
  min-width: 0;
}

.booking-form-panel-details {
  padding-left: 1.75rem;
  border-left: 1px solid var(--line);
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-row {
  display: flex;
  gap: 1rem;

  .form-group {
    flex: 1;
  }
}

.form-section-title {
  margin: 1rem 0 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--line);
  color: var(--ink);
  font-weight: 700;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--ink);
  font-size: var(--text-sm);
  font-weight: 500;
}

.required {
  color: var(--danger);
}

input[type="text"],
input[type="number"],
input[type="tel"],
input[type="email"] {
  width: 100%;
  padding: 0.75rem 0.85rem;
  border: 1px solid var(--line-strong);
  border-radius: var(--radius-sm);
  background: #ffffff;

  &:focus {
    outline: none;
    border-color: var(--accent);
  }
}

.disabled-input {
  background-color: var(--surface-muted);
  color: var(--muted);
  cursor: not-allowed;
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
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.slot-checkbox::before {
  content: "";
  position: absolute;
  top: 0.4rem;
  bottom: 0.4rem;
  left: 0.35rem;
  width: 0;
  border-radius: 999px;
  background: var(--accent);
  opacity: 0;
  transition:
    opacity 0.2s ease,
    width 0.2s ease;
}

.slot-checkbox:hover,
.slot-checkbox:focus-within {
  border-color: rgba(var(--blue-900-rgb), 0.42);
  background: var(--accent-soft);
  box-shadow: 0 0 0 3px rgba(var(--blue-900-rgb), 0.12);
  transform: translateY(-1px);
}

.slot-checkbox.is-selected {
  border-color: rgba(var(--blue-900-rgb), 0.36);
  background: var(--accent-soft);
  color: var(--accent-hover);
  box-shadow: 0 6px 16px rgba(var(--blue-900-rgb), 0.1);
}

.slot-checkbox.is-selected::before {
  width: 4px;
  opacity: 1;
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
}

.slot-index {
  color: var(--accent);
  text-align: center;
  font-size: clamp(0.9rem, 1.05vw, 1.1rem);
}

.slot-time {
  text-align: center;
  font-size: clamp(0.9rem, 1.05vw, 1.1rem);
}

.slot-checkbox.is-selected .slot-index,
.slot-checkbox.is-selected .slot-time {
  color: var(--accent-hover);
}

.equipment-list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.equipment-list-heading {
  margin: 0 0 0.6rem;
  color: var(--ink);
  font-size: var(--text-sm);
  font-weight: 700;
}

.equipment-option,
.equipment-readonly-item {
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

  strong {
    color: var(--ink);
  }

  span {
    color: var(--muted);
  }
}

.equipment-option {
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

.equipment-option input[type="checkbox"] {
  flex-shrink: 0;
  margin: 0;
  accent-color: var(--accent);
}

.equipment-option input[type="number"] {
  width: 5rem;
  min-height: 2.2rem;
  margin-left: auto;
}

.equipment-readonly-panel {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}

.equipment-readonly-list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.equipment-helper {
  color: var(--muted);
  font-size: var(--text-sm);
}

.error-text {
  display: block;
  margin-top: 0.25rem;
  color: var(--danger);
  font-size: var(--text-sm);
}

@media (max-width: 900px) {
  .modal-container {
    max-width: min(100%, 680px);
  }

  .booking-form-layout {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }

  .booking-form-panel-details {
    padding-top: 1.25rem;
    padding-left: 0;
    border-top: 1px solid var(--line);
    border-left: 0;
  }

  .slots-grid {
    max-height: 42vh;
  }
}

@media (max-width: 640px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .modal-body {
    padding: 1rem;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .modal-footer {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .modal-footer-actions {
    display: contents;
  }

  .confirm-overlay {
    padding: 1rem;
  }

  .confirm-actions {
    flex-direction: column-reverse;
  }

  .slots-grid {
    gap: 0.28rem;
    max-height: 40vh;
    padding: 0.35rem;
  }

  .slot-checkbox {
    grid-template-columns: minmax(2rem, 0.25fr) minmax(0, 1fr);
    min-height: 2.35rem;
    padding: 0 0.55rem;
  }

  .slot-index,
  .slot-time {
    font-size: 0.88rem;
  }

  .equipment-option,
  .equipment-readonly-item {
    align-items: flex-start;
  }

  .equipment-option input[type="number"] {
    width: 100%;
    margin-left: 0;
  }
}
</style>
