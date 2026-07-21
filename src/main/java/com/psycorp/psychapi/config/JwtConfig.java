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
        @WithName("secret")
        @WithDefault("your-super-secret-key-at-least-32-chars")
        String secret();
    }

    interface Verify {
        @WithName("expires-at")
        @WithDefault("true")
        boolean expiresAt();
    }
}
