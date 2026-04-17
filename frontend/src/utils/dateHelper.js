// src/utils/dateHelper.js

/**
 * 將單一數字補零 (例如: 8 -> '08')
 */
const padZero = (num) => num.toString().padStart(2, "0");

export const groupContiguousSlots = (slots) => {
  if (!slots || slots.length === 0) return [];

  const sorted = [...slots].sort((a, b) => a - b);
  const groups = [];
  let currentGroup = [sorted[0]];

  for (let index = 1; index < sorted.length; index += 1) {
    if (sorted[index] === sorted[index - 1] + 1) {
      currentGroup.push(sorted[index]);
    } else {
      groups.push(currentGroup);
      currentGroup = [sorted[index]];
    }
  }

  groups.push(currentGroup);
  return groups;
};

export const formatSlotsAsTimeRange = (slots) => {
  if (!slots || slots.length === 0) return "";

  const sortedSlots = [...slots].sort((a, b) => a - b);
  const start = sortedSlots[0];
  const end = sortedSlots[sortedSlots.length - 1] + 1;

  return `${padZero(start)}:00 - ${padZero(end)}:00`;
};

export const formatSlotGroupsAsTimeRange = (slots) => {
  return groupContiguousSlots(slots)
    .map((group) => formatSlotsAsTimeRange(group))
    .join("、");
};

/**
 * 將後端的 slots 陣列轉換為 FullCalendar 可用的 start / end 時間字串
 * @param {string} dateStr - 日期字串 (YYYY-MM-DD)
 * @param {number[]} slots - 時段陣列 (例如 [8, 9, 10])
 * @returns {Object} { start: string, end: string }
 */
export const convertSlotsToTimeRange = (dateStr, slots) => {
  if (!slots || slots.length === 0) return null;

  // 假設 slots 是連續的，取最小值為開始，最大值 + 1 為結束
  const minSlot = Math.min(...slots);
  const maxSlot = Math.max(...slots);

  const startTime = `${padZero(minSlot)}:00:00`;
  // 注意：如果是 23，結束時間應該是隔天的 00:00，但在 FullCalendar 同一天內可以寫 24:00:00 或直接用 ISO 字串處理
  // 為了安全起見，我們組裝出完整的 ISO 8601 字串

  const startStr = `${dateStr}T${startTime}`;

  // 處理跨日問題 (例如 slot 包含 23)
  let endStr;
  if (maxSlot === 23) {
    const nextDay = new Date(dateStr);
    nextDay.setDate(nextDay.getDate() + 1);
    const nextDayStr = nextDay.toISOString().split("T")[0];
    endStr = `${nextDayStr}T00:00:00`;
  } else {
    endStr = `${dateStr}T${padZero(maxSlot + 1)}:00:00`;
  }

  return {
    start: startStr,
    end: endStr,
  };
};

/**
 * 取得狀態對應的顏色設定 (支援 FullCalendar event 屬性)
 * @param {number} status - 預約狀態碼
 * @returns {Object} 顏色設定物件
 */
export const getEventColorConfig = (status, isMine = false) => {
  if (!isMine) {
    // 他人的預約 (僅顯示已佔用)
    return {
      backgroundColor: "#4A9A57",
      borderColor: "#3d8148",
      textColor: "#ffffff",
    };
  }

  // 我的預約
  switch (status) {
    case 1: // 審核中
      return {
        backgroundColor: "#fdcb6e", // 橘黃
        borderColor: "#e1b12c",
        textColor: "#2d3436",
      };
    case 2: // 已通過
      return {
        backgroundColor: "#0984e3", // 品牌藍
        borderColor: "#74b9ff",
        textColor: "#ffffff",
      };
    case 3: // 已拒絕
      return {
        backgroundColor: "#d63031", // 淡紅
        borderColor: "#ff7675",
        textColor: "#ffffff",
      };
    default:
      return {
        backgroundColor: "#dfe6e9",
        borderColor: "#b2bec3",
      };
  }
};

export const getBookingStatusMeta = (status) => {
  switch (status) {
    case 1:
      return { text: "審核中", className: "is-pending" };
    case 2:
      return { text: "已通過", className: "is-approved" };
    case 3:
      return { text: "已拒絕", className: "is-rejected" };
    default:
      return { text: "已撤回", className: "is-withdrawn" };
  }
};

export const getReviewEventColorConfig = (status) => {
  switch (status) {
    case 1:
      return {
        backgroundColor: "#f4d37b",
        borderColor: "#d7ac35",
        textColor: "#2d3436",
      };
    case 2:
      return {
        backgroundColor: "#2f8f63",
        borderColor: "#246f4d",
        textColor: "#ffffff",
      };
    case 3:
      return {
        backgroundColor: "#ca5656",
        borderColor: "#a83b3b",
        textColor: "#ffffff",
      };
    default:
      return {
        backgroundColor: "#7b8794",
        borderColor: "#616d79",
        textColor: "#ffffff",
      };
  }
};
