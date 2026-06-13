package dev.jbyanx.finsight.domain.income.mapper;

import dev.jbyanx.finsight.domain.income.dto.*;
import dev.jbyanx.finsight.domain.income.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface IncomeMapper {

    IncomeSourceResponse toSourceResponse(IncomeSource source);

    List<IncomeSourceResponse> toSourceResponseList(List<IncomeSource> sources);

    @Mapping(target = "incomeSourceDescription",
            expression = "java(entry.getIncomeSource() != null ? entry.getIncomeSource().getDescription() : null)")
    @Mapping(target = "destinationName",
            expression = "java(mapDestinationName(entry))")
    IncomeEntryResponse toEntryResponse(IncomeEntry entry);

    List<IncomeEntryResponse> toEntryResponseList(List<IncomeEntry> entries);

    @Mapping(target = "incomeSourceDescription",
            expression = "java(expense.getIncomeSource() != null ? expense.getIncomeSource().getDescription() : null)")
    OperationalExpenseResponse toExpenseResponse(OperationalExpense expense);

    List<OperationalExpenseResponse> toExpenseResponseList(List<OperationalExpense> expenses);

    default String mapDestinationName(IncomeEntry entry) {
        return switch (entry.getDestinationType()) {
            case CASH -> "Efectivo";
            case SAVINGS -> entry.getSavingsAccount() != null
                    ? entry.getSavingsAccount().getNickname() != null
                    ? entry.getSavingsAccount().getNickname()
                    : entry.getSavingsAccount().getInstitution()
                    : null;
            case POCKET -> entry.getPocket() != null
                    ? entry.getPocket().getName()
                    : null;
        };
    }
}