import { describe, expect, it } from "vitest";
import enUS from "../messages/en-US.json";
import zhTW from "../messages/zh-TW.json";

const flattenMessages = (messages, prefix = "") => {
  return Object.entries(messages).reduce((result, [key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key;

    if (value && typeof value === "object" && !Array.isArray(value)) {
      Object.assign(result, flattenMessages(value, path));
    } else {
      result[path] = value;
    }

    return result;
  }, {});
};

const getInterpolationParameters = (message) => {
  if (typeof message !== "string") return [];

  return [...message.matchAll(/\{([^{}]+)\}/g)]
    .map((match) => match[1])
    .sort();
};

describe("locale messages", () => {
  const flattenedZhTW = flattenMessages(zhTW);
  const flattenedEnUS = flattenMessages(enUS);

  it("keeps the same translation keys in every locale", () => {
    expect(Object.keys(flattenedEnUS).sort()).toEqual(
      Object.keys(flattenedZhTW).sort(),
    );
  });

  it.each([
    ["zh-TW", flattenedZhTW],
    ["en-US", flattenedEnUS],
  ])("contains only non-empty string leaves for %s", (_locale, messages) => {
    for (const [key, value] of Object.entries(messages)) {
      expect(value, `${key} must be a string`).toEqual(expect.any(String));
      expect(value.trim(), `${key} must not be empty`).not.toBe("");
    }
  });

  it("keeps interpolation parameters consistent between locales", () => {
    for (const key of Object.keys(flattenedZhTW)) {
      expect(
        getInterpolationParameters(flattenedEnUS[key]),
        `${key} must use the same interpolation parameters`,
      ).toEqual(getInterpolationParameters(flattenedZhTW[key]));
    }
  });
});
