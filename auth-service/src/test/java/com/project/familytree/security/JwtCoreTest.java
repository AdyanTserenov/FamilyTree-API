package com.project.familytree.security;

import com.project.familytree.impls.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtCoreTest {

    private JwtCore jwtCore;

    private static final String SECRET =
            "testSecretKeyThatIsLongEnoughForHMACSHA256Algorithm123456789";
    private static final int LIFETIME_MS = 3_600_000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtCore = new JwtCore();
        ReflectionTestUtils.setField(jwtCore, "secret", SECRET);
        ReflectionTestUtils.setField(jwtCore, "lifetime", LIFETIME_MS);
    }

    private Authentication buildAuthentication(String email) {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, email, "hashed", null, null, Collections.emptyList()
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
    }

    // ─── generateToken ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken: содержит email как subject")
    void generateToken_containsEmailAsSubject() {
        Authentication auth = buildAuthentication("ivan@test.com");

        String token = jwtCore.generateToken(auth);

        assertThat(token).isNotBlank();

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getSubject()).isEqualTo("ivan@test.com");
    }

    @Test
    @DisplayName("generateToken: токен не просрочен сразу после создания")
    void generateToken_isNotExpiredImmediately() {
        Authentication auth = buildAuthentication("ivan@test.com");

        String token = jwtCore.generateToken(auth);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("generateToken: expiration примерно через 1 час")
    void generateToken_expirationIsApproximatelyOneHour() {
        Authentication auth = buildAuthentication("ivan@test.com");

        long before = System.currentTimeMillis();
        String token = jwtCore.generateToken(auth);
        long after = System.currentTimeMillis();

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        long expMs = claims.getExpiration().getTime();
        // Expiration должен быть в диапазоне [before + lifetime, after + lifetime]
        assertThat(expMs).isBetween(before + LIFETIME_MS - 1000, after + LIFETIME_MS + 1000);
    }

    // ─── getEmailFromJwt ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getEmailFromJwt: возвращает email из токена")
    void getEmailFromJwt_returnsCorrectEmail() {
        Authentication auth = buildAuthentication("test@example.com");
        String token = jwtCore.generateToken(auth);

        String email = jwtCore.getEmailFromJwt(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("getEmailFromJwt: round-trip для разных email")
    void getEmailFromJwt_roundTripForDifferentEmails() {
        String[] emails = {"user1@test.com", "admin@company.org", "test+tag@mail.ru"};

        for (String email : emails) {
            Authentication auth = buildAuthentication(email);
            String token = jwtCore.generateToken(auth);
            assertThat(jwtCore.getEmailFromJwt(token)).isEqualTo(email);
        }
    }
}
