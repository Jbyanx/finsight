package dev.jbyanx.finsight.domain.telegram.service;

import dev.jbyanx.finsight.domain.telegram.dto.LinkCodeResponse;
import dev.jbyanx.finsight.domain.telegram.dto.TelegramLinkResponse;

public interface TelegramLinkService {

    LinkCodeResponse generateLinkCode(String userId);

    TelegramLinkResponse linkAccount(String code, Long chatId);

    String resolveUserId(Long chatId); // null si no está vinculado

    void unlink(String userId);
}