package com.visafe.domain.member.dto.response;

import com.visafe.domain.member.entity.MemberWorkerProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "근로자 검색 응답 DTO")
public record WorkerSearchResponse(
        @Schema(description = "근로자 회원 ID (계약 등록 시 workerMemberId)", example = "12")
        Long memberId,

        @Schema(description = "이름", example = "Nguyễn Văn An")
        String name,

        @Schema(description = "국적 (ISO 3166-1)", example = "VN")
        String nationality,

        @Schema(description = "생년월일 (동명이인 구분)", example = "1998-04-17")
        LocalDate birthDate,

        @Schema(description = "외국인등록번호 뒷자리 4개 (동명이인 구분)", example = "4821")
        String alienRegNoLast4) {

    public static WorkerSearchResponse from(MemberWorkerProfile profile) {
        return new WorkerSearchResponse(
                profile.getMemberId(),
                profile.getMember().getName(),
                profile.getNationality(),
                profile.getBirthDate(),
                profile.getAlienRegNoLast4());
    }
}
