package dev.jbyanx.finsight.domain.telegram.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LinkCodeResponse {
    private String code;
    private int expiresInSeconds;
}