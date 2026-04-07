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
    void shouldNormalizeGenZVariations() {
        assertEquals(SummaryStyle.GEN_Z, SummaryStyle.fromInput("genz"));
        assertEquals(SummaryStyle.GEN_Z, SummaryStyle.fromInput("Gen Z"));
        assertEquals(SummaryStyle.GEN_Z, SummaryStyle.fromInput("gen-z"));
    }

    @Test
    void shouldThrowWhenStyleIsUnsupported() {
        assertThrows(InvalidRequestException.class, () -> SummaryStyle.fromInput("academic"));
    }
}
