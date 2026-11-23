export type UiLanguage = "ko" | "vi";

export const DEFAULT_UI_LANGUAGE: UiLanguage = "ko";

export function resolveUiLanguage(value: unknown): UiLanguage {
  return value === "vi" ? "vi" : DEFAULT_UI_LANGUAGE;
}
