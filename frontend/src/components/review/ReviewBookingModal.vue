<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <header class="modal-header">
        <div class="modal-title-group">
          <span class="modal-title-icon">
            <component :is="headerIcon" :size="44" aria-hidden="true" />
          </span>
          <div>
            <p class="eyebrow">{{ t("pages.review.modal.venueReview") }}</p>
            <h2>{{ booking?.purpose || t("pages.review.modal.venueDetails") }}</h2>
          </div>
        </div>
        <button class="close-btn" type="button" @click="$emit('close')">✕</button>
      </header>

      <div v-if="loading" class="loading-panel">{{ t("pages.review.modal.loadingDetails") }}</div>

      <div v-else-if="booking" class="modal-body">
        <section class="status-strip" :aria-label="t('pages.review.modal.currentStatusAria')">
          <span class="status-strip-label">{{ t("pages.review.modal.currentStatus") }}</span>
          <span class="status-pill" :class="statusMeta.className">
            {{ t(statusMeta.labelKey) }}
          </span>
          <span class="status-strip-id review-id-pill">{{ t("common.bookingId", { id: booking.id }) }}</span>
        </section>

        <section class="summary-grid">
          <article class="summary-card">
            <span class="summary-label">{{ t("pages.review.modal.useDate") }}</span>
            <strong>{{ formatDateDisplay(booking.bookingDate) }}</strong>
            <span class="summary-subtle">{{ formatSlotGroupsAsTimeRange(booking.slots) }}</span>
          </article>

          <article class="summary-card">
            <span class="summary-label">{{ t("pages.review.modal.venue") }}</span>
            <strong>
              {{ formatVenueDisplayName(booking.venueName, locale) || t("common.notProvided") }}
            </strong>
            <span class="summary-subtle">{{ t("pages.review.modal.submittedAt", { time: formatDateTime(booking.createdAt) }) }}</span>
          </article>
        </section>

        <section class="detail-grid">
          <article class="detail-card detail-card-wide">
            <span class="detail-label">{{ t("common.purpose") }}</span>
            <p>{{ booking.purpose || t("common.noPurpose") }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">{{ t("common.timeSlot") }}</span>
            <p>{{ formatSlotGroupsAsTimeRange(booking.slots) || t("common.notProvided") }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">{{ t("common.participantCount") }}</span>
            <p>{{ t("common.people", { count: booking.pCount || 0 }) }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">{{ t("common.applicant") }}</span>
            <p>{{ contactInfo.name || t("common.notProvided") }}</p>
            <p class="detail-subtle">{{ contactInfo.phone || t("pages.review.modal.noPhone") }}</p>
            <p class="detail-subtle">{{ contactInfo.email || t("pages.review.modal.noEmail") }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">{{ t("pages.review.equipmentMode") }}</span>
            <p v-if="equipmentLoading">{{ t("pages.review.equipmentLoading") }}</p>
            <div v-else-if="equipmentBookings.length" class="equipment-review-list">
              <article
                v-for="equipmentBooking in equipmentBookings"
                :key="equipmentBooking.id"
                class="equipment-review-item"
              >
                <div class="equipment-review-content">
                  <strong>{{ getEquipmentSummary(equipmentBooking) }}</strong>
                  <span class="detail-subtle equipment-booking-meta">
                    <span class="review-id-pill">{{ t("common.equipmentBookingId", { id: equipmentBooking.id }) }}</span>
                    <span>{{ equipmentBooking.timeRange || t("common.noTimeRange") }}</span>
                    <span>{{ equipmentBooking.purpose || t("common.noPurpose") }}</span>
                  </span>
                  <span class="detail-subtle">
                    {{ equipmentBooking.contact.name || equipmentBooking.userId || t("pages.review.noApplicant") }}｜
                    {{ equipmentBooking.contact.phone || t("pages.review.modal.noPhone") }}｜
                    {{ equipmentBooking.contact.email || t("pages.review.modal.noEmail") }}
                  </span>
                  <span
                    class="equipment-status-pill"
                    :class="getReviewEquipmentStatusMeta(equipmentBooking.status).className"
                  >
                    {{ t(getReviewEquipmentStatusMeta(equipmentBooking.status).labelKey) }}
                  </span>
                </div>
              </article>
            </div>
            <p v-else>{{ t("pages.review.modal.noEquipment") }}</p>
          </article>
        </section>
      </div>

      <footer class="modal-footer">
        <button class="btn btn-secondary" type="button" :disabled="processing" @click="$emit('close')">
          <span class="btn-icon">
            <X :size="16" />
          </span>
          <span>{{ t("common.actions.close") }}</span>
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
import { useI18n } from "vue-i18n";
import { Building2, Check, Clock3, RotateCcw, X, XCircle } from "lucide-vue-next";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { getBookingStatusMeta } from "@/utils/bookingMeta";
import {
  formatEquipmentItemSummary,
  getEquipmentBookingStatusMeta,
} from "@/utils/equipment";
import { formatVenueDisplayName } from "@/utils/venueLabels";

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
});

const emit = defineEmits([
  "close",
  "approve",
  "update-status",
]);

const headerIcon = Building2;
const { locale, t } = useI18n();

const statusMeta = computed(() => getBookingStatusMeta(props.booking?.status));

const getReviewEquipmentStatusMeta = (status) => getEquipmentBookingStatusMeta(status);

const getEquipmentSummary = (booking) => formatEquipmentItemSummary(booking?.items, {
  separator: t("pages.equipmentShared.listSeparator"),
  fallback: t("pages.review.modal.noEquipment"),
  getName: (item) => item.equipmentName
    || t("pages.equipmentShared.unnamedEquipment", { id: item.equipmentId }),
});

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
        { key: "reject", label: t("pages.review.modal.rejectRequest"), icon: XCircle, variant: "btn-danger", type: "update-status", status: 3 },
        { key: "approve", label: t("pages.review.modal.approveRequest"), icon: Check, variant: "btn-primary", type: "approve" },
      ];
    case 2:
      return [
        { key: "revoke", label: t("pages.review.actions.changeToRejected"), icon: XCircle, variant: "btn-danger", type: "update-status", status: 3 },
      ];
    case 3:
      return [
        { key: "pending", label: t("pages.review.actions.changeToPending"), icon: Clock3, variant: "btn-secondary-alt", type: "update-status", status: 1 },
        { key: "approve-rejected", label: t("pages.review.actions.changeToApproved"), icon: Check, variant: "btn-primary", type: "update-status", status: 2 },
      ];
    case 0:
      return [
        { key: "restore", label: t("pages.review.actions.changeToPending"), icon: RotateCcw, variant: "btn-secondary-alt", type: "update-status", status: 1 },
      ];
    default:
      return [];
  }
});

const formatDateDisplay = (dateString) => {
  if (!dateString) return t("common.notProvided");

  const date = new Date(`${dateString}T00:00:00`);
  return date.toLocaleDateString(locale.value, {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
  });
};

const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return t("common.notProvided");

  const date = new Date(dateTimeString);
  return date.toLocaleString(locale.value, {
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
