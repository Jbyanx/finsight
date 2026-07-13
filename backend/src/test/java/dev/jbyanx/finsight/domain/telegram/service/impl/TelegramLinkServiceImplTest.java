package dev.jbyanx.finsight.domain.telegram.service.impl;

import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
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
    @Mock
    private ConversationService conversationService;

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

    /** Código válido, sin usar y sin vencer. */
    private LinkCode validCode(String code) {
        return LinkCode.builder()
                .code(code)
                .user(mockUser)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
    }

    private TelegramLink linkTo(long chatId) {
        return TelegramLink.builder()
                .user(mockUser)
                .chatId(chatId)
                .build();
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
        LinkCode code = validCode("ABC234");

        when(linkCodeRepository.findByCodeAndUsedFalse("ABC234"))
                .thenReturn(Optional.of(code));
        when(telegramLinkRepository.existsByChatId(12345L)).thenReturn(false);
        when(telegramLinkRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        TelegramLinkResponse response = telegramLinkService.linkAccount("ABC234", 12345L);

        assertTrue(response.isLinked());
        assertEquals("Jabes", response.getUserName());
        assertTrue(code.isUsed()); // el código quedó consumido
        verify(linkCodeRepository).save(code);
        verify(telegramLinkRepository).save(any(TelegramLink.class));
    }

    @Test
    void linkAccount_PurgesStaleConversationFromPreviousOwner() {
        // El chat 123 pudo haber pertenecido a OTRA cuenta antes. Su historial
        // financiero no debe filtrarse a la cuenta que se vincula ahora.
        when(linkCodeRepository.findByCodeAndUsedFalse("ABC234"))
                .thenReturn(Optional.of(validCode("ABC234")));
        when(telegramLinkRepository.existsByChatId(123L)).thenReturn(false);
        when(telegramLinkRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        telegramLinkService.linkAccount("ABC234", 123L);

        verify(conversationService).deleteByChatId("123");
    }

    @Test
    void linkAccount_WhenUserAlreadyLinked_ThrowsInsteadOfConstraintViolation() {
        // user_id es UNIQUE en la tabla: sin esta validación, vincular un segundo
        // chat al mismo usuario reventaba con un 500 por violación de constraint.
        when(linkCodeRepository.findByCodeAndUsedFalse("ABC234"))
                .thenReturn(Optional.of(validCode("ABC234")));
        when(telegramLinkRepository.existsByChatId(456L)).thenReturn(false);
        when(telegramLinkRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(linkTo(111L)));

        assertThrows(UnauthorizedException.class,
                () -> telegramLinkService.linkAccount("ABC234", 456L));

        verify(telegramLinkRepository, never()).save(any());
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
        when(linkCodeRepository.findByCodeAndUsedFalse("OKK234"))
                .thenReturn(Optional.of(validCode("OKK234")));
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
        when(telegramLinkRepository.findByChatId(111L))
                .thenReturn(Optional.of(linkTo(111L)));

        assertEquals("user-1", telegramLinkService.resolveUserId(111L));
    }

    @Test
    void resolveUserId_NotLinked_ReturnsNull() {
        when(telegramLinkRepository.findByChatId(222L)).thenReturn(Optional.empty());

        assertNull(telegramLinkService.resolveUserId(222L));
    }

    // ---------- unlink (web) ----------

    @Test
    void unlink_Exists_DeletesLinkAndPurgesConversation() {
        TelegramLink link = linkTo(111L);
        when(telegramLinkRepository.findByUserId("user-1")).thenReturn(Optional.of(link));

        telegramLinkService.unlink("user-1");

        verify(telegramLinkRepository).delete(link);
        // El historial de Mongo NO sobrevive a la desvinculación.
        verify(conversationService).deleteByChatId("111");
    }

    @Test
    void unlink_NotExists_DoesNothing() {
        when(telegramLinkRepository.findByUserId("user-x")).thenReturn(Optional.empty());

        telegramLinkService.unlink("user-x");

        verify(telegramLinkRepository, never()).delete(any());
        verify(conversationService, never()).deleteByChatId(any());
    }

    // ---------- unlinkByChatId (bot) ----------

    @Test
    void unlinkByChatId_WhenLinked_DeletesLinkAndPurgesConversation() {
        TelegramLink link = linkTo(123L);
        when(telegramLinkRepository.findByChatId(123L)).thenReturn(Optional.of(link));

        boolean result = telegramLinkService.unlinkByChatId(123L);

        assertTrue(result);
        verify(telegramLinkRepository).delete(link);
        verify(conversationService).deleteByChatId("123");
    }

    @Test
    void unlinkByChatId_WhenNotLinked_ReturnsFalseAndTouchesNothing() {
        when(telegramLinkRepository.findByChatId(999L)).thenReturn(Optional.empty());

        assertFalse(telegramLinkService.unlinkByChatId(999L));

        verify(telegramLinkRepository, never()).delete(any());
        verify(conversationService, never()).deleteByChatId(any());
    }
}