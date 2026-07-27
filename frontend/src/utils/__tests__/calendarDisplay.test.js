/**
 * calendarDisplay.js 單元測試。
 * 驗證日期 key、每日 event 計數與 more link 文案輸出。
 */
import { createI18n } from "vue-i18n";
import { describe, expect, it } from "vitest";
import enUS from "../../i18n/messages/en-US.json";
import zhTW from "../../i18n/messages/zh-TW.json";
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
  const i18n = createI18n({
    legacy: false,
    locale: "zh-TW",
    messages: {
      "zh-TW": zhTW,
      "en-US": enUS,
    },
  });

  it.each([
    ["zh-TW", 3, "還有 3 筆"],
    ["en-US", 2, "2 more"],
  ])(
    "renders the interpolated common.moreItems label for %s",
    (locale, count, expectedLabel) => {
      i18n.global.locale.value = locale;
      const translatedLabel = i18n.global.t("common.moreItems", { count });

      expect(translatedLabel).toBe(expectedLabel);
      expect(renderMoreLinkContent(translatedLabel)).toEqual({
        html: `<span class="calendar-more-link-text">${expectedLabel}</span>`,
      });
    },
  );

  it("renders a fully translated label without changing it", () => {
    expect(renderMoreLinkContent("Custom translated label")).toEqual({
      html: '<span class="calendar-more-link-text">Custom translated label</span>',
    });
  });
});
