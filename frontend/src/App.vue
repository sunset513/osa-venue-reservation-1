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
import NavBar from "@/components/NavBar.vue";
import Toast from "@/components/Toast.vue";
import { useToast } from "@/utils/useToast.js";

const { toasts, removeToast } = useToast();
</script>

<style lang="scss">
.app-container {
  min-height: 100vh;
}

.main-content {
  min-height: calc(100vh - var(--header-height));
}
</style>
