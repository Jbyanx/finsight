package dev.jbyanx.finsight.domain.savings.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SavingsAccountPatchRequest {
    private String nickname;
    private Boolean isShared;
    private String sharedOwnerName;
}