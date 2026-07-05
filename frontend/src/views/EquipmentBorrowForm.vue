<template>
  <div class="equipment-borrow-page page-enter">
    <header class="page-header equipment-borrow-header">
      <button class="back-btn" type="button" @click="router.back()">← 返回上一頁</button>
      <p class="hero-eyebrow">Equipment Request</p>
      <h1>設備借用申請</h1>
      <p>填寫借用日期、時段與設備數量，送出後由審核者確認。</p>
    </header>

    <div v-if="loading" class="loading-state">載入設備資料中...</div>

    <div v-else-if="loadError" class="empty-state equipment-feedback">
      <h3>目前無法載入設備資料</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadEquipments">重新載入</button>
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
            >
              <input v-model="form.slots" type="checkbox" :value="slot.value" />
              <span>{{ slot.label }}</span>
            </label>
          </div>
        </div>
      </section>

      <section class="form-panel">
        <div class="form-section-title">借用內容</div>
        <label class="form-group">
          <span>借用用途 <b class="required">*</b></span>
          <input v-model.trim="form.purpose" type="text" required placeholder="例如：社團活動器材" />
        </label>

        <div class="equipment-list">
          <label v-for="equipment in equipmentOptions" :key="equipment.id" class="equipment-option">
            <input
              type="checkbox"
              :checked="isEquipmentSelected(equipment.id)"
              @change="toggleEquipment(equipment.id, $event.target.checked)"
            />
            <strong>{{ equipment.name }}</strong>
            <span>總量 {{ equipment.totalQuantity }}</span>
            <input
              v-if="isEquipmentSelected(equipment.id)"
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
        <button type="button" class="btn btn-secondary" :disabled="submitting" @click="router.back()">取消</button>
        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? "送出中..." : "送出設備借用" }}
        </button>
      </footer>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { createEquipmentBooking, listEquipments } from "@/api/equipment";
import {
  buildEquipmentBookingItems,
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
  label: `${String(hour).padStart(2, "0")}:00 - ${String(hour + 1).padStart(2, "0")}:00`,
}));

const selectedEquipmentIds = computed(() => new Set(form.equipmentItems.map((item) => item.equipmentId)));

const isEquipmentSelected = (equipmentId) => selectedEquipmentIds.value.has(equipmentId);

const toggleEquipment = (equipmentId, checked) => {
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

  if (form.slots.length === 0) {
    toast.warning("請至少選擇一個借用時段");
    return;
  }

  if (equipmentItems.length === 0) {
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
      items: equipmentItems,
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
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem;
  max-height: 25rem;
  overflow-y: auto;
  padding: 0.5rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-muted);
}

.slot-checkbox,
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
}

.slot-checkbox.is-selected {
  background: var(--accent-soft);
  color: var(--accent);
}

.equipment-list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  margin-bottom: 1rem;
}

.equipment-option {
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

@media (max-width: 820px) {
  .borrow-form {
    grid-template-columns: 1fr;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .slots-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }
}
</style>
