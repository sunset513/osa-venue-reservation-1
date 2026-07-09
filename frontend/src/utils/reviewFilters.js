const toSearchableText = (value) => {
  if (value === null || value === undefined) return "";
  return String(value).trim().toLowerCase();
};

const matchesKeywordFields = (keyword, fields = []) => {
  const normalizedKeyword = toSearchableText(keyword);
  if (!normalizedKeyword) return true;

  return fields.some((field) => toSearchableText(field).includes(normalizedKeyword));
};

export const matchesReviewDateRange = (dateValue, filters = {}) => {
  const normalizedDate = typeof dateValue === "string" ? dateValue : "";
  const { startDate = "", endDate = "" } = filters;

  if (!startDate && !endDate) return true;
  if (!normalizedDate) return false;
  if (startDate && normalizedDate < startDate) return false;
  if (endDate && normalizedDate > endDate) return false;

  return true;
};

export const hasActiveReviewFilters = (filters = {}) => {
  return (
    toSearchableText(filters.keyword) !== ""
    || Boolean(filters.startDate)
    || Boolean(filters.endDate)
  );
};

export const countReviewStatuses = (records = []) => {
  return records.reduce(
    (counts, record) => {
      counts.all += 1;

      if (Number(record?.status) === 1) counts.pending += 1;
      if (Number(record?.status) === 2) counts.approved += 1;
      if (Number(record?.status) === 3) counts.rejected += 1;

      return counts;
    },
    { all: 0, pending: 0, approved: 0, rejected: 0 },
  );
};

export const filterVenueReviewList = (records = [], filters = {}) => {
  return records.filter((record) => {
    return (
      matchesReviewDateRange(record?.bookingDate, filters)
      && matchesKeywordFields(filters.keyword, [
        record?.purpose,
        record?.venueName,
        record?.contactName,
        record?.id,
      ])
    );
  });
};

export const filterEquipmentReviewList = (records = [], filters = {}) => {
  return records.filter((record) => {
    return (
      matchesReviewDateRange(record?.borrowDate, filters)
      && matchesKeywordFields(filters.keyword, [
        record?.itemSummary,
        record?.purpose,
        record?.contact?.name,
        record?.relatedVenueBookingTitle,
        record?.relatedVenueName,
        record?.id,
        record?.relatedVenueBookingId,
      ])
    );
  });
};
