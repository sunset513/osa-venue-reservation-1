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
          <span class="status-strip-id">場地預約編號 #{{ booking.id }}</span>
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
                <div class="equipment-review-content">
                  <strong>{{ equipmentBooking.itemSummary }}</strong>
                  <span class="detail-subtle">
                    設備借用編號 #{{ equipmentBooking.id }}｜{{ equipmentBooking.timeRange }}｜{{ equipmentBooking.purpose }}
                  </span>
                  <span class="detail-subtle">
                    {{ equipmentBooking.contact.name || equipmentBooking.userId || "未提供申請人" }}｜
                    {{ equipmentBooking.contact.phone || "未提供電話" }}｜
                    {{ equipmentBooking.contact.email || "未提供 Email" }}
                  </span>
                  <span
                    class="equipment-status-pill"
                    :class="getReviewEquipmentStatusMeta(equipmentBooking.status).className"
                  >
                    {{ getReviewEquipmentStatusMeta(equipmentBooking.status).text }}
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
                    class="btn equipment-action-btn"
                    :class="action.variant"
                    :disabled="equipmentProcessingId === equipmentBooking.id"
                    @click="$emit('update-equipment-status', equipmentBooking.id, action.status)"
                  >
                    <span class="btn-icon">
                      <component :is="action.icon" :size="14" />
                    </span>
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

const statusMeta = computed(() => {
  const meta = getBookingStatusMeta(props.booking?.status);
  return {
    ...meta,
    text: meta.text === "審核中" ? "待審核" : meta.text,
  };
});

const getReviewEquipmentStatusMeta = (status) => {
  const meta = getEquipmentBookingStatusMeta(status);
  return {
    ...meta,
    text: meta.text === "審核中" ? "待審核" : meta.text,
  };
};

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
        { key: "pending", label: "改為待審核", icon: Clock3, variant: "btn-secondary-alt", type: "update-status", status: 1 },
        { key: "approve-rejected", label: "改為通過", icon: Check, variant: "btn-primary", type: "update-status", status: 2 },
      ];
    case 0:
      return [
        { key: "restore", label: "改為待審核", icon: RotateCcw, variant: "btn-secondary-alt", type: "update-status", status: 1 },
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
@import "@/assets/styles/_modal.scss";
</style>
