export const createLocalizedError = (translationKey, cause) => {
  return Object.assign(new Error("Localized application error"), {
    translationKey,
    cause,
  });
};

export const resolveErrorMessage = (error, t, fallbackKey = "errors.unknown") => {
  if (error?.translationKey) {
    return t(error.translationKey);
  }

  if (error?.code) {
    const errorKey = `errors.${error.code}`;
    const translated = t(errorKey);
    if (translated !== errorKey) return translated;
  }

  return t(fallbackKey);
};
