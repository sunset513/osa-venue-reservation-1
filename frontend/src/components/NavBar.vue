<template>
  <nav class="navbar site-header">
    <div class="nav-topbar"></div>
    <div class="container navbar-inner">
      <div class="nav-brand" @click="$router.push('/')">
        <span class="brand-mark">NCU OSA</span>
        <div class="brand-copy">
          <strong>國立中央大學學務處</strong>
          <span>場地租借系統</span>
        </div>
      </div>
      <div ref="menuRef" class="nav-user">
        <button
          type="button"
          class="avatar-trigger"
          aria-label="開啟使用者選單"
          aria-haspopup="menu"
          :aria-expanded="isMenuOpen"
          @click="toggleMenu"
        >
          <div class="avatar-circle">
            <User :size="20" stroke-width="2.5" />
          </div>
          <ChevronDown :size="16" class="avatar-chevron" :class="{ 'is-open': isMenuOpen }" />
        </button>

        <transition name="menu-fade">
          <div v-if="isMenuOpen" class="user-menu card" role="menu" aria-label="使用者選單">
            <button
              type="button"
              class="menu-item"
              :class="{ 'is-active': isHistoryPage }"
              role="menuitem"
              @click="goToMyBookings"
            >
              我的預約歷史紀錄
            </button>
          </div>
        </transition>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ChevronDown, User } from "lucide-vue-next";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const menuRef = ref(null);
const isMenuOpen = ref(false);

const isHistoryPage = computed(() => route.name === "MyBookingHistory");

const closeMenu = () => {
  isMenuOpen.value = false;
};

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value;
};

const goToMyBookings = async () => {
  closeMenu();

  if (route.name === "MyBookingHistory") return;

  await router.push({ name: "MyBookingHistory" });
};

const handleDocumentClick = (event) => {
  if (!menuRef.value?.contains(event.target)) {
    closeMenu();
  }
};

const handleKeydown = (event) => {
  if (event.key === "Escape") {
    closeMenu();
  }
};

watch(
  () => route.fullPath,
  () => {
    closeMenu();
  },
);

onMounted(() => {
  document.addEventListener("click", handleDocumentClick);
  document.addEventListener("keydown", handleKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleDocumentClick);
  document.removeEventListener("keydown", handleKeydown);
});
</script>

<style lang="scss" scoped>
.navbar {
  height: calc(var(--header-height) + 18px);
  background: #dbe5f0;
  border-bottom: 1px solid rgba(var(--blue-900-rgb), 0.12);
}

.nav-topbar {
  height: 18px;
  background: #243f6b;
}

.navbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  min-height: var(--header-height);
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  cursor: pointer;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 5rem;
  min-height: 2.35rem;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  background: #f6f8fb;
  color: #284f90;
  border: 1px solid rgba(36, 63, 107, 0.12);
  font-size: var(--text-sm);
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;

  strong {
    color: #19355a;
    font-size: var(--text-lg);
    line-height: 1.2;
  }

  span {
    color: #32527f;
    font-size: var(--text-sm);
    font-weight: 700;
  }
}

.avatar-circle {
  width: 2.6rem;
  height: 2.6rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f8fbff;
  color: #284f90;
  border: 1px solid rgba(36, 63, 107, 0.14);
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;

  &:hover {
    background: #eef4fb;
    transform: translateY(-1px);
  }
}

.nav-user {
  position: relative;
}

.avatar-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.avatar-chevron {
  color: #32527f;
  transition: transform 0.2s ease;

  &.is-open {
    transform: rotate(180deg);
  }
}

.user-menu {
  position: absolute;
  top: calc(100% + 0.75rem);
  right: 0;
  min-width: 14rem;
  padding: 0.45rem;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  background: rgba(255, 255, 255, 0.98);
  z-index: 30;
}

.menu-item {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border: 0;
  border-radius: calc(var(--radius-sm) - 2px);
  background: transparent;
  color: var(--ink);
  text-align: left;
  font-size: var(--text-sm);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;

  &:hover,
  &.is-active {
    background: var(--accent-soft);
    color: var(--accent);
  }
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 640px) {
  .brand-mark {
    display: none;
  }

  .brand-copy strong {
    font-size: var(--text-base);
  }

  .user-menu {
    min-width: 12.5rem;
  }
}
</style>
