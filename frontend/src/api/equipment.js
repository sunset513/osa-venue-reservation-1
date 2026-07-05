// src/api/equipment.js
import request from "./index";

// ==========================================
// Equipment Management
// ==========================================

/**
 * Fetch active equipment master records from the new backend module.
 * The old frontend grouped equipment by venue, but the backend now treats equipment
 * as independent stock with optional venue rules, so grouping should happen in utils/views.
 *
 * @param {Object} options - Query options.
 * @param {boolean} options.includeDeleted - Whether soft-deleted equipment should be included.
 * @returns {Promise<Array>} Equipment master records.
 */
export const listEquipments = ({ includeDeleted = false } = {}) => {
  return request.get("/equipments", {
    params: { includeDeleted },
  });
};

/**
 * Fetch all equipment with current or specified-hour usage details.
 * This powers the status page and avoids forcing the view to stitch master data,
 * approved equipment bookings, and related venue booking details by itself.
 *
 * @param {Object} query - Optional date/hour query.
 * @param {string} query.date - ISO date string, YYYY-MM-DD.
 * @param {number} query.hour - Hour index from 0 to 23.
 * @returns {Promise<Array>} Equipment status rows.
 */
export const getEquipmentStatuses = ({ date, hour } = {}) => {
  const params = {};
  if (date) params.date = date;
  if (hour !== undefined && hour !== null && hour !== "") params.hour = hour;

  return request.get("/equipments/status", { params });
};

/**
 * Backward-compatible alias used by existing pages during migration.
 * It returns the independent equipment list instead of the removed grouped endpoint.
 *
 * @returns {Promise<Array>} Equipment master records.
 */
export const fetchEquipmentGroups = () => {
  return listEquipments();
};

/**
 * Create a new equipment master record.
 * The payload follows the backend EquipmentCreateDTO, where venueRules is optional.
 *
 * @param {Object} equipmentData - Equipment payload.
 * @returns {Promise<number>} Created or restored equipment ID.
 */
export const createEquipment = (equipmentData) => {
  return request.post("/equipments", equipmentData);
};

/**
 * Update an existing equipment item.
 * @param {number|string} id - Equipment ID.
 * @param {Object} equipmentData - Equipment payload.
 * @returns {Promise<string>} Success message.
 */
export const updateEquipment = (id, equipmentData) => {
  return request.put(`/equipments/${id}`, equipmentData);
};

/**
 * Soft delete an equipment item.
 * @param {number|string} id - Equipment ID.
 * @returns {Promise<string>} Success message.
 */
export const deleteEquipment = (id) => {
  return request.delete(`/equipments/${id}`);
};

/**
 * Create a standalone or venue-related equipment booking request.
 *
 * @param {Object} payload - EquipmentBookingCreateDTO-compatible payload.
 * @returns {Promise<number>} Created equipment booking ID.
 */
export const createEquipmentBooking = (payload) => {
  return request.post("/equipment-bookings", payload);
};

/**
 * Query the current user's equipment booking records.
 * The backend accepts pageNo/pageSize plus optional status, date, equipment,
 * relatedVenueBookingId, and standaloneOnly filters.
 *
 * @param {Object} query - EquipmentBookingQueryDTO-compatible payload.
 * @returns {Promise<Object>} Paginated equipment booking page.
 */
export const queryMyEquipmentBookings = (query = {}) => {
  return request.post("/equipment-bookings/query", query);
};

/**
 * Update one of the current user's equipment booking requests.
 *
 * @param {number|string} id - Equipment booking ID.
 * @param {Object} payload - EquipmentBookingUpdateDTO-compatible payload.
 * @returns {Promise<null>} Empty success payload.
 */
export const updateEquipmentBooking = (id, payload) => {
  return request.put(`/equipment-bookings/${id}`, payload);
};

/**
 * Withdraw one of the current user's equipment booking requests.
 *
 * @param {number|string} id - Equipment booking ID.
 * @returns {Promise<null>} Empty success payload.
 */
export const withdrawEquipmentBooking = (id) => {
  return request.put(`/equipment-bookings/${id}/withdraw`);
};

/**
 * Check equipment availability and venue-rule compatibility before submitting.
 *
 * @param {Object} payload - EquipmentAvailabilityQueryDTO-compatible payload.
 * @returns {Promise<Object>} Availability result by equipment item.
 */
export const checkEquipmentAvailability = (payload) => {
  return request.post("/equipment-bookings/availability", payload);
};

/**
 * Fetch paginated equipment borrow history.
 * This preserves the old function name but routes to the new personal query API.
 *
 * @param {Object} pagination - Pagination and filter params.
 * @returns {Promise<Object>} Paginated borrow history.
 */
export const fetchEquipmentBorrowHistory = ({
  pageNum,
  pageNo,
  pageSize = 10,
  ...filters
} = {}) => {
  return queryMyEquipmentBookings({
    ...filters,
    pageNo: pageNo || pageNum || 1,
    pageSize,
  });
};

/**
 * Query equipment booking requests from the reviewer perspective.
 *
 * @param {Object} query - EquipmentBookingQueryDTO-compatible payload.
 * @returns {Promise<Object>} Paginated equipment review page.
 */
export const queryEquipmentReviews = (query = {}) => {
  return request.post("/equipment-reviews/query", query);
};

/**
 * Fetch equipment review items related to a venue booking.
 *
 * @param {number|string} bookingId - Venue booking ID.
 * @returns {Promise<Array>} Related equipment booking review records.
 */
export const getEquipmentReviewsByVenueBooking = (bookingId) => {
  return request.get(`/equipment-reviews/by-venue-booking/${bookingId}`);
};

/**
 * Fetch the pending standalone equipment booking count for the review badge.
 *
 * @returns {Promise<number>} Pending standalone equipment booking count.
 */
export const getStandaloneEquipmentPendingCount = () => {
  return request.get("/equipment-reviews/standalone/pending-count");
};

/**
 * Approve a pending equipment booking request.
 *
 * @param {number|string} id - Equipment booking ID.
 * @returns {Promise<null>} Empty success payload.
 */
export const approveEquipmentReview = (id) => {
  return request.put(`/equipment-reviews/${id}/approve`);
};

/**
 * Reject an equipment booking request without storing a reason.
 *
 * @param {number|string} id - Equipment booking ID.
 * @returns {Promise<null>} Empty success payload.
 */
export const rejectEquipmentReview = (id) => {
  return request.put(`/equipment-reviews/${id}/reject`);
};

/**
 * Update equipment review status directly, matching the venue booking review flow.
 *
 * @param {number|string} id - Equipment booking ID.
 * @param {number} status - Reviewer target status: 1 pending, 2 approved, 3 rejected.
 * @returns {Promise<null>} Empty success payload.
 */
export const updateEquipmentReviewStatus = (id, status) => {
  return request.put(`/equipment-reviews/${id}/status`, { status });
};
