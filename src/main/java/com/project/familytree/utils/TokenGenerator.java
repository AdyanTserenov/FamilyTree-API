package com.project.familytree.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.Base64;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TokenGenerator {
    private static final SecureRandom RNG = new SecureRandom();

    public static String urlSafe(int bytes) {
        byte[] buff = new byte[bytes];
        RNG.nextBytes(buff);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buff);
    }
}
