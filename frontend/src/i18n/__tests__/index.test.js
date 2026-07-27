import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { LOCALE_STORAGE_KEY } from "../localeManager";

const setupBrowserGlobals = (storedLocale = null) => {
  const getItem = vi.fn(() => storedLocale);
  const setItem = vi.fn();
  const reload = vi.fn();

  vi.stubGlobal("window", {
    localStorage: { getItem, setItem },
    location: { reload },
  });
  vi.stubGlobal("document", {
    documentElement: { lang: "" },
    createElement: vi.fn(() => ({})),
  });

  return { getItem, setItem, reload };
};

beforeEach(() => {
  vi.resetModules();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("i18n locale switching", () => {
  it("restores a supported locale and synchronizes html lang", async () => {
    const { getItem } = setupBrowserGlobals("en-US");
    const { default: i18n } = await import("../index");

    expect(getItem).toHaveBeenCalledWith(LOCALE_STORAGE_KEY);
    expect(i18n.global.locale.value).toBe("en-US");
    expect(document.documentElement.lang).toBe("en-US");
  });

  it("switches locale without reloading the page", async () => {
    const { setItem, reload } = setupBrowserGlobals("zh-TW");
    const { default: i18n, setLocale } = await import("../index");

    expect(setLocale("en-US")).toBe("en-US");
    expect(i18n.global.locale.value).toBe("en-US");
    expect(setItem).toHaveBeenCalledWith(LOCALE_STORAGE_KEY, "en-US");
    expect(document.documentElement.lang).toBe("en-US");
    expect(reload).not.toHaveBeenCalled();
  });

  it("falls back to Traditional Chinese for an unsupported locale", async () => {
    const { setItem } = setupBrowserGlobals("en-US");
    const { default: i18n, setLocale } = await import("../index");

    expect(setLocale("fr-FR")).toBe("zh-TW");
    expect(i18n.global.locale.value).toBe("zh-TW");
    expect(setItem).toHaveBeenCalledWith(LOCALE_STORAGE_KEY, "zh-TW");
    expect(document.documentElement.lang).toBe("zh-TW");
  });
});
