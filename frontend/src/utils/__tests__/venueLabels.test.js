import { describe, expect, it } from "vitest";
import {
  formatVenueDisplayName,
  normalizeVenueDisplayName,
} from "@/utils/venueLabels";

describe("venue display labels", () => {
  it("normalizes known backend aliases", () => {
    expect(normalizeVenueDisplayName("場地 1")).toBe("志道樓 2 樓會議室 (A)");
  });

  it("keeps the canonical Chinese name in the Chinese locale", () => {
    expect(formatVenueDisplayName("場地 2", "zh-TW")).toBe("志道樓 1 樓研討室 (B)");
  });

  it("appends the English translation to known venues in the English locale", () => {
    expect(formatVenueDisplayName("場地 1", "en-US")).toBe(
      "志道樓 2 樓會議室 (A) (Zhi-Dao Hall 2F Conference Room A)",
    );
  });

  it.each([
    ["會議室", "會議室 (Conference Room)"],
    ["交誼廳", "交誼廳 (Lounge)"],
    [
      "學務長會議室",
      "學務長會議室 (Vice President for Student Affairs Conference Room)",
    ],
  ])("appends the English translation for %s", (venueName, expected) => {
    expect(formatVenueDisplayName(venueName, "en-US")).toBe(expected);
  });

  it("preserves unknown venue names instead of guessing a translation", () => {
    expect(formatVenueDisplayName("新場地", "en-US")).toBe("新場地");
  });
});
