package com.timscdr.eventease.ui.organizer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AttendeeCsvTest {
    @Test public void csvEscapingProtectsSpreadsheetFormulasAndQuotes() {
        assertEquals("\"'=HYPERLINK(\"\"https://example.invalid\"\",\"\"open\"\")\"",
                OrganizerAttendeesActivity.csv("=HYPERLINK(\"https://example.invalid\",\"open\")"));
        assertEquals("\"'  +SUM(A1:A2)\"", OrganizerAttendeesActivity.csv("  +SUM(A1:A2)"));
        assertEquals("\"Normal, \"\"quoted\"\" value\"",
                OrganizerAttendeesActivity.csv("Normal, \"quoted\" value"));
        assertEquals("\"\"", OrganizerAttendeesActivity.csv(null));
    }
}
