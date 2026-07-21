package dev.jbyanx.finsight.shared.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void format_Thousands_UsesDotSeparator() {
        assertEquals("$200.000", Money.format(new BigDecimal("200000")));
    }

    @Test
    void format_Millions_UsesDotSeparators() {
        assertEquals("$2.600.000", Money.format(new BigDecimal("2600000")));
    }

    @Test
    void format_SmallAmount_NoSeparator() {
        assertEquals("$500", Money.format(new BigDecimal("500")));
    }

    @Test
    void format_Zero_ReturnsZero() {
        assertEquals("$0", Money.format(BigDecimal.ZERO));
    }

    @Test
    void format_WithDecimals_RoundsToWholePeso() {
        assertEquals("$1.235", Money.format(new BigDecimal("1234.56")));
    }

    @Test
    void format_Null_ReturnsZero() {
        assertEquals("$0", Money.format(null));
    }
}