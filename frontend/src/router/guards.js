import { fetchAllUnits, fetchVenueDetail } from "@/api/venue";
import { warning } from "@/utils/useToast";

let cachedUnitIds = null;

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

const getRedirectPath = (to) => {
  return typeof to.meta.redirectOnInvalid === "string" ? to.meta.redirectOnInvalid : "/";
};

const redirectToSafePage = (to) => {
  return {
    path: getRedirectPath(to),
    replace: true,
  };
};

const notifyInvalidRoute = (message) => {
  warning(message);
};

export const validateRouteAccess = async (to) => {
  if (to.meta.validateUnit) {
    const unitId = String(to.params.unitId ?? "");

    if (!isValidRouteParam(unitId)) {
      notifyInvalidRoute("單位網址格式不正確，已導回首頁");
      return redirectToSafePage(to);
    }

    try {
      const unitExists = await ensureUnitExists(unitId);

      if (!unitExists) {
        notifyInvalidRoute("找不到指定的管理單位，已導回首頁");
        return redirectToSafePage(to);
      }
    } catch (error) {
      console.error("驗證單位路由失敗:", error);
      notifyInvalidRoute("目前無法驗證單位網址，已導回首頁");
      return redirectToSafePage(to);
    }
  }

  if (to.meta.validateVenue) {
    const venueId = String(to.params.venueId ?? "");

    if (!isValidRouteParam(venueId)) {
      notifyInvalidRoute("場地網址格式不正確，已導回首頁");
      return redirectToSafePage(to);
    }

    try {
      await ensureVenueExists(venueId);
    } catch (error) {
      console.error("驗證場地路由失敗:", error);
      notifyInvalidRoute("找不到指定場地或目前無法驗證，已導回首頁");
      return redirectToSafePage(to);
    }
  }

  return true;
};
