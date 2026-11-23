import { z } from "zod";
import type { components } from "@/shared/api";
import type { LoginLabels } from "./labels";

export type LoginValues = components["schemas"]["LoginRequest"];

/** 백엔드 LoginRequest 검증(@NotBlank·@Email)과 같은 규칙 */
export function createLoginSchema(labels: LoginLabels) {
  return z.object({
    email: z
      .string()
      .trim()
      .min(1, { error: labels.emailRequired })
      .pipe(z.email({ error: labels.emailInvalid })),
    password: z.string().min(1, { error: labels.passwordRequired }),
  }) satisfies z.ZodType<LoginValues>;
}
