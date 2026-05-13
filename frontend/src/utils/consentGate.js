let acceptedConsent = false;

export const hasAcceptedConsent = () => acceptedConsent;

export const acceptConsent = () => {
  acceptedConsent = true;
};

export const resetConsentForTests = () => {
  acceptedConsent = false;
};
