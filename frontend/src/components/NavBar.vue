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
      <div class="nav-user">
        <div class="avatar-circle">
          <User :size="20" stroke-width="2.5" />
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { User } from "lucide-vue-next";
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

@media (max-width: 640px) {
  .brand-mark {
    display: none;
  }

  .brand-copy strong {
    font-size: var(--text-base);
  }
}
</style>
