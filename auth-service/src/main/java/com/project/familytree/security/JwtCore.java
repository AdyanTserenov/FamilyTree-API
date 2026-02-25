package com.project.familytree.security;

import com.project.familytree.impls.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT utility for auth-service.
 * Uses the same secret and algorithm (HS256) as family-tree-auth-starter's JwtUtils
 * so that tokens issued here are accepted by tree-service.
 *
 * Property: family.auth.jwt.secret (same key used by tree-service via the starter)
 */
@Component
public class JwtCore {

    @Value("${family.auth.jwt.secret:mySuperSecretKeyThatIsLongEnoughForHMACSHA256Algorithm123456789}")
    private String secret;

    @Value("${family.auth.jwt.lifetime:3600000}")
    private int lifetime;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + lifetime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmailFromJwt(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
