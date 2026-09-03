package io.showkart.auth.application;

import io.showkart.auth.application.port.RefreshTokenRepositoryPort;
import io.showkart.auth.application.port.TokenIssuerPort;
import io.showkart.auth.application.port.UserRepositoryPort;
import io.showkart.auth.domain.AccessToken;
import io.showkart.auth.domain.RefreshTokenValue;
import io.showkart.auth.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepositoryPort refreshTokens;
    private final TokenIssuerPort tokens;
    private final UserRepositoryPort users;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepositoryPort refreshTokens,
                               TokenIssuerPort tokens,
                               UserRepositoryPort users,
                               Clock clock) {
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result refresh(Command command) {
        if (command.rawRefreshToken() == null || command.rawRefreshToken().isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        String hash = tokens.hashRefreshToken(command.rawRefreshToken());
        RefreshTokenRecord existing = refreshTokens.findByHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();
        if (!existing.isActive(now)) {
            throw new InvalidRefreshTokenException();
        }
        UserAccount owner = users.findById(existing.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        RefreshTokenValue newRefresh = tokens.issueRefreshToken();
        UUID newId = UUID.randomUUID();
        // Save first so the FK replaced_by=newId is satisfiable. The pessimistic lock on the
        // old row (acquired via findByHash's @Lock) plus the "revokedAt is null" guard inside
        // revokeIfActive together prevent two /refresh calls from both rotating the same token.
        refreshTokens.save(new RefreshTokenRecord(
                newId,
                owner.id(),
                newRefresh.sha256Hex(),
                now,
                now.plusSeconds(tokens.refreshTtlSeconds()),
                null,
                null
        ));
        refreshTokens.revoke(existing.id(), newId);

        AccessToken access = tokens.issueAccessToken(owner.id(), owner.roles());
        return new Result(access, newRefresh);
    }
}
