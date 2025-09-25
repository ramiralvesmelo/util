package br.com.ramiralvesmelo.util.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaScriptEncoderTest {

    @Test
    void shouldReturnNullLiteralWhenNull() {
        assertEquals("null", JavaScriptEncoder.escape(null));
    }

    @Test
    void shouldQuoteAndEscapeBasics() {
        assertEquals("\"abc\"", JavaScriptEncoder.escape("abc"));
        assertEquals("\"a\\\"b\"", JavaScriptEncoder.escape("a\"b"));
        assertEquals("\"a\\\\b\"", JavaScriptEncoder.escape("a\\b"));
    }

    @Test
    void shouldNormalizeNewlinesAndTabs() {
        assertEquals("\"line1\\nline2\"", JavaScriptEncoder.escape("line1\nline2"));
        assertEquals("\"tab\\tstop\"", JavaScriptEncoder.escape("tab\tstop"));
        assertEquals("\"crlf\\nline\"", JavaScriptEncoder.escape("crlf\r\nline"));
    }
}
