package dev.jbyanx.finsight.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato de dinero para los mensajes que arma el CÓDIGO (no el modelo).
 *
 * Motivo: los previews del modelo mostraban "$200.000" pero los mensajes de
 * confirmación del executor mostraban "200000" crudo. Unificamos acá.
 *
 * Convención COP: separador de miles con punto y sin decimales
 * (el peso colombiano no se usa con centavos en la práctica).
 */
public final class Money {

    private Money() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) return "$0";

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        return "$" + formatter.format(amount.setScale(0, RoundingMode.HALF_UP));
    }
}