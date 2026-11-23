package com.visafe.infra.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.visafe.domain.contract.extraction.ContractExtractor;
import com.visafe.domain.contract.extraction.ExtractionResult;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.config.LlmCondition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Claude가 원본 PDF·이미지를 직접 읽어 조항과 19개 항목을 추출한다. (OCR 없음)
 * <p>
 * 정확도가 가장 중요해 effort는 high다. 응답은 구조화 출력으로 받아 {@link ExtractionResult}로 바꾸며,
 * 없는 항목 키는 버리고 비어 있는 값은 읽지 못한 것으로 본다. 값의 형식·범위 검사는 저장하는 쪽({@link ContractExtractionWorker})이 한다.
 */
@Component
@ConditionalOnExpression(LlmCondition.ENABLED)
@RequiredArgsConstructor
public class ClaudeContractExtractor implements ContractExtractor {

    private static final long MAX_TOKENS = 16000;

    static final String SYSTEM_PROMPT = """
            당신은 한국 근로계약서 원본(PDF 또는 사진)을 읽어 내용을 그대로 옮기는 역할을 합니다.
            문서에 실제로 적힌 내용만 옮기고, 적혀 있지 않거나 읽을 수 없는 값은 추측하지 마세요.

            [조항 clauses]
            - 계약서 전문을 읽는 순서대로 조항(번호가 붙은 문단 또는 하나의 항목 칸) 단위로 나눠 모두 옮깁니다.
            - text에는 한국어 원문을 옮깁니다. 영어 등 다른 언어가 함께 인쇄된 양식이면 한국어 부분만 옮깁니다.
            - 칸에 고용주가 채운 값이 있으면 그 값까지 포함해 문장으로 옮깁니다.
            - clauseNo는 "제3조"처럼 문서에 적힌 조항 번호이고, 없으면 빈 문자열입니다.
            - pageNo는 1부터 시작하는 쪽 번호이고, 모르면 0입니다.

            [항목 terms]
            - 아래 19개 항목마다 하나씩, key는 아래 목록의 키를 그대로 씁니다.
            - value는 고용주가 채운 값만 씁니다. 양식에 미리 인쇄된 안내 문구나 예시는 값이 아닙니다.
            - 비어 있거나 읽을 수 없으면 value는 빈 문자열입니다.
            - 형식: 날짜는 YYYY-MM-DD, 시각은 24시간제 HH:MM, 금액·분·일수는 단위 없이 숫자만,
              국적은 ISO 3166-1 alpha-2 대문자(예: VN)입니다.
            - 글씨가 흐리거나 표현이 모호해 원본과 비교해 확인이 필요하면 needsCheck를 true로 둡니다.
            - clauseIndex는 근거가 된 조항의 clauses 안 위치(0부터)이고, 없으면 -1입니다.

            [항목 목록] (키 | 이름 | 값 종류)
            %s
            """.formatted(termList());

    private final AnthropicClient anthropicClient;

    @Override
    public ExtractionResult extract(byte[] content, String mimeType) {
        Output output = ClaudeRequests.result(anthropicClient
                .messages()
                .create(ClaudeRequests.structured(Output.class, OutputConfig.Effort.HIGH, MAX_TOKENS)
                        .system(SYSTEM_PROMPT)
                        .addUserMessageOfContentBlockParams(
                                List.of(source(content, mimeType), ContentBlockParam.ofText("이 근로계약서를 읽어 주세요.")))
                        .build()));
        return toResult(output);
    }

    /** Claude 응답을 추출 결과로 바꾼다. 없는 항목 키는 버리고, 같은 키가 여러 번 오면 처음 것을 쓴다. */
    static ExtractionResult toResult(Output output) {
        List<ExtractionResult.Clause> clauses = new ArrayList<>();
        for (Clause clause : output.clauses()) {
            if (clause.text() == null || clause.text().isBlank()) {
                continue;
            }
            clauses.add(new ExtractionResult.Clause(
                    blankToNull(clause.clauseNo()),
                    clause.pageNo() > 0 ? clause.pageNo() : null,
                    clause.text().strip()));
        }
        Map<TermField, ExtractionResult.Term> terms = new EnumMap<>(TermField.class);
        for (Term term : output.terms()) {
            TermField.fromKey(term.key())
                    .ifPresent(field -> terms.putIfAbsent(
                            field,
                            new ExtractionResult.Term(
                                    blankToNull(term.value()),
                                    term.needsCheck(),
                                    term.clauseIndex() >= 0 ? term.clauseIndex() : null)));
        }
        return new ExtractionResult(clauses, terms);
    }

    private static ContentBlockParam source(byte[] content, String mimeType) {
        String data = Base64.getEncoder().encodeToString(content);
        return switch (mimeType) {
            case "application/pdf" ->
                ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                        .source(Base64PdfSource.builder().data(data).build())
                        .build());
            case "image/jpeg", "image/png" ->
                ContentBlockParam.ofImage(Base64ImageSource.builder()
                        .mediaType(Base64ImageSource.MediaType.of(mimeType))
                        .data(data)
                        .build());
            default -> throw new IllegalArgumentException("지원하지 않는 원본 형식입니다: " + mimeType);
        };
    }

    private static String termList() {
        return Arrays.stream(TermField.values())
                .map(field -> "- " + field.getKey() + " | " + field.getLabel() + " | " + field.getType())
                .collect(Collectors.joining("\n"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * Claude 응답 형식.
     *
     * @param clauses 원문 조항 (읽는 순서)
     * @param terms 항목 (19개 키마다 하나)
     */
    record Output(List<Clause> clauses, List<Term> terms) {}

    record Clause(
            @JsonPropertyDescription("문서에 적힌 조항 번호, 없으면 빈 문자열")
            String clauseNo,

            @JsonPropertyDescription("1부터 시작하는 쪽 번호, 모르면 0") int pageNo,
            @JsonPropertyDescription("한국어 원문") String text) {}

    record Term(
            @JsonPropertyDescription("항목 키") String key,

            @JsonPropertyDescription("고용주가 채운 값, 비어 있거나 읽을 수 없으면 빈 문자열")
            String value,

            @JsonPropertyDescription("흐리거나 모호해 확인이 필요한지") boolean needsCheck,

            @JsonPropertyDescription("근거 조항의 clauses 안 위치(0부터), 없으면 -1")
            int clauseIndex) {}
}
