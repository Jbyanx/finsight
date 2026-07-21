package dev.jbyanx.finsight.domain.telegram.bot;

/**
 * Convierte el texto (del modelo o de los handlers) al HTML de Telegram.
 *
 * POR QUÉ HTML Y NO MARKDOWN:
 * El Markdown legacy de Telegram rechaza el mensaje completo si las entidades
 * quedan desbalanceadas (un asterisco suelto basta), y el modelo genera texto
 * libre: no podemos garantizar balance. Cuando falla, caíamos al fallback de
 * texto plano y el usuario veía los asteriscos crudos (bug del /help).
 *
 * Con HTML el resultado es determinista: escapamos TODO primero, y solo después
 * emitimos etiquetas <b> a partir de coincidencias YA balanceadas. Cualquier
 * asterisco suelto que sobre se elimina. Es imposible producir una entidad
 * inválida, así que el envío no puede fallar por formato.
 */
public final class TelegramFormatter {

    private TelegramFormatter() {
    }

    public static String toHtml(String text) {
        if (text == null) return "";

        // 1) Escapamos primero: a partir de acá, ningún carácter del contenido
        //    puede ser interpretado como etiqueta.
        String result = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        // 2) Encabezados Markdown (# Título) -> negrita. Telegram no tiene headers.
        result = result.replaceAll("(?m)^\\s*#{1,6}\\s*(.+?)\\s*$", "<b>$1</b>");

        // 3) Negrita doble (**texto**), que es lo que pedimos al modelo.
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");

        // 4) Negrita simple (*texto*) por si el modelo la emite igual.
        //    Exige contenido sin saltos de línea para no capturar de más.
        result = result.replaceAll("\\*(\\S(?:[^*\\n]*\\S)?)\\*", "<b>$1</b>");

        // 5) Cualquier asterisco huérfano que haya quedado se elimina: es lo que
        //    rompía el render. Ya no puede desbalancear nada.
        result = result.replace("*", "");

        return result;
    }
}