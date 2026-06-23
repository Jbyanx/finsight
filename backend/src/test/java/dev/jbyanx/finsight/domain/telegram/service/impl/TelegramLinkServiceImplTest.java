package dev.jbyanx.finsight.domain.telegram.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.telegram.dto.LinkCodeResponse;
import dev.jbyanx.finsight.domain.telegram.dto.TelegramLinkResponse;
import dev.jbyanx.finsight.domain.telegram.entity.LinkCode;
import dev.jbyanx.finsight.domain.telegram.entity.TelegramLink;
import dev.jbyanx.finsight.domain.telegram.repository.LinkCodeRepository;
import dev.jbyanx.finsight.domain.telegram.repository.TelegramLinkRepository;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramLinkServiceImplTest {

    @Mock
    private TelegramLinkRepository telegramLinkRepository;
    @Mock
    private LinkCodeRepository linkCodeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TelegramLinkServiceImpl telegramLinkService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id("user-1").name("Jabes").build();

        // Los campos @Value no se inyectan con @InjectMocks — se setean a mano.
        ReflectionTestUtils.setField(telegramLinkService, "CHARS",
                "ABCDEFGHJKLMNPQRSTUVWXYZ23456789");
        ReflectionTestUtils.setField(telegramLinkService, "expirationMinutes", 5);
    }

    // ---------- generateLinkCode ----------

    @Test
    void generateLinkCode_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));

        LinkCodeResponse response = telegramLinkService.generateLinkCode("user-1");

        assertNotNull(response);
        assertEquals(6, response.getCode().length());
        assertEquals(5 * 60, response.getExpiresInSeconds());
        verify(linkCodeRepository).save(any(LinkCode.class));
    }

    @Test
    void generateLinkCode_UserNotFound_ThrowsException() {
        when(userRepository.findById("user-bad")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> telegramLinkService.generateLinkCode("user-bad"));

        verify(linkCodeRepository, never()).save(any());
    }

    // ---------- linkAccount ----------

    @Test
    void linkAccount_Success() {
        LinkCode validCode = LinkCode.builder()
                .code("ABC234")
                .user(mockUser)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        when(linkCodeRepository.findByCodeAndUsedFalse("ABC234"))
                .thenReturn(Optional.of(validCode));
        when(telegramLinkRepository.existsByChatId(12345L)).thenReturn(false);

        TelegramLinkResponse response = telegramLinkService.linkAccount("ABC234", 12345L);

        assertTrue(response.isLinked());
        assertEquals("Jabes", response.getUserName());
        assertTrue(validCode.isUsed()); // el código quedó consumido
        verify(linkCodeRepository).save(validCode);
        verify(telegramLinkRepository).save(any(TelegramLink.class));
    }

    @Test
    void linkAccount_CodeNotFound_ThrowsException() {
        when(linkCodeRepository.findByCodeAndUsedFalse("BADXXX"))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> telegramLinkService.linkAccount("BADXXX", 12345L));

        verify(telegramLinkRepository, never()).save(any());
    }

    @Test
    void linkAccount_CodeExpired_ThrowsException() {
        LinkCode expiredCode = LinkCode.builder()
                .code("EXP234")
                .user(mockUser)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // ya venció
                .used(false)
                .build();

        when(linkCodeRepository.findByCodeAndUsedFalse("EXP234"))
                .thenReturn(Optional.of(expiredCode));

        assertThrows(UnauthorizedException.class,
                () -> telegramLinkService.linkAccount("EXP234", 12345L));

        verify(telegramLinkRepository, never()).save(any());
    }

    @Test
    void linkAccount_ChatAlreadyLinked_ThrowsException() {
        LinkCode validCode = LinkCode.builder()
                .code("OKK234")
                .user(mockUser)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        when(linkCodeRepository.findByCodeAndUsedFalse("OKK234"))
                .thenReturn(Optional.of(validCode));
        when(telegramLinkRepository.existsByChatId(999L)).thenReturn(true);

        assertThrows(UnauthorizedException.class,
                () -> telegramLinkService.linkAccount("OKK234", 999L));

        // No se consume el código ni se crea el vínculo si el chat ya estaba tomado.
        verify(linkCodeRepository, never()).save(any());
        verify(telegramLinkRepository, never()).save(any());
    }

    // ---------- resolveUserId ----------

    @Test
    void resolveUserId_Linked_ReturnsUserId() {
        TelegramLink link = TelegramLink.builder()
                .user(mockUser)
                .chatId(111L)
                .build();

        when(telegramLinkRepository.findByChatId(111L)).thenReturn(Optional.of(link));

        assertEquals("user-1", telegramLinkService.resolveUserId(111L));
    }

    @Test
    void resolveUserId_NotLinked_ReturnsNull() {
        when(telegramLinkRepository.findByChatId(222L)).thenReturn(Optional.empty());

        assertNull(telegramLinkService.resolveUserId(222L));
    }

    // ---------- unlink ----------

    @Test
    void unlink_Exists_DeletesLink() {
        TelegramLink link = TelegramLink.builder()
                .user(mockUser)
                .chatId(111L)
                .build();

        when(telegramLinkRepository.findByUserId("user-1")).thenReturn(Optional.of(link));

        telegramLinkService.unlink("user-1");

        verify(telegramLinkRepository).delete(link);
    }

    @Test
    void unlink_NotExists_DoesNothing() {
        when(telegramLinkRepository.findByUserId("user-x")).thenReturn(Optional.empty());

        telegramLinkService.unlink("user-x");

        verify(telegramLinkRepository, never()).delete(any());
    }
}