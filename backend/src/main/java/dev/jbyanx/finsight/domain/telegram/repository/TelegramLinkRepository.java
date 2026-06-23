package dev.jbyanx.finsight.domain.telegram.repository;

import dev.jbyanx.finsight.domain.telegram.entity.TelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TelegramLinkRepository extends JpaRepository<TelegramLink, String> {
    Optional<TelegramLink> findByChatId(Long chatId);
    Optional<TelegramLink> findByUserId(String userId);
    boolean existsByChatId(Long chatId);
}