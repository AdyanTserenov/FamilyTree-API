package com.project.familytree.security;

import com.project.familytree.services.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {
    private final JwtCore jwtCore;
    private final UserService userService;
    private final SecurityResponseUtil securityResponseUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String headerAuth = request.getHeader("Authorization");
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                String jwt = headerAuth.substring(7);
                try {
                    String email = jwtCore.getEmailFromJwt(jwt);
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (ExpiredJwtException e) {
                    securityResponseUtil.sendError(response, HttpStatus.UNAUTHORIZED, "Токен просрочен");
                    return;
                } catch (io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.SignatureException e) {
                    securityResponseUtil.sendError(response, HttpStatus.UNAUTHORIZED, "Некорректный токен");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            securityResponseUtil.sendError(response, HttpStatus.UNAUTHORIZED, "Ошибка аутентификации");
        }
    }
}
