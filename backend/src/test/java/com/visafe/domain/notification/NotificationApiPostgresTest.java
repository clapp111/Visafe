package com.visafe.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 알림 목록(N1), 읽음 처리(N2), 모두 읽음(N3)을 실제 PostgreSQL로 확인한다. 알림은 테스트에서 직접 저장한다.
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class NotificationApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    private String workerToken;
    private String employerToken;
    private Long first;
    private Long second;
    private Long third;
    private Long employerNotification;

    @BeforeEach
    void setUp() throws Exception {
        workerToken = login("worker1@demo.visafe.test");
        employerToken = login("employer@demo.visafe.test");
        Member worker =
                memberRepository.findByLoginEmail("worker1@demo.visafe.test").orElseThrow();
        Member employer =
                memberRepository.findByLoginEmail("employer@demo.visafe.test").orElseThrow();
        first = save(worker, NotificationType.REVIEW_REQUESTED, "/contracts/1");
        second = save(worker, NotificationType.CONTRACT_CONFIRMED, "/contracts/1");
        third = save(worker, NotificationType.TERMINATION_REQUESTED, null);
        employerNotification = save(employer, NotificationType.CONTRACT_RETURNED, "/contracts/2");
        em.flush();
        em.clear();
    }

    @Test
    void listShowsOwnNotificationsLatestFirst() throws Exception {
        list(workerToken, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].notificationId").value(third))
                .andExpect(jsonPath("$.content[0].type").value("TERMINATION_REQUESTED"))
                .andExpect(jsonPath("$.content[0].linkPath").isEmpty())
                .andExpect(jsonPath("$.content[0].readAt").isEmpty())
                .andExpect(jsonPath("$.content[2].notificationId").value(first))
                .andExpect(jsonPath("$.content[2].title").value("제목 REVIEW_REQUESTED"))
                .andExpect(jsonPath("$.content[2].linkPath").value("/contracts/1"))
                .andExpect(jsonPath("$.content[2].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(3));

        list(workerToken, "page=1&size=2")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notificationId").value(first))
                .andExpect(jsonPath("$.page.totalPages").value(2));
        list(employerToken, "")
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.content[0].notificationId").value(employerNotification));
        list(workerToken, "size=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_REQUEST"));
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void markReadKeepsFirstReadTimeAndOnlyOwnNotification() throws Exception {
        markRead(second, workerToken).andExpect(status().isNoContent());
        em.flush();
        em.clear();
        Instant readAt = em.find(Notification.class, second).getReadAt();
        assertThat(readAt).isNotNull();
        em.clear();
        markRead(second, workerToken).andExpect(status().isNoContent());
        assertThat(em.find(Notification.class, second).getReadAt()).isEqualTo(readAt);

        list(workerToken, "unreadOnly=true")
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.content[*].notificationId")
                        .value(org.hamcrest.Matchers.contains(third.intValue(), first.intValue())));
        list(workerToken, "").andExpect(jsonPath("$.content[1].readAt").isNotEmpty());

        markRead(employerNotification, workerToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"));
        markRead(Long.MAX_VALUE, workerToken).andExpect(status().isNotFound());
    }

    @Test
    void markAllReadOnlyAffectsOwnNotifications() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all").header(HttpHeaders.AUTHORIZATION, workerToken))
                .andExpect(status().isNoContent());

        list(workerToken, "")
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.content[0].readAt").isNotEmpty());
        list(workerToken, "unreadOnly=true")
                .andExpect(jsonPath("$.content.length()").value(0));
        list(employerToken, "").andExpect(jsonPath("$.unreadCount").value(1));
    }

    private Long save(Member member, NotificationType type, String linkPath) {
        Notification notification = Notification.builder()
                .member(member)
                .type(type)
                .title("제목 " + type)
                .message("내용 " + type)
                .linkPath(linkPath)
                .build();
        em.persist(notification);
        return notification.getId();
    }

    private ResultActions list(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications?" + query).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions markRead(Long notificationId, String token) throws Exception {
        return mockMvc.perform(
                patch("/api/v1/notifications/" + notificationId + "/read").header(HttpHeaders.AUTHORIZATION, token));
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }
}
