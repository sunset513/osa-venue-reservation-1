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

export const normalizeVenueDisplayName = (venueName) => {
  if (typeof venueName !== "string") return "";

  const normalizedName = venueName.trim();
  return VENUE_DISPLAY_NAME_MAP[normalizedName] || normalizedName;
};
