import { fetchAllUnits, fetchVenueDetail } from "@/api/venue";
import { useAuthSessionStore } from "@/stores/authSession";

const NOT_FOUND_ROUTE_NAME = "NotFound";

let cachedUnitIds = null;

export const resetRouteValidationCache = () => {
  cachedUnitIds = null;
};

export const isValidRouteParam = (value) => {
  if (typeof value !== "string" || value.trim() === "") {
    return false;
  }

  return /^\d+$/.test(value);
};

export const ensureUnitExists = async (unitId) => {
  if (!cachedUnitIds) {
    const units = await fetchAllUnits();
    cachedUnitIds = new Set(units.map((unit) => String(unit.id)));
  }

  return cachedUnitIds.has(String(unitId));
};

export const ensureVenueExists = async (venueId) => {
  await fetchVenueDetail(venueId);
  return true;
};

const isConsentRoute = (to) => {
  return to.name === "ConsentAgreement" || to.path === "/consent-agreement";
};

const isNotFoundRoute = (to) => {
  return to.name === NOT_FOUND_ROUTE_NAME || to.path === "/404";
};

export const redirectToNotFound = () => {
  return {
    name: NOT_FOUND_ROUTE_NAME,
    replace: true,
  };
};

export const validateRouteAccess = async (to) => {
  if (isNotFoundRoute(to) || isConsentRoute(to)) {
    return true;
  }

  const authSession = useAuthSessionStore();

  try {
    await authSession.ensureCurrentUser();
  } catch (error) {
    console.error("Failed to load current user before route access validation.", error);
    return false;
  }

  if (to.meta?.requiresReviewer && !authSession.isReviewer) {
    return redirectToNotFound();
  }

  if (to.meta?.validateUnit) {
    const unitId = String(to.params.unitId ?? "");

    if (!isValidRouteParam(unitId)) {
      return redirectToNotFound();
    }

    try {
      const unitExists = await ensureUnitExists(unitId);

      if (!unitExists) {
        return redirectToNotFound();
      }
    } catch (error) {
      console.error("Failed to validate unit route parameter.", error);
      return redirectToNotFound();
    }
  }

  if (to.meta?.validateVenue) {
    const venueId = String(to.params.venueId ?? "");

    if (!isValidRouteParam(venueId)) {
      return redirectToNotFound();
    }

    try {
      await ensureVenueExists(venueId);
    } catch (error) {
      console.error("Failed to validate venue route parameter.", error);
      return redirectToNotFound();
    }
  }

  if (!authSession.hasAcceptedConsent) {
    return {
      name: "ConsentAgreement",
      query: { redirect: to.fullPath },
      replace: true,
    };
  }

  return true;
};
