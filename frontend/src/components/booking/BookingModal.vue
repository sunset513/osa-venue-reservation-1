<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container">
      <header class="modal-header">
        <h2>{{ mode === "create" ? "新增預約申請" : "修改預約申請" }}</h2>
        <button class="close-btn" @click="closeModal">✕</button>
      </header>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="booking-form">
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
            <div class="slots-grid">
              <label v-for="hour in 24" :key="hour" class="slot-checkbox">
                <input
                  type="checkbox"
                  :value="hour - 1"
                  v-model="formData.slots"
                />
                <span>{{ padZero(hour - 1) }}:00 - {{ padZero(hour) }}:00</span>
              </label>
            </div>
            <small class="error-text" v-if="formErrors.slots"
              >請至少選擇一個時段</small
            >
          </div>

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
            <label>需借用設備</label>
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
        </form>
      </div>

      <footer class="modal-footer">
        <button
          class="btn btn-secondary"
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
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { Plus, Save, X } from "lucide-vue-next";
import { createBooking, updateBooking } from "@/api/booking";
import { useToast } from "@/utils/useToast.js";

const { success, error: showError, warning } = useToast();

const props = defineProps({
  visible: Boolean,
  mode: { type: String, default: "create" },
  initialData: Object,
  venueInfo: Object,
});

const emit = defineEmits(["update:visible", "success"]);

const isSubmitting = ref(false);
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
    if (newVal && props.initialData) {
      Object.assign(formData, {
        venueId: props.venueInfo?.id,
        bookingDate: props.initialData.dateStr || "",
        slots: props.initialData.slots ? [...props.initialData.slots] : [],
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
      formErrors.slots = false;
    }
  },
);

const padZero = (num) => num.toString().padStart(2, "0");

const closeModal = () => {
  if (isSubmitting.value) return;
  emit("update:visible", false);
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
  background: var(--card);
  width: 100%;
  max-width: 500px;
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
  padding: 1.5rem;
  overflow-y: auto;
  background: #fbfcfe;
}
.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--line);
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
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
  max-height: 150px;
  overflow-y: auto;
  padding: 0.5rem;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: #ffffff;
}
.slot-checkbox,
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
@media (max-width: 640px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .modal-footer {
    flex-direction: column-reverse;
  }
}
</style>
