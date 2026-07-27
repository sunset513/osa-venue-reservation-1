// src/api/index.js
import axios from "axios";
// import { error as showErrorToast } from "@/utils/useToast.js"; // todo: 待優化 - 目前直接從 useToast 引入錯誤提示函式，未來可改為事件總線或全局狀態管理觸發 Toast

// 建立 Axios 實例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 10000, // 請求超時時間 10 秒
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

const redirectToLogin = () => {
  if (typeof window === "undefined") {
    return;
  }

  import("@/stores/authSession")
    .then(({ useAuthSessionStore }) => {
      useAuthSessionStore().clearSessionState();
    })
    .catch(() => {});

  const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  const loginUrl = `/auth/login?redirect=${encodeURIComponent(currentPath)}`;

  if (window.location.pathname !== "/auth/login") {
    window.location.assign(loginUrl);
  }
};

// Response 攔截器 (接收響應後)
request.interceptors.response.use(
  (response) => {
    // 解析後端的統一回應格式格式: { success, message, data }
    const res = response.data;
    console.log("API 回應:", res);
    if (res.success) {
      return res.data; // 直接回傳 data 給業務邏輯使用
    } else {
      // 處理業務邏輯錯誤 (例如 400 參數錯誤)
      console.error("API 業務錯誤:", res.message);
      return Promise.reject(Object.assign(new Error("API business error"), {
        code: "API_BUSINESS_ERROR",
        serverMessage: res.message,
      }));
    }
  },
  (error) => {
    // 處理 HTTP 狀態碼錯誤 (如 401, 404, 500)
    if (error.response) {
      const status = error.response.status;
      let errorCode = "HTTP_ERROR";

      switch (status) {
        case 401:
          errorCode = "AUTH_REQUIRED";
          redirectToLogin();
          break;
        case 403:
          errorCode = "FORBIDDEN";
          break;
        case 404:
          errorCode = "NOT_FOUND";
          break;
        case 500:
          errorCode = "SERVER_ERROR";
          break;
        default:
          errorCode = "HTTP_ERROR";
      }

      console.error("HTTP request failed:", status, error);
      return Promise.reject(Object.assign(new Error("HTTP request failed"), {
        code: errorCode,
        status,
        cause: error,
      }));
    } else {
      console.error("網路連線失敗，請檢查伺服器狀態");
      return Promise.reject(Object.assign(new Error("Network request failed"), {
        code: "NETWORK_ERROR",
        cause: error,
      }));
    }
  },
);

export default request;
