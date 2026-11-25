"use client";

import { type FormEvent, type ReactNode, useState } from "react";
import type { components } from "@/shared/api";
import { cn } from "@/shared/lib";
import { Button, FieldMessage, fieldControlClass } from "@/shared/ui";
import type { ContractReviewLabels } from "../model/labels";

type Question = components["schemas"]["QuestionResponse"];

const INPUT_ID = "contract-review-question";
const MAX_LENGTH = 500;

export interface QuestionsSectionProps {
  /** 이전 질문·답변 (오래된 순) */
  questions: Question[];
  onAsk: (question: string) => void;
  /** 답변을 기다리는 중 (최대 20초) */
  isAsking: boolean;
  /** 답변을 기다리는 동안 먼저 보여 줄 질문 */
  pendingQuestion?: string;
  /** 질문을 보내지 못했을 때 입력란 아래 안내 */
  errorMessage?: string;
  onShowSource: (pageNo: number) => void;
  labels: ContractReviewLabels;
}

/** 계약에 대해 질문하기. 추천 질문 칩도 같은 질문으로 보낸다. */
export function QuestionsSection({
  questions,
  onAsk,
  isAsking,
  pendingQuestion,
  errorMessage,
  onShowSource,
  labels,
}: QuestionsSectionProps) {
  const [draft, setDraft] = useState("");
  const trimmed = draft.trim();

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!trimmed || isAsking) return;
    onAsk(trimmed);
    setDraft("");
  };

  return (
    <section
      aria-labelledby="contract-review-questions-title"
      className="flex flex-col gap-4 rounded-[10px] border border-border p-6"
    >
      <h2
        id="contract-review-questions-title"
        className="text-2xl leading-normal font-bold"
      >
        {labels.questionsTitle}
      </h2>
      <p className="text-[15px] text-muted">{labels.questionsHint}</p>

      <div className="flex flex-wrap gap-2">
        {labels.questionSuggestions.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            disabled={isAsking}
            onClick={() => onAsk(suggestion)}
            className="min-h-10 cursor-pointer rounded-[20px] border border-primary-border bg-primary-subtle px-3.5 text-[15px] text-primary disabled:cursor-not-allowed disabled:opacity-50"
          >
            {suggestion}
          </button>
        ))}
      </div>

      {(questions.length > 0 || isAsking) && (
        <ol
          aria-label={labels.questionsList}
          aria-live="polite"
          className="flex flex-col gap-3 rounded-md bg-surface-muted p-4"
        >
          {questions.map((question, index) => (
            <li
              key={question.questionId ?? index}
              className="flex flex-col gap-3"
            >
              <QuestionBubble lang={question.language}>
                {question.question}
              </QuestionBubble>
              <Answer
                question={question}
                onShowSource={onShowSource}
                labels={labels}
              />
            </li>
          ))}
          {isAsking && (
            <li className="flex flex-col gap-3">
              {pendingQuestion && (
                <QuestionBubble>{pendingQuestion}</QuestionBubble>
              )}
              <div
                role="status"
                className="flex items-center gap-3 rounded-md border border-border bg-surface px-4 py-3.5 text-muted"
              >
                <span
                  aria-hidden="true"
                  className="size-5 shrink-0 animate-spin rounded-full border-3 border-divider border-t-primary"
                />
                {labels.answering}
              </div>
            </li>
          )}
        </ol>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-2">
        <div className="flex gap-3">
          <label htmlFor={INPUT_ID} className="sr-only">
            {labels.questionInput}
          </label>
          <input
            id={INPUT_ID}
            type="text"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            maxLength={MAX_LENGTH}
            placeholder={labels.questionPlaceholder}
            disabled={isAsking}
            aria-invalid={errorMessage ? true : undefined}
            aria-describedby={errorMessage ? `${INPUT_ID}-message` : undefined}
            className={cn(
              fieldControlClass(errorMessage ? "error" : undefined),
              "h-12 min-w-0 flex-1 text-[17px]",
            )}
          />
          <Button
            type="submit"
            size="md"
            disabled={!trimmed || isAsking}
            className="shrink-0"
          >
            {labels.ask}
          </Button>
        </div>
        {errorMessage && (
          <FieldMessage id={`${INPUT_ID}-message`}>{errorMessage}</FieldMessage>
        )}
      </form>
    </section>
  );
}

function QuestionBubble({
  lang,
  children,
}: {
  lang?: string;
  children: ReactNode;
}) {
  return (
    <p
      lang={lang}
      className="max-w-[85%] self-end rounded-md border border-border bg-surface px-3.5 py-2.5"
    >
      {children}
    </p>
  );
}

function Answer({
  question,
  onShowSource,
  labels,
}: {
  question: Question;
  onShowSource: (pageNo: number) => void;
  labels: ContractReviewLabels;
}) {
  if (!question.answer) {
    return (
      <p className="rounded-md border border-border bg-surface px-4 py-3.5 text-muted">
        {labels.answerFailed}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-2 rounded-md border border-border bg-surface px-4 py-3.5">
      <p lang={question.language}>{question.answer}</p>
      {question.citations?.map((citation, index) => (
        <blockquote
          key={citation.clauseId ?? index}
          className="mt-1 flex flex-wrap items-baseline gap-x-2 gap-y-1 border-l-3 border-border-strong bg-surface-muted px-4 py-3 text-[15px]"
        >
          <span lang="ko">&ldquo;{citation.quotedText}&rdquo;</span>
          {citation.pageNo != null && (
            <button
              type="button"
              onClick={() => onShowSource(citation.pageNo ?? 1)}
              className="cursor-pointer text-sm text-primary underline"
            >
              {labels.showSource(citation.pageNo)}
            </button>
          )}
        </blockquote>
      ))}
      {question.referredToCenter && (
        <p className="text-sm text-muted">{labels.centerNotice}</p>
      )}
    </div>
  );
}
