package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashToolsTest {

    @Mock
    private CashService cashService;

    @InjectMocks
    private CashTools cashTools;

    @Test
    void getCashBalance_WithUserIdInContext_ReturnsBalance() {
        when(cashService.getByUser("user-1")).thenReturn(
                CashResponse.builder().balance(new BigDecimal("45000")).build());

        ToolContext ctx = new ToolContext(Map.of(ToolContextKeys.USER_ID, "user-1"));
        String result = cashTools.getCashBalance(ctx);

        assertTrue(result.contains("45000"));
        verify(cashService).getByUser("user-1");
    }

    @Test
    void getCashBalance_NoUserIdInContext_DoesNotQuery() {
        ToolContext ctx = new ToolContext(Map.of());

        String result = cashTools.getCashBalance(ctx);

        assertTrue(result.toLowerCase().contains("no se pudo identificar"));
        verify(cashService, never()).getByUser(anyString());
    }
}