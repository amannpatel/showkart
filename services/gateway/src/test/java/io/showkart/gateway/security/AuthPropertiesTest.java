package io.showkart.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthPropertiesTest {

    @Test
    void short_secret_fails_fast_at_validate() {
        AuthProperties p = new AuthProperties();
        p.getJwt().setSecret("short");
        assertThatThrownBy(p::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void null_secret_fails_fast_at_validate() {
        AuthProperties p = new AuthProperties();
        assertThatThrownBy(p::validate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void whitespace_only_secret_of_length_32_still_fails() {
        AuthProperties p = new AuthProperties();
        p.getJwt().setSecret(" ".repeat(40));
        assertThatThrownBy(p::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("whitespace");
    }

    @Test
    void exactly_32_bytes_of_real_content_is_accepted() {
        AuthProperties p = new AuthProperties();
        p.getJwt().setSecret("abcdefghijklmnopqrstuvwxyz012345");
        assertThatCode(p::validate).doesNotThrowAnyException();
    }
}
