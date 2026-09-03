package io.showkart.auth.application;

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

class RefreshTokenServiceTest {

    private FakeUserRepository users;
    private FakeRefreshRepository refreshTokens;
    private FakeTokenIssuer tokens;
    private RefreshTokenService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC);
        users = new FakeUserRepository();
        refreshTokens = new FakeRefreshRepository(clock);
        tokens = new FakeTokenIssuer();
        service = new RefreshTokenService(refreshTokens, tokens, users, clock);

        userId = UUID.randomUUID();
        users.byId.put(userId, new UserAccount(userId, "u@x.com", "$2a$hash", "ROLE_USER", clock.instant()));
    }

    @Test
    @DisplayName("valid refresh: revokes old, issues new access + new refresh, links via replaced_by")
    void valid_refresh_rotates() {
        UUID oldId = UUID.randomUUID();
        String oldHash = tokens.hashRefreshToken("old-raw-token");
        refreshTokens.save(new RefreshTokenRecord(
                oldId, userId, oldHash,
                Instant.parse("2026-09-03T11:00:00Z"),
                Instant.parse("2026-09-10T12:00:00Z"),
                null, null));

        RefreshTokenUseCase.Result result = service.refresh(new RefreshTokenUseCase.Command("old-raw-token"));

        assertThat(result.accessToken().jwt()).isNotBlank();
        assertThat(result.refreshToken().raw()).isNotBlank().isNotEqualTo("old-raw-token");

        RefreshTokenRecord oldNow = refreshTokens.byId.get(oldId);
        assertThat(oldNow.revokedAt()).isNotNull();
        assertThat(oldNow.replacedBy()).isNotNull();

        RefreshTokenRecord newRow = refreshTokens.byId.get(oldNow.replacedBy());
        assertThat(newRow).isNotNull();
        assertThat(newRow.tokenHash()).isEqualTo(result.refreshToken().sha256Hex());
        assertThat(newRow.revokedAt()).isNull();
    }

    @Test
    @DisplayName("replay: reusing an already-rotated token throws InvalidRefreshTokenException")
    void replay_rejected() {
        UUID oldId = UUID.randomUUID();
        String oldHash = tokens.hashRefreshToken("old-raw-token");
        refreshTokens.save(new RefreshTokenRecord(
                oldId, userId, oldHash,
                Instant.parse("2026-09-03T11:00:00Z"),
                Instant.parse("2026-09-10T12:00:00Z"),
                null, null));
        service.refresh(new RefreshTokenUseCase.Command("old-raw-token"));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("old-raw-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("unknown token: throws InvalidRefreshTokenException")
    void unknown_rejected() {
        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("nope")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("expired token: throws InvalidRefreshTokenException even though the row exists")
    void expired_rejected() {
        UUID id = UUID.randomUUID();
        String hash = tokens.hashRefreshToken("expired-raw");
        refreshTokens.save(new RefreshTokenRecord(
                id, userId, hash,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z"),
                null, null));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("expired-raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("blank input: throws InvalidRefreshTokenException without hitting the repository")
    void blank_rejected() {
        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("   ")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    static final class FakeUserRepository implements UserRepositoryPort {
        final Map<UUID, UserAccount> byId = new HashMap<>();
        @Override public Optional<UserAccount> findByEmailIgnoreCase(String email) { return Optional.empty(); }
        @Override public Optional<UserAccount> findById(UUID userId) { return Optional.ofNullable(byId.get(userId)); }
        @Override public UserAccount save(UserAccount user) { byId.put(user.id(), user); return user; }
    }

    static final class FakeRefreshRepository implements RefreshTokenRepositoryPort {
        final Map<UUID, RefreshTokenRecord> byId = new HashMap<>();
        final Map<String, UUID> byHash = new HashMap<>();
        private final Clock clock;
        FakeRefreshRepository(Clock clock) { this.clock = clock; }
        @Override public RefreshTokenRecord save(RefreshTokenRecord r) {
            byId.put(r.id(), r); byHash.put(r.tokenHash(), r.id()); return r;
        }
        @Override public Optional<RefreshTokenRecord> findByHash(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash)).map(byId::get);
        }
        @Override public void revoke(UUID id, UUID replacementId) {
            RefreshTokenRecord r = byId.get(id);
            if (r == null) return;
            byId.put(id, new RefreshTokenRecord(r.id(), r.userId(), r.tokenHash(),
                    r.issuedAt(), r.expiresAt(), clock.instant(), replacementId));
        }
    }

    static final class FakeTokenIssuer implements TokenIssuerPort {
        private final AtomicInteger counter = new AtomicInteger();
        @Override public AccessToken issueAccessToken(UUID userId, String roles) {
            return new AccessToken("jwt-" + counter.incrementAndGet() + "-" + userId, 3600);
        }
        @Override public RefreshTokenValue issueRefreshToken() {
            String raw = "refresh-" + counter.incrementAndGet();
            return new RefreshTokenValue(raw, hashRefreshToken(raw));
        }
        @Override public String hashRefreshToken(String rawToken) {
            return "h(" + rawToken + ")";
        }
        @Override public long refreshTtlSeconds() { return 604800; }
    }
}
