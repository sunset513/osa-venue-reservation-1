import { describe, expect, it } from "vitest";
import {
  countReviewStatuses,
  filterEquipmentReviewList,
  filterVenueReviewList,
  hasActiveReviewFilters,
  matchesReviewDateRange,
} from "../reviewFilters";

describe("matchesReviewDateRange", () => {
  it("returns true when no date filters are active", () => {
    expect(matchesReviewDateRange("2026-07-09", {})).toBe(true);
  });

  it("supports one-sided date ranges", () => {
    expect(matchesReviewDateRange("2026-07-09", { startDate: "2026-07-01" })).toBe(true);
    expect(matchesReviewDateRange("2026-07-09", { endDate: "2026-07-31" })).toBe(true);
    expect(matchesReviewDateRange("2026-06-30", { startDate: "2026-07-01" })).toBe(false);
    expect(matchesReviewDateRange("2026-08-01", { endDate: "2026-07-31" })).toBe(false);
  });

  it("supports bounded date ranges", () => {
    expect(
      matchesReviewDateRange("2026-07-09", {
        startDate: "2026-07-01",
        endDate: "2026-07-31",
      }),
    ).toBe(true);

    expect(
      matchesReviewDateRange("2026-08-01", {
        startDate: "2026-07-01",
        endDate: "2026-07-31",
      }),
    ).toBe(false);
  });
});

describe("filterVenueReviewList", () => {
  const records = [
    {
      id: 12,
      bookingDate: "2026-07-15",
      purpose: "專案討論",
      venueName: "第一會議室",
      contactName: "Alice",
      status: 1,
    },
    {
      id: 88,
      bookingDate: "2026-08-02",
      purpose: "社團培訓",
      venueName: "演講廳",
      contactName: "Bob",
      status: 2,
    },
  ];

  it("matches keyword against purpose, venue name, contact name, and id", () => {
    expect(filterVenueReviewList(records, { keyword: "討論" })).toHaveLength(1);
    expect(filterVenueReviewList(records, { keyword: "演講" })).toHaveLength(1);
    expect(filterVenueReviewList(records, { keyword: "alice" })).toHaveLength(1);
    expect(filterVenueReviewList(records, { keyword: "88" })).toHaveLength(1);
  });

  it("applies bookingDate date ranges", () => {
    expect(
      filterVenueReviewList(records, {
        startDate: "2026-07-01",
        endDate: "2026-07-31",
      }),
    ).toEqual([records[0]]);
  });
});

describe("filterEquipmentReviewList", () => {
  const records = [
    {
      id: 301,
      borrowDate: "2026-07-18",
      itemSummary: "投影機 x 1",
      purpose: "成果發表",
      contact: { name: "Carol" },
      relatedVenueBookingId: 12,
      relatedVenueBookingTitle: "期末發表",
      relatedVenueName: "第一會議室",
      status: 1,
    },
    {
      id: 302,
      borrowDate: "2026-08-01",
      itemSummary: "麥克風 x 2",
      purpose: "迎新活動",
      contact: { name: "David" },
      relatedVenueBookingId: null,
      relatedVenueBookingTitle: null,
      relatedVenueName: "",
      status: 3,
    },
  ];

  it("matches keyword against equipment, purpose, contact, related booking, and ids", () => {
    expect(filterEquipmentReviewList(records, { keyword: "投影機" })).toHaveLength(1);
    expect(filterEquipmentReviewList(records, { keyword: "carol" })).toHaveLength(1);
    expect(filterEquipmentReviewList(records, { keyword: "期末發表" })).toHaveLength(1);
    expect(filterEquipmentReviewList(records, { keyword: "301" })).toHaveLength(1);
    expect(filterEquipmentReviewList(records, { keyword: "12" })).toHaveLength(1);
  });

  it("applies borrowDate date ranges", () => {
    expect(
      filterEquipmentReviewList(records, {
        startDate: "2026-07-01",
        endDate: "2026-07-31",
      }),
    ).toEqual([records[0]]);
  });
});

describe("review filter helpers", () => {
  it("detects whether any review filters are active", () => {
    expect(hasActiveReviewFilters({ keyword: "", startDate: "", endDate: "" })).toBe(false);
    expect(hasActiveReviewFilters({ keyword: "abc", startDate: "", endDate: "" })).toBe(true);
  });

  it("counts review statuses", () => {
    expect(
      countReviewStatuses([
        { status: 1 },
        { status: 1 },
        { status: 2 },
        { status: 3 },
        { status: 0 },
      ]),
    ).toEqual({
      all: 5,
      pending: 2,
      approved: 1,
      rejected: 1,
    });
  });
});
