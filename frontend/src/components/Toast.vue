<template>
  <Teleport to="body">
    <div class="toast-container" :class="`toast-container-${position}`">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="toast"
          :class="`toast-${toast.type}`"
        >
          <div class="toast-content">
            <span class="toast-icon" v-html="getIconSVG(toast.type)"></span>
            <span class="toast-message">{{ toast.message }}</span>
          </div>
          <button
            class="toast-close"
            @click="removeToast(toast.id)"
            aria-label="關閉"
          >
            ✕
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { defineProps, defineEmits } from "vue";

defineProps({
  toasts: {
    type: Array,
    default: () => [],
  },
  position: {
    type: String,
    default: "top-right", // top-left, top-right, bottom-left, bottom-right
  },
});

const emit = defineEmits(["remove"]);

// SVG 圖標定義
const iconSVGs = {
  success: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><polyline points="20 6 9 17 4 12"></polyline></svg>`,
  error: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`,
  warning: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3.05h16.94a2 2 0 0 0 1.71-3.05L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`,
  info: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`,
};

const getIconSVG = (type) => {
  return iconSVGs[type] || iconSVGs.info;
};

const removeToast = (id) => {
  emit("remove", id);
};
</script>

<style lang="scss" scoped>
.toast-container {
  position: fixed;
  z-index: 9999;
  pointer-events: none;

  &-top-left {
    top: 1rem;
    left: 1rem;
  }
  &-top-right {
    top: 1rem;
    right: 1rem;
  }
  &-bottom-left {
    bottom: 1rem;
    left: 1rem;
  }
  &-bottom-right {
    bottom: 1rem;
    right: 1rem;
  }
}

.toast {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  margin-bottom: 0.5rem;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  pointer-events: auto;
  background-color: white;
  color: #333;
  min-width: 280px;
  max-width: 400px;
  word-break: break-word;
  animation: slideIn 0.3s ease-out;

  .toast-content {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex: 1;
  }

  .toast-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .toast-message {
    flex: 1;
    font-size: 0.95rem;
    line-height: 1.5;
  }

  .toast-close {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    color: #999;
    font-size: 1.2rem;
    flex-shrink: 0;

    &:hover {
      color: #333;
    }
  }

  &-success {
    border-left: 4px solid #28a745;

    .toast-icon {
      color: #28a745;
    }
  }

  &-error {
    border-left: 4px solid #dc3545;

    .toast-icon {
      color: #dc3545;
    }
  }

  &-warning {
    border-left: 4px solid #ffc107;

    .toast-icon {
      color: #ffc107;
    }
  }

  &-info {
    border-left: 4px solid #0984e3;

    .toast-icon {
      color: #0984e3;
    }
  }
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.toast-move {
  transition: transform 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
