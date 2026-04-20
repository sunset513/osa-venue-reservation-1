<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container">
      <header class="modal-header">
        <div class="modal-title-group">
          <span class="modal-title-icon">▦</span>
          <h2>{{ formattedTitle }}</h2>
        </div>
        <button class="close-btn" @click="closeModal">✕</button>
      </header>

      <div class="modal-body">
        <div v-if="bookings.length === 0" class="empty-state">
          這一天目前沒有活動，可以直接新增預約。
        </div>

        <div v-else class="schedule-list">
          <article
            v-for="booking in bookings"
            :key="booking.id"
            class="schedule-card"
            :class="{ 'is-editable': booking.isEditable }"
            @click="handleCardClick(booking)"
          >
            <div class="schedule-main">
              <div class="schedule-top">
                <span class="venue-chip">{{ venueName || "未提供場地" }}</span>
                <span class="contact-name">{{ booking.contactName }}</span>
              </div>

              <div class="schedule-meta">
                <p class="purpose">{{ booking.purpose || "未填寫用途" }}</p>
              </div>
            </div>

            <div class="schedule-side">
              <span class="time-range">{{ booking.timeRange }}</span>

              <div class="schedule-badges">
                <span class="status-badge" :class="booking.statusClass">
                  {{ booking.statusText }}
                </span>
                <span class="count-badge">
                  {{ booking.participantCount }}人
                </span>
              </div>
            </div>
          </article>
        </div>
      </div>

      <footer class="modal-footer">
        <button class="btn btn-secondary" @click="closeModal">
          <span class="btn-icon">
            <X :size="16" />
          </span>
          <span>關閉</span>
        </button>
        <button class="btn btn-primary" @click="$emit('create')">
          <span class="btn-icon">
            <Plus :size="16" />
          </span>
          <span>新增預約</span>
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { Plus, X } from "lucide-vue-next";

const props = defineProps({
  visible: Boolean,
  selectedDate: {
    type: String,
    default: "",
  },
  dayOfWeek: {
    type: String,
    default: "",
  },
  venueName: {
    type: String,
    default: "",
  },
  bookings: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["close", "create", "edit-booking"]);

const formattedTitle = computed(() => {
  if (!props.selectedDate) return "預約詳情";

  const [year, month, day] = props.selectedDate.split("-");
  return `${year}年${Number(month)}月${Number(day)}日 ${props.dayOfWeek} 預約詳情`;
});

const closeModal = () => {
  emit("close");
};

const handleCardClick = (booking) => {
  if (!booking.isEditable) return;
  emit("edit-booking", booking.originalData);
};
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1200;
  padding: 1rem;
}

.modal-container {
  background: #ffffff;
  width: min(100%, 1080px);
  max-height: min(90vh, 900px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title-group {
  display: flex;
  align-items: center;
  gap: 1rem;

  h2 {
    margin: 0;
    font-size: 1.55rem;
    color: var(--ink);
  }
}

.modal-title-icon {
  font-size: 1.6rem;
  color: var(--accent);
}

.close-btn {
  border: none;
  background: none;
  font-size: 2rem;
  color: var(--muted);
  cursor: pointer;
  line-height: 1;
}

.modal-body {
  padding: 1.75rem 2rem;
  overflow-y: auto;
  background: #fbfcfe;
}

.empty-state {
  min-height: 240px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius);
  background: #ffffff;
  color: var(--muted-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-lg);
}

.schedule-list {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
  background: #ffffff;
}

.schedule-card {
  padding: 1.4rem 1.6rem;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  background: #ffffff;
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;

  &:last-child {
    border-bottom: none;
  }

  &.is-editable {
    cursor: pointer;

    &:hover {
      background: rgba(var(--blue-900-rgb), 0.04);
      box-shadow: inset 0 0 0 1px rgba(var(--blue-900-rgb), 0.1);
      transform: translateY(-1px);
    }
  }

  &:not(.is-editable):hover {
    background: rgba(var(--blue-900-rgb), 0.02);
  }
}

.schedule-main {
  flex: 1;
  min-width: 0;
}

.schedule-top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.venue-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.45rem 0.9rem;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  border: 1px solid rgba(var(--blue-900-rgb), 0.08);
  font-size: var(--text-base);
  font-weight: 700;
}

.contact-name {
  font-size: var(--text-base);
  font-weight: 700;
  color: var(--ink);
}

.time-range {
  margin-left: auto;
  font-size: var(--text-base);
  font-weight: 700;
  color: var(--ink);
}

.schedule-meta {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;

  p {
    margin: 0;
  }
}

.purpose {
  font-size: var(--text-base);
  color: var(--muted-strong);
  line-height: 1.6;
}

.schedule-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 0.9rem;
}

.time-range {
  font-size: var(--text-sm);
  font-weight: 700;
  color: var(--accent);
  letter-spacing: 0.02em;
  text-align: right;
}

.schedule-badges {
  display: flex;
  align-items: flex-end;
  gap: 0.7rem;
  align-self: flex-end;
}

.status-badge,
.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 0.85rem;
  border-radius: 999px;
  color: #ffffff;
  font-size: var(--text-sm);
  font-weight: 700;
  line-height: 1;
}

.status-badge.is-pending {
  background: var(--status-pending);
}

.status-badge.is-approved {
  background: var(--status-approved);
}

.status-badge.is-rejected {
  background: var(--status-rejected);
}

.status-badge.is-withdrawn {
  background: var(--status-occupied);
}

.count-badge {
  background: var(--status-info);
}

.modal-footer {
  padding: 1.25rem 2rem 1.6rem;
  border-top: 1px solid var(--line);
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.2rem;
  height: 1.2rem;
  border-radius: 999px;
  border: 2px solid currentColor;
  font-size: 0.9rem;
  line-height: 1;
  font-weight: 700;
}

@media (max-width: 768px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .modal-container {
    max-height: 92vh;
    border-radius: 14px;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .modal-title-group {
    gap: 0.65rem;

    h2 {
      font-size: 1.1rem;
      line-height: 1.4;
    }
  }

  .schedule-card {
    padding: 1rem;
    flex-direction: column;
  }

  .schedule-top {
    gap: 0.5rem;
    margin-bottom: 0.75rem;
  }

  .venue-chip {
    font-size: var(--text-sm);
    padding: 0.35rem 0.7rem;
  }

  .contact-name,
  .time-range,
  .purpose {
    font-size: 0.95rem;
  }

  .time-range {
    width: auto;
    margin-left: 0;
  }

  .schedule-badges {
    align-self: flex-start;
  }

  .schedule-side {
    width: 100%;
    align-items: flex-start;
  }

  .time-range {
    text-align: left;
  }

  .modal-footer {
    flex-direction: column-reverse;
    gap: 0.75rem;
  }

  .btn {
    flex: 1;
  }
}
</style>
