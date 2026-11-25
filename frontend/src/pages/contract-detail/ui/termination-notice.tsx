import type { components } from "@/shared/api";
import { useCurrentLanguage } from "@/shared/i18n";
import { formatDate } from "@/shared/lib";
import { Alert, Button, FieldMessage } from "@/shared/ui";
import type { ContractDetailLabels } from "../model/labels";

type Detail = components["schemas"]["ContractDetailResponse"];
type Termination = NonNullable<Detail["termination"]>;

export type TerminationAction = "agree" | "decline" | "cancel";

export interface TerminationNoticeProps {
  termination: Termination;
  availableActions: NonNullable<Detail["availableActions"]>;
  /** 상대방 이름 (고용주에게는 근로자, 근로자에게는 사업체) */
  counterparty?: string;
  /** [동의] — 확인 창을 연다 (M5 AGREE는 창에서) */
  onAgree: () => void;
  /** [거절] (M5 DECLINE) */
  onDecline: () => void;
  /** [요청 취소] (M6) */
  onCancel: () => void;
  /** 처리 중인 일 (버튼을 잠근다) */
  pending?: TerminationAction;
  errorMessage?: string;
  labels: ContractDetailLabels;
}

/** 진행 중(REQUESTED)이거나 합의된(AGREED) 종료 안내 */
export function TerminationNotice({
  termination,
  availableActions,
  counterparty,
  onAgree,
  onDecline,
  onCancel,
  pending,
  errorMessage,
  labels,
}: TerminationNoticeProps) {
  const language = useCurrentLanguage();
  const t = labels.termination;
  const endDate =
    termination.endDate && formatDate(termination.endDate, language);
  const agreed = termination.status === "AGREED";
  const byMe = termination.requestedByMe === true;
  const canRespond = availableActions.includes("RESPOND_TERMINATION");
  const canCancel = availableActions.includes("CANCEL_TERMINATION");

  const title = agreed
    ? t.agreed
    : byMe
      ? t.requestedByMe
      : counterparty
        ? t.requestedByOther(counterparty)
        : t.requestedByOtherFallback;
  const hint = agreed
    ? endDate && t.agreedHint(endDate)
    : byMe
      ? t.requestedByMeHint
      : t.requestedByOtherHint;

  return (
    <Alert
      tone={agreed ? "info" : byMe ? "info" : "warning"}
      accent="top"
      title={title}
      className="mb-8"
      action={
        (canRespond || canCancel) && (
          <div className="flex flex-wrap gap-3">
            {canRespond && (
              <>
                <Button
                  variant="secondary"
                  size="md"
                  disabled={pending !== undefined}
                  onClick={onDecline}
                >
                  {pending === "decline" ? t.responding : t.decline}
                </Button>
                <Button
                  size="md"
                  disabled={pending !== undefined}
                  onClick={onAgree}
                >
                  {t.agree}
                </Button>
              </>
            )}
            {canCancel && (
              <Button
                variant="secondary"
                size="md"
                disabled={pending !== undefined}
                onClick={onCancel}
              >
                {pending === "cancel" ? t.responding : t.cancel}
              </Button>
            )}
          </div>
        )
      }
    >
      <dl className="mt-1 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-base">
        <dt className="text-muted">{t.lastWorkDay}</dt>
        <dd className="font-bold">{endDate}</dd>
        <dt className="text-muted">{t.reason}</dt>
        <dd>{termination.reasonText || t.noReason}</dd>
      </dl>
      {hint && <p className="mt-2">{hint}</p>}
      {errorMessage && (
        <div className="mt-2">
          <FieldMessage>{errorMessage}</FieldMessage>
        </div>
      )}
    </Alert>
  );
}
