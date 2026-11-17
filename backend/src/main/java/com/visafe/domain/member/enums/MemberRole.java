package com.visafe.domain.member.enums;

/**
 * members.role
 */
public enum MemberRole {
    /** 고용주 (사업주 소속 필수) */
    EMPLOYER,
    /** 근로자 (member_worker_profiles 1:1) */
    WORKER,
    /** 관리자·감독 기관 검토자 */
    ADMIN
}
