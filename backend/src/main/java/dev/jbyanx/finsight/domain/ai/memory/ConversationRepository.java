package dev.jbyanx.finsight.domain.ai.memory;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio Mongo de conversaciones. El id es el chatId (String).
 */
public interface ConversationRepository extends MongoRepository<Conversation, String> {
}