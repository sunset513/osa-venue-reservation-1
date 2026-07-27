<template>
  <div class="placeholder-page">
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">← {{ t("pages.placeholder.back") }}</button>
      <h1>{{ t("pages.placeholder.title") }}</h1>
      <p>{{ t("pages.placeholder.description") }}</p>
    </header>

    <div class="empty-state">
      <div class="icon">🚧</div>
      <h2>{{ t("pages.placeholder.underConstruction") }}</h2>
      <p>{{ t("pages.placeholder.calendarComingSoon") }}</p>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from "vue-i18n";

const { t } = useI18n();
// 這裡目前只需要一個空殼，用來確保路由可以正常跳轉
</script>

<style lang="scss" scoped>
.placeholder-page {
  text-align: center;

  .page-header {
    .back-btn {
      margin-bottom: 1rem;
    }
  }

  .empty-state {
    padding: 4rem 2rem;

    .icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    h2 {
      margin-bottom: 0.5rem;
    }

    p {
      color: var(--muted);
    }
  }
}
</style>
