package dev.jbyanx.finsight.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class SessionDTO {
    private String id;
    private String deviceInfo;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}