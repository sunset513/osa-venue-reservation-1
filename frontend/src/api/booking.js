// src/api/booking.js
import request from "./index";

// ==========================================
// 預約操作 (Booking Operations)
// ==========================================

/**
 * 2.1 提交預約申請
 * @param {Object} bookingData - 預約表單數據
 * @returns {Promise<number>} 新建立的預約案 ID
 */
export const createBooking = (bookingData) => {
  return request.post("/bookings", bookingData);
};

/**
 * 2.2 查看個人預約清單
 * @returns {Promise<Array>} 個人的預約申請清單
 */
export const fetchMyBookings = () => {
  return request.get("/bookings/my");
};

/**
 * 2.3 修改預約申請
 * @param {number|string} id - 預約案 ID
 * @param {Object} bookingData - 修改後的預約數據
 * @returns {Promise<null>}
 */
export const updateBooking = (id, bookingData) => {
  return request.put(`/bookings/${id}`, bookingData);
};

/**
 * 2.4 撤回預約申請
 * @param {number|string} id - 預約案 ID
 * @returns {Promise<null>}
 */
export const withdrawBooking = (id) => {
  return request.put(`/bookings/${id}/withdraw`);
};

// ==========================================
// 日曆視圖 (Calendar Views)
// ==========================================

/**
 * 3.1 獲取月曆視圖數據 (僅摘要)
 * @param {number|string} venueId - 場地 ID
 * @param {number} year - 年份 (如 2026)
 * @param {number} month - 月份 (1-12)
 * @returns {Promise<Object>} 月曆佔用摘要
 */
export const fetchCalendarMonth = (venueId, year, month) => {
  return request.get("/bookings/calendar/month", {
    params: { venueId, year, month },
  });
};

/**
 * 3.2 獲取周曆視圖數據 (包含時段)
 * @param {number|string} venueId - 場地 ID
 * @param {string} date - 周開始日期 (必須為周一，ISO 8601 格式 YYYY-MM-DD)
 * @returns {Promise<Object>} 周內每日詳細資訊
 */
export const fetchCalendarWeek = (venueId, date) => {
  return request.get("/bookings/calendar/week", {
    params: { venueId, date },
  });
};

/**
 * 3.3 獲取日曆詳細視圖數據 (包含時段與我的預約詳情)
 * @param {number|string} venueId - 場地 ID
 * @param {string} date - 查詢日期 (ISO 8601 格式 YYYY-MM-DD)
 * @returns {Promise<Object>} 單日最詳細信息
 */
export const fetchCalendarDay = (venueId, date) => {
  return request.get("/bookings/calendar/day", {
    params: { venueId, date },
  });
};
