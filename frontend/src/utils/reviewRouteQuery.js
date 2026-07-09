const firstQueryValue = (value) => {
  if (Array.isArray(value)) return value[0];
  return value;
};

const toTrimmedText = (value) => {
  const text = firstQueryValue(value);
  return typeof text === "string" ? text.trim() : "";
};

const parseEquipmentStatus = (value) => {
  const status = toTrimmedText(value).toLowerCase();

  if (status === "all") return "";
  if (["1", "2", "3"].includes(status)) return status;

  return null;
};

export const parseReviewRouteQuery = (query = {}) => {
  const mode = toTrimmedText(query.mode).toLowerCase();
  const equipmentStatus = parseEquipmentStatus(query.equipmentStatus);

  return {
    activeReviewMode: mode === "equipment" ? "equipment" : null,
    equipmentKeyword: toTrimmedText(query.equipmentKeyword),
    equipmentStatus,
  };
};
