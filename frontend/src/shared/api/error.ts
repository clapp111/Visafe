import { isAxiosError } from "axios";
import type { components } from "./schema";

type ErrorResponse = components["schemas"]["ErrorResponse"];

export function getApiErrorCode(error: unknown): string | undefined {
  if (!isAxiosError<ErrorResponse>(error)) {
    return undefined;
  }

  const errorCode = error.response?.data?.errorCode;
  return typeof errorCode === "string" ? errorCode : undefined;
}
