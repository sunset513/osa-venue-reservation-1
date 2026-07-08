import { describe, expect, it } from "vitest";
import {
  buildEquipmentBookingUpdatePayload,
  buildEquipmentPayload,
  canEditEquipmentBookingOnHistoryPage,
  flattenEquipmentGroups,
  getEquipmentBookingEditTarget,
  getEquipmentStatusMeta,
  normalizeEquipmentBorrowPage,
  normalizeEquipmentGroups,
} from "../equipment";

describe("normalizeEquipmentGroups", () => {
  it("normalizes grouped equipment data", () => {
    expect(
      normalizeEquipmentGroups([
        {
          venueName: "Venue A",
          equipmentList: [
            {
              venueId: "1",
              venueName: "Venue A",
              equipmentId: "5",
              equipmentName: "Projector",
              quantity: "2",
              isInUse: true,
            },
          ],
        },
      ]),
    ).toEqual([
      {
        venueName: "Venue A",
        equipmentList: [
          {
            venueId: 1,
            venueName: "Venue A",
            equipmentId: 5,
            equipmentName: "Projector",
            quantity: 2,
            isInUse: true,
          },
        ],
      },
    ]);
  });

  it("returns an empty array for invalid input", () => {
    expect(normalizeEquipmentGroups(null)).toEqual([]);
  });
});

describe("getEquipmentBookingEditTarget", () => {
  it("allows pending standalone equipment bookings to edit on the history page", () => {
    const record = { status: 1, relatedVenueBookingId: null };

    expect(getEquipmentBookingEditTarget(record)).toBe("equipment");
    expect(canEditEquipmentBookingOnHistoryPage(record)).toBe(true);
  });

  it("routes pending venue-linked equipment bookings to venue history", () => {
    const record = { status: 1, relatedVenueBookingId: 32 };

    expect(getEquipmentBookingEditTarget(record)).toBe("venue");
    expect(canEditEquipmentBookingOnHistoryPage(record)).toBe(false);
  });

  it.each([0, 2, 3])("does not allow status %s to edit", (status) => {
    expect(getEquipmentBookingEditTarget({ status })).toBeNull();
  });
});

describe("buildEquipmentBookingUpdatePayload", () => {
  it("builds the backend update payload from editable form data", () => {
    expect(
      buildEquipmentBookingUpdatePayload({
        borrowDate: "2026-08-12",
        slots: [3, "1", 1, 24, -1, "bad"],
        purpose: "  meeting  ",
        contactInfo: {
          name: "  Alex  ",
          phone: "  0912345678 ",
          email: " alex@example.com ",
        },
        relatedVenueBookingId: "",
        equipmentItems: [
          { equipmentId: "5", quantity: "2" },
          { equipmentId: null, quantity: 1 },
          { equipmentId: 8, quantity: 0 },
        ],
      }),
    ).toEqual({
      borrowDate: "2026-08-12",
      slots: [1, 3],
      purpose: "meeting",
      contactInfo: {
        name: "Alex",
        phone: "0912345678",
        email: "alex@example.com",
      },
      relatedVenueBookingId: null,
      items: [{ equipmentId: 5, quantity: 2 }],
    });
  });
});

describe("flattenEquipmentGroups", () => {
  it("flattens normalized equipment groups", () => {
    const groups = [
      {
        venueName: "A",
        equipmentList: [{ equipmentId: 1, equipmentName: "Projector" }],
      },
      {
        venueName: "B",
        equipmentList: [{ equipmentId: 2, equipmentName: "HDMI Cable" }],
      },
    ];

    expect(flattenEquipmentGroups(groups)).toHaveLength(2);
    expect(flattenEquipmentGroups(groups).map((item) => item.equipmentName)).toEqual([
      "Projector",
      "HDMI Cable",
    ]);
  });
});

describe("getEquipmentStatusMeta", () => {
  it("maps active status", () => {
    expect(getEquipmentStatusMeta(true)).toMatchObject({
      className: "is-in-use",
    });
  });

  it("maps idle status", () => {
    expect(getEquipmentStatusMeta(false)).toMatchObject({
      className: "is-idle",
    });
  });
});

describe("normalizeEquipmentBorrowPage", () => {
  it("normalizes borrow history pagination", () => {
    const page = normalizeEquipmentBorrowPage({
      totalCount: "11",
      currentPage: "2",
      pageSize: "5",
      data: [
        {
          id: "9",
          borrowDate: "2026-08-12",
          slots: [1, 2],
          status: "1",
          purpose: "meeting",
          contactInfo: JSON.stringify({ name: "Alex", phone: "0912345678" }),
          relatedVenueBookingId: "33",
          items: [{ equipmentId: "3", equipmentName: "Projector", quantity: "2" }],
        },
      ],
    });

    expect(page).toMatchObject({
      total: 11,
      totalCount: 11,
      totalPages: 3,
      pageNo: 2,
      currentPage: 2,
      pageSize: 5,
      hasNext: false,
    });
    expect(page.items).toEqual(page.data);
    expect(page.items[0]).toMatchObject({
      id: 9,
      borrowDate: "2026-08-12",
      slots: [1, 2],
      status: 1,
      purpose: "meeting",
      relatedVenueBookingId: 33,
      contact: {
        name: "Alex",
        phone: "0912345678",
      },
      items: [{ id: null, equipmentId: 3, equipmentName: "Projector", quantity: 2 }],
      itemSummary: "Projector x 2",
    });
  });
});

describe("buildEquipmentPayload", () => {
  it("trims equipment name and normalizes numeric fields", () => {
    expect(
      buildEquipmentPayload({
        equipmentName: "  Projector ",
        venueId: "1",
        quantity: "2",
      }),
    ).toEqual({
      equipmentName: "Projector",
      venueId: 1,
      quantity: 2,
    });
  });

  it("defaults quantity to 1", () => {
    expect(buildEquipmentPayload({ equipmentName: "Projector", venueId: 1 }).quantity).toBe(1);
  });
});
