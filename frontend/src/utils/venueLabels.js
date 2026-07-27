/**
 * 場地顯示名稱共用轉換。
 * 後端部分 API 仍可能回傳短名稱，這裡統一轉成和選擇場地頁一致的顯示文案。
 */
const VENUE_DISPLAY_NAME_MAP = {
  "場地 1": "志道樓 2 樓會議室 (A)",
  "場地1": "志道樓 2 樓會議室 (A)",
  "會議室 A": "志道樓 2 樓會議室 (A)",
  "會議室 (A)": "志道樓 2 樓會議室 (A)",
  "志道樓 2 樓會議室 (A)": "志道樓 2 樓會議室 (A)",
  "場地 2": "志道樓 1 樓研討室 (B)",
  "場地2": "志道樓 1 樓研討室 (B)",
  "研討室 B": "志道樓 1 樓研討室 (B)",
  "研討室 (B)": "志道樓 1 樓研討室 (B)",
  "志道樓 1 樓研討室 (B)": "志道樓 1 樓研討室 (B)",
};

const VENUE_ENGLISH_NAME_MAP = {
  "志道樓 2 樓會議室 (A)": "Zhi-Dao Hall 2F Conference Room A",
  "志道樓 1 樓研討室 (B)": "Zhi-Dao Hall 1F Seminar Room B",
  "會議室": "Conference Room",
  "交誼廳": "Lounge",
  "學務長會議室": "Vice President for Student Affairs Conference Room",
};

export const normalizeVenueDisplayName = (venueName) => {
  if (typeof venueName !== "string") return "";

  const normalizedName = venueName.trim();
  return VENUE_DISPLAY_NAME_MAP[normalizedName] || normalizedName;
};

export const formatVenueDisplayName = (venueName, locale = "zh-TW") => {
  const normalizedName = normalizeVenueDisplayName(venueName);
  if (!normalizedName || !String(locale).toLowerCase().startsWith("en")) {
    return normalizedName;
  }

  const englishName = VENUE_ENGLISH_NAME_MAP[normalizedName];
  return englishName ? `${normalizedName} (${englishName})` : normalizedName;
};
