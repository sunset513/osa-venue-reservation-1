<template>
  <div class="equipment-borrow-page page-enter">
    <header class="page-header equipment-borrow-header">
      <button class="back-btn" type="button" @click="router.back()">
        <span class="btn-icon" aria-hidden="true">
          <ArrowLeft :size="16" />
        </span>
        <span>返回上一頁</span>
      </button>
      <p class="hero-eyebrow">Equipment Request</p>
      <h1 class="page-title">
        <Wrench :size="28" aria-hidden="true" class="page-title-icon" />
        <span>設備借用申請</span>
      </h1>
      <p>填寫借用日期、時段與設備數量，送出後由審核者確認。</p>
    </header>

    <div v-if="loading" class="loading-state">載入設備資料中...</div>

    <div v-else-if="loadError" class="empty-state equipment-feedback">
      <h3>目前無法載入設備資料</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadEquipments">
        <span class="btn-icon" aria-hidden="true">
          <RotateCcw :size="16" />
        </span>
        <span>重新載入</span>
      </button>
    </div>

    <form v-else class="borrow-form card" @submit.prevent="submitBorrowRequest">
      <section class="form-panel">
        <div class="form-section-title">借用時間</div>
        <div class="form-row">
          <label class="form-group">
            <span>借用日期 <b class="required">*</b></span>
            <input v-model="form.borrowDate" type="date" required />
          </label>
        </div>

        <div class="form-group">
          <span>選擇時段 <b class="required">*</b></span>
          <div class="slots-grid" role="group">
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

      <section class="form-panel">
        <div class="form-section-title">借用內容</div>
        <label class="form-group">
          <span>借用用途 <b class="required">*</b></span>
          <input v-model.trim="form.purpose" type="text" required placeholder="例如：社團活動器材" />
        </label>

        <div class="equipment-list-heading">選擇設備</div>
        <p class="equipment-list-helper">
          綁定特定場地的設備需從對應場地的場地借用流程一併申請，無法在此頁單獨借用。
        </p>
        <div class="equipment-list">
          <label
            v-for="equipment in equipmentOptions"
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
            <span>總量 {{ equipment.totalQuantity }}</span>
            <input
              v-if="isEquipmentSelected(equipment.id) && !isStandaloneRestricted(equipment)"
              v-model.number="equipmentQuantities[equipment.id]"
              type="number"
              min="1"
              :max="equipment.totalQuantity"
              aria-label="設備借用數量"
            />
          </label>
        </div>

        <div class="form-section-title">聯絡人資訊</div>
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

      <footer class="form-actions">
        <button type="button" class="btn btn-secondary" :disabled="submitting" @click="router.back()">
          <span class="btn-icon" aria-hidden="true">
            <X :size="16" />
          </span>
          <span>取消</span>
        </button>
        <button type="submit" class="btn btn-primary" :disabled="submitting">
          <template v-if="!submitting">
            <span class="btn-icon" aria-hidden="true">
              <Send :size="16" />
            </span>
          </template>
          <span>{{ submitting ? "送出中..." : "送出設備借用" }}</span>
        </button>
      </footer>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ArrowLeft, RotateCcw, Send, Wrench, X } from "lucide-vue-next";
import { createEquipmentBooking, listEquipments } from "@/api/equipment";
import {
  buildEquipmentBookingItems,
  canBorrowEquipmentStandalone,
  formatEquipmentBoundVenueText,
  normalizeEquipmentMasters,
} from "@/utils/equipment";
import { useToast } from "@/utils/useToast";

const router = useRouter();
const toast = useToast();

const loading = ref(true);
const loadError = ref("");
const submitting = ref(false);
const equipmentOptions = ref([]);
const equipmentQuantities = reactive({});
const slotRangeAnchor = ref(null);
const formErrors = reactive({ slots: false });

const form = reactive({
  borrowDate: new Date().toLocaleDateString("sv-SE"),
  slots: [],
  purpose: "",
  contactInfo: {
    name: "測試生",
    email: "student@ncu.edu.tw",
    phone: "0912345678",
  },
  equipmentItems: [],
});

const slotOptions = Array.from({ length: 24 }, (_, hour) => ({
  value: hour,
  label: String(hour),
  timeRange: `${String(hour).padStart(2, "0")}:00 - ${String(hour + 1).padStart(2, "0")}:00`,
}));

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

const selectedEquipmentIds = computed(() => new Set(form.equipmentItems.map((item) => item.equipmentId)));

const isEquipmentSelected = (equipmentId) => selectedEquipmentIds.value.has(equipmentId);

const isStandaloneRestricted = (equipment) => !canBorrowEquipmentStandalone(equipment);

const getEquipmentBoundVenueText = (equipment) => formatEquipmentBoundVenueText(equipment);

const toggleEquipment = (equipmentId, checked) => {
  const equipment = equipmentOptions.value.find((option) => option.id === equipmentId);
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

const loadEquipments = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    equipmentOptions.value = normalizeEquipmentMasters(await listEquipments());
  } catch (error) {
    console.error("載入設備資料失敗:", error);
    loadError.value = error.message || "請稍後再試一次。";
  } finally {
    loading.value = false;
  }
};

const submitBorrowRequest = async () => {
  const equipmentItems = buildEquipmentBookingItems(
    form.equipmentItems.map((item) => ({
      equipmentId: item.equipmentId,
      quantity: equipmentQuantities[item.equipmentId] || item.quantity,
    })),
  );
  const standaloneEquipmentItems = equipmentItems.filter((item) => {
    const equipment = equipmentOptions.value.find((option) => option.id === item.equipmentId);
    return Boolean(equipment) && canBorrowEquipmentStandalone(equipment);
  });

  if (form.slots.length === 0) {
    formErrors.slots = true;
    toast.warning("請至少選擇一個借用時段");
    return;
  }

  formErrors.slots = false;

  if (standaloneEquipmentItems.length === 0) {
    toast.warning("請至少選擇一項設備");
    return;
  }

  submitting.value = true;

  try {
    await createEquipmentBooking({
      borrowDate: form.borrowDate,
      slots: [...form.slots].sort((a, b) => a - b),
      purpose: form.purpose,
      contactInfo: form.contactInfo,
      relatedVenueBookingId: null,
      items: standaloneEquipmentItems,
    });
    toast.success("設備借用申請已送出");
    await router.push({ name: "EquipmentBorrowHistory" });
  } catch (error) {
    toast.error(error.message || "設備借用申請送出失敗");
  } finally {
    submitting.value = false;
  }
};

onMounted(loadEquipments);
</script>

<style lang="scss" scoped>
.equipment-borrow-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.equipment-borrow-header {
  margin-bottom: 0;
}

.hero-eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  text-transform: uppercase;
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
}

.page-title-icon {
  color: currentColor;
  flex-shrink: 0;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.borrow-form {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(320px, 1.1fr);
  gap: 1.5rem;
  padding: 1.35rem;
}

.form-panel {
  min-width: 0;
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

.equipment-list {
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

  strong {
    color: var(--ink);
  }

  span {
    color: var(--muted);
  }

  input[type="number"] {
    width: 5rem;
    margin-left: auto;
  }
}

.equipment-option.is-selected {
  border-color: rgba(var(--blue-900-rgb), 0.28);
  background: var(--accent-soft);
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

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 0.85rem;
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}

.equipment-feedback {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
  text-align: center;
}

.error-text {
  color: var(--danger);
  font-size: var(--text-sm);
  margin-top: 0.25rem;
  display: block;
}

@media (max-width: 820px) {
  .borrow-form {
    grid-template-columns: 1fr;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .slots-grid {
    gap: 0.28rem;
    max-height: 40vh;
    padding: 0.35rem;
  }

  .form-actions {
    flex-direction: column-reverse;
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
}
</style>
