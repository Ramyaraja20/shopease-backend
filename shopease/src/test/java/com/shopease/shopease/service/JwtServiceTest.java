package com.shopease.shopease.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    @Test
    void generatedTokenIsValidAndContainsEmail() {
        String token = jwtService.generateToken("ramya@test.com", "USER");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("ramya@test.com", jwtService.extractEmail(token));
    }

    @Test
    void tokenSignedWithDifferentSecretIsInvalid() {
        JwtService attacker = new JwtService();
        ReflectionTestUtils.setField(attacker, "secret", "another-secret-key-that-is-also-32-characters-long!");
        ReflectionTestUtils.setField(attacker, "expirationMs", 3600000L);

        String forgedToken = attacker.generateToken("ramya@test.com", "ADMIN");

        assertFalse(jwtService.isTokenValid(forgedToken));
    }

    @Test
    void expiredTokenIsInvalid() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);

        String token = jwtService.generateToken("ramya@test.com", "USER");

        assertFalse(jwtService.isTokenValid(token));
    }
}