<template>
  <div class="app-container">
    <NavBar />
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
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
  background-color: #f8f9fa;
}
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}
// 基礎轉場動畫
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
