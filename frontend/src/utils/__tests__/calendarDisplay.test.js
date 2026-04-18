/**
 * calendarDisplay.js 單元測試。
 * 驗證日期 key、每日 event 計數與 more link 文案輸出。
 */
import { describe, expect, it } from "vitest";
import { formatDateKey, getDailyEventCount, renderMoreLinkContent } from "../calendarDisplay";

describe("formatDateKey", () => {
  it("formats dates as yyyy-mm-dd", () => {
    expect(formatDateKey(new Date("2026-04-18T08:30:00"))).toBe("2026-04-18");
  });

  it("pads single digit month and day", () => {
    expect(formatDateKey(new Date("2026-01-05T00:00:00"))).toBe("2026-01-05");
  });
});

describe("getDailyEventCount", () => {
  it("counts events on the same day", () => {
    const events = [
      { start: "2026-04-18T08:00:00" },
      { start: "2026-04-18T13:00:00" },
      { start: "2026-04-19T09:00:00" },
    ];

    expect(getDailyEventCount(events, new Date("2026-04-18T00:00:00"))).toBe(2);
  });

  it("does not count events on other days", () => {
    const events = [{ start: "2026-04-19T09:00:00" }];

    expect(getDailyEventCount(events, new Date("2026-04-18T00:00:00"))).toBe(0);
  });

  it("ignores events without a start value", () => {
    const events = [{ title: "No start" }, { start: "2026-04-18T10:00:00" }];

    expect(getDailyEventCount(events, new Date("2026-04-18T00:00:00"))).toBe(1);
  });
});

describe("renderMoreLinkContent", () => {
  it('renders "個" labels for the venue calendar', () => {
    expect(renderMoreLinkContent({ num: 3 }, "個")).toEqual({
      html: '<span class="calendar-more-link-text">還有 3 個</span>',
    });
  });

  it('renders "筆" labels by default', () => {
    expect(renderMoreLinkContent({ num: 2 })).toEqual({
      html: '<span class="calendar-more-link-text">還有 2 筆</span>',
    });
  });
});
