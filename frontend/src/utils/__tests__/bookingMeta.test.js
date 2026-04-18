/**
 * bookingMeta.js 單元測試。
 * 驗證聯絡資訊解析與狀態 metadata 對應是否維持穩定。
 */
import { describe, expect, it } from "vitest";
import { getBookingStatusMeta, parseContactInfo } from "../bookingMeta";

describe("parseContactInfo", () => {
  it("parses valid contact info JSON", () => {
    expect(parseContactInfo('{"name":"Alice","phone":"0912","email":"a@example.com"}')).toEqual({
      name: "Alice",
      phone: "0912",
      email: "a@example.com",
    });
  });

  it("returns empty contact info for empty values", () => {
    expect(parseContactInfo("")).toEqual({
      name: "",
      phone: "",
      email: "",
    });
  });

  it("returns empty contact info for invalid JSON", () => {
    expect(parseContactInfo("{invalid-json")).toEqual({
      name: "",
      phone: "",
      email: "",
    });
  });
});

describe("getBookingStatusMeta", () => {
  it("maps pending status", () => {
    expect(getBookingStatusMeta(1)).toEqual({
      text: "審核中",
      className: "is-pending",
    });
  });

  it("maps approved status", () => {
    expect(getBookingStatusMeta(2)).toEqual({
      text: "已通過",
      className: "is-approved",
    });
  });

  it("maps rejected status", () => {
    expect(getBookingStatusMeta(3)).toEqual({
      text: "已拒絕",
      className: "is-rejected",
    });
  });

  it("falls back to withdrawn status", () => {
    expect(getBookingStatusMeta(0)).toEqual({
      text: "已撤回",
      className: "is-withdrawn",
    });
  });
});
