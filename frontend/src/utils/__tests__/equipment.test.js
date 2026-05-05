import { describe, expect, it } from "vitest";
import {
  buildEquipmentPayload,
  flattenEquipmentGroups,
  getEquipmentStatusMeta,
  normalizeEquipmentBorrowPage,
  normalizeEquipmentGroups,
} from "../equipment";

describe("normalizeEquipmentGroups", () => {
  it("normalizes grouped equipment data", () => {
    expect(
      normalizeEquipmentGroups([
        {
          venueName: "場地 A",
          equipmentList: [
            {
              venueId: "1",
              venueName: "場地 A",
              equipmentId: "5",
              equipmentName: "投影機",
              quantity: "2",
              isInUse: true,
            },
          ],
        },
      ]),
    ).toEqual([
      {
        venueName: "場地 A",
        equipmentList: [
          {
            venueId: 1,
            venueName: "場地 A",
            equipmentId: 5,
            equipmentName: "投影機",
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

describe("flattenEquipmentGroups", () => {
  it("flattens normalized equipment groups", () => {
    const groups = [
      {
        venueName: "A",
        equipmentList: [{ equipmentId: 1, equipmentName: "麥克風" }],
      },
      {
        venueName: "B",
        equipmentList: [{ equipmentId: 2, equipmentName: "HDMI線" }],
      },
    ];

    expect(flattenEquipmentGroups(groups)).toHaveLength(2);
    expect(flattenEquipmentGroups(groups).map((item) => item.equipmentName)).toEqual([
      "麥克風",
      "HDMI線",
    ]);
  });
});

describe("getEquipmentStatusMeta", () => {
  it("maps active status", () => {
    expect(getEquipmentStatusMeta(true)).toEqual({
      text: "使用中",
      className: "is-in-use",
    });
  });

  it("maps idle status", () => {
    expect(getEquipmentStatusMeta(false)).toEqual({
      text: "閒置",
      className: "is-idle",
    });
  });
});

describe("normalizeEquipmentBorrowPage", () => {
  it("normalizes borrow history pagination", () => {
    expect(
      normalizeEquipmentBorrowPage({
        totalCount: "11",
        currentPage: "2",
        pageSize: "5",
        data: [{ equipmentId: "3", equipmentName: "冷氣遙控器" }],
      }),
    ).toEqual({
      totalCount: 11,
      totalPages: 3,
      currentPage: 2,
      pageSize: 5,
      data: [
        {
          venueId: null,
          venueName: "未提供場地",
          equipmentId: 3,
          equipmentName: "冷氣遙控器",
          borrowDate: "",
          timeSlots: "未提供時段",
          purpose: "未填寫用途",
        },
      ],
    });
  });
});

describe("buildEquipmentPayload", () => {
  it("trims equipment name and normalizes numeric fields", () => {
    expect(
      buildEquipmentPayload({
        equipmentName: "  投影機  ",
        venueId: "1",
        quantity: "2",
      }),
    ).toEqual({
      equipmentName: "投影機",
      venueId: 1,
      quantity: 2,
    });
  });

  it("defaults quantity to 1", () => {
    expect(buildEquipmentPayload({ equipmentName: "麥克風", venueId: 1 }).quantity).toBe(1);
  });
});
