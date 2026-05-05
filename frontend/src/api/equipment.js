// src/api/equipment.js
import request from "./index";

// ==========================================
// Equipment Management
// ==========================================

/**
 * Fetch all equipment grouped by venue, including current usage status.
 * @returns {Promise<Array>} Equipment groups by venue.
 */
export const fetchEquipmentGroups = () => {
  return request.get("/equipment");
};

/**
 * Create a new equipment item and associate it with a venue.
 * @param {Object} equipmentData - Equipment payload.
 * @returns {Promise<number>} Created or restored equipment ID.
 */
export const createEquipment = (equipmentData) => {
  return request.post("/equipment", equipmentData);
};

/**
 * Update an existing equipment item.
 * @param {number|string} id - Equipment ID.
 * @param {Object} equipmentData - Equipment payload.
 * @returns {Promise<string>} Success message.
 */
export const updateEquipment = (id, equipmentData) => {
  return request.put(`/equipment/${id}`, equipmentData);
};

/**
 * Soft delete an equipment item.
 * @param {number|string} id - Equipment ID.
 * @returns {Promise<string>} Success message.
 */
export const deleteEquipment = (id) => {
  return request.delete(`/equipment/${id}`);
};

/**
 * Fetch paginated equipment borrow history.
 * @param {Object} pagination - Pagination params.
 * @param {number} pagination.pageNum - Page number, starting from 1.
 * @param {number} pagination.pageSize - Page size.
 * @returns {Promise<Object>} Paginated borrow history.
 */
export const fetchEquipmentBorrowHistory = ({ pageNum = 1, pageSize = 10 } = {}) => {
  return request.get("/equipment/history", {
    params: { pageNum, pageSize },
  });
};
