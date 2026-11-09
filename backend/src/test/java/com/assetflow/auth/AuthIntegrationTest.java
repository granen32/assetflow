package com.assetflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.assetflow.auth.service.TokenService;
import com.assetflow.member.domain.Member;
import com.assetflow.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 Spring Bean·보안 필터·JPA·Flyway를 함께 검증한다. HTTP 호출은 MockMvc로 처리한다.
 * DB는 PostgreSQL 모드의 H2이므로 실제 PostgreSQL 및 브라우저 통합 검증과는 구분한다.
 * 테스트 트랜잭션은 각 테스트 종료 후 롤백되어 다른 테스트에 회원 데이터가 남지 않는다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "assetflow.auth.jwt-secret=integration-test-secret-at-least-32-bytes",
        "assetflow.auth.cookie-secure=false"
})
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MemberRepository members;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;

    @Test
    void registersHashesPasswordAndRestoresSession() throws Exception {
        MvcResult result = register("alice@example.com");
        Member member = members.findByEmail("alice@example.com").orElseThrow();
        assertThat(member.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwords.matches("password123", member.getPasswordHash())).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "passwordHash");
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("HttpOnly", "SameSite=Strict", "Max-Age=1800");
        mvc.perform(get("/api/v1/auth/me").cookie(authCookie(result)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void logsInAndRejectsWrongPasswordUnknownAndLegacyMembers() throws Exception {
        register("alice@example.com");
        mvc.perform(withCsrf(post("/api/v1/auth/login")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andExpect(header().exists("Set-Cookie"));
        for (String email : new String[]{"alice@example.com", "unknown@example.com", "legacy@example.com"}) {
            if (email.startsWith("legacy")) members.saveAndFlush(new Member(email, "기존 회원"));
            mvc.perform(withCsrf(post("/api/v1/auth/login")).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(java.util.Map.of("email", email, "password", "wrong-password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호를 확인해 주세요."));
        }
    }

    @Test
    void rejectsDuplicateInvalidAndOverlongBytePasswords() throws Exception {
        register("alice@example.com");
        mvc.perform(withCsrf(post("/api/v1/auth/register")).contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("alice@example.com", "password123")))
                .andExpect(status().isConflict());
        for (String password : new String[]{"short", "가".repeat(25)}) {
            mvc.perform(withCsrf(post("/api/v1/auth/register")).contentType(MediaType.APPLICATION_JSON)
                            .content(registrationBody("bob@example.com", password)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void csrfIsRequiredForLoginRegistrationLogoutAndAccountWrites() throws Exception {
        for (String path : new String[]{"/auth/login", "/auth/register", "/auth/logout", "/accounts"}) {
            mvc.perform(post("/api/v1" + path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void accountOwnershipComesFromJwtAndOtherMembersCannotReadOrRename() throws Exception {
        MockCookie alice = authCookie(register("alice@example.com"));
        long aliceId = members.findByEmail("alice@example.com").orElseThrow().getId();
        MvcResult account = mvc.perform(withCsrf(post("/api/v1/accounts")).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":999,\"name\":\"생활비\",\"type\":\"CHECKING\",\"openingBalance\":0,\"currency\":\"KRW\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.memberId").value(aliceId))
                .andExpect(jsonPath("$.balance").value(0)).andReturn();
        long accountId = json.readTree(account.getResponse().getContentAsString()).get("id").asLong();
        MockCookie bob = authCookie(register("bob@example.com"));
        mvc.perform(get("/api/v1/accounts?memberId=" + aliceId).cookie(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/accounts/" + accountId + "?memberId=" + aliceId).cookie(bob))
                .andExpect(status().isNotFound());
        mvc.perform(withCsrf(patch("/api/v1/accounts/" + accountId)).cookie(bob)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"탈취\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(withCsrf(patch("/api/v1/accounts/" + accountId)).cookie(alice)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"수정 계좌\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("수정 계좌"));
        mvc.perform(get("/api/v1/accounts").cookie(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("수정 계좌"));
    }

    @Test
    void rejectsTamperedAndExpiredTokensButAllowsReloginAndLogout() throws Exception {
        MockCookie valid = authCookie(register("alice@example.com"));
        String token = valid.getValue();
        int signatureStart = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, signatureStart) + (token.charAt(signatureStart) == 'a' ? 'b' : 'a') + token.substring(signatureStart + 1);
        Instant now = Instant.now();
        String expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer(TokenService.ISSUER).subject("1")
                        .issuedAt(now.minusSeconds(3600)).expiresAt(now.minusSeconds(120)).build())).getTokenValue();
        for (String invalid : new String[]{tampered, expired}) {
            mvc.perform(get("/api/v1/auth/me").cookie(new MockCookie(TokenService.COOKIE_NAME, invalid)))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(withCsrf(post("/api/v1/auth/login")).cookie(new MockCookie(TokenService.COOKIE_NAME, expired))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
        mvc.perform(withCsrf(post("/api/v1/auth/logout")).cookie(valid))
                .andExpect(status().isNoContent()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    private MvcResult register(String email) throws Exception {
        return mvc.perform(withCsrf(post("/api/v1/auth/register")).contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(email, "password123")))
                .andExpect(status().isCreated()).andReturn();
    }

    private String registrationBody(String email, String password) throws Exception {
        return json.writeValueAsString(java.util.Map.of("email", email, "nickname", "학습자", "password", password));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        // 검증을 우회하는 모의 CSRF 값 대신 실제 발급 API의 쿠키·마스킹 토큰을 함께 돌려보낸다.
        MvcResult result = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        String cookie = result.getResponse().getHeaders("Set-Cookie").stream().filter(value -> value.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
        return request.cookie(MockCookie.parse(cookie)).header(body.get("headerName").asText(), body.get("token").asText());
    }

    private MockCookie authCookie(MvcResult result) {
        return MockCookie.parse(result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith(TokenService.COOKIE_NAME + "=")).findFirst().orElseThrow());
    }
}
