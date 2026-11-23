package com.visafe.infra.ai;

/**
 * AI 구현을 고르는 조건식. API 키가 있으면 Claude 구현을, 없으면 stub 구현을 빈으로 등록한다.
 * <p>
 * {@code @ConditionalOnExpression}에 그대로 쓴다. (예: {@code @ConditionalOnExpression(LlmCondition.ENABLED)})
 */
public final class LlmCondition {

    /** API 키가 있을 때 */
    public static final String ENABLED = "'${visafe.llm.api-key:}'.trim() != ''";

    /** API 키가 없을 때 */
    public static final String DISABLED = "'${visafe.llm.api-key:}'.trim() == ''";

    private LlmCondition() {}
}
