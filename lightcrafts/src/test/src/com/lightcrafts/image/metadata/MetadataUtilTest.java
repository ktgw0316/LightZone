package com.lightcrafts.image.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Masahiro Kitagawa on 2016/10/20.
 */
public class MetadataUtilTest {
    @Test
    public void convertAPEXToEV() throws Exception {

    }

    @Test
    public void convertBiasFromAPEX() throws Exception {

    }

    @Test
    public void convertFStopFromAPEX() throws Exception {

    }

    @Test
    public void convertISOFromAPEX() throws Exception {

    }

    @Test
    public void convertShutterSpeedFromAPEX() throws Exception {

    }

    @Test
    public void convertEVToAPEX() throws Exception {

    }

    @Test
    public void fixFStop() throws Exception {

    }

    @Test
    public void fixShutterSpeed() throws Exception {

    }

    @Test
    public void isFullSizedImage() throws Exception {

    }

    @Test
    public void maxTagValue() throws Exception {

    }

    @Test
    public void removePreviewMetadataFrom() throws Exception {

    }

    @Test
    public void removeWidthHeightFrom() throws Exception {

    }

    @Test
    public void shutterSpeedString() throws Exception {

    }

    @Test
    public void undupMakeModel() throws Exception {
        // The model contains the make
        assertThat(
                MetadataUtil.undupMakeModel("Canon", "Canon EOS 10D"))
                .isEqualTo("Canon EOS 10D");

        // The make contains the first word of the model
        assertThat(
                MetadataUtil.undupMakeModel("Nikon Corporation", "Nikon D2X"))
                .isEqualTo("Nikon D2X");

        // Other cases
        assertThat(
                MetadataUtil.undupMakeModel("OLYMPUS IMAGING CORP.", "E-PL7"))
                .isEqualTo("OLYMPUS IMAGING CORP. E-PL7");
    }

}