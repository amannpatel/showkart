package io.showkart.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DEFERRED TO EPIC 7 CI: on Docker Desktop for Windows, sibling containers spawned by
 * Testcontainers advertise via 172.17.0.1 which is unreachable from the JDK build
 * container. See Story 1.2 spec change log (decision 2a). This file is architecturally
 * correct and will pass unmodified on a Linux CI runner.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoginIntegrationTest {

    private static final String TEST_SECRET =
            "test-secret-must-be-at-least-32-bytes-long-yes-really-thirty-two-bytes-min";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("auth_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("auth.jwt.secret", () -> TEST_SECRET);
        registry.add("auth.jwt.access-ttl-seconds", () -> "3600");
        registry.add("auth.refresh.ttl-seconds", () -> "604800");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetUsers() {
        jdbc.execute("TRUNCATE users, refresh_token RESTART IDENTITY CASCADE");
    }

    @Test
    void happy_path_login_returns_signed_jwt_and_persists_refresh() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"aarav@example.com\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"aarav@example.com\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        JsonNode body = json.readTree(login.getResponse().getContentAsString());
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(body.get("accessToken").asText()).getPayload();
        assertThat(claims.getSubject()).isNotBlank();
        assertThat(claims.get("roles", String.class)).isEqualTo("ROLE_USER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(3_600_000L);

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM refresh_token WHERE revoked_at IS NULL", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void wrong_password_returns_401_invalid_credentials() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"anandi@example.com\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"anandi@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void unknown_email_returns_401_invalid_credentials_same_shape() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\",\"password\":\"anything\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_rotates_and_replayed_old_token_is_rejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"kiran@example.com\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"kiran@example.com\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isOk()).andReturn();
        String oldRefresh = json.readTree(login.getResponse().getContentAsString()).get("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }
}
