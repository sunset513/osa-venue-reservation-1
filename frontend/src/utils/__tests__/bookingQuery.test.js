import { describe, expect, it } from "vitest";
import { buildBookingQueryPayload, normalizeBookingPage } from "../bookingQuery";

describe("buildBookingQueryPayload", () => {
  it("builds default query payload", () => {
    expect(buildBookingQueryPayload()).toEqual({
      venueId: null,
      statusList: null,
      startDate: null,
      endDate: null,
      pageNo: 1,
      pageSize: 20,
    });
  });

  it("normalizes venue, status list, dates, and pagination", () => {
    expect(
      buildBookingQueryPayload({
        venueId: "2",
        statusList: ["1", 2, ""],
        startDate: "2026-05-01",
        endDate: "2026-05-31",
        pageNo: "3",
        pageSize: "50",
      }),
    ).toEqual({
      venueId: 2,
      statusList: [1, 2],
      startDate: "2026-05-01",
      endDate: "2026-05-31",
      pageNo: 3,
      pageSize: 50,
    });
  });

  it("supports a single status field", () => {
    expect(buildBookingQueryPayload({ status: "3" }).statusList).toEqual([3]);
  });
});

describe("normalizeBookingPage", () => {
  it("normalizes booking page defaults", () => {
    expect(normalizeBookingPage()).toEqual({
      total: 0,
      pageNo: 1,
      pageSize: 20,
      totalPages: 0,
      hasNext: false,
      items: [],
    });
  });

  it("calculates pagination fields when missing", () => {
    expect(
      normalizeBookingPage({
        total: "45",
        pageNo: "2",
        pageSize: "20",
        items: [{ id: 1 }],
      }),
    ).toEqual({
      total: 45,
      pageNo: 2,
      pageSize: 20,
      totalPages: 3,
      hasNext: true,
      items: [{ id: 1 }],
    });
  });
});
