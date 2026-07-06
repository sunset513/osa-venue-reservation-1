<template>
  <div class="equipment-page page-enter">
    <header class="page-header equipment-header">
      <button class="back-btn" type="button" @click="router.push('/')">
        ← {{ BACK_TO_UNIT_SELECTOR_LABEL }}
      </button>
      <p class="hero-eyebrow">Equipment Status</p>
      <h1>設備狀態管理</h1>
      <p>查看各項設備目前是否出借，並展開檢視借用人、用途與關聯場地。</p>
    </header>

    <section class="status-toolbar card">
      <label>
        查詢日期
        <input v-model="queryDate" type="date" />
      </label>
      <label>
        查詢小時
        <select v-model.number="queryHour">
          <option v-for="hour in hourOptions" :key="hour" :value="hour">
            {{ String(hour).padStart(2, "0") }}:00
          </option>
        </select>
      </label>
      <button type="button" class="btn btn-secondary" @click="loadStatuses">重新查詢</button>
      <button type="button" class="btn btn-primary" @click="router.push({ name: 'EquipmentBorrowForm' })">
        新增設備借用
      </button>
    </section>

    <div v-if="loading" class="loading-state">載入設備資料中...</div>

    <div v-else-if="loadError" class="empty-state equipment-feedback">
      <h3>目前無法載入設備資料</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadStatuses">重新載入</button>
    </div>

    <div v-else class="equipment-content">
      <section class="equipment-table-wrap">
        <table class="equipment-table">
          <thead>
            <tr>
              <th>設備名稱</th>
              <th>總量</th>
              <th>借出</th>
              <th>可用</th>
              <th>狀態</th>
              <th>紀錄</th>
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
                    <ChevronDown
                      :size="16"
                      aria-hidden="true"
                      :class="{ 'is-collapsed': !isExpanded(equipment.equipmentId) }"
                    />
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
                  <RouterLink class="history-url-link" :to="borrowHistoryRoute(equipment)">
                    查看借用紀錄
                  </RouterLink>
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
                      <strong>#{{ booking.equipmentBookingId }}｜{{ booking.purpose }}</strong>
                      <span>{{ booking.borrowDate }}｜{{ formatSlotGroupsAsTimeRange(booking.slots) }}</span>
                      <span>數量 {{ booking.quantity }}｜申請人 {{ booking.userId }}</span>
                      <span v-if="booking.relatedVenueName">關聯場地：{{ booking.relatedVenueName }}</span>
                      <span>{{ booking.contact.name || "未提供姓名" }}｜{{ booking.contact.phone || "未提供電話" }}</span>
                    </article>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ChevronDown } from "lucide-vue-next";
import { getEquipmentStatuses } from "@/api/equipment";
import {
  getEquipmentStatusMeta,
  normalizeEquipmentStatuses,
} from "@/utils/equipment";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { BACK_TO_UNIT_SELECTOR_LABEL } from "@/utils/navigationLabels";

const router = useRouter();

const loading = ref(true);
const loadError = ref("");
const equipmentStatuses = ref([]);
const expandedEquipmentIds = ref(new Set());
const queryDate = ref(new Date().toLocaleDateString("sv-SE"));
const queryHour = ref(new Date().getHours());
const hourOptions = Array.from({ length: 24 }, (_, hour) => hour);

const isExpanded = (equipmentId) => expandedEquipmentIds.value.has(equipmentId);

const toggleExpanded = (equipmentId) => {
  // Keep the expanded state in a Set so the user can inspect multiple active
  // equipment rows at once without losing context when checking related details.
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

const loadStatuses = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    // The status endpoint returns one row per equipment and already contains
    // active approved booking details, so the view does not need additional
    // per-equipment requests before showing the expandable detail section.
    equipmentStatuses.value = normalizeEquipmentStatuses(
      await getEquipmentStatuses({
        date: queryDate.value,
        hour: queryHour.value,
      }),
    );
  } catch (error) {
    console.error("載入設備狀態失敗:", error);
    equipmentStatuses.value = [];
    loadError.value = error.message || "請稍後再試一次。";
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
  min-width: 760px;
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
  gap: 0.45rem;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink);
  cursor: pointer;

  &:disabled {
    cursor: default;
  }

  .is-collapsed {
    transform: rotate(-90deg);
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

.history-url-link {
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
  text-decoration: underline;
  text-underline-offset: 0.18rem;
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
}
</style>
