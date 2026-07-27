<template>
  <div class="equipment-page page-enter">
    <header class="page-header equipment-header">
      <button class="back-btn" type="button" @click="router.push('/')">
        <ArrowLeft :size="16" aria-hidden="true" />
        {{ t("nav.backToUnits") }}
      </button>
      <p class="hero-eyebrow">{{ t("pages.equipmentStatus.eyebrow") }}</p>
      <h1>{{ t("pages.equipmentStatus.heading") }}</h1>
      <p>{{ t("pages.equipmentStatus.description") }}</p>
    </header>

    <section class="status-toolbar card">
      <label>
        {{ t("pages.equipmentStatus.queryDate") }}
        <input v-model="queryDate" type="date" />
      </label>
      <label>
        {{ t("pages.equipmentStatus.queryTimeSlot") }}
        <select v-model.number="queryHour">
          <option v-for="hour in hourOptions" :key="hour" :value="hour">
            {{ String(hour).padStart(2, "0") }}:00
          </option>
        </select>
      </label>
      <button type="button" class="btn btn-secondary" @click="loadStatuses">
        <span class="btn-icon" aria-hidden="true">
          <RefreshCw :size="16" />
        </span>
        <span>{{ t("pages.equipmentStatus.refreshQuery") }}</span>
      </button>
      <button type="button" class="btn btn-secondary admin-create-btn" @click="openCreateModal">
        <span class="btn-icon" aria-hidden="true">
          <PackagePlus :size="16" />
        </span>
        <span>{{ t("pages.equipmentStatus.createEquipment") }}</span>
      </button>
      <button
        type="button"
        class="btn btn-secondary route-borrow-link"
        @click="router.push({ name: 'EquipmentBorrowForm' })"
      >
        <span class="btn-icon" aria-hidden="true">
          <ArrowRight :size="16" />
        </span>
        <span>{{ t("pages.equipmentStatus.goToBorrow") }}</span>
      </button>
    </section>

    <div v-if="loading" class="loading-state">{{ t("pages.equipmentStatus.loading") }}</div>

    <div v-else-if="loadError" class="empty-state equipment-feedback">
      <h3>{{ t("pages.equipmentStatus.loadFailedTitle") }}</h3>
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-secondary" @click="loadStatuses">
        <span class="btn-icon" aria-hidden="true">
          <RefreshCw :size="16" />
        </span>
        <span>{{ t("pages.equipmentStatus.reload") }}</span>
      </button>
    </div>

    <div v-else class="equipment-content">
      <section class="equipment-table-wrap">
        <table class="equipment-table">
          <thead>
            <tr>
              <th>{{ t("pages.equipmentStatus.columns.name") }}</th>
              <th>{{ t("pages.equipmentStatus.columns.total") }}</th>
              <th>{{ t("pages.equipmentStatus.columns.borrowed") }}</th>
              <th>{{ t("pages.equipmentStatus.columns.available") }}</th>
              <th>{{ t("pages.equipmentStatus.columns.status") }}</th>
              <th>{{ t("pages.equipmentStatus.columns.actions") }}</th>
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
                    <strong>
                      {{
                        equipment.equipmentName
                          || t("pages.equipmentShared.unnamedEquipment", { id: equipment.equipmentId })
                      }}
                    </strong>
                  </button>
                </td>
                <td>{{ equipment.totalQuantity }}</td>
                <td>{{ equipment.borrowedQuantity }}</td>
                <td>{{ equipment.availableQuantity }}</td>
                <td>
                  <span class="status-pill" :class="getEquipmentStatusMeta(equipment.isInUse).className">
                    {{ t(getEquipmentStatusMeta(equipment.isInUse).labelKey) }}
                  </span>
                </td>
                <td>
                  <div class="action-buttons">
                    <RouterLink class="history-url-link" :to="equipmentReviewRoute(equipment)">
                      <History :size="15" aria-hidden="true" />
                      <span>{{ t("pages.equipmentStatus.borrowRecords") }}</span>
                    </RouterLink>
                    <button type="button" class="action-btn" @click="openEditModal(equipment)">
                      <PencilLine :size="15" aria-hidden="true" />
                      <span>{{ t("common.actions.edit") }}</span>
                    </button>
                    <button
                      type="button"
                      class="action-btn action-btn-danger"
                      :disabled="deletingEquipmentId === equipment.equipmentId"
                      @click="handleDeleteEquipment(equipment)"
                    >
                      <Trash2 :size="15" aria-hidden="true" />
                      <span>
                        {{
                          deletingEquipmentId === equipment.equipmentId
                            ? t("pages.equipmentStatus.deleting")
                            : t("common.actions.delete")
                        }}
                      </span>
                    </button>
                  </div>
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
                      <strong>
                        #{{ booking.equipmentBookingId }}
                        {{ booking.purpose || t("common.noPurpose") }}
                      </strong>
                      <span>{{ formatDateLabel(booking.borrowDate) }} {{ formatSlotGroupsAsTimeRange(booking.slots) }}</span>
                      <span>
                        {{
                          t("pages.equipmentStatus.bookingSummary", {
                            quantity: booking.quantity,
                            applicant: booking.userId || t("common.notProvided"),
                          })
                        }}
                      </span>
                      <span v-if="booking.relatedVenueName">
                        {{
                          t("pages.equipmentStatus.relatedVenue", {
                            name: formatVenueDisplayName(booking.relatedVenueName, locale),
                          })
                        }}
                      </span>
                      <span>
                        {{ booking.contact.name || t("pages.equipmentStatus.nameNotProvided") }}
                        /
                        {{ booking.contact.phone || t("pages.equipmentStatus.phoneNotProvided") }}
                      </span>
                    </article>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </section>
    </div>

    <EquipmentMasterEditModal
      v-model:visible="isEditModalVisible"
      :equipment-id="editingEquipmentId"
      :equipment-name="editingEquipmentName"
      @saved="handleEquipmentSaved"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import {
  ArrowRight,
  ArrowLeft,
  History,
  PackagePlus,
  PencilLine,
  RefreshCw,
  Trash2,
} from "lucide-vue-next";
import EquipmentMasterEditModal from "@/components/equipment/EquipmentMasterEditModal.vue";
import { deleteEquipment, getEquipmentStatuses } from "@/api/equipment";
import { getEquipmentStatusMeta, normalizeEquipmentStatuses } from "@/utils/equipment";
import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { useToast } from "@/utils/useToast";
import { resolveErrorMessage } from "@/utils/errorMessage";
import { formatVenueDisplayName } from "@/utils/venueLabels";

const router = useRouter();
const { locale, t } = useI18n();
const { success, warning } = useToast();

const loading = ref(true);
const loadError = ref("");
const equipmentStatuses = ref([]);
const expandedEquipmentIds = ref(new Set());
const queryDate = ref(new Date().toLocaleDateString("sv-SE"));
const queryHour = ref(new Date().getHours());
const hourOptions = Array.from({ length: 24 }, (_, hour) => hour);
const isEditModalVisible = ref(false);
const editingEquipmentId = ref(null);
const editingEquipmentName = ref("");
const deletingEquipmentId = ref(null);

const isExpanded = (equipmentId) => expandedEquipmentIds.value.has(equipmentId);

const toggleExpanded = (equipmentId) => {
  const nextExpandedIds = new Set(expandedEquipmentIds.value);
  if (nextExpandedIds.has(equipmentId)) {
    nextExpandedIds.delete(equipmentId);
  } else {
    nextExpandedIds.add(equipmentId);
  }
  expandedEquipmentIds.value = nextExpandedIds;
};

const equipmentReviewRoute = (equipment) => ({
  name: "ReviewCalendar",
  query: {
    mode: "equipment",
    equipmentKeyword: equipment?.equipmentName || "",
    equipmentStatus: "all",
  },
});

const formatDateLabel = (value) => {
  if (!value) return t("pages.equipmentStatus.dateNotProvided");

  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat(locale.value, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
};

const openCreateModal = () => {
  editingEquipmentId.value = null;
  editingEquipmentName.value = "";
  isEditModalVisible.value = true;
};

const openEditModal = (equipment) => {
  editingEquipmentId.value = equipment.equipmentId;
  editingEquipmentName.value = equipment.equipmentName || "";
  isEditModalVisible.value = true;
};

const handleDeleteEquipment = async (equipment) => {
  const equipmentId = equipment?.equipmentId;
  const equipmentName = equipment?.equipmentName || t("pages.equipmentStatus.thisEquipment");

  if (!equipmentId || deletingEquipmentId.value === equipmentId) return;

  const confirmed = window.confirm(t("pages.equipmentStatus.deleteConfirm", { name: equipmentName }));
  if (!confirmed) return;

  deletingEquipmentId.value = equipmentId;

  try {
    await deleteEquipment(equipmentId);

    if (editingEquipmentId.value === equipmentId) {
      isEditModalVisible.value = false;
      editingEquipmentId.value = null;
      editingEquipmentName.value = "";
    }

    success(t("pages.equipmentStatus.deleteSuccess", { name: equipmentName }));
    await loadStatuses();
  } catch (deleteError) {
    warning(resolveErrorMessage(deleteError, t, "pages.equipmentStatus.deleteFailed"));
  } finally {
    deletingEquipmentId.value = null;
  }
};

const handleEquipmentSaved = async () => {
  await loadStatuses();
};

const loadStatuses = async () => {
  loading.value = true;
  loadError.value = "";

  try {
    equipmentStatuses.value = normalizeEquipmentStatuses(
      await getEquipmentStatuses({
        date: queryDate.value,
        hour: queryHour.value,
      }),
    );
  } catch (error) {
    console.error("Failed to load equipment statuses", error);
    equipmentStatuses.value = [];
    loadError.value = resolveErrorMessage(error, t, "pages.equipmentStatus.loadFailedHelp");
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

.equipment-header p:last-child {
  margin: 0;
  max-width: 52rem;
  color: var(--muted);
  line-height: 1.6;
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

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.admin-create-btn {
  border: 1px solid rgba(36, 63, 107, 0.16);
  background: linear-gradient(135deg, rgba(243, 247, 252, 0.98), rgba(232, 240, 250, 0.96));
  color: #243f6b;
  box-shadow: 0 10px 22px rgba(36, 63, 107, 0.12);

  &:hover:not(:disabled) {
    border-color: rgba(36, 63, 107, 0.28);
    box-shadow: 0 12px 26px rgba(36, 63, 107, 0.18);
    transform: translateY(-1px);
  }
}

.route-borrow-link {
  flex-shrink: 0;
  margin-left: auto;
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
  min-width: 840px;
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
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink);
  cursor: pointer;

  &:disabled {
    cursor: default;
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

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.7rem;
  width: 100%;
}

.history-url-link,
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--accent);
  font-size: var(--text-sm);
  font-weight: 800;
}

.history-url-link {
  margin-right: auto;
  text-decoration: underline;
  text-underline-offset: 0.18rem;
}

.action-btn {
  min-height: 2rem;
  padding: 0.35rem 0.7rem;
  border: 1px solid rgba(var(--blue-900-rgb), 0.14);
  border-radius: 999px;
  background: #ffffff;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    color 0.2s ease;

  &:hover:not(:disabled) {
    border-color: rgba(var(--blue-900-rgb), 0.24);
    background: rgba(var(--blue-900-rgb), 0.03);
    box-shadow: 0 6px 16px rgba(var(--blue-900-rgb), 0.08);
  }

  &:disabled {
    opacity: 0.55;
    cursor: wait;
  }
}

.action-btn-danger {
  color: var(--danger);
  border-color: rgba(196, 69, 69, 0.26);
  background: rgba(196, 69, 69, 0.04);

  &:hover:not(:disabled) {
    border-color: rgba(196, 69, 69, 0.42);
    background: rgba(196, 69, 69, 0.08);
    box-shadow: 0 6px 16px rgba(196, 69, 69, 0.12);
  }
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

  .action-buttons {
    flex-direction: column;
    align-items: flex-end;
  }

  .route-borrow-link {
    margin-left: 0;
  }

  .history-url-link {
    margin-right: 0;
  }
}
</style>
