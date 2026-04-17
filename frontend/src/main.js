/**
 * Vue 應用程式入口文件
 *
 * 功能：
 * - 初始化 Vue 應用實例
 * - 配置 Pinia 狀態管理
 * - 配置 Vue Router 路由
 * - 引入全局樣式
 * - 實作全局錯誤攔截
 *
 * @author 預約系統前端團隊
 * @date 2026-04-04
 */

import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";

// 導入全局設計系統
import "./style/style.css";

// 建立 Vue 應用實例
const app = createApp(App);

// 註冊全局錯誤處理 (加分項：捕獲未處理的錯誤，避免畫面直接白屏)
app.config.errorHandler = (err, instance, info) => {
  console.error("[Vue Global Error]:", err);
  console.info("Error Info:", info);
  // 未來可以在這裡串接 Sentry 或回傳 Log 給後端
};

// 使用 Pinia 狀態管理
app.use(createPinia());

// 使用 Vue Router 路由
app.use(router);

// 掛載應用到 #app 元素
app.mount("#app");
