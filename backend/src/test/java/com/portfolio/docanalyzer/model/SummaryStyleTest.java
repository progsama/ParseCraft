package com.portfolio.docanalyzer.model;

import com.portfolio.docanalyzer.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SummaryStyleTest {

    @Test
    void shouldParseFormalStyle() {
        assertEquals(SummaryStyle.FORMAL, SummaryStyle.fromInput("formal"));
    }

    @Test
    void shouldParseEverydayAndAliases() {
        assertEquals(SummaryStyle.EVERYDAY, SummaryStyle.fromInput("everyday"));
        assertEquals(SummaryStyle.EVERYDAY, SummaryStyle.fromInput("genz"));
        assertEquals(SummaryStyle.EVERYDAY, SummaryStyle.fromInput("casual"));
        assertEquals(SummaryStyle.EVERYDAY, SummaryStyle.fromInput("Gen Z"));
    }

    @Test
    void shouldParseBardAndAliases() {
        assertEquals(SummaryStyle.BARD, SummaryStyle.fromInput("bard"));
        assertEquals(SummaryStyle.BARD, SummaryStyle.fromInput("herald"));
        assertEquals(SummaryStyle.BARD, SummaryStyle.fromInput("Bard/Herald"));
    }

    @Test
    void shouldThrowWhenStyleIsUnsupported() {
        assertThrows(InvalidRequestException.class, () -> SummaryStyle.fromInput("academic"));
    }
}
