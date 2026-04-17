<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <header class="modal-header">
        <div class="modal-title-group">
          <span class="modal-title-icon">審</span>
          <div>
            <p class="eyebrow">申請審核</p>
            <h2>{{ booking?.purpose || "預約申請詳情" }}</h2>
          </div>
        </div>
        <button class="close-btn" type="button" @click="$emit('close')">✕</button>
      </header>

      <div v-if="loading" class="loading-panel">載入申請資訊中...</div>

      <div v-else-if="booking" class="modal-body">
        <section class="summary-grid">
          <article class="summary-card status-card">
            <span class="summary-label">目前狀態</span>
            <span class="status-pill" :class="statusMeta.className">
              {{ statusMeta.text }}
            </span>
            <span class="summary-subtle">申請編號 #{{ booking.id }}</span>
          </article>

          <article class="summary-card">
            <span class="summary-label">使用日期</span>
            <strong>{{ formatDateDisplay(booking.bookingDate) }}</strong>
            <span class="summary-subtle">{{ formatSlotGroupsAsTimeRange(booking.slots) }}</span>
          </article>

          <article class="summary-card">
            <span class="summary-label">場地</span>
            <strong>{{ booking.venueName || "未提供" }}</strong>
            <span class="summary-subtle">申請時間 {{ formatDateTime(booking.createdAt) }}</span>
          </article>
        </section>

        <section class="detail-grid">
          <article class="detail-card detail-card-wide">
            <span class="detail-label">申請目的</span>
            <p>{{ booking.purpose || "未填寫用途" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">申請時段</span>
            <p>{{ formatSlotGroupsAsTimeRange(booking.slots) || "未提供" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">使用人數</span>
            <p>{{ booking.pCount || 0 }} 人</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">聯絡人</span>
            <p>{{ contactInfo.name || "未提供" }}</p>
            <p class="detail-subtle">{{ contactInfo.phone || "未提供電話" }}</p>
            <p class="detail-subtle">{{ contactInfo.email || "未提供信箱" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">設備借用</span>
            <ul v-if="booking.equipments?.length" class="equipment-list">
              <li v-for="equipment in booking.equipments" :key="equipment">{{ equipment }}</li>
            </ul>
            <p v-else>未借用設備</p>
          </article>
        </section>
      </div>

      <footer class="modal-footer">
        <button class="btn btn-secondary" type="button" :disabled="processing" @click="$emit('close')">
          關閉
        </button>
        <button
          v-for="action in actions"
          :key="action.key"
          class="btn"
          :class="action.variant"
          type="button"
          :disabled="processing"
          @click="emitAction(action)"
        >
          {{ action.label }}
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { formatSlotGroupsAsTimeRange, getBookingStatusMeta } from "@/utils/dateHelper";

const props = defineProps({
  visible: Boolean,
  booking: {
    type: Object,
    default: null,
  },
  loading: Boolean,
  processing: Boolean,
});

const emit = defineEmits(["close", "approve", "update-status"]);

const statusMeta = computed(() => getBookingStatusMeta(props.booking?.status));

const contactInfo = computed(() => {
  if (!props.booking?.contactInfo) {
    return { name: "", phone: "", email: "" };
  }

  try {
    return JSON.parse(props.booking.contactInfo);
  } catch (error) {
    console.error("聯絡資訊解析失敗:", error);
    return { name: "", phone: "", email: "" };
  }
});

const actions = computed(() => {
  switch (props.booking?.status) {
    case 1:
      return [
        { key: "reject", label: "拒絕申請", variant: "btn-danger", type: "update-status", status: 3 },
        { key: "approve", label: "通過申請", variant: "btn-primary", type: "approve" },
      ];
    case 2:
      return [
        { key: "revoke", label: "改為拒絕", variant: "btn-danger", type: "update-status", status: 3 },
      ];
    case 3:
      return [
        { key: "pending", label: "改為審核中", variant: "btn-secondary-alt", type: "update-status", status: 1 },
        { key: "approve-rejected", label: "改為通過", variant: "btn-primary", type: "update-status", status: 2 },
      ];
    case 0:
      return [
        { key: "restore", label: "改為審核中", variant: "btn-secondary-alt", type: "update-status", status: 1 },
      ];
    default:
      return [];
  }
});

const formatDateDisplay = (dateString) => {
  if (!dateString) return "未提供";

  const date = new Date(`${dateString}T00:00:00`);
  return date.toLocaleDateString("zh-TW", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
  });
};

const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return "未提供";

  const date = new Date(dateTimeString);
  return date.toLocaleString("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

const emitAction = (action) => {
  if (action.type === "approve") {
    emit("approve");
    return;
  }

  emit("update-status", action.status);
};
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.45);
}

.modal-container {
  width: min(100%, 960px);
  max-height: min(92vh, 920px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid rgba(var(--blue-900-rgb), 0.1);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
}

.modal-header,
.modal-footer {
  padding: 1.4rem 1.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.modal-header {
  border-bottom: 1px solid var(--line);
}

.modal-title-group {
  display: flex;
  align-items: center;
  gap: 1rem;

  h2,
  p {
    margin: 0;
  }
}

.eyebrow {
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.modal-title-icon {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f4d37b 0%, #efd28a 100%);
  color: #684b00;
  font-size: 1.15rem;
  font-weight: 800;
}

.close-btn {
  padding: 0;
  border: none;
  background: none;
  color: var(--muted);
  font-size: 1.8rem;
  cursor: pointer;
  line-height: 1;
}

.loading-panel {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted-strong);
  background: #fbfcfe;
  font-size: var(--text-lg);
}

.modal-body {
  padding: 1.75rem;
  overflow-y: auto;
  background:
    radial-gradient(circle at top right, rgba(244, 211, 123, 0.22), transparent 30%),
    #fbfcfe;
}

.summary-grid,
.detail-grid {
  display: grid;
  gap: 1rem;
}

.summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 1rem;
}

.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card,
.detail-card {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: #ffffff;
  padding: 1.1rem 1.2rem;
}

.status-card {
  background: linear-gradient(180deg, #fff9e8 0%, #ffffff 100%);
}

.detail-card-wide {
  grid-column: span 2;
}

.summary-card,
.detail-card {
  strong,
  p {
    margin: 0;
  }
}

.summary-label,
.detail-label {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.summary-subtle,
.detail-subtle {
  display: block;
  margin-top: 0.45rem;
  color: var(--muted);
  font-size: var(--text-sm);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2rem;
  margin-top: 0.15rem;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  color: #ffffff;
  font-size: var(--text-sm);
  font-weight: 700;
}

.status-pill.is-pending {
  background: var(--status-pending);
  color: #2d3436;
}

.status-pill.is-approved {
  background: var(--status-approved);
}

.status-pill.is-rejected {
  background: var(--status-rejected);
}

.status-pill.is-withdrawn {
  background: var(--status-occupied);
}

.equipment-list {
  margin: 0;
  padding-left: 1.2rem;
}

.modal-footer {
  border-top: 1px solid var(--line);
  justify-content: flex-end;
  background: #ffffff;
}

.btn-danger {
  background: var(--danger);
  color: #ffffff;
}

.btn-danger:hover {
  box-shadow: 0 8px 18px rgba(196, 69, 69, 0.22);
}

.btn-secondary-alt {
  background: #f3f6fb;
  border-color: rgba(var(--blue-900-rgb), 0.12);
  color: var(--accent);
}

@media (max-width: 768px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .summary-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-card-wide {
    grid-column: span 1;
  }

  .modal-footer {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
  }
}
</style>
