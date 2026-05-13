import { beforeEach, describe, expect, it } from "vitest";
import { acceptConsent, hasAcceptedConsent, resetConsentForTests } from "../consentGate";

describe("consentGate", () => {
  beforeEach(() => {
    resetConsentForTests();
  });

  it("starts without accepted consent", () => {
    expect(hasAcceptedConsent()).toBe(false);
  });

  it("marks consent as accepted", () => {
    acceptConsent();

    expect(hasAcceptedConsent()).toBe(true);
  });

  it("resets consent for isolated tests", () => {
    acceptConsent();
    resetConsentForTests();

    expect(hasAcceptedConsent()).toBe(false);
  });
});
