export const DEFAULT_LOCALE = "zh-TW";
export const SUPPORTED_LOCALES = ["zh-TW", "en-US"];
export const LOCALE_STORAGE_KEY = "osa-venue-locale";

export const normalizeLocale = (locale) => {
  return SUPPORTED_LOCALES.includes(locale) ? locale : DEFAULT_LOCALE;
};

export const readStoredLocale = () => {
  if (typeof window === "undefined") return DEFAULT_LOCALE;

  try {
    return normalizeLocale(window.localStorage.getItem(LOCALE_STORAGE_KEY));
  } catch {
    return DEFAULT_LOCALE;
  }
};

export const persistLocale = (locale) => {
  const normalizedLocale = normalizeLocale(locale);

  if (typeof window !== "undefined") {
    try {
      window.localStorage.setItem(LOCALE_STORAGE_KEY, normalizedLocale);
    } catch {
      // Storage can be unavailable in privacy-restricted browsing contexts.
    }
  }

  return normalizedLocale;
};

export const updateDocumentLocale = (locale) => {
  const normalizedLocale = normalizeLocale(locale);

  if (typeof document !== "undefined") {
    document.documentElement.lang = normalizedLocale;
  }

  return normalizedLocale;
};
