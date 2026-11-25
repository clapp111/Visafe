import { useCurrentLanguage } from "@/shared/i18n";
import { formatDate } from "@/shared/lib";
import { Button, Dialog, FieldMessage } from "@/shared/ui";
import type { ContractDetailLabels } from "../model/labels";

/** 종료 동의 확인 창. 되돌릴 수 없으므로 한 번 더 묻는다 */
export function TerminationAgreeDialog({
  endDate,
  onClose,
  onConfirm,
  isSubmitting,
  errorMessage,
  labels,
}: {
  /** 마지막 근무일 (YYYY-MM-DD) */
  endDate?: string;
  onClose: () => void;
  /** [동의] (M5 AGREE) */
  onConfirm: () => void;
  isSubmitting?: boolean;
  errorMessage?: string;
  labels: ContractDetailLabels;
}) {
  const language = useCurrentLanguage();
  const t = labels.termination;
  return (
    <Dialog
      open
      onClose={onClose}
      title={t.agreeTitle}
      description={endDate && t.agreeDescription(formatDate(endDate, language))}
      actions={
        <>
          <Button variant="secondary" size="md" onClick={onClose}>
            {t.close}
          </Button>
          <Button size="md" disabled={isSubmitting} onClick={onConfirm}>
            {isSubmitting ? t.responding : t.agreeConfirm}
          </Button>
        </>
      }
    >
      {errorMessage && <FieldMessage>{errorMessage}</FieldMessage>}
    </Dialog>
  );
}
