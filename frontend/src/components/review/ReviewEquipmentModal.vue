<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container equipment-modal-container">
      <header class="modal-header">
        <div class="modal-title-group">
          <span class="modal-title-icon">
            <component :is="headerIcon" :size="44" aria-hidden="true" />
          </span>
          <div>
            <p class="eyebrow">設備借用審核</p>
            <h2>{{ booking?.itemSummary || "設備預約申請詳情" }}</h2>
          </div>
        </div>
        <button class="close-btn" type="button" @click="$emit('close')">✕</button>
      </header>

      <div v-if="booking" class="modal-body">
        <section class="status-strip" aria-label="目前申請狀態">
          <span class="status-strip-label">目前狀態</span>
          <span class="status-pill" :class="statusMeta.className">
            {{ statusMeta.text }}
          </span>
          <span class="status-strip-id review-id-pill">設備借用編號 #{{ booking.id }}</span>
        </section>

        <section class="summary-grid">
          <article class="summary-card">
            <span class="summary-label">借用時段</span>
            <strong>{{ formatDateDisplay(booking.borrowDate) }}</strong>
            <span class="summary-subtle">{{ booking.timeRange }}</span>
          </article>

          <article class="summary-card">
            <span class="summary-label">借用型態</span>
            <strong>{{ booking.relatedVenueName || "單獨借用設備" }}</strong>
            <span class="summary-subtle" v-if="booking.relatedVenueBookingId">
              <span class="review-id-pill">場地預約編號 #{{ booking.relatedVenueBookingId }}</span>
            </span>
            <span class="summary-subtle" v-else-if="booking.relatedVenueName">
              關聯場地
            </span>
            <span class="summary-subtle" v-else>
              無綁定場地
            </span>
          </article>
        </section>

        <section class="detail-grid">
          <article class="detail-card detail-card-wide">
            <span class="detail-label">申請目的</span>
            <p>{{ booking.purpose || "未填寫用途" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">申請人</span>
            <p>{{ booking.contact.name || booking.userId || "未提供" }}</p>
            <p class="detail-subtle">{{ booking.contact.phone || "未提供電話" }}</p>
            <p class="detail-subtle">{{ booking.contact.email || "未提供信箱" }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">設備明細</span>
            <div class="equipment-review-list">
              <article
                v-for="item in booking.items"
                :key="item.equipmentId"
                class="equipment-review-item"
              >
                <div class="equipment-review-content">
                  <strong>{{ item.equipmentName }}</strong>
                </div>
                <div class="equipment-review-actions">
                  <span class="status-pill quantity-pill">x {{ item.quantity }}</span>
                </div>
              </article>
            </div>
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
          @click="$emit('update-status', booking.id, action.status)"
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
import { Wrench, Check, Clock3, RotateCcw, X, XCircle } from "lucide-vue-next";
import { getEquipmentBookingStatusMeta } from "@/utils/equipment";

const props = defineProps({
  visible: Boolean,
  booking: {
    type: Object,
    default: null,
  },
  processing: Boolean,
});

const emit = defineEmits([
  "close",
  "update-status",
]);

const headerIcon = Wrench;

const statusMeta = computed(() => {
  const meta = getEquipmentBookingStatusMeta(props.booking?.status);
  return {
    ...meta,
    text: meta.text === "審核中" ? "待審核" : meta.text,
  };
});

const formatDateDisplay = (dateString) => {
  if (!dateString) return "未提供日期";
  const date = new Date(`${dateString}T00:00:00`);
  if (Number.isNaN(date.getTime())) return dateString;
  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
  }).format(date);
};

const actions = computed(() => {
  if (!props.booking) return [];
  switch (props.booking.status) {
    case 1:
      return [
        { key: "reject", label: "拒絕申請", icon: XCircle, variant: "btn-danger", status: 3 },
        { key: "approve", label: "通過申請", icon: Check, variant: "btn-primary", status: 2 },
      ];
    case 2:
      return [
        { key: "reject-approved", label: "改為拒絕", icon: XCircle, variant: "btn-danger", status: 3 },
      ];
    case 3:
      return [
        { key: "pending-rejected", label: "改為待審核", icon: Clock3, variant: "btn-secondary-alt", status: 1 },
        { key: "approve-rejected", label: "改為通過", icon: Check, variant: "btn-primary", status: 2 },
      ];
    default:
      return [];
  }
});
</script>

<style scoped>
@import "@/assets/styles/_modal.scss";

.equipment-modal-container {
  position: relative;
  background: linear-gradient(180deg, #c5ccd4 0%, #d2d9e0 100%);
  border-left-color: #a3afbd;
  box-shadow:
    inset 0 0 0 1px rgba(108, 122, 137, 0.15),
    -20px 0 36px rgba(15, 23, 42, 0.18);

  &::before {
    content: "";
    position: absolute;
    inset: 0 auto 0 0;
    width: 6px;
    background: linear-gradient(180deg, #748393 0%, #566270 100%);
  }

  .modal-header {
    background: #b8c1cb;
    border-bottom-color: #a3afbd;
  }

  .modal-body {
    background: linear-gradient(180deg, #d3dae1 0%, #dee4ea 100%);
  }

  .modal-footer {
    background: #c5ccd4;
    border-top-color: #a3afbd;
  }

  .loading-panel {
    background: #d3dae1;
  }

  .modal-title-icon {
    border-radius: 14px;
    background: rgba(68, 80, 94, 0.15);
    color: #42505e;
  }

  .eyebrow {
    color: #4d5865;
  }
}
</style>
