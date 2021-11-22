/*
 * Copyright (c) 2021. Masahiro Kitagawa
 */

package com.lightcrafts.jai.utils;

import com.sun.media.jai.util.ImageUtil;
import org.jetbrains.annotations.NotNull;

import javax.media.jai.RasterFormatTag;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;

import static javax.media.jai.RasterAccessor.*;

public final class OpImageUtil {
    @NotNull
    public static RasterFormatTag getFormatTag(@NotNull Raster raster) {
        final var sm = raster.getSampleModel();
        final int formatTagID = getFormatTagID(sm);
        return new RasterFormatTag(sm, formatTagID);
    }

    // cf. javax.media.jai.RasterAccessor#findCompatibleTag
    private static int getFormatTagID(@NotNull SampleModel sampleModel) {
        int dataType = sampleModel.getTransferType();
        final int tag;
        if (sampleModel instanceof ComponentSampleModel) {
            tag = dataType | UNCOPIED;
        } else if (ImageUtil.isBinary(sampleModel)) {
            tag = DataBuffer.TYPE_BYTE | COPIED;
        } else if (dataType == DataBuffer.TYPE_BYTE ||
                dataType == DataBuffer.TYPE_USHORT ||
                dataType == DataBuffer.TYPE_SHORT) {
            tag = TAG_INT_COPIED;
        } else {
            tag = dataType | COPIED;
        }
        return tag | UNEXPANDED;
    }
}
