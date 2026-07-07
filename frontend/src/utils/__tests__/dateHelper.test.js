import { describe, expect, it } from "vitest";
import { getEventColorConfig } from "../dateHelper";

describe("getEventColorConfig", () => {
  it("uses blue for my approved bookings", () => {
    expect(getEventColorConfig(2, true)).toMatchObject({
      backgroundColor: "#0984e3",
      borderColor: "#74b9ff",
      textColor: "#ffffff",
    });
  });

  it("uses occupied green for other approved bookings", () => {
    expect(getEventColorConfig(2, false)).toMatchObject({
      backgroundColor: "#4A9A57",
      borderColor: "#3d8148",
      textColor: "#ffffff",
    });
  });

  it("uses neutral gray for other pending bookings", () => {
    expect(getEventColorConfig(1, false)).toMatchObject({
      backgroundColor: "#e5e7eb",
      borderColor: "#94a3b8",
      textColor: "#334155",
    });
  });
});
