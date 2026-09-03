package io.showkart.auth.application;

import io.showkart.auth.application.port.PasswordEncoderPort;
import io.showkart.auth.application.port.RefreshTokenRepositoryPort;
import io.showkart.auth.application.port.TokenIssuerPort;
import io.showkart.auth.application.port.UserRepositoryPort;
import io.showkart.auth.domain.AccessToken;
import io.showkart.auth.domain.RefreshTokenValue;
import io.showkart.auth.domain.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC);
    private FakeUserRepository users;
    private CountingPasswords passwords;
    private FakeTokenIssuer tokens;
    private FakeRefreshRepository refreshTokens;
    private LoginService service;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        passwords = new CountingPasswords();
        tokens = new FakeTokenIssuer();
        refreshTokens = new FakeRefreshRepository(clock);
        service = new LoginService(users, passwords, tokens, refreshTokens, clock);
    }

    @Test
    @DisplayName("happy path: issues access + refresh, persists refresh row hashed")
    void happy_path() {
        UUID id = UUID.randomUUID();
        users.byEmail.put("u@x.com", new UserAccount(id, "u@x.com", "$stored", "ROLE_USER", clock.instant()));
        passwords.trueFor.put("goodpw|$stored", Boolean.TRUE);

        LoginUseCase.Result r = service.login(new LoginUseCase.Command("U@X.COM", "goodpw"));

        assertThat(r.accessToken().jwt()).isNotBlank();
        assertThat(r.refreshToken().raw()).isNotBlank();
        assertThat(refreshTokens.byHash).containsKey(r.refreshToken().sha256Hex());
        assertThat(refreshTokens.byId).hasSize(1);
    }

    @Test
    @DisplayName("wrong password: InvalidCredentialsException; bcrypt still invoked exactly once")
    void wrong_password() {
        UUID id = UUID.randomUUID();
        users.byEmail.put("u@x.com", new UserAccount(id, "u@x.com", "$stored", "ROLE_USER", clock.instant()));

        assertThatThrownBy(() -> service.login(new LoginUseCase.Command("u@x.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(passwords.matchesCalls.get()).isEqualTo(1);
        assertThat(refreshTokens.byId).isEmpty();
    }

    @Test
    @DisplayName("unknown email: InvalidCredentialsException; bcrypt STILL invoked once (constant-time posture)")
    void unknown_email() {
        assertThatThrownBy(() -> service.login(new LoginUseCase.Command("nobody@x.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(passwords.matchesCalls.get()).isEqualTo(1);
        assertThat(refreshTokens.byId).isEmpty();
    }

    static final class FakeUserRepository implements UserRepositoryPort {
        final Map<String, UserAccount> byEmail = new HashMap<>();
        @Override public Optional<UserAccount> findByEmailIgnoreCase(String email) { return Optional.ofNullable(byEmail.get(email)); }
        @Override public Optional<UserAccount> findById(UUID userId) { return Optional.empty(); }
        @Override public UserAccount save(UserAccount user) { byEmail.put(user.email(), user); return user; }
    }

    static final class CountingPasswords implements PasswordEncoderPort {
        final Map<String, Boolean> trueFor = new HashMap<>();
        final AtomicInteger matchesCalls = new AtomicInteger();
        @Override public String encode(String rawPassword) { return "$enc(" + rawPassword + ")"; }
        @Override public boolean matches(String raw, String hash) {
            matchesCalls.incrementAndGet();
            return Boolean.TRUE.equals(trueFor.get(raw + "|" + hash));
        }
    }

    static final class FakeTokenIssuer implements TokenIssuerPort {
        private final AtomicInteger counter = new AtomicInteger();
        @Override public AccessToken issueAccessToken(UUID userId, String roles) {
            return new AccessToken("jwt-" + counter.incrementAndGet(), 3600);
        }
        @Override public RefreshTokenValue issueRefreshToken() {
            String raw = "refresh-" + counter.incrementAndGet();
            return new RefreshTokenValue(raw, "h(" + raw + ")");
        }
        @Override public String hashRefreshToken(String raw) { return "h(" + raw + ")"; }
        @Override public long refreshTtlSeconds() { return 604800; }
    }

    static final class FakeRefreshRepository implements RefreshTokenRepositoryPort {
        final Map<UUID, RefreshTokenRecord> byId = new HashMap<>();
        final Map<String, UUID> byHash = new HashMap<>();
        private final Clock clock;
        FakeRefreshRepository(Clock clock) { this.clock = clock; }
        @Override public RefreshTokenRecord save(RefreshTokenRecord r) { byId.put(r.id(), r); byHash.put(r.tokenHash(), r.id()); return r; }
        @Override public Optional<RefreshTokenRecord> findByHash(String hash) { return Optional.ofNullable(byHash.get(hash)).map(byId::get); }
        @Override public void revoke(UUID id, UUID replacementId) { }
    }
}
