import { describe, expect, it } from "vitest";
import { parseReviewRouteQuery } from "../reviewRouteQuery";

describe("parseReviewRouteQuery", () => {
  it("activates equipment review mode from route query", () => {
    expect(parseReviewRouteQuery({ mode: "equipment" }).activeReviewMode).toBe("equipment");
  });

  it("trims equipment keyword values for the review filter input", () => {
    expect(parseReviewRouteQuery({ equipmentKeyword: "  Projector  " }).equipmentKeyword).toBe("Projector");
  });

  it("maps all equipment status to the empty status filter value", () => {
    expect(parseReviewRouteQuery({ equipmentStatus: "all" }).equipmentStatus).toBe("");
  });

  it("ignores unknown query values", () => {
    expect(parseReviewRouteQuery({ mode: "venue", equipmentStatus: "archived" })).toEqual({
      activeReviewMode: null,
      equipmentKeyword: "",
      equipmentStatus: null,
    });
  });
});
