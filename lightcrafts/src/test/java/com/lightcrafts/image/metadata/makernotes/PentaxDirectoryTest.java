package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.values.UndefinedMetaValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import static com.lightcrafts.image.metadata.makernotes.PentaxTags.PENTAX_DATE;
import static com.lightcrafts.image.metadata.makernotes.PentaxTags.PENTAX_TIME;
import static org.assertj.core.api.Assertions.assertThat;

class PentaxDirectoryTest {

    private PentaxDirectory dir;

    @BeforeEach
    void setUp() {
        dir = new PentaxDirectory();
    }

    @Test
    void putValue() {
        // Given
        final var pattern = "yyyy-MM-dd HH:mm:ss";
        final var formatter = DateTimeFormatter.ofPattern(pattern);

        // When put a PENTAX_DATE value
        final byte[] yymd = { 0x07, (byte) 0xE5, 9, 21 };
        final var date = new UndefinedMetaValue(yymd);
        dir.putValue(PENTAX_DATE, date);

        // Then captureDateTime should be set
        final var dateTime = dir.getCaptureDateTime();
        final var dateStr = "2021-09-21 00:00:00";
        assertThat(dateTime.format(formatter))
                .isEqualTo(dateStr);

        // When put PENTAX_TIME
        final byte[] hms = { 6, 12, 34 };
        final var time = new UndefinedMetaValue(hms);
        dir.putValue(PENTAX_TIME, time);

        // Then captureDateTime should increase
        final var modifiedDateTime = dir.getCaptureDateTime();
        final var modifiedDateStr = "2021-09-21 06:12:34";
        assertThat(modifiedDateTime.format(formatter))
                .isEqualTo(modifiedDateStr);

        // TODO: Add tests for other tags
    }
}