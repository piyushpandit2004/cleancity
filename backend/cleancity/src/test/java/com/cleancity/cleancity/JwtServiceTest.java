package com.cleancity.cleancity;

import com.cleancity.cleancity.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "VGhpc0lzQVNlY3JldEtleUZvckNsZWFuQ2l0eVRlc3RpbmcxMjM0NQ==";

    private static final String EMAIL = "piyush@example.com";

    @BeforeEach
    void setUp() {
        // Normal token ke liye 1 minute expiration
        jwtService = new JwtService(SECRET, 60_000);
    }

    @Test
    void generateToken_shouldGenerateValidToken() {

        // Act
        String token = jwtService.generateToken(EMAIL);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {

        // Arrange
        String token = jwtService.generateToken(EMAIL);

        // Act
        String extractedEmail = jwtService.extractEmail(token);

        // Assert
        assertEquals(EMAIL, extractedEmail);
    }

    @Test
    void isTokenValid_shouldReturnTrueForCorrectEmail() {

        // Arrange
        String token = jwtService.generateToken(EMAIL);

        // Act
        boolean result = jwtService.isTokenValid(token, EMAIL);

        // Assert
        assertTrue(result);
    }

    @Test
    void isTokenValid_shouldReturnFalseForWrongEmail() {

        // Arrange
        String token = jwtService.generateToken(EMAIL);

        // Act
        boolean result = jwtService.isTokenValid(
                token,
                "wrong@example.com"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    void extractEmail_shouldThrowExceptionForExpiredToken() {

        // Arrange: Negative expiration se expired token banega
        JwtService expiredJwtService = new JwtService(
                SECRET,
                -1_000
        );

        String expiredToken =
                expiredJwtService.generateToken(EMAIL);

        // Act & Assert
        assertThrows(
                ExpiredJwtException.class,
                () -> expiredJwtService.extractEmail(expiredToken)
        );
    }
}