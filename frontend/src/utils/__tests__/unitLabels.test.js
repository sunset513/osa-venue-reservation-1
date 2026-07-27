import { describe, expect, it } from "vitest";
import { formatUnitDisplayName } from "@/utils/unitLabels";

describe("unit display labels", () => {
  it("keeps the Chinese unit name in the Chinese locale", () => {
    expect(formatUnitDisplayName("學務處本部", "zh-TW")).toBe("學務處本部");
  });

  it("appends the English unit name in the English locale", () => {
    expect(formatUnitDisplayName("學務處本部", "en-US")).toBe(
      "學務處本部 (Office of Student Affairs)",
    );
  });

  it("preserves unknown unit names instead of guessing a translation", () => {
    expect(formatUnitDisplayName("新單位", "en-US")).toBe("新單位");
  });
});
