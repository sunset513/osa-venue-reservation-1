import { describe, expect, it, vi } from "vitest";
import { createLocalizedError, resolveErrorMessage } from "../errorMessage";

const createTranslator = (messages = {}) => {
  return vi.fn((key) => messages[key] ?? key);
};

describe("resolveErrorMessage", () => {
  it("prefers an explicit translationKey over an error code", () => {
    const t = createTranslator({
      "pages.bookingModal.toast.venueUpdateFailed": "Unable to update venue",
      "errors.NETWORK_ERROR": "Network error",
    });

    expect(
      resolveErrorMessage(
        {
          translationKey: "pages.bookingModal.toast.venueUpdateFailed",
          code: "NETWORK_ERROR",
        },
        t,
      ),
    ).toBe("Unable to update venue");
    expect(t).toHaveBeenCalledTimes(1);
    expect(t).toHaveBeenCalledWith(
      "pages.bookingModal.toast.venueUpdateFailed",
    );
  });

  it("resolves a known error code through the errors namespace", () => {
    const t = createTranslator({
      "errors.NETWORK_ERROR": "Network connection failed",
    });

    expect(resolveErrorMessage({ code: "NETWORK_ERROR" }, t)).toBe(
      "Network connection failed",
    );
    expect(t).toHaveBeenCalledWith("errors.NETWORK_ERROR");
  });

  it("uses the requested fallback when an error code is unknown", () => {
    const t = createTranslator({
      "pages.equipmentBorrow.submitFailed": "Unable to submit request",
    });

    expect(
      resolveErrorMessage(
        { code: "UNRECOGNIZED_ERROR" },
        t,
        "pages.equipmentBorrow.submitFailed",
      ),
    ).toBe("Unable to submit request");
    expect(t).toHaveBeenNthCalledWith(1, "errors.UNRECOGNIZED_ERROR");
    expect(t).toHaveBeenNthCalledWith(
      2,
      "pages.equipmentBorrow.submitFailed",
    );
  });

  it("uses errors.unknown by default when no localized metadata exists", () => {
    const t = createTranslator({
      "errors.unknown": "An unexpected error occurred",
    });

    expect(resolveErrorMessage(new Error("backend detail"), t)).toBe(
      "An unexpected error occurred",
    );
    expect(t).toHaveBeenCalledWith("errors.unknown");
  });
});

describe("createLocalizedError", () => {
  it("creates an Error with its translation key and original cause", () => {
    const cause = new Error("request failed");
    const error = createLocalizedError(
      "pages.equipmentShared.loadEquipmentFailed",
      cause,
    );

    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe("Localized application error");
    expect(error.translationKey).toBe(
      "pages.equipmentShared.loadEquipmentFailed",
    );
    expect(error.cause).toBe(cause);
  });
});
