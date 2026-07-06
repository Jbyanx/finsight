package dev.jbyanx.finsight.domain.ai.pending;

import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmationHandlerTest {

    @Mock private ConversationService conversationService;
    @Mock private PendingActionExecutor executor;

    private ConfirmationHandler handler;

    private Conversation withPending(PendingAction action) {
        Conversation conv = Conversation.builder()
                .chatId("123").userId("user-1").build();
        conv.setPendingAction(action);
        return conv;
    }

    // El handler usa @RequiredArgsConstructor; lo instanciamos manual para inyectar mocks.
    private void init() {
        handler = new ConfirmationHandler(conversationService, executor);
    }

    @Test
    void handleCallback_Confirm_ExecutesAndClears() {
        init();
        PendingAction action = PendingAction.of(PendingActionExecutor.PAY_DEBT, null)
                .with("debtId", "d-1").with("amount", "250000");
        Conversation conv = withPending(action);
        when(conversationService.getOrCreate("123", "user-1")).thenReturn(conv);
        when(executor.execute("user-1", action)).thenReturn("Pago registrado.");

        String result = handler.handleCallback("123", "user-1", CallbackActions.CONFIRM);

        assertEquals("Pago registrado.", result);
        verify(executor).execute("user-1", action);
        verify(conversationService).clearPending(conv);
    }

    @Test
    void handleCallback_Cancel_DoesNotExecute() {
        init();
        Conversation conv = withPending(PendingAction.of(PendingActionExecutor.PAY_DEBT, null));
        when(conversationService.getOrCreate("123", "user-1")).thenReturn(conv);

        String result = handler.handleCallback("123", "user-1", CallbackActions.CANCEL);

        assertTrue(result.toLowerCase().contains("cancelado"));
        verify(executor, never()).execute(any(), any());
        verify(conversationService).clearPending(conv);
    }

    @Test
    void handleCallback_NoPending_ReturnsExpiredMessage() {
        init();
        Conversation conv = Conversation.builder().chatId("123").userId("user-1").build();
        conv.setPendingAction(null);
        when(conversationService.getOrCreate("123", "user-1")).thenReturn(conv);

        String result = handler.handleCallback("123", "user-1", CallbackActions.CONFIRM);

        assertTrue(result.toLowerCase().contains("ya no está disponible"));
        verify(executor, never()).execute(any(), any());
    }
}