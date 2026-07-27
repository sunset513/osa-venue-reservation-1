<template>
  <div class="app-container">
    <NavBar />
    <main class="site-main">
      <div class="container main-content">
        <router-view v-slot="{ Component, route }">
          <component :is="Component" :key="route.fullPath" />
        </router-view>
      </div>
    </main>
    <Toast :toasts="toasts" @remove="removeToast" />
  </div>
</template>

<script setup>
import { watchEffect } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import NavBar from "@/components/NavBar.vue";
import Toast from "@/components/Toast.vue";
import { useToast } from "@/utils/useToast.js";

const route = useRoute();
const { locale, t } = useI18n();
const { toasts, removeToast } = useToast();

watchEffect(() => {
  locale.value;
  document.title = t(route.meta.titleKey || "pages.unitSelector.documentTitle");
});
</script>

<style lang="scss">
.app-container {
  min-height: 100vh;
}

.main-content {
  min-height: calc(100vh - var(--header-height));
}
</style>
