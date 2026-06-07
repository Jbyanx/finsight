package dev.jbyanx.finsight.shared.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}