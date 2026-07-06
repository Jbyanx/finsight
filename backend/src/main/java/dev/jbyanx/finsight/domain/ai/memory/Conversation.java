package dev.jbyanx.finsight.domain.ai.memory;

import dev.jbyanx.finsight.domain.ai.pending.PendingAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento de conversación del bot, en MongoDB (colección conversationsBot).
 *
 * Guarda, por chat de Telegram:
 *   - messages: historial reciente (memoria conversacional para el modelo).
 *   - pendingAction: la mutación propuesta que espera confirmación (o null).
 *
 * MongoDB encaja acá porque el historial es no estructurado y varía en forma
 * (decisión de arquitectura ya documentada).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "conversationsBot")
public class Conversation {

    /** id del documento = chatId de Telegram (uno por chat). */
    @Id
    private String chatId;

    /** userId dueño del chat (resuelto al vincular). Para trazabilidad. */
    private String userId;

    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    /** Acción de escritura esperando confirmación. null si no hay ninguna. */
    private PendingAction pendingAction;

    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;   // "user" | "assistant"
        private String content;
        private Instant at;
    }

    public void addMessage(String role, String content) {
        this.messages.add(Message.builder()
                .role(role).content(content).at(Instant.now()).build());
        this.updatedAt = Instant.now();
    }

    /** Mantiene solo los últimos N mensajes para no crecer sin límite. */
    public void trimTo(int max) {
        if (messages.size() > max) {
            messages = new ArrayList<>(messages.subList(messages.size() - max, messages.size()));
        }
    }
}