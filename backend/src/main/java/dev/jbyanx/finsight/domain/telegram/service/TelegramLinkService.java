package dev.jbyanx.finsight.domain.telegram.service;

import dev.jbyanx.finsight.domain.telegram.dto.LinkCodeResponse;
import dev.jbyanx.finsight.domain.telegram.dto.TelegramLinkResponse;

public interface TelegramLinkService {

    LinkCodeResponse generateLinkCode(String userId);

    TelegramLinkResponse linkAccount(String code, Long chatId);

    String resolveUserId(Long chatId); // null si no está vinculado

    /** Desvincula desde la web (el usuario ya está autenticado). */
    void unlink(String userId);

    /**
     * Desvincula desde el bot, donde solo conocemos el chatId.
     *
     * @return true si había un vínculo y se eliminó; false si no había nada.
     */
    boolean unlinkByChatId(Long chatId);
}