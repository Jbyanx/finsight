package dev.jbyanx.finsight.domain.savings.mapper;

import dev.jbyanx.finsight.domain.savings.dto.PocketResponse;
import dev.jbyanx.finsight.domain.savings.dto.SavingsAccountResponse;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SavingsAccountMapper {

    @Mapping(target = "availableBalance",
            expression = "java(account.getAvailableBalance())")
    @Mapping(target = "pockets",
            expression = "java(mapActivePockets(account))")
    SavingsAccountResponse toResponse(SavingsAccount account);

    List<SavingsAccountResponse> toResponseList(List<SavingsAccount> accounts);

    @Mapping(target = "progressPercentage",
            expression = "java(pocket.getProgressPercentage())")
    PocketResponse toPocketResponse(Pocket pocket);

    default java.util.List<PocketResponse> mapActivePockets(SavingsAccount account) {
        return account.getPockets().stream()
                .filter(Pocket::isActive)
                .map(this::toPocketResponse)
                .toList();
    }
}