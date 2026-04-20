/**
 * 日曆畫面共用顯示 helper。
 * 這裡集中處理與 FullCalendar 畫面呈現有關、但不依賴 Vue state 的純函式。
 */

/**
 * 將 Date 物件轉成 YYYY-MM-DD key，方便比對 event 所屬日期。
 */
export const formatDateKey = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

/**
 * 統計指定日期在 event 陣列中的筆數，用於月曆日期角落的計數 badge。
 */
export const getDailyEventCount = (events, date) => {
  if (!Array.isArray(events) || !date) return 0;

  const dateKey = formatDateKey(date);
  return events.filter((event) => event.start?.split("T")[0] === dateKey).length;
};

/**
 * 產生 FullCalendar more link 的 HTML 內容。
 * `unitLabel` 讓不同頁面可以保留「個 / 筆」等量詞差異。
 */
export const renderMoreLinkContent = (arg, unitLabel = "筆") => {
  return {
    html: `<span class="calendar-more-link-text">還有 ${arg.num} ${unitLabel}</span>`,
  };
};
