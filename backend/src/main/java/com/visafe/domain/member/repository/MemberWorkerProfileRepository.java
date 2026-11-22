package com.visafe.domain.member.repository;

import com.visafe.domain.member.entity.MemberWorkerProfile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberWorkerProfileRepository extends JpaRepository<MemberWorkerProfile, Long> {

    /**
     * 이름 앞부분이 일치하는 근로자 ID를 찾는다. 대소문자와 베트남어 성조 부호를 무시한다. (PostgreSQL unaccent)
     * prefix의 %, _, \는 호출하는 쪽에서 \로 이스케이프해 넘긴다.
     */
    @Query(value = """
            SELECT m.id
            FROM members m
            JOIN member_worker_profiles p ON p.member_id = m.id
            WHERE m.role = 'WORKER'
              AND lower(unaccent(m.name)) LIKE lower(unaccent(:prefix)) || '%' ESCAPE '\\'
            ORDER BY lower(unaccent(m.name)), p.birth_date, m.id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> searchWorkerIdsByNamePrefix(@Param("prefix") String prefix, @Param("limit") int limit);

    @Query("select p from MemberWorkerProfile p join fetch p.member where p.memberId in :memberIds")
    List<MemberWorkerProfile> findAllWithMemberByMemberIdIn(@Param("memberIds") Collection<Long> memberIds);
}
