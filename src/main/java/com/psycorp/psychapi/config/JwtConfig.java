package com.psycorp.psychapi.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "jwt")
public interface JwtConfig {
    @WithName("expires-in")
    @WithDefault("604800")
    long expiresIn();

    @WithName("issuer")
    @WithDefault("psych-api")
    String issuer();

    SignKey sign();

    Verify verify();

    interface SignKey {
        @WithName("key.location")
        @WithDefault("keys/privateKey.pem")
        String keyLocation();
    }

    interface Verify {
        @WithName("expires-at")
        @WithDefault("true")
        boolean expiresAt();
    }
}
