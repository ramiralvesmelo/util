package br.com.ramiralvesmelo.util.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.security.JsSanitizer;

class JsSanitizerTest {

    @Test
    void shouldReturnNullLiteralWhenNull() {
        assertEquals("null", JsSanitizer.clean(null));
    }

    @Test
    void shouldQuoteAndEscapeBasics() {
        assertEquals("\"abc\"", JsSanitizer.clean("abc"));
        assertEquals("\"a\\\"b\"", JsSanitizer.clean("a\"b"));
        assertEquals("\"a\\\\b\"", JsSanitizer.clean("a\\b"));
    }

    @Test
    void shouldNormalizeNewlinesAndTabs() {
        assertEquals("\"line1\\nline2\"", JsSanitizer.clean("line1\nline2"));
        assertEquals("\"tab\\tstop\"", JsSanitizer.clean("tab\tstop"));
        assertEquals("\"crlf\\nline\"", JsSanitizer.clean("crlf\r\nline"));
    }
}
