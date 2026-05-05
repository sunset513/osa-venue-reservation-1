/**
 * Pure helpers for the paginated booking query API.
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

const normalizeStatusList = (filters = {}) => {
  const rawStatuses = Array.isArray(filters.statusList)
    ? filters.statusList
    : [filters.status ?? filters.statusFilter].filter((value) => value !== undefined);

  const statuses = rawStatuses
    .map(toNumberOrNull)
    .filter((status) => status !== null);

  return statuses.length > 0 ? statuses : null;
};

export const buildBookingQueryPayload = (filters = {}) => {
  return {
    venueId: toNumberOrNull(filters.venueId),
    statusList: normalizeStatusList(filters),
    startDate: filters.startDate || null,
    endDate: filters.endDate || null,
    pageNo: toPositiveInteger(filters.pageNo, 1),
    pageSize: toPositiveInteger(filters.pageSize, 20),
  };
};

export const normalizeBookingPage = (page = {}) => {
  const pageNo = toPositiveInteger(page.pageNo, 1);
  const pageSize = toPositiveInteger(page.pageSize, 20);
  const total = toPositiveInteger(page.total, 0);
  const calculatedTotalPages = Math.ceil(total / pageSize);
  const totalPages = Number.isInteger(Number(page.totalPages))
    ? Number(page.totalPages)
    : calculatedTotalPages;

  return {
    total,
    pageNo,
    pageSize,
    totalPages,
    hasNext: typeof page.hasNext === "boolean" ? page.hasNext : pageNo < totalPages,
    items: Array.isArray(page.items) ? page.items : [],
  };
};
