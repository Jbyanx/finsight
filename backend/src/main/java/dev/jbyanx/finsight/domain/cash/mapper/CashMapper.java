package dev.jbyanx.finsight.domain.cash.mapper;

import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CashMapper {
    CashResponse toResponse(Cash cash);
}