package dev.jbyanx.finsight.domain.card.mapper;

import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.dto.UtilizationStatus;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    @Mapping(target = "availableAmount", expression = "java(card.getCreditLimit().subtract(card.getUsedAmount()))")
    @Mapping(target = "utilizationPercentage", expression = "java(card.getUtilizationPercentage())")
    @Mapping(target = "utilizationStatus", expression = "java(mapStatus(card))")
    CreditCardResponse toResponse(CreditCard card);

    List<CreditCardResponse> toResponseList(List<CreditCard> cards);

    default UtilizationStatus mapStatus(CreditCard card) {
        if (card.isRedAlert()) return UtilizationStatus.RED;
        if (card.isYellowAlert()) return UtilizationStatus.YELLOW;
        return UtilizationStatus.SAFE;
    }
}