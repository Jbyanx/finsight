package dev.jbyanx.finsight.domain.telegram.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TelegramLinkResponse {
    private boolean linked;
    private String userName;
}