package dev.jbyanx.finsight.domain.ai.memory;

import dev.jbyanx.finsight.domain.ai.pending.PendingAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Gestiona la conversación por chat: historial y acción pendiente.
 *
 * Aísla el acceso a Mongo del resto de la lógica de IA. Si un día cambia el
 * almacenamiento, solo se toca acá.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int MAX_HISTORY = 20;

    private final ConversationRepository repository;

    public Conversation getOrCreate(String chatId, String userId) {
        return repository.findById(chatId).orElseGet(() ->
                Conversation.builder()
                        .chatId(chatId)
                        .userId(userId)
                        .updatedAt(Instant.now())
                        .build());
    }

    public void recordExchange(Conversation conv, String userMsg, String assistantMsg) {
        conv.addMessage("user", userMsg);
        conv.addMessage("assistant", assistantMsg);
        conv.trimTo(MAX_HISTORY);
        repository.save(conv);
    }

    public void setPending(Conversation conv, PendingAction action) {
        conv.setPendingAction(action);
        conv.setUpdatedAt(Instant.now());
        repository.save(conv);
    }

    public void clearPending(Conversation conv) {
        conv.setPendingAction(null);
        conv.setUpdatedAt(Instant.now());
        repository.save(conv);
    }
}