<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container equipment-modal-container">
      <header class="modal-header">
        <div class="modal-title-group">
          <span class="modal-title-icon">
            <component :is="headerIcon" :size="44" aria-hidden="true" />
          </span>
          <div>
            <p class="eyebrow">{{ t("pages.review.modal.equipmentReview") }}</p>
            <h2>{{ booking ? getEquipmentSummary(booking) : t("pages.review.modal.equipmentDetails") }}</h2>
          </div>
        </div>
        <button class="close-btn" type="button" @click="$emit('close')">✕</button>
      </header>

      <div v-if="booking" class="modal-body">
        <section class="status-strip" :aria-label="t('pages.review.modal.currentStatusAria')">
          <span class="status-strip-label">{{ t("pages.review.modal.currentStatus") }}</span>
          <span class="status-pill" :class="statusMeta.className">
            {{ t(statusMeta.labelKey) }}
          </span>
          <span class="status-strip-id review-id-pill">{{ t("common.equipmentBookingId", { id: booking.id }) }}</span>
        </section>

        <section class="summary-grid">
          <article class="summary-card">
            <span class="summary-label">{{ t("pages.review.modal.borrowingPeriod") }}</span>
            <strong>{{ formatDateDisplay(booking.borrowDate) }}</strong>
            <span class="summary-subtle">{{ booking.timeRange || t("common.noTimeRange") }}</span>
          </article>

          <article class="summary-card">
            <span class="summary-label">{{ t("pages.review.modal.borrowingType") }}</span>
            <strong>
              {{
                formatVenueDisplayName(booking.relatedVenueName, locale)
                  || t("pages.review.modal.standaloneEquipment")
              }}
            </strong>
            <span class="summary-subtle" v-if="booking.relatedVenueBookingId">
              <span class="review-id-pill">{{ t("common.bookingId", { id: booking.relatedVenueBookingId }) }}</span>
            </span>
            <span class="summary-subtle" v-else-if="booking.relatedVenueName">
              {{ t("pages.review.modal.linkedVenue") }}
            </span>
            <span class="summary-subtle" v-else>
              {{ t("pages.review.modal.noLinkedVenue") }}
            </span>
          </article>
        </section>

        <section class="detail-grid">
          <article class="detail-card detail-card-wide">
            <span class="detail-label">{{ t("common.purpose") }}</span>
            <p>{{ booking.purpose || t("common.noPurpose") }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">{{ t("common.applicant") }}</span>
            <p>{{ booking.contact.name || booking.userId || t("common.notProvided") }}</p>
            <p class="detail-subtle">{{ booking.contact.phone || t("pages.review.modal.noPhone") }}</p>
            <p class="detail-subtle">{{ booking.contact.email || t("pages.review.modal.noEmail") }}</p>
          </article>

          <article class="detail-card">
            <span class="detail-label">{{ t("pages.review.modal.equipmentItems") }}</span>
            <div class="equipment-review-list">
              <article
                v-for="item in booking.items"
                :key="item.equipmentId"
                class="equipment-review-item"
              >
                <div class="equipment-review-content">
                  <strong>
                    {{
                      item.equipmentName
                        || t("pages.equipmentShared.unnamedEquipment", { id: item.equipmentId })
                    }}
                  </strong>
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
          <span>{{ t("common.actions.close") }}</span>
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
import { useI18n } from "vue-i18n";
import { Wrench, Check, Clock3, RotateCcw, X, XCircle } from "lucide-vue-next";
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
  processing: Boolean,
});

const emit = defineEmits([
  "close",
  "update-status",
]);

const headerIcon = Wrench;
const { locale, t } = useI18n();

const statusMeta = computed(() => getEquipmentBookingStatusMeta(props.booking?.status));

const getEquipmentSummary = (booking) => formatEquipmentItemSummary(booking?.items, {
  separator: t("pages.equipmentShared.listSeparator"),
  fallback: t("pages.review.modal.noEquipment"),
  getName: (item) => item.equipmentName
    || t("pages.equipmentShared.unnamedEquipment", { id: item.equipmentId }),
});

const formatDateDisplay = (dateString) => {
  if (!dateString) return t("pages.review.modal.noDate");
  const date = new Date(`${dateString}T00:00:00`);
  if (Number.isNaN(date.getTime())) return dateString;
  return new Intl.DateTimeFormat(locale.value, {
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
        { key: "reject", label: t("pages.review.modal.rejectRequest"), icon: XCircle, variant: "btn-danger", status: 3 },
        { key: "approve", label: t("pages.review.modal.approveRequest"), icon: Check, variant: "btn-primary", status: 2 },
      ];
    case 2:
      return [
        { key: "reject-approved", label: t("pages.review.actions.changeToRejected"), icon: XCircle, variant: "btn-danger", status: 3 },
      ];
    case 3:
      return [
        { key: "pending-rejected", label: t("pages.review.actions.changeToPending"), icon: Clock3, variant: "btn-secondary-alt", status: 1 },
        { key: "approve-rejected", label: t("pages.review.actions.changeToApproved"), icon: Check, variant: "btn-primary", status: 2 },
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
