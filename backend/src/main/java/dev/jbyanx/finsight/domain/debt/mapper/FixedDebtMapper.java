package dev.jbyanx.finsight.domain.debt.mapper;

import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.entity.FixedDebt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FixedDebtMapper {

    @Mapping(target = "paidAmount",
            expression = "java(debt.getPaidAmount())")
    @Mapping(target = "progressPercentage",
            expression = "java(debt.getProgressPercentage())")
    @Mapping(target = "paidOff",
            expression = "java(debt.isPaidOff())")
    FixedDebtResponse toResponse(FixedDebt debt);

    List<FixedDebtResponse> toResponseList(List<FixedDebt> debts);
}