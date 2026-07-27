// vite.config.js
import { fileURLToPath, URL } from "node:url";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  // 載入環境變數
  const env = loadEnv(mode, process.cwd(), "");
  const usePolling = process.env.DOCKER_USE_POLLING === "true";
  const pollingInterval = Number(process.env.DOCKER_POLLING_INTERVAL || 0);
  const hmrClientPort = Number(process.env.HMR_CLIENT_PORT || 0);

  return {
    base: env.VITE_APP_BASE_PATH || "/",
    plugins: [vue()],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    server: {
      host: "0.0.0.0",
      port: 5173,
      watch: usePolling
        ? {
            usePolling: true,
            interval: pollingInterval > 0 ? pollingInterval : 100,
          }
        : undefined,
      hmr: hmrClientPort > 0 ? { clientPort: hmrClientPort } : undefined,
      proxy: {
        "/api": {
          // 優先讀取變數 VITE_PROXY_TARGET，若無則預設本地 localhost
          target: env.VITE_PROXY_TARGET || "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: "node",
    },
  };
});
