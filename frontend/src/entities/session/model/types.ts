import type { components } from "@/shared/api";

type Member = components["schemas"]["MemberResponse"];

export type MemberRole = NonNullable<Member["role"]>;
export type SessionMember = Member & { name: string; role: MemberRole };
