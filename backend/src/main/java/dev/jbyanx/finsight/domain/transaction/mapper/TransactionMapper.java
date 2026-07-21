package dev.jbyanx.finsight.domain.transaction.mapper;

import dev.jbyanx.finsight.domain.transaction.dto.TransactionResponse;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "originName", expression = "java(mapOriginName(transaction))")
    @Mapping(target = "pocketName", expression = "java(mapPocketName(transaction))")
    TransactionResponse toResponse(Transaction transaction);

    default String mapPocketName(Transaction transaction) {
        return transaction.getPocket() != null ? transaction.getPocket().getName() : null;
    }

    default String mapOriginName(Transaction transaction) {
        return switch (transaction.getOriginType()) {
            case CASH -> "Efectivo";
            case CREDIT_CARD -> transaction.getCreditCard() != null
                    ? transaction.getCreditCard().getNickname() != null
                    ? transaction.getCreditCard().getNickname()
                    : transaction.getCreditCard().getInstitution()
                    : null;
            case SAVINGS -> transaction.getSavingsAccount() != null
                    ? transaction.getSavingsAccount().getNickname() != null
                    ? transaction.getSavingsAccount().getNickname()
                    : transaction.getSavingsAccount().getInstitution()
                    : null;
        };
    }
}