/**
 * Pure helpers for equipment API data.
 */

const toNumberOrNull = (value) => {
  if (value === null || value === undefined || value === "") return null;

  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
};

const toPositiveInteger = (value, fallback) => {
  const numberValue = Number(value);
  return Number.isInteger(numberValue) && numberValue > 0 ? numberValue : fallback;
};

const toText = (value, fallback = "") => {
  if (value === null || value === undefined) return fallback;
  return String(value);
};

export const normalizeEquipmentItem = (item = {}) => {
  return {
    venueId: toNumberOrNull(item.venueId),
    venueName: toText(item.venueName, "未提供場地"),
    equipmentId: toNumberOrNull(item.equipmentId),
    equipmentName: toText(item.equipmentName, "未命名設備"),
    quantity: toPositiveInteger(item.quantity, 0),
    isInUse: Boolean(item.isInUse),
  };
};

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

export const getEquipmentStatusMeta = (isInUse) => {
  if (isInUse) {
    return { text: "使用中", className: "is-in-use" };
  }

  return { text: "閒置", className: "is-idle" };
};

export const normalizeEquipmentBorrowRecord = (record = {}) => {
  return {
    venueId: toNumberOrNull(record.venueId),
    venueName: toText(record.venueName, "未提供場地"),
    equipmentId: toNumberOrNull(record.equipmentId),
    equipmentName: toText(record.equipmentName, "未命名設備"),
    borrowDate: toText(record.borrowDate),
    timeSlots: toText(record.timeSlots, "未提供時段"),
    purpose: toText(record.purpose, "未填寫用途"),
  };
};

export const normalizeEquipmentBorrowPage = (page = {}) => {
  const pageSize = toPositiveInteger(page.pageSize, 10);
  const totalCount = toPositiveInteger(page.totalCount, 0);
  const totalPages = Number.isInteger(Number(page.totalPages))
    ? Number(page.totalPages)
    : Math.ceil(totalCount / pageSize);

  return {
    totalCount,
    totalPages,
    currentPage: toPositiveInteger(page.currentPage, 1),
    pageSize,
    data: Array.isArray(page.data)
      ? page.data.map(normalizeEquipmentBorrowRecord)
      : [],
  };
};

export const buildEquipmentPayload = (form = {}) => {
  return {
    equipmentName: toText(form.equipmentName).trim(),
    venueId: toNumberOrNull(form.venueId),
    quantity: toPositiveInteger(form.quantity, 1),
  };
};
