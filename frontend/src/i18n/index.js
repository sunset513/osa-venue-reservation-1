import { createI18n } from "vue-i18n";
import enUS from "./messages/en-US.json";
import zhTW from "./messages/zh-TW.json";
import {
  DEFAULT_LOCALE,
  normalizeLocale,
  persistLocale,
  readStoredLocale,
  updateDocumentLocale,
} from "./localeManager";

const initialLocale = readStoredLocale();

const i18n = createI18n({
  legacy: false,
  locale: initialLocale,
  fallbackLocale: DEFAULT_LOCALE,
  messages: {
    "zh-TW": zhTW,
    "en-US": enUS,
  },
});

export const setLocale = (locale) => {
  const normalizedLocale = normalizeLocale(locale);
  i18n.global.locale.value = normalizedLocale;
  persistLocale(normalizedLocale);
  updateDocumentLocale(normalizedLocale);
  return normalizedLocale;
};

updateDocumentLocale(initialLocale);

export default i18n;
