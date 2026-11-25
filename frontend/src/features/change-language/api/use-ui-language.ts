"use client";

import { useMutation } from "@tanstack/react-query";
import { api, getApiErrorCode, type components } from "@/shared/api";
import {
  messages,
  resolveUiLanguage,
  useGuestLanguage,
  type UiLanguage,
} from "@/shared/i18n";

type Member = components["schemas"]["MemberResponse"];
type LanguageContext =
  | { mode: "guest" }
  | {
      mode: "member";
      member: Member | undefined;
      onMemberUpdated: (member: Member) => void;
    };

async function updateUiLanguage({
  uiLanguage,
}: {
  memberId: number;
  uiLanguage: UiLanguage;
}): Promise<Member> {
  const request: components["schemas"]["MemberUpdateRequest"] = { uiLanguage };
  const { data } = await api.patch<Member>("/members/me", request);
  return data;
}

export function useUiLanguage(context: LanguageContext) {
  const guest = useGuestLanguage();
  const mutation = useMutation({ mutationFn: updateUiLanguage });

  // 세션을 복원하는 동안(회원 정보 전)에는 이 브라우저에 저장한 언어를 먼저 보인다
  const language =
    context.mode === "member" && context.member
      ? resolveUiLanguage(context.member.uiLanguage)
      : guest.language;
  const memberId = context.mode === "member" ? context.member?.id : undefined;
  const isCurrentMember =
    memberId !== undefined && mutation.variables?.memberId === memberId;
  const isChangingLanguage = isCurrentMember && mutation.isPending;
  const canChangeLanguage =
    context.mode === "guest" || (memberId !== undefined && !isChangingLanguage);
  const error = isCurrentMember ? mutation.error : null;

  function changeLanguage(next: UiLanguage) {
    if (!canChangeLanguage || next === language) {
      return;
    }

    if (context.mode === "guest") {
      guest.setGuestLanguage(next);
    } else if (memberId !== undefined) {
      mutation.mutate(
        { memberId, uiLanguage: next },
        { onSuccess: context.onMemberUpdated },
      );
    }
  }

  return {
    language,
    messages: messages[language],
    changeLanguage,
    canChangeLanguage,
    isChangingLanguage,
    hasLanguageChangeError: error !== null,
    errorCode: getApiErrorCode(error),
    dismissLanguageError: mutation.reset,
  };
}
