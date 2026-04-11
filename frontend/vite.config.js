import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 開啟 Proxy 代理
    proxy: {
      "/api": {
        target: "http://localhost:8080", // 你的後端位置
        changeOrigin: true, // 允許跨域
        // 如果後端沒有 /api 前綴，可以在這裡 rewrite，但你的後端有，所以不用寫 rewrite
      },
    },
  },
});
