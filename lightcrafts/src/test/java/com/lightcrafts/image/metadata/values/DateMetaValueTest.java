package com.lightcrafts.image.metadata.values;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DateMetaValueTest {

    @Test
    void shouldCreateWithLocalDateTimeArray() {
        final LocalDateTime[] dates = {LocalDateTime.now(), LocalDateTime.now().plusDays(1)};
        final var dateMetaValue = new DateMetaValue(dates);

        final List<LocalDateTime> result = dateMetaValue.getDateValues();

        assertArrayEquals(dates, result.toArray());
    }

    @Test
    void shouldCreateWithLongValue() {
        final long epochMillis = System.currentTimeMillis();
        final var dateMetaValue = new DateMetaValue(epochMillis);

        final LocalDateTime result = dateMetaValue.getDateValue();

        assertEquals(epochMillis, result.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    @Test
    void shouldCreateWithStringArray() {
        final String[] dates = {"2022-12-31T23:59:59", "2023-01-01T00:00:00"};
        final var dateMetaValue = new DateMetaValue(dates);

        final List<LocalDateTime> result = dateMetaValue.getDateValues();

        assertEquals(LocalDateTime.parse(dates[0]), result.get(0));
        assertEquals(LocalDateTime.parse(dates[1]), result.get(1));
    }

    @Test
    void shouldThrowExceptionForInvalidStringArray() {
        final String[] dates = {"invalid date", "2023-01-01T00:00:00"};

        assertThrows(IllegalArgumentException.class, () -> new DateMetaValue(dates));
    }

    @Test
    void shouldReturnTrueForLegalValue() {
        final var dateMetaValue = new DateMetaValue();

        assertTrue(dateMetaValue.isLegalValue("2022-12-31T23:59:59"));
    }

    @Test
    void shouldReturnFalseForIllegalValue() {
        final var dateMetaValue = new DateMetaValue();

        assertFalse(dateMetaValue.isLegalValue("invalid date"));
    }

    @Test
    void shouldThrowExceptionWhenSettingValueForNonEditableInstance() {
        final var dateMetaValue = new DateMetaValue();
        final var date = LocalDateTime.now();

        assertThrows(IllegalStateException.class, () -> dateMetaValue.setDateValueAt(date, 2));
    }

    @Test
    void shouldSetValueAtSpecificIndex() {
        final var dateMetaValue = new DateMetaValue();
        dateMetaValue.setIsChangeable(true);
        final var date = LocalDateTime.now();

        dateMetaValue.setDateValueAt(date, 2);

        assertEquals(date, dateMetaValue.getDateValueAt(2));
    }

    @Test
    void shouldThrowExceptionWhenSettingValueAtNegativeIndex() {
        final var dateMetaValue = new DateMetaValue();
        dateMetaValue.setIsChangeable(true);
        final var date = LocalDateTime.now();

        assertThrows(IndexOutOfBoundsException.class, () -> dateMetaValue.setDateValueAt(date, -1));
    }
}
