import { ref, nextTick } from "vue";

/**
 * 全域 Toast composable。
 * 負責集中管理提示訊息清單，讓頁面、元件與 router guard 都能共用同一套通知機制。
 */
const toasts = ref([]);
let nextId = 0;

/**
 * 顯示 toast 訊息
 * @param {Object} options - 選項
 * @param {string} options.message - 訊息內容
 * @param {string} options.type - 類型: success, error, warning, info (預設: info)
 * @param {number} options.duration - 顯示時長(毫秒)，0 表示不自動關閉 (預設: 3000)
 * @returns {number} toast ID
 */
const showToast = async ({ message, type = "info", duration = 3000 }) => {
  const id = nextId++;
  const toast = {
    id,
    message,
    type,
  };

  toasts.value.push(toast);

  // 等待 Vue 批量更新完成 (解決 FullCalendar eventClick 回調的響應式問題)
  await nextTick();

  // 自動關閉
  if (duration > 0) {
    setTimeout(() => {
      removeToast(id);
    }, duration);
  }

  return id;
};

/**
 * 移除指定 ID 的 toast
 * @param {number} id - Toast ID
 */
const removeToast = (id) => {
  const index = toasts.value.findIndex((t) => t.id === id);
  if (index > -1) {
    toasts.value.splice(index, 1);
  }
};

/**
 * 清空所有 toast
 */
const clearToasts = () => {
  toasts.value = [];
};

/**
 * 便捷方法 - 成功
 * @param {string} message - 訊息
 * @param {number} duration - 顯示時長
 */
const success = (message, duration = 3000) => {
  return showToast({ message, type: "success", duration });
};

/**
 * 便捷方法 - 錯誤
 * @param {string} message - 訊息
 * @param {number} duration - 顯示時長
 */
const error = (message, duration = 3000) => {
  return showToast({ message, type: "error", duration });
};

/**
 * 便捷方法 - 警告
 * @param {string} message - 訊息
 * @param {number} duration - 顯示時長
 */
const warning = (message, duration = 3000) => {
  return showToast({ message, type: "warning", duration });
};

/**
 * 便捷方法 - 資訊
 * @param {string} message - 訊息
 * @param {number} duration - 顯示時長
 */
const info = (message, duration = 3000) => {
  return showToast({ message, type: "info", duration });
};

/**
 * 提供元件內使用的 composable 入口。
 */
export const useToast = () => {
  return {
    toasts,
    showToast,
    removeToast,
    clearToasts,
    success,
    error,
    warning,
    info,
  };
};

// 也匯出單獨的方法，讓非組件上下文也能直接觸發提示訊息。
export { showToast, removeToast, clearToasts, success, error, warning, info };
