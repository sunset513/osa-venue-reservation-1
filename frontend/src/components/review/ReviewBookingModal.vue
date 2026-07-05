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
        <section class="status-strip" aria-label="目前申請狀態">
          <span class="status-strip-label">目前狀態</span>
          <span class="status-pill" :class="statusMeta.className">
            {{ statusMeta.text }}
          </span>
          <span class="status-strip-id">申請編號 #{{ booking.id }}</span>
        </section>

        <section class="summary-grid">
          <article class="summary-card">
            <span class="summary-label">使用日期</span>
            <strong>{{ formatDateDisplay(booking.bookingDate) }}</strong>
            <span class="summary-subtle">{{ formatSlotGroupsAsTimeRange(booking.slots) }}</span>
          </article>

          <article class="summary-card">
            <span class="summary-label">場地</span>
            <strong>{{ booking.venueName || "未提供" }}</strong>
            <span class="summary-subtle">送件時間 {{ formatDateTime(booking.createdAt) }}</span>
          </article>
        </section>

        <section class="detail-grid">
          <article class="detail-card detail-card-wide">
            <span class="detail-label">申請目的</span>
            <p>{{ booking.purpose || "未填寫用途" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">使用時段</span>
            <p>{{ formatSlotGroupsAsTimeRange(booking.slots) || "未提供" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">使用人數</span>
            <p>{{ booking.pCount || 0 }} 人</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">申請人</span>
            <p>{{ contactInfo.name || "未提供" }}</p>
            <p class="detail-subtle">{{ contactInfo.phone || "未提供電話" }}</p>
            <p class="detail-subtle">{{ contactInfo.email || "未提供信箱" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">設備借用</span>
            <p v-if="equipmentLoading">載入設備申請中...</p>
            <div v-else-if="equipmentBookings.length" class="equipment-review-list">
              <article
                v-for="equipmentBooking in equipmentBookings"
                :key="equipmentBooking.id"
                class="equipment-review-item"
              >
                <div>
                  <strong>{{ equipmentBooking.itemSummary }}</strong>
                  <span class="detail-subtle">
                    {{ equipmentBooking.timeRange }}｜{{ equipmentBooking.purpose }}
                  </span>
                  <span class="detail-subtle">
                    {{ equipmentBooking.contact.name || equipmentBooking.userId || "未提供申請人" }}｜
                    {{ equipmentBooking.contact.phone || "未提供電話" }}｜
                    {{ equipmentBooking.contact.email || "未提供 Email" }}
                  </span>
                  <span
                    class="equipment-status-pill"
                    :class="getEquipmentBookingStatusMeta(equipmentBooking.status).className"
                  >
                    {{ getEquipmentBookingStatusMeta(equipmentBooking.status).text }}
                  </span>
                </div>
                <div
                  v-if="getEquipmentReviewActions(equipmentBooking).length"
                  class="equipment-review-actions"
                >
                  <button
                    v-for="action in getEquipmentReviewActions(equipmentBooking)"
                    :key="action.key"
                    type="button"
                    class="inline-action"
                    :class="action.variant"
                    :disabled="equipmentProcessingId === equipmentBooking.id"
                    @click="$emit('update-equipment-status', equipmentBooking.id, action.status)"
                  >
                    {{ action.label }}
                  </button>
                </div>
              </article>
            </div>
            <p v-else>未借用設備</p>
          </article>
        </section>
      </div>

      <footer class="modal-footer">
        <button class="btn btn-secondary" type="button" :disabled="processing" @click="$emit('close')">
          <span class="btn-icon">
            <X :size="16" />
          </span>
          <span>關閉</span>
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
          <span class="btn-icon">
            <component :is="action.icon" :size="16" />
          </span>
          <span>{{ action.label }}</span>
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { Check, Clock3, RotateCcw, X, XCircle } from "lucide-vue-next";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { getBookingStatusMeta } from "@/utils/bookingMeta";
import { getEquipmentBookingStatusMeta } from "@/utils/equipment";

const props = defineProps({
  visible: Boolean,
  booking: {
    type: Object,
    default: null,
  },
  loading: Boolean,
  processing: Boolean,
  equipmentBookings: {
    type: Array,
    default: () => [],
  },
  equipmentLoading: Boolean,
  equipmentProcessingId: {
    type: [Number, String],
    default: null,
  },
});

const emit = defineEmits([
  "close",
  "approve",
  "update-status",
  "approve-equipment",
  "reject-equipment",
  "update-equipment-status",
]);

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
        { key: "reject", label: "拒絕申請", icon: XCircle, variant: "btn-danger", type: "update-status", status: 3 },
        { key: "approve", label: "通過申請", icon: Check, variant: "btn-primary", type: "approve" },
      ];
    case 2:
      return [
        { key: "revoke", label: "改為拒絕", icon: XCircle, variant: "btn-danger", type: "update-status", status: 3 },
      ];
    case 3:
      return [
        { key: "pending", label: "改為審核中", icon: Clock3, variant: "btn-secondary-alt", type: "update-status", status: 1 },
        { key: "approve-rejected", label: "改為通過", icon: Check, variant: "btn-primary", type: "update-status", status: 2 },
      ];
    case 0:
      return [
        { key: "restore", label: "改為審核中", icon: RotateCcw, variant: "btn-secondary-alt", type: "update-status", status: 1 },
      ];
    default:
      return [];
  }
});

const getEquipmentReviewActions = (equipmentBooking) => {
  // Equipment review records can now move between pending, approved, and
  // rejected states after the first decision. Keeping the available transitions
  // close to the display logic makes each review row explain what the reviewer
  // can do next without introducing another modal-level state machine.
  switch (equipmentBooking?.status) {
    case 1:
      return [
        { key: "reject", label: "拒絕", variant: "is-danger", status: 3 },
        { key: "approve", label: "核准", variant: "is-primary", status: 2 },
      ];
    case 2:
      return [
        { key: "reject-approved", label: "改為拒絕", variant: "is-danger", status: 3 },
      ];
    case 3:
      return [
        { key: "pending-rejected", label: "改為審核中", variant: "is-secondary", status: 1 },
        { key: "approve-rejected", label: "改為核准", variant: "is-primary", status: 2 },
      ];
    default:
      return [];
  }
};

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
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0;
  background: rgba(15, 23, 42, 0.38);
}

.modal-container {
  width: min(100%, 540px);
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-left: 1px solid #d7dde5;
  border-radius: 18px 0 0 18px;
  box-shadow: -16px 0 32px rgba(15, 23, 42, 0.14);
  animation: drawerEnter 0.2s ease-out;
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
  border-bottom: 1px solid #d7dde5;
  background: #f7f8fa;
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
  color: #5f6b7a;
  font-size: var(--text-sm);
  font-weight: 800;
}

.modal-title-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #202936;
  color: #ffffff;
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
  flex: 1;
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
  flex: 1;
  overflow-y: auto;
  background: #f5f6f8;
}

.summary-grid,
.detail-grid {
  display: grid;
  gap: 1rem;
}

.summary-grid {
  grid-template-columns: 1fr;
  margin-bottom: 1rem;
}

.detail-grid {
  grid-template-columns: 1fr;
}

.summary-card,
.detail-card {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #ffffff;
  padding: 1.1rem 1.2rem;
}

.detail-card-wide {
  grid-column: auto;
}

.status-strip {
  margin-bottom: 1rem;
  padding: 0 0 1rem;
  border-bottom: 1px solid #d7dde5;
  display: grid;
  grid-template-columns: auto auto 1fr;
  align-items: center;
  gap: 0.8rem;
}

.status-strip-label,
.status-strip-id {
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.status-strip-id {
  justify-self: end;
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

.equipment-review-list {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.equipment-review-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.65rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;

  div {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
  }
}

.equipment-status-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 1.75rem;
  padding: 0.25rem 0.55rem;
  border-radius: 999px;
  font-size: var(--text-sm);
  font-weight: 800;

  &.is-pending {
    background: rgba(217, 119, 6, 0.12);
    color: #a15c00;
  }

  &.is-approved {
    background: rgba(46, 139, 87, 0.12);
    color: var(--status-approved);
  }

  &.is-rejected {
    background: rgba(196, 69, 69, 0.1);
    color: var(--danger);
  }

  &.is-withdrawn {
    background: var(--surface-muted);
    color: var(--muted-strong);
  }
}

.equipment-review-actions {
  flex: 0 0 auto;
}

.inline-action {
  min-height: 1.9rem;
  padding: 0.25rem 0.55rem;
  border: 1px solid transparent;
  border-radius: 7px;
  font-size: var(--text-sm);
  font-weight: 800;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.58;
  }

  &.is-primary {
    background: var(--accent-soft);
    color: var(--accent);
  }

  &.is-danger {
    background: rgba(196, 69, 69, 0.1);
    color: var(--danger);
  }

  &.is-secondary {
    background: #f3f6fb;
    color: var(--accent);
  }
}

.modal-footer {
  border-top: 1px solid #d7dde5;
  justify-content: flex-end;
  flex-wrap: wrap;
  background: #ffffff;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
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
    align-items: stretch;
  }

  .modal-container {
    width: 100%;
    border-left: 0;
    border-radius: 0;
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

  .status-strip {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .status-strip-id {
    justify-self: start;
  }

  .modal-footer {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
  }
}

@keyframes drawerEnter {
  from {
    opacity: 0;
    transform: translateX(24px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
