<template>
  <div class="not-found-page page-enter">
    <section class="not-found-card card" aria-labelledby="not-found-title">
      <p class="not-found-code">404</p>
      <h1 id="not-found-title">{{ t("pages.notFound.title") }}</h1>
      <p class="not-found-copy">
        {{ t("pages.notFound.description") }}
      </p>
      <RouterLink class="btn btn-primary not-found-action" :to="{ name: 'Home' }">
        {{ t("pages.notFound.backHome") }}
      </RouterLink>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from "vue-router";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
</script>

<style lang="scss" scoped>
.not-found-page {
  min-height: calc(100vh - var(--header-height) - 18px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1.25rem;
}

.not-found-card {
  width: min(100%, 640px);
  padding: 2.5rem;
  text-align: center;
}

.not-found-code {
  margin: 0;
  color: var(--accent);
  font-size: clamp(3rem, 9vw, 5rem);
  font-weight: 900;
  letter-spacing: 0.08em;
}

.not-found-card h1 {
  margin-top: 0.75rem;
  color: var(--ink);
  font-size: clamp(2rem, 5vw, 3rem);
}

.not-found-copy {
  max-width: 28rem;
  margin: 1rem auto 0;
  color: var(--muted);
  font-size: var(--text-base);
  line-height: 1.7;
}

.not-found-action {
  margin-top: 1.75rem;
}

@media (max-width: 640px) {
  .not-found-card {
    padding: 2rem 1.25rem;
  }
}
</style>
