package com.project.familytree.services;

import com.project.familytree.exceptions.*;
import com.project.familytree.impls.TokenDetails;
import com.project.familytree.impls.TokenType;
import com.project.familytree.models.ResetToken;
import com.project.familytree.models.Token;
import com.project.familytree.models.VerifyToken;
import com.project.familytree.repositories.TokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    // ─── createVerifyToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createVerifyToken: отменяет старые токены и сохраняет новый")
    void createVerifyToken_cancelsOldAndSavesNew() {
        when(tokenRepository.findActiveVerifyTokensByUserId(1L))
                .thenReturn(List.of());
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = tokenService.createVerifyToken(1L);

        assertThat(rawToken).isNotBlank();
        verify(tokenRepository).findActiveVerifyTokensByUserId(1L);
        verify(tokenRepository).save(any(VerifyToken.class));
    }

    @Test
    @DisplayName("createVerifyToken: отменяет существующие активные токены")
    void createVerifyToken_cancelsExistingActiveTokens() {
        VerifyToken existing = new VerifyToken(1L, Duration.ofHours(24));
        when(tokenRepository.findActiveVerifyTokensByUserId(1L))
                .thenReturn(List.of(existing));
        when(tokenRepository.saveAll(anyList())).thenReturn(List.of(existing));
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        tokenService.createVerifyToken(1L);

        verify(tokenRepository).saveAll(argThat(tokens ->
                ((List<?>) tokens).size() == 1
        ));
        assertThat(existing.getDetails().isConsumed()).isTrue();
    }

    // ─── createResetToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createResetToken: отменяет старые токены и сохраняет новый")
    void createResetToken_cancelsOldAndSavesNew() {
        when(tokenRepository.findActiveResetTokensByUserId(1L))
                .thenReturn(List.of());
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = tokenService.createResetToken(1L);

        assertThat(rawToken).isNotBlank();
        verify(tokenRepository).save(any(ResetToken.class));
    }

    // ─── validateToken ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken: возвращает userId для валидного токена")
    void validateToken_returnsUserIdForValidToken() {
        VerifyToken token = new VerifyToken(42L, Duration.ofHours(24));
        String rawToken = token.getRawToken();
        String hash = Token.hash(rawToken);

        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        Long userId = tokenService.validateToken(rawToken, TokenType.VERIFY);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("validateToken: бросает InvalidTokenException для пустого токена")
    void validateToken_throwsForBlankToken() {
        assertThatThrownBy(() -> tokenService.validateToken("", TokenType.VERIFY))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("пуст");

        assertThatThrownBy(() -> tokenService.validateToken(null, TokenType.VERIFY))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("validateToken: бросает TokenNotFoundException если токен не найден")
    void validateToken_throwsIfNotFound() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.validateToken("nonexistent-token", TokenType.VERIFY))
                .isInstanceOf(TokenNotFoundException.class);
    }

    @Test
    @DisplayName("validateToken: бросает InvalidTokenException при несовпадении типа")
    void validateToken_throwsIfWrongType() {
        ResetToken token = new ResetToken(1L, Duration.ofHours(24));
        String rawToken = token.getRawToken();
        String hash = Token.hash(rawToken);

        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        // Передаём VERIFY, но токен типа RESET
        assertThatThrownBy(() -> tokenService.validateToken(rawToken, TokenType.VERIFY))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("тип");
    }

    @Test
    @DisplayName("validateToken: бросает TokenAlreadyUsedException если токен уже использован")
    void validateToken_throwsIfConsumed() {
        VerifyToken token = new VerifyToken(1L, Duration.ofHours(24));
        token.getDetails().setConsumed(true);
        String rawToken = token.getRawToken();
        String hash = Token.hash(rawToken);

        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.validateToken(rawToken, TokenType.VERIFY))
                .isInstanceOf(TokenAlreadyUsedException.class);
    }

    @Test
    @DisplayName("validateToken: бросает TokenExpiredException если токен просрочен")
    void validateToken_throwsIfExpired() {
        // Создаём токен с TTL в прошлом через TokenDetails напрямую
        // Используем ReflectionTestUtils-подобный подход через конструктор VerifyToken
        // и подменяем expiresAt через мок-объект
        VerifyToken token = mock(VerifyToken.class);
        TokenDetails details = TokenDetails.of(
                "somehash", 1L,
                Instant.now().minus(Duration.ofHours(48)),
                Instant.now().minus(Duration.ofHours(24)), // уже истёк
                false
        );
        when(token.getDetails()).thenReturn(details);
        when(token.getType()).thenReturn(TokenType.VERIFY);

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.validateToken("any-raw-token", TokenType.VERIFY))
                .isInstanceOf(TokenExpiredException.class);
    }

    // ─── consumeToken ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("consumeToken: помечает токен как использованный")
    void consumeToken_setsConsumedTrue() {
        VerifyToken token = new VerifyToken(1L, Duration.ofHours(24));
        String rawToken = token.getRawToken();
        String hash = Token.hash(rawToken);

        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        tokenService.consumeToken(rawToken);

        assertThat(token.getDetails().isConsumed()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("consumeToken: бросает TokenNotFoundException если токен не найден")
    void consumeToken_throwsIfNotFound() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.consumeToken("nonexistent"))
                .isInstanceOf(TokenNotFoundException.class);
    }

    // ─── cancelActiveTokens ───────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelActiveTokens: помечает все активные токены как использованные")
    void cancelActiveTokens_marksAllConsumed() {
        VerifyToken t1 = new VerifyToken(1L, Duration.ofHours(24));
        VerifyToken t2 = new VerifyToken(1L, Duration.ofHours(24));

        when(tokenRepository.findActiveVerifyTokensByUserId(1L))
                .thenReturn(List.of(t1, t2));
        when(tokenRepository.saveAll(anyList())).thenReturn(List.of(t1, t2));

        tokenService.cancelActiveTokens(1L, TokenType.VERIFY);

        assertThat(t1.getDetails().isConsumed()).isTrue();
        assertThat(t2.getDetails().isConsumed()).isTrue();

        ArgumentCaptor<List<Token>> captor = ArgumentCaptor.forClass(List.class);
        verify(tokenRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("cancelActiveTokens: не вызывает saveAll если нет активных токенов")
    void cancelActiveTokens_doesNotSaveIfEmpty() {
        when(tokenRepository.findActiveVerifyTokensByUserId(1L))
                .thenReturn(List.of());

        tokenService.cancelActiveTokens(1L, TokenType.VERIFY);

        verify(tokenRepository, never()).saveAll(anyList());
    }
}
