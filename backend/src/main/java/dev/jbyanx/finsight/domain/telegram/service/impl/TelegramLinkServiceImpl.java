package dev.jbyanx.finsight.domain.telegram.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.telegram.dto.LinkCodeResponse;
import dev.jbyanx.finsight.domain.telegram.dto.TelegramLinkResponse;
import dev.jbyanx.finsight.domain.telegram.entity.LinkCode;
import dev.jbyanx.finsight.domain.telegram.entity.TelegramLink;
import dev.jbyanx.finsight.domain.telegram.repository.LinkCodeRepository;
import dev.jbyanx.finsight.domain.telegram.repository.TelegramLinkRepository;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TelegramLinkServiceImpl implements TelegramLinkService {

    private final TelegramLinkRepository telegramLinkRepository;
    private final LinkCodeRepository linkCodeRepository;
    private final UserRepository userRepository;

    @Value("${telegram.link-code.chars}")
    private String CHARS; // caracteres para el código (sin O,0,I,1)

    @Value("${telegram.link-code.expiration-minutes}")
    private int expirationMinutes;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public LinkCodeResponse generateLinkCode(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String code = generateRandomCode();

        linkCodeRepository.save(LinkCode.builder()
                .code(code)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .build());

        return LinkCodeResponse.builder()
                .code(code)
                .expiresInSeconds(expirationMinutes * 60)
                .build();
    }

    @Override
    @Transactional
    public TelegramLinkResponse linkAccount(String code, Long chatId) {
        LinkCode linkCode = linkCodeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired code"));

        if (!linkCode.isValid()) {
            throw new UnauthorizedException("Code has expired");
        }

        if (telegramLinkRepository.existsByChatId(chatId)) {
            throw new UnauthorizedException(
                    "This Telegram account is already linked to another user");
        }

        linkCode.setUsed(true);
        linkCodeRepository.save(linkCode);

        User user = linkCode.getUser();

        telegramLinkRepository.save(TelegramLink.builder()
                .user(user)
                .chatId(chatId)
                .build());

        return TelegramLinkResponse.builder()
                .linked(true)
                .userName(user.getName())
                .build();
    }

    @Override
    public String resolveUserId(Long chatId) {
        return telegramLinkRepository.findByChatId(chatId)
                .map(link -> link.getUser().getId())
                .orElse(null);
    }

    @Override
    @Transactional
    public void unlink(String userId) {
        telegramLinkRepository.findByUserId(userId)
                .ifPresent(telegramLinkRepository::delete);
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}