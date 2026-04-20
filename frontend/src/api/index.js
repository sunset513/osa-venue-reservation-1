// src/api/index.js
import axios from "axios";
// import { error as showErrorToast } from "@/utils/useToast.js"; // todo: 待優化 - 目前直接從 useToast 引入錯誤提示函式，未來可改為事件總線或全局狀態管理觸發 Toast

// 建立 Axios 實例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 10000, // 請求超時時間 10 秒
  headers: {
    "Content-Type": "application/json",
  },
});

// Request 攔截器 (發送請求前)
request.interceptors.request.use(
  (config) => {
    if (!config.headers?.Authorization) {
      // 配合後端 MockAuthInterceptor，帶入暫時性身分校驗 Token
      // 未來串接 Portal 或 JWT 時，可改從 localStorage 或 Pinia Store 讀取
      const mockToken = "mock-token-123";
      config.headers["Authorization"] = mockToken;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

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
      // showErrorToast(res.message || "系統發生未知錯誤");
      return Promise.reject(new Error(res.message || "系統發生未知錯誤"));
    }
  },
  (error) => {
    // 處理 HTTP 狀態碼錯誤 (如 401, 404, 500)
    if (error.response) {
      const status = error.response.status;
      let errorMsg = "發生未知錯誤";

      switch (status) {
        case 401:
          errorMsg = "尚未登入或 Token 已過期";
          break;
        case 403:
          errorMsg = "無權限執行此操作";
          break;
        case 404:
          errorMsg = "請求的資源不存在";
          break;
        case 500:
          errorMsg = "伺服器內部錯誤";
          break;
        default:
          errorMsg = `連線錯誤 (${status})`;
      }

      console.error(errorMsg);
      // 實際觸發 HTTP 狀態碼錯誤的 Toast
      // showErrorToast(errorMsg);
    } else {
      console.error("網路連線失敗，請檢查伺服器狀態");
      // showErrorToast("網路連線失敗，請檢查伺服器狀態");
    }
    return Promise.reject(error);
  },
);

export default request;
