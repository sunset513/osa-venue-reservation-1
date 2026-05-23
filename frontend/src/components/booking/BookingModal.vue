<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container">
      <header class="modal-header">
        <h2>{{ mode === "create" ? "新增預約申請" : "修改預約申請" }}</h2>
        <button class="close-btn" @click="closeModal">✕</button>
      </header>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="booking-form">
          <div class="booking-form-layout">
            <section class="booking-form-panel booking-form-panel-times">
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
            <small class="error-text" v-if="formErrors.slots"
              >請至少選擇一個時段</small
            >
          </div>
            </section>

            <section class="booking-form-panel booking-form-panel-details">
          <div class="form-section-title">借用說明</div>
          <div class="form-row">
            <div class="form-group">
              <label>使用用途 <span class="required">*</span></label>
              <input
                type="text"
                v-model="formData.purpose"
                placeholder="例如：系學會開會"
                required
              />
            </div>
            <div class="form-group">
              <label>預估人數 <span class="required">*</span></label>
              <input
                type="number"
                v-model="formData.participantCount"
                min="1"
                required
              />
            </div>
          </div>

          <div class="form-section-title">聯絡人資訊</div>
          <div class="form-row">
            <div class="form-group">
              <label>姓名 <span class="required">*</span></label>
              <input type="text" v-model="formData.contactInfo.name" required />
            </div>
            <div class="form-group">
              <label>電話 <span class="required">*</span></label>
              <input type="tel" v-model="formData.contactInfo.phone" required />
            </div>
          </div>
          <div class="form-group">
            <label>電子郵件 <span class="required">*</span></label>
            <input type="email" v-model="formData.contactInfo.email" required />
          </div>

          <div
            class="form-group"
            v-if="venueInfo && venueInfo.equipments?.length > 0"
          >
            <div class="form-section-title">需借用設備</div>
            <div class="equipments-flex">
              <label
                v-for="eq in venueInfo.equipments"
                :key="eq.id"
                class="eq-checkbox"
              >
                <input
                  type="checkbox"
                  :value="eq.id"
                  v-model="formData.equipmentIds"
                />
                {{ eq.name }}
              </label>
            </div>
          </div>
            </section>
          </div>
        </form>
      </div>

      <footer class="modal-footer">
        <button
          v-if="mode === 'edit'"
          class="btn btn-danger"
          type="button"
          @click="handleWithdraw"
          :disabled="isSubmitting"
        >
          <span class="btn-icon">
            <Undo2 :size="16" />
          </span>
          <span>撤回申請</span>
        </button>

        <div class="modal-footer-actions">
          <button
            class="btn btn-secondary"
            type="button"
            @click="closeModal"
            :disabled="isSubmitting"
          >
            <span class="btn-icon">
              <X :size="16" />
            </span>
            <span>取消</span>
          </button>
          <button
            class="btn btn-primary"
            type="button"
            @click="handleSubmit"
            :disabled="isSubmitting"
          >
            <template v-if="!isSubmitting">
              <span class="btn-icon">
                <Plus v-if="mode === 'create'" :size="16" />
                <Save v-if="mode === 'edit'" :size="16" />
              </span>
            </template>
            <span>
              {{
                isSubmitting
                  ? "送出中..."
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
          <p>撤回後這筆預約會改成已撤回，目前頁面會先以前端狀態更新，尚未串接 API。</p>
          <div class="confirm-actions">
            <button
              class="btn btn-secondary"
              type="button"
              @click="closeWithdrawConfirm"
            >
              取消
            </button>
            <button
              class="btn btn-danger"
              type="button"
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
import { ref, reactive, watch } from "vue";
import { Plus, Save, Undo2, X } from "lucide-vue-next";
import { createBooking, updateBooking } from "@/api/booking";
import { useToast } from "@/utils/useToast.js";

const { success, error: showError, warning } = useToast();

const props = defineProps({
  visible: Boolean,
  mode: { type: String, default: "create" },
  initialData: Object,
  venueInfo: Object,
});

const emit = defineEmits(["update:visible", "success", "withdraw-booking"]);

const isSubmitting = ref(false);
const isWithdrawConfirmVisible = ref(false);
const slotRangeAnchor = ref(null);
const formErrors = reactive({ slots: false });

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
  equipmentIds: [],
});

// ✨ 資料回顯邏輯
watch(
  () => props.visible,
  (newVal) => {
    isWithdrawConfirmVisible.value = false;

    if (newVal && props.initialData) {
      const initialSlots = props.initialData.slots ? [...props.initialData.slots] : [];
      const sortedInitialSlots = [...new Set(initialSlots)].sort((a, b) => a - b);

      Object.assign(formData, {
        venueId: props.venueInfo?.id,
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
        equipmentIds: props.initialData.equipmentIds
          ? [...props.initialData.equipmentIds]
          : [], // ✨ 設備 ID 回顯
      });
      slotRangeAnchor.value = sortedInitialSlots[0] ?? null;
      formErrors.slots = false;
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

  if (slot < currentStart) {
    slotRangeAnchor.value = currentEnd;
    formData.slots = buildSlotRange(slot, currentEnd);
  } else if (slot > currentEnd) {
    slotRangeAnchor.value = currentStart;
    formData.slots = buildSlotRange(currentStart, slot);
  } else {
    const anchor = slotRangeAnchor.value ?? currentStart;
    formData.slots = buildSlotRange(anchor, slot);
    slotRangeAnchor.value = anchor;
  }

  formErrors.slots = false;
};

const closeModal = () => {
  if (isSubmitting.value) return;
  if (isWithdrawConfirmVisible.value) {
    isWithdrawConfirmVisible.value = false;
    return;
  }
  emit("update:visible", false);
};

const handleWithdraw = () => {
  if (isSubmitting.value || props.mode !== "edit" || !props.initialData?.id) return;
  isWithdrawConfirmVisible.value = true;
};

const closeWithdrawConfirm = () => {
  if (isSubmitting.value) return;
  isWithdrawConfirmVisible.value = false;
};

const confirmWithdraw = () => {
  if (isSubmitting.value || props.mode !== "edit" || !props.initialData?.id) return;
  isWithdrawConfirmVisible.value = false;
  emit("withdraw-booking", props.initialData.id);
};

const handleSubmit = async () => {
  if (formData.slots.length === 0) {
    formErrors.slots = true;
    return;
  }
  formErrors.slots = false;

  if (
    !formData.purpose ||
    !formData.contactInfo.name ||
    !formData.contactInfo.phone
  ) {
    warning("請填寫所有必填欄位 (*)");
    return;
  }

  isSubmitting.value = true;
  try {
    if (props.mode === "create") {
      await createBooking(formData);
      success("申請已成功送出！");
    } else {
      await updateBooking(props.initialData.id, formData);
      success("申請已成功修改！");
    }

    // ✨ 執行成功：發送成功通知並關閉彈窗
    emit("success");
    emit("update:visible", false);
  } catch (error) {
    showError(error.message || "操作失敗，請確認時段是否衝突");
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1200;
  padding: 1rem;
}
.modal-container {
  position: relative;
  background: var(--card);
  width: 100%;
  max-width: 1040px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow: hidden;
}
.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
  h2 {
    margin: 0;
    font-size: var(--text-xl);
    color: var(--ink);
  }
  .close-btn {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: var(--muted);
  }
}
.modal-body {
  padding: 1.5rem 1.75rem;
  overflow-y: auto;
  background: #fbfcfe;
}
.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}
.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
}
.btn-danger {
  border-color: rgba(185, 28, 28, 0.18);
  background: #fff5f5;
  color: #b91c1c;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
  color: #ffffff;
}
.confirm-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.5rem;
}

.confirm-dialog {
  width: min(100%, 360px);
  background: #ffffff;
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 1.25rem;

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
  font-weight: 700;
  color: var(--ink);
  margin: 1rem 0 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--line);
}
label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--ink);
  font-size: var(--text-sm);
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
  padding: 0 0.7rem;
  margin: 0;
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
  text-align: center;
  color: var(--accent);
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

.eq-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--text-sm);
  cursor: pointer;
  margin: 0;
}
.equipments-flex {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}
.error-text {
  color: var(--danger);
  font-size: var(--text-sm);
  margin-top: 0.25rem;
  display: block;
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
    padding-left: 0;
    border-left: 0;
    border-top: 1px solid var(--line);
    padding-top: 1.25rem;
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

  .slot-index {
    font-size: 0.88rem;
  }

  .slot-time {
    font-size: 0.88rem;
  }
}
</style>
