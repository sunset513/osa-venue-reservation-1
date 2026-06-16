import { defineStore } from "pinia";
import { fetchCurrentUser } from "@/api/auth";

const CONSENT_STORAGE_PREFIX = "venue:consent:";
const ACCEPTED_VALUE = "accepted";
const CONSENT_ROUTE = "/consent-agreement";
const REVIEW_ROUTE = "/review";
const USER_DEFAULT_ROUTE = "/";

const canUseSessionStorage = () => {
  return typeof window !== "undefined" && typeof window.sessionStorage !== "undefined";
};

const isSafeInternalPath = (path) => {
  return typeof path === "string" && path.startsWith("/") && !path.startsWith("//");
};

export const useAuthSessionStore = defineStore("authSession", {
  state: () => ({
    currentUser: null,
    hasLoadedUser: false,
    isLoadingUser: false,
    hasAcceptedConsent: false,
    loadUserPromise: null,
  }),

  getters: {
    userIdentifier: (state) => state.currentUser?.identifier ?? null,
    isReviewer: (state) => state.currentUser?.isReviewer === true,
    consentStorageKey() {
      return this.userIdentifier ? `${CONSENT_STORAGE_PREFIX}${this.userIdentifier}` : null;
    },
  },

  actions: {
    async ensureCurrentUser() {
      if (this.hasLoadedUser && this.currentUser) {
        this.refreshConsentState();
        return this.currentUser;
      }

      if (this.loadUserPromise) {
        return this.loadUserPromise;
      }

      this.isLoadingUser = true;
      this.loadUserPromise = fetchCurrentUser()
        .then((user) => {
          this.currentUser = user;
          this.hasLoadedUser = true;
          this.refreshConsentState();
          return user;
        })
        .catch((error) => {
          this.clearSessionState();
          throw error;
        })
        .finally(() => {
          this.isLoadingUser = false;
          this.loadUserPromise = null;
        });

      return this.loadUserPromise;
    },

    refreshConsentState() {
      if (!this.consentStorageKey || !canUseSessionStorage()) {
        this.hasAcceptedConsent = false;
        return;
      }

      this.hasAcceptedConsent =
        window.sessionStorage.getItem(this.consentStorageKey) === ACCEPTED_VALUE;
    },

    acceptConsentForCurrentUser() {
      if (!this.consentStorageKey) {
        return;
      }

      if (canUseSessionStorage()) {
        window.sessionStorage.setItem(this.consentStorageKey, ACCEPTED_VALUE);
      }

      this.hasAcceptedConsent = true;
    },

    getPostConsentRoute() {
      if (this.isReviewer) {
        return REVIEW_ROUTE;
      }

      const defaultRoute = this.currentUser?.defaultRoute;
      if (!isSafeInternalPath(defaultRoute) || defaultRoute.startsWith(CONSENT_ROUTE)) {
        return USER_DEFAULT_ROUTE;
      }

      return defaultRoute;
    },

    clearSessionState() {
      this.currentUser = null;
      this.hasLoadedUser = false;
      this.isLoadingUser = false;
      this.hasAcceptedConsent = false;
      this.loadUserPromise = null;
    },
  },
});
