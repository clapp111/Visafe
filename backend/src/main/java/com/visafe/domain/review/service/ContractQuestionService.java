package com.visafe.domain.review.service;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractClause;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.repository.ContractClauseRepository;
import com.visafe.domain.contract.service.ContractAccessService;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.entity.MemberWorkerProfile;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.member.repository.MemberWorkerProfileRepository;
import com.visafe.domain.review.answerer.ContractQuestionAnswerer;
import com.visafe.domain.review.answerer.QuestionAnswer;
import com.visafe.domain.review.answerer.QuestionSource;
import com.visafe.domain.review.dto.request.QuestionCreateRequest;
import com.visafe.domain.review.dto.response.QuestionResponse;
import com.visafe.domain.review.entity.ReviewQuestion;
import com.visafe.domain.review.entity.ReviewQuestionCitation;
import com.visafe.domain.review.repository.ReviewQuestionCitationRepository;
import com.visafe.domain.review.repository.ReviewQuestionRepository;
import com.visafe.domain.translation.enums.TranslationStatus;
import com.visafe.domain.translation.repository.TranslationRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractQuestionService {

    /** 답변을 기다리는 최대 시간 (초) */
    private static final long ANSWER_TIMEOUT_SECONDS = 20;

    private final ContractAccessService contractAccessService;
    private final MemberRepository memberRepository;
    private final MemberWorkerProfileRepository memberWorkerProfileRepository;
    private final ContractClauseRepository contractClauseRepository;
    private final TranslationRepository translationRepository;
    private final ReviewQuestionRepository reviewQuestionRepository;
    private final ReviewQuestionCitationRepository reviewQuestionCitationRepository;
    private final ContractQuestionAnswerer contractQuestionAnswerer;
    private final TransactionTemplate transactionTemplate;
    /** 답변기를 부를 실행기 (Spring Boot 기본 실행기, @Async와 같음) */
    private final AsyncTaskExecutor applicationTaskExecutor;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 근로자가 현재 회차에서 한 질문을 조회한다.
     *
     * <p>질문 목록 API(W3)에서 쓴다. 질문한 순서대로 답변과 근거 조항을 함께 돌려준다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 근로자
     * @return 질문 목록 (오래된 순)
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> list(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        List<ReviewQuestion> questions = reviewQuestionRepository.findByContractVersionIdAndAskedByIdOrderByIdAsc(
                contract.getCurrentVersion().getId(), member.getMemberId());
        Map<Long, List<ReviewQuestionCitation>> citations = citationsOf(questions);
        return questions.stream()
                .map(question -> QuestionResponse.of(question, citations.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    /**
     * 계약에 대해 질문하고 답을 받는다.
     *
     * <p>질문하기 API(W4)에서 쓴다. 근로자가 계약 내용을 볼 수 있을 때(현재 회차에 완료된 번역본이 있을 때)만 질문할 수 있다.
     * 질문을 저장한 뒤 답변기를 최대 20초 기다리며, 오래 걸리는 호출이 DB 연결을 잡지 않도록 질문 저장과 답변 저장을 따로 커밋한다.
     * 답변기가 실패하거나 시간이 초과되면 질문만 남기고 답 없이 돌려준다. 근거 조항은 현재 회차의 조항만, 같은 조항은 한 번만 저장한다.</p>
     *
     * @param contractId 계약 ID
     * @param request 질문
     * @param member 로그인한 근로자
     * @return 저장한 질문과 답 (답하지 못하면 answer가 null)
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 현재 회차에 완료된 번역본이 없는 경우
     */
    public QuestionResponse ask(Long contractId, QuestionCreateRequest request, MemberDetails member) {
        Asked asked = transactionTemplate.execute(status -> saveQuestion(contractId, request, member));
        QuestionAnswer answer = callAnswerer(asked.source());
        return transactionTemplate.execute(status -> saveAnswer(asked, answer));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 질문을 저장하고 답변기에 넘길 내용을 모은다.
     *
     * @param contractId 계약 ID
     * @param request 질문
     * @param member 로그인한 근로자
     * @return 저장한 질문 ID, 원문 조항 ID, 답변기에 넘길 내용
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 현재 회차에 완료된 번역본이 없는 경우
     */
    private Asked saveQuestion(Long contractId, QuestionCreateRequest request, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        boolean translated = translationRepository
                .findFirstByContractVersionIdAndStatusOrderByTranslationNoDesc(version.getId(), TranslationStatus.DONE)
                .isPresent();
        if (!translated) {
            throw new RestApiException(CustomErrorCode.TRANSLATION_NOT_READY);
        }
        String language = memberWorkerProfileRepository
                .findById(member.getMemberId())
                .map(MemberWorkerProfile::getPreferredLanguage)
                .orElse(contract.getTargetLanguage());
        String question = request.question().strip();
        ReviewQuestion saved = reviewQuestionRepository.save(ReviewQuestion.ask(
                version, memberRepository.getReferenceById(member.getMemberId()), language, question));

        List<ContractClause> clauses = contractClauseRepository.findByContractFileIdOrderBySeqAsc(
                version.getContractFile().getId());
        QuestionSource source = new QuestionSource(
                language,
                question,
                clauses.stream()
                        .map(clause -> new QuestionSource.Clause(clause.getClauseNo(), clause.getTextKo()))
                        .toList());
        return new Asked(
                saved.getId(), clauses.stream().map(ContractClause::getId).toList(), source);
    }

    /**
     * 답변기를 최대 20초 기다리며 부른다.
     *
     * @param source 답변기에 넘길 내용
     * @return 답, 실패하거나 시간이 초과되면 {@code null}
     */
    private QuestionAnswer callAnswerer(QuestionSource source) {
        CompletableFuture<QuestionAnswer> future =
                CompletableFuture.supplyAsync(() -> contractQuestionAnswerer.answer(source), applicationTaskExecutor);
        try {
            return future.get(ANSWER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("[질문 답변] {}초 안에 답하지 못함", ANSWER_TIMEOUT_SECONDS);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("[질문 답변] 실패", e);
            return null;
        }
    }

    /**
     * 답과 근거 조항을 저장하고 응답을 만든다.
     *
     * @param asked 저장한 질문
     * @param answer 답, 없으면 질문만 돌려준다
     * @return 질문과 답
     */
    private QuestionResponse saveAnswer(Asked asked, QuestionAnswer answer) {
        ReviewQuestion question =
                reviewQuestionRepository.findById(asked.questionId()).orElseThrow();
        if (answer == null || answer.answer() == null || answer.answer().isBlank()) {
            return QuestionResponse.of(question, List.of());
        }
        question.answer(answer.answer(), answer.referredToCenter(), Instant.now());
        Map<Long, String> quotes = new LinkedHashMap<>();
        for (QuestionAnswer.Citation citation : answer.citations()) {
            int index = citation.clauseIndex();
            if (index < 0 || index >= asked.clauseIds().size()) {
                continue;
            }
            String quoted = citation.quotedText() == null
                    ? asked.source().clauses().get(index).textKo()
                    : citation.quotedText();
            quotes.putIfAbsent(asked.clauseIds().get(index), quoted);
        }
        List<ReviewQuestionCitation> citations = reviewQuestionCitationRepository.saveAll(quotes.entrySet().stream()
                .map(entry -> ReviewQuestionCitation.of(
                        question, contractClauseRepository.getReferenceById(entry.getKey()), entry.getValue()))
                .toList());
        return QuestionResponse.of(question, citationsOf(List.of(question)).getOrDefault(question.getId(), citations));
    }

    /**
     * 질문들의 근거 조항을 질문별로 모은다.
     *
     * @param questions 질문
     * @return 질문 ID → 근거 조항 (저장한 순서)
     */
    private Map<Long, List<ReviewQuestionCitation>> citationsOf(List<ReviewQuestion> questions) {
        if (questions.isEmpty()) {
            return Map.of();
        }
        return reviewQuestionCitationRepository
                .findWithClauseByQuestionIds(
                        questions.stream().map(ReviewQuestion::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        citation -> citation.getReviewQuestion().getId(), LinkedHashMap::new, Collectors.toList()));
    }

    private record Asked(Long questionId, List<Long> clauseIds, QuestionSource source) {}
}
