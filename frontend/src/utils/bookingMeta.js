/**
 * 預約顯示層共用 metadata。
 * 這個檔案只放與預約資訊呈現相關的純函式，不處理 Vue state 或 API。
 */
const EMPTY_CONTACT_INFO = {
  name: "",
  phone: "",
  email: "",
};

/**
 * 將後端存的 contactInfo JSON 安全解析成畫面可用物件。
 * 若內容為空或格式錯誤，回傳空白聯絡資訊避免畫面噴錯。
 */
export const parseContactInfo = (contactInfo) => {
  if (!contactInfo) return { ...EMPTY_CONTACT_INFO };

  try {
    return JSON.parse(contactInfo);
  } catch {
    return { ...EMPTY_CONTACT_INFO };
  }
};

/**
 * 將預約狀態碼轉成畫面顯示文案與對應 class。
 */
export const getBookingStatusMeta = (status) => {
  switch (status) {
    case 1:
      return { text: "審核中", className: "is-pending" };
    case 2:
      return { text: "已通過", className: "is-approved" };
    case 3:
      return { text: "已拒絕", className: "is-rejected" };
    default:
      return { text: "已撤回", className: "is-withdrawn" };
  }
};
