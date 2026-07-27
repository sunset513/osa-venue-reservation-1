import { afterEach, describe, expect, it, vi } from "vitest";
import {
  DEFAULT_LOCALE,
  LOCALE_STORAGE_KEY,
  SUPPORTED_LOCALES,
  normalizeLocale,
  persistLocale,
  readStoredLocale,
  updateDocumentLocale,
} from "../localeManager";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("locale constants", () => {
  it("uses Traditional Chinese as the default and supports English", () => {
    expect(DEFAULT_LOCALE).toBe("zh-TW");
    expect(SUPPORTED_LOCALES).toEqual(["zh-TW", "en-US"]);
    expect(LOCALE_STORAGE_KEY).toBe("osa-venue-locale");
  });
});

describe("normalizeLocale", () => {
  it.each(["zh-TW", "en-US"])("keeps supported locale %s", (locale) => {
    expect(normalizeLocale(locale)).toBe(locale);
  });

  it.each([undefined, null, "", "en", "zh-CN", "invalid"])(
    "falls back for unsupported locale %s",
    (locale) => {
      expect(normalizeLocale(locale)).toBe(DEFAULT_LOCALE);
    },
  );
});

describe("readStoredLocale", () => {
  it("returns the stored supported locale", () => {
    vi.stubGlobal("window", {
      localStorage: {
        getItem: vi.fn(() => "en-US"),
      },
    });

    expect(readStoredLocale()).toBe("en-US");
    expect(window.localStorage.getItem).toHaveBeenCalledWith(LOCALE_STORAGE_KEY);
  });

  it("falls back when the stored locale is invalid", () => {
    vi.stubGlobal("window", {
      localStorage: {
        getItem: vi.fn(() => "fr-FR"),
      },
    });

    expect(readStoredLocale()).toBe(DEFAULT_LOCALE);
  });

  it("falls back when storage is unavailable", () => {
    vi.stubGlobal("window", {
      localStorage: {
        getItem: vi.fn(() => {
          throw new Error("storage unavailable");
        }),
      },
    });

    expect(readStoredLocale()).toBe(DEFAULT_LOCALE);
  });
});

describe("persistLocale", () => {
  it("normalizes and persists the selected locale", () => {
    const setItem = vi.fn();
    vi.stubGlobal("window", { localStorage: { setItem } });

    expect(persistLocale("en-US")).toBe("en-US");
    expect(setItem).toHaveBeenCalledWith(LOCALE_STORAGE_KEY, "en-US");
  });

  it("persists the fallback for an unsupported locale", () => {
    const setItem = vi.fn();
    vi.stubGlobal("window", { localStorage: { setItem } });

    expect(persistLocale("fr-FR")).toBe(DEFAULT_LOCALE);
    expect(setItem).toHaveBeenCalledWith(LOCALE_STORAGE_KEY, DEFAULT_LOCALE);
  });

  it("still returns the normalized locale when storage rejects writes", () => {
    vi.stubGlobal("window", {
      localStorage: {
        setItem: vi.fn(() => {
          throw new Error("storage unavailable");
        }),
      },
    });

    expect(persistLocale("en-US")).toBe("en-US");
  });
});

describe("updateDocumentLocale", () => {
  it("updates the html lang attribute", () => {
    vi.stubGlobal("document", { documentElement: { lang: "" } });

    expect(updateDocumentLocale("en-US")).toBe("en-US");
    expect(document.documentElement.lang).toBe("en-US");
  });

  it("uses the fallback for an unsupported locale", () => {
    vi.stubGlobal("document", { documentElement: { lang: "" } });

    expect(updateDocumentLocale("fr-FR")).toBe(DEFAULT_LOCALE);
    expect(document.documentElement.lang).toBe(DEFAULT_LOCALE);
  });
});
