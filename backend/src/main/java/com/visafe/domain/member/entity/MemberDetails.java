package com.visafe.domain.member.entity;

import com.visafe.domain.member.enums.MemberRole;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증된 회원 정보 (SecurityContext의 principal).
 * 엔티티를 그대로 들고 다니지 않고 인증·권한에 필요한 값만 복사한다. (지연 로딩·트랜잭션 밖 접근 문제 방지)
 *
 * <p>컨트롤러에서 {@code @AuthenticationPrincipal MemberDetails member}로 받는다.
 */
@Getter
public class MemberDetails implements UserDetails {

    private final Long memberId;
    private final String loginEmail;
    private final String passwordHash;
    private final MemberRole role;

    private MemberDetails(Long memberId, String loginEmail, String passwordHash, MemberRole role) {
        this.memberId = memberId;
        this.loginEmail = loginEmail;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static MemberDetails from(Member member) {
        return new MemberDetails(member.getId(), member.getLoginEmail(), member.getPasswordHash(), member.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Spring Security의 username = 로그인 이메일 */
    @Override
    public String getUsername() {
        return loginEmail;
    }
}
