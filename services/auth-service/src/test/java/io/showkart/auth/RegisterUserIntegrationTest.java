package io.showkart.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class RegisterUserIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("auth_db");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void happy_path_creates_user_with_bcrypt_hash() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"aarav@example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.email").value("aarav@example.com"))
                .andExpect(header().exists("X-Correlation-Id"));

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?",
                String.class,
                "aarav@example.com"
        );
        assertThat(hash).matches("^\\$2[aby]\\$.*");

        String roles = jdbc.queryForObject(
                "SELECT roles FROM users WHERE email = ?",
                String.class,
                "aarav@example.com"
        );
        assertThat(roles).isEqualTo("ROLE_USER");
    }

    @Test
    void duplicate_email_case_insensitive_returns_409() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"priya@example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"PRIYA@Example.COM","password":"different-pass"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    void weak_password_returns_400_weak_password() throws Exception {
        // Passes @Size but fails app-layer WeakPasswordException? No — @Size(min=8) triggers first.
        // Use a 7-char password that also passes @NotBlank/@Email, so @Size fires.
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"short@example.com","password":"1234567"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(matchesRegex("WEAK_PASSWORD|INVALID_INPUT")))
                .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    void malformed_json_returns_400_invalid_input() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void correlation_id_from_request_is_echoed() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .header("X-Correlation-Id", "req-abc-123")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"kiran@example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "req-abc-123"));
    }
}
