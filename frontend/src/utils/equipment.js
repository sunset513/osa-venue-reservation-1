/**
 * Pure helpers for equipment API data.
 */

import { formatSlotGroupsAsTimeRange } from "@/utils/dateHelper";
import { parseContactInfo } from "@/utils/bookingMeta";

const EMPTY_PAGE = {
  total: 0,
  totalPages: 0,
  pageNo: 1,
  pageSize: 10,
  hasNext: false,
  items: [],
};

const toNumberOrNull = (value) => {
  if (value === null || value === undefined || value === "") return null;

  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
};

const toPositiveInteger = (value, fallback) => {
  const numberValue = Number(value);
  return Number.isInteger(numberValue) && numberValue > 0 ? numberValue : fallback;
};

const toNonNegativeInteger = (value, fallback = 0) => {
  const numberValue = Number(value);
  return Number.isInteger(numberValue) && numberValue >= 0 ? numberValue : fallback;
};

const toText = (value, fallback = "") => {
  if (value === null || value === undefined) return fallback;
  return String(value);
};

const normalizeAllowedVenue = (venue = {}) => ({
  venueId: toNumberOrNull(venue.venueId),
  venueName: toText(venue.venueName, "未提供場地"),
  ruleNote: toText(venue.ruleNote),
});

export const normalizeEquipmentMaster = (item = {}) => {
  const allowedVenues = Array.isArray(item.allowedVenues)
    ? item.allowedVenues.map(normalizeAllowedVenue)
    : [];

  return {
    id: toNumberOrNull(item.id),
    name: toText(item.name, "未命名設備"),
    totalQuantity: toPositiveInteger(item.totalQuantity, 0),
    description: toText(item.description),
    borrowNote: toText(item.borrowNote),
    venueRestricted: Boolean(item.venueRestricted || allowedVenues.length > 0),
    allowedVenues,
    deletedAt: item.deletedAt || null,
  };
};

export const normalizeEquipmentMasters = (items = []) => {
  if (!Array.isArray(items)) return [];
  return items.map(normalizeEquipmentMaster);
};

export const isEquipmentAllowedForVenue = (equipment, venueId) => {
  const normalizedVenueId = toNumberOrNull(venueId);
  if (!normalizedVenueId) return true;

  // Equipment without venue rules can be used by any venue booking. When rules
  // exist, the frontend mirrors the backend rule check so users only see
  // relevant choices before submitting the request.
  if (!equipment.venueRestricted || equipment.allowedVenues.length === 0) {
    return true;
  }

  return equipment.allowedVenues.some((venue) => venue.venueId === normalizedVenueId);
};

export const normalizeEquipmentStatus = (item = {}) => {
  const activeBookings = Array.isArray(item.activeBookings)
    ? item.activeBookings.map((booking) => ({
        equipmentBookingId: toNumberOrNull(booking.equipmentBookingId),
        userId: toText(booking.userId, "未提供申請人"),
        borrowDate: toText(booking.borrowDate),
        slots: Array.isArray(booking.slots) ? booking.slots.map(Number).filter(Number.isFinite) : [],
        quantity: toPositiveInteger(booking.quantity, 0),
        purpose: toText(booking.purpose, "未填寫用途"),
        contact: parseContactInfo(booking.contactInfo),
        relatedVenueBookingId: toNumberOrNull(booking.relatedVenueBookingId),
        relatedVenueId: toNumberOrNull(booking.relatedVenueId),
        relatedVenueName: toText(booking.relatedVenueName),
      }))
    : [];

  return {
    equipmentId: toNumberOrNull(item.equipmentId),
    equipmentName: toText(item.equipmentName, "未命名設備"),
    totalQuantity: toPositiveInteger(item.totalQuantity, 0),
    borrowedQuantity: toNonNegativeInteger(item.borrowedQuantity, 0),
    availableQuantity: toNonNegativeInteger(item.availableQuantity, 0),
    isInUse: Boolean(item.inUse),
    activeBookings,
  };
};

export const normalizeEquipmentStatuses = (items = []) => {
  if (!Array.isArray(items)) return [];
  return items.map(normalizeEquipmentStatus);
};

export const normalizeEquipmentBookingItem = (item = {}) => ({
  id: toNumberOrNull(item.id),
  equipmentId: toNumberOrNull(item.equipmentId),
  equipmentName: toText(item.equipmentName, "未命名設備"),
  quantity: toPositiveInteger(item.quantity, 0),
});

export const normalizeEquipmentBooking = (record = {}) => {
  const slots = Array.isArray(record.slots) ? record.slots.map(Number).filter(Number.isFinite) : [];
  const items = Array.isArray(record.items) ? record.items.map(normalizeEquipmentBookingItem) : [];

  return {
    id: toNumberOrNull(record.id),
    userId: toText(record.userId),
    borrowDate: toText(record.borrowDate),
    slots,
    timeRange: formatSlotGroupsAsTimeRange(slots) || "未提供時段",
    status: toNumberOrNull(record.status),
    purpose: toText(record.purpose, "未填寫用途"),
    contact: parseContactInfo(record.contactInfo),
    relatedVenueBookingId: toNumberOrNull(record.relatedVenueBookingId),
    relatedVenueId: toNumberOrNull(record.relatedVenueId),
    relatedVenueName: toText(record.relatedVenueName),
    reviewedBy: toText(record.reviewedBy),
    reviewedAt: record.reviewedAt || null,
    version: toPositiveInteger(record.version, 1),
    createdAt: record.createdAt || null,
    updatedAt: record.updatedAt || null,
    items,
    itemSummary: items.length
      ? items.map((item) => `${item.equipmentName} x ${item.quantity}`).join("、")
      : "未選擇設備",
  };
};

export const normalizeEquipmentBookingPage = (page = {}) => {
  const total = toNonNegativeInteger(page.total ?? page.totalCount, EMPTY_PAGE.total);
  const pageSize = toPositiveInteger(page.pageSize, EMPTY_PAGE.pageSize);
  const totalPages = toNonNegativeInteger(
    page.totalPages,
    pageSize > 0 ? Math.ceil(total / pageSize) : 0,
  );

  const rawItems = Array.isArray(page.items)
    ? page.items
    : Array.isArray(page.data)
      ? page.data
      : [];

  return {
    total,
    totalCount: total,
    totalPages,
    pageNo: toPositiveInteger(page.pageNo ?? page.currentPage, EMPTY_PAGE.pageNo),
    currentPage: toPositiveInteger(page.currentPage ?? page.pageNo, EMPTY_PAGE.pageNo),
    pageSize,
    hasNext: Boolean(page.hasNext),
    items: rawItems.map(normalizeEquipmentBooking),
    data: rawItems.map(normalizeEquipmentBooking),
  };
};

export const getEquipmentStatusMeta = (isInUse) => {
  if (isInUse) {
    return { text: "使用中", className: "is-in-use" };
  }

  return { text: "閒置", className: "is-idle" };
};

export const getEquipmentBookingStatusMeta = (status) => {
  switch (Number(status)) {
    case 1:
      return { text: "審核中", className: "is-pending" };
    case 2:
      return { text: "已通過", className: "is-approved" };
    case 3:
      return { text: "已被拒絕", className: "is-rejected" };
    case 0:
      return { text: "已撤回", className: "is-withdrawn" };
    default:
      return { text: "未知", className: "is-withdrawn" };
  }
};

export const buildEquipmentMasterPayload = (form = {}) => ({
  name: toText(form.name || form.equipmentName).trim(),
  totalQuantity: toPositiveInteger(form.totalQuantity ?? form.quantity, 1),
  description: toText(form.description),
  borrowNote: toText(form.borrowNote),
  venueRules: Array.isArray(form.venueRules)
    ? form.venueRules
        .map((rule) => ({
          venueId: toNumberOrNull(rule?.venueId),
          ruleNote: toText(rule?.ruleNote).trim(),
        }))
        .filter((rule) => rule.venueId)
    : [],
});

export const buildEquipmentPayload = (form = {}) => ({
  equipmentName: toText(form.equipmentName).trim(),
  venueId: toNumberOrNull(form.venueId),
  quantity: toPositiveInteger(form.quantity, 1),
});

export const buildEquipmentBookingItems = (items = []) => {
  if (!Array.isArray(items)) return [];

  return items
    .map((item) => ({
      equipmentId: toNumberOrNull(item.equipmentId),
      quantity: toPositiveInteger(item.quantity, 0),
    }))
    .filter((item) => item.equipmentId && item.quantity > 0);
};

export const getEquipmentBookingEditTarget = (record = {}) => {
  if (Number(record.status) !== 1) return null;

  return toNumberOrNull(record.relatedVenueBookingId) ? "venue" : "equipment";
};

export const canEditEquipmentBookingOnHistoryPage = (record = {}) => {
  return getEquipmentBookingEditTarget(record) === "equipment";
};

export const buildEquipmentBookingUpdatePayload = (form = {}) => ({
  borrowDate: toText(form.borrowDate || form.bookingDate),
  slots: Array.isArray(form.slots)
    ? [...new Set(form.slots.map(Number).filter((slot) =>
        Number.isInteger(slot) && slot >= 0 && slot <= 23,
      ))].sort((left, right) => left - right)
    : [],
  purpose: toText(form.purpose).trim(),
  contactInfo: {
    name: toText(form.contactInfo?.name).trim(),
    phone: toText(form.contactInfo?.phone).trim(),
    email: toText(form.contactInfo?.email).trim(),
  },
  relatedVenueBookingId: toNumberOrNull(form.relatedVenueBookingId),
  items: buildEquipmentBookingItems(form.items ?? form.equipmentItems),
});

// Backward-compatible helpers retained for older components while their data
// source moves from the removed grouped endpoint to the new equipment module.
export const normalizeEquipmentItem = (item = {}) => ({
  venueId: toNumberOrNull(item.venueId),
  venueName: toText(item.venueName, "未提供場地"),
  equipmentId: toNumberOrNull(item.equipmentId ?? item.id),
  equipmentName: toText(item.equipmentName ?? item.name, "未命名設備"),
  quantity: toPositiveInteger(item.quantity ?? item.totalQuantity, 0),
  isInUse: Boolean(item.isInUse ?? item.inUse),
});

export const normalizeEquipmentGroup = (group = {}) => {
  const equipmentList = Array.isArray(group.equipmentList)
    ? group.equipmentList.map(normalizeEquipmentItem)
    : [];

  return {
    venueName: toText(group.venueName || equipmentList[0]?.venueName, "未提供場地"),
    equipmentList,
  };
};

export const normalizeEquipmentGroups = (groups = []) => {
  if (!Array.isArray(groups)) return [];
  return groups.map(normalizeEquipmentGroup);
};

export const flattenEquipmentGroups = (groups = []) => {
  return normalizeEquipmentGroups(groups).flatMap((group) => group.equipmentList);
};

export const normalizeEquipmentBorrowRecord = normalizeEquipmentBooking;

export const normalizeEquipmentBorrowPage = normalizeEquipmentBookingPage;
