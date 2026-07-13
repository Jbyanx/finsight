package dev.jbyanx.finsight.domain.telegram.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelegramFormatterTest {

    @Test
    void toHtml_DoubleAsterisk_BecomesBold() {
        assertEquals("<b>Hola</b>", TelegramFormatter.toHtml("**Hola**"));
    }

    @Test
    void toHtml_SingleAsterisk_BecomesBold() {
        assertEquals("<b>Hola</b>", TelegramFormatter.toHtml("*Hola*"));
    }

    @Test
    void toHtml_MarkdownHeader_BecomesBold() {
        assertEquals("<b>Titulo</b>", TelegramFormatter.toHtml("## Titulo"));
    }

    @Test
    void toHtml_StrayAsterisk_IsRemovedNotLeftDangling() {
        // Esto es lo que rompía el envío: un asterisco huérfano.
        String result = TelegramFormatter.toHtml("Total * pendiente");

        assertFalse(result.contains("*"));
        assertEquals("Total  pendiente", result);
    }

    @Test
    void toHtml_EscapesHtmlSpecialChars() {
        String result = TelegramFormatter.toHtml("5 < 10 & 20 > 15");

        assertEquals("5 &lt; 10 &amp; 20 &gt; 15", result);
    }

    @Test
    void toHtml_UserContentWithTag_IsEscapedNotInterpreted() {
        String result = TelegramFormatter.toHtml("compré <script> en la tienda");

        assertTrue(result.contains("&lt;script&gt;"));
        assertFalse(result.contains("<script>"));
    }

    @Test
    void toHtml_BalancedTagsOnly_NeverEmitsUnclosedBold() {
        String result = TelegramFormatter.toHtml("**Tarjetas:** Nu * RappiCard");

        long open = result.split("<b>", -1).length - 1;
        long close = result.split("</b>", -1).length - 1;
        assertEquals(open, close);
    }

    @Test
    void toHtml_Null_ReturnsEmpty() {
        assertEquals("", TelegramFormatter.toHtml(null));
    }
}