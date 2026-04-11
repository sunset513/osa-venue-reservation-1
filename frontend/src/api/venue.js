// src/api/venue.js
import request from "./index";

/**
 * 1.1 取得所有管理單位清單
 * @returns {Promise<Array>} 單位列表
 */
export const fetchAllUnits = () => {
  return request.get("/public/units");
};

/**
 * 1.2 根據單位取得場地清單
 * @param {number|string} unitId - 單位 ID
 * @returns {Promise<Array>} 場地列表
 */
export const fetchVenuesByUnit = (unitId) => {
  return request.get("/public/venues", {
    params: { unitId },
  });
};

/**
 * 1.3 取得單一場地的詳細資訊
 * @param {number|string} id - 場地 ID
 * @returns {Promise<Object>} 場地詳細資訊 (含設備清單)
 */
export const fetchVenueDetail = (id) => {
  return request.get(`/public/venues/${id}`);
};
