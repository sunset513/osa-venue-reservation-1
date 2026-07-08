import { beforeEach, describe, expect, it, vi } from "vitest";

const mockFetchAllUnits = vi.fn();
const mockFetchVenueDetail = vi.fn();
const mockStore = {
  ensureCurrentUser: vi.fn(),
  hasAcceptedConsent: true,
  isReviewer: false,
};

vi.mock("@/api/venue", () => ({
  fetchAllUnits: (...args) => mockFetchAllUnits(...args),
  fetchVenueDetail: (...args) => mockFetchVenueDetail(...args),
}));

vi.mock("@/stores/authSession", () => ({
  useAuthSessionStore: () => mockStore,
}));

const createRoute = (overrides = {}) => ({
  name: "Home",
  path: "/",
  fullPath: "/",
  meta: {},
  params: {},
  ...overrides,
});

describe("validateRouteAccess", () => {
  let redirectToNotFound;
  let resetRouteValidationCache;
  let validateRouteAccess;

  beforeEach(async () => {
    vi.resetModules();

    mockFetchAllUnits.mockReset();
    mockFetchVenueDetail.mockReset();
    mockStore.ensureCurrentUser = vi.fn().mockResolvedValue({ id: 1 });
    mockStore.hasAcceptedConsent = true;
    mockStore.isReviewer = false;

    ({ redirectToNotFound, resetRouteValidationCache, validateRouteAccess } = await import("../guards"));
    resetRouteValidationCache();
  });

  it("allows the canonical 404 route without requiring consent", async () => {
    mockStore.hasAcceptedConsent = false;

    const result = await validateRouteAccess(
      createRoute({
        name: "NotFound",
        path: "/404",
        fullPath: "/404",
      }),
    );

    expect(result).toBe(true);
    expect(mockStore.ensureCurrentUser).not.toHaveBeenCalled();
  });

  it("redirects reviewer-only routes to 404 for non-reviewers before consent checks", async () => {
    mockStore.hasAcceptedConsent = false;

    const result = await validateRouteAccess(
      createRoute({
        name: "ReviewCalendar",
        path: "/review",
        fullPath: "/review",
        meta: { requiresReviewer: true },
      }),
    );

    expect(result).toEqual(redirectToNotFound());
  });

  it("redirects equipment history to 404 for non-reviewers", async () => {
    const result = await validateRouteAccess(
      createRoute({
        name: "EquipmentBorrowHistory",
        path: "/equipment-history",
        fullPath: "/equipment-history",
        meta: { requiresReviewer: true },
      }),
    );

    expect(result).toEqual(redirectToNotFound());
  });

  it("redirects equipment status management to 404 for non-reviewers", async () => {
    const result = await validateRouteAccess(
      createRoute({
        name: "EquipmentStatus",
        path: "/equipment-status",
        fullPath: "/equipment-status",
        meta: { requiresReviewer: true },
      }),
    );

    expect(result).toEqual(redirectToNotFound());
  });

  it("redirects invalid unit params to 404", async () => {
    mockStore.hasAcceptedConsent = false;

    const result = await validateRouteAccess(
      createRoute({
        name: "VenueSelector",
        path: "/unit/abc",
        fullPath: "/unit/abc",
        meta: { validateUnit: true },
        params: { unitId: "abc" },
      }),
    );

    expect(result).toEqual(redirectToNotFound());
    expect(mockFetchAllUnits).not.toHaveBeenCalled();
  });

  it("redirects unknown unit ids to 404", async () => {
    mockFetchAllUnits.mockResolvedValue([{ id: 1 }, { id: 2 }]);

    const result = await validateRouteAccess(
      createRoute({
        name: "VenueSelector",
        path: "/unit/999999",
        fullPath: "/unit/999999",
        meta: { validateUnit: true },
        params: { unitId: "999999" },
      }),
    );

    expect(result).toEqual(redirectToNotFound());
  });

  it("redirects invalid venue params such as '/venue/review' to 404", async () => {
    const result = await validateRouteAccess(
      createRoute({
        name: "VenueCalendar",
        path: "/venue/review",
        fullPath: "/venue/review",
        meta: { validateVenue: true },
        params: { venueId: "review" },
      }),
    );

    expect(result).toEqual(redirectToNotFound());
    expect(mockFetchVenueDetail).not.toHaveBeenCalled();
  });

  it("redirects missing venue records to 404", async () => {
    mockFetchVenueDetail.mockRejectedValue(new Error("not found"));

    const result = await validateRouteAccess(
      createRoute({
        name: "VenueCalendar",
        path: "/venue/999999",
        fullPath: "/venue/999999",
        meta: { validateVenue: true },
        params: { venueId: "999999" },
      }),
    );

    expect(result).toEqual(redirectToNotFound());
  });

  it("redirects valid routes to the consent page only after route checks pass", async () => {
    mockStore.hasAcceptedConsent = false;

    const result = await validateRouteAccess(
      createRoute({
        name: "MyBookingHistory",
        path: "/my-bookings",
        fullPath: "/my-bookings",
      }),
    );

    expect(result).toEqual({
      name: "ConsentAgreement",
      query: { redirect: "/my-bookings" },
      replace: true,
    });
  });
});
