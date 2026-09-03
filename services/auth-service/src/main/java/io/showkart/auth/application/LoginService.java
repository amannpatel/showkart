package io.showkart.auth.application;

import io.showkart.auth.application.port.RefreshTokenRepositoryPort;
import io.showkart.auth.application.port.TokenIssuerPort;
import io.showkart.auth.application.port.UserRepositoryPort;
import io.showkart.auth.application.port.PasswordEncoderPort;
import io.showkart.auth.domain.AccessToken;
import io.showkart.auth.domain.RefreshTokenValue;
import io.showkart.auth.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordEncoderPort passwords;
    private final TokenIssuerPort tokens;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final Clock clock;
    // Real bcrypt hash of an unreachable random value: makes the "unknown email" path
    // spend the same bcrypt work as the "wrong password" path to close the timing side channel.
    private final String dummyHash;

    public LoginService(UserRepositoryPort users,
                        PasswordEncoderPort passwords,
                        TokenIssuerPort tokens,
                        RefreshTokenRepositoryPort refreshTokens,
                        Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.dummyHash = passwords.encode("__dummy_" + UUID.randomUUID());
    }

    @Override
    @Transactional
    public Result login(Command command) {
        String email = normalizeEmail(command.email());
        Optional<UserAccount> maybeUser = users.findByEmailIgnoreCase(email);

        String hash = maybeUser.map(UserAccount::passwordHash).orElse(dummyHash);
        boolean passwordMatches = passwords.matches(command.password(), hash);
        if (maybeUser.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        UserAccount user = maybeUser.get();

        AccessToken access = tokens.issueAccessToken(user.id(), user.roles());
        RefreshTokenValue refresh = tokens.issueRefreshToken();
        Instant now = clock.instant();
        refreshTokens.save(new RefreshTokenRecord(
                UUID.randomUUID(),
                user.id(),
                refresh.sha256Hex(),
                now,
                now.plusSeconds(tokens.refreshTtlSeconds()),
                null,
                null
        ));
        return new Result(access, refresh);
    }

    private static String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
}
