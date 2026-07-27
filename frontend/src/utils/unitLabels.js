const UNIT_ENGLISH_NAME_MAP = {
  "學務處本部": "Office of Student Affairs",
};

export const formatUnitDisplayName = (unitName, locale = "zh-TW") => {
  if (typeof unitName !== "string") return "";

  const normalizedName = unitName.trim();
  if (!normalizedName || !String(locale).toLowerCase().startsWith("en")) {
    return normalizedName;
  }

  const englishName = UNIT_ENGLISH_NAME_MAP[normalizedName];
  return englishName ? `${normalizedName} (${englishName})` : normalizedName;
};
