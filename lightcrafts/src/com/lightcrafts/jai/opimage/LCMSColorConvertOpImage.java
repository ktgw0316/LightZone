/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.utils.LCMS_ColorSpace;

import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;

import java.awt.image.*;
import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.Map;

public class LCMSColorConvertOpImage extends PointOpImage {
    private final LCMSColorConvertDescriptor.RenderingIntent intent;
    private final LCMSColorConvertDescriptor.RenderingIntent proofIntent;
    private final ColorModel targetColorModel;
    private final ICC_Profile proof;
    private LCMS.Transform transform = null;
    final RenderedImage source;

    LCMSColorConvertOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout,
                            ColorModel colorModel,
                            LCMSColorConvertDescriptor.RenderingIntent intent,
                            ICC_Profile proof,
                            LCMSColorConvertDescriptor.RenderingIntent proofingIntent) {
        super(source, layout, config, true);
        this.source = source;
        targetColorModel = colorModel;
        this.proof = proof;
        this.intent = intent;
        this.proofIntent = proofingIntent;

        permitInPlaceOperation();
    }

    static private int mapLCMSType(int csType, int transferType) {
        if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_USHORT)
            throw new IllegalArgumentException( "Unsupported Data Type: " + transferType );

        switch(csType) {
            case ColorSpace.TYPE_GRAY:
                return transferType == DataBuffer.TYPE_BYTE ? LCMS.TYPE_GRAY_8 : LCMS.TYPE_GRAY_16;
            case ColorSpace.TYPE_RGB:
                return transferType == DataBuffer.TYPE_BYTE ? LCMS.TYPE_RGB_8 : LCMS.TYPE_RGB_16;
            case ColorSpace.TYPE_CMYK:
                return transferType == DataBuffer.TYPE_BYTE ? LCMS.TYPE_CMYK_8 : LCMS.TYPE_CMYK_16;
            case ColorSpace.TYPE_Lab:
                return transferType == DataBuffer.TYPE_BYTE ? LCMS.TYPE_Lab_8 : LCMS.TYPE_Lab_16;
            default:
                throw new IllegalArgumentException( "Unsupported Color Space Type: " + csType );
        }
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        synchronized (this) {
            if (transform == null) {
                int lcms_intent = intent.getValue() < 4 ? intent.getValue() : LCMS.INTENT_RELATIVE_COLORIMETRIC;
                int lcms_proofIntent = proofIntent.getValue() < 4 ? proofIntent.getValue() : LCMS.INTENT_RELATIVE_COLORIMETRIC;
                int lcms_flags = intent.getValue() == 4 || proofIntent.getValue() == 4
                                 ? LCMS.cmsFLAGS_BLACKPOINTCOMPENSATION
                                 : 0;

                ColorSpace sourceCS = source.getColorModel().getColorSpace();
                LCMS.Profile sourceProfile = sourceCS instanceof LCMS_ColorSpace
                                             ? ((LCMS_ColorSpace) sourceCS).getProfile()
                                             : new LCMS.Profile(((ICC_ColorSpace)sourceCS).getProfile());

                ColorSpace targetCS = targetColorModel.getColorSpace();
                LCMS.Profile targetProfile = targetCS instanceof LCMS_ColorSpace
                                             ? ((LCMS_ColorSpace) targetCS).getProfile()
                                             : new LCMS.Profile(((ICC_ColorSpace)targetCS).getProfile());

                LCMS.Profile proofProfile = proof != null ? new LCMS.Profile(proof) : null;

                int inType = mapLCMSType(sourceCS.getType(), source.getColorModel().getTransferType());
                int outType = mapLCMSType(targetCS.getType(), colorModel.getTransferType());

                transform = proofProfile != null
                            ? new LCMS.Transform(sourceProfile, inType, targetProfile, outType, proofProfile,
                                                 lcms_proofIntent, lcms_intent, lcms_flags)
                            : new LCMS.Transform(sourceProfile, inType, targetProfile, outType, lcms_intent, lcms_flags);
            }
        }

        RasterFormatTag[] formatTags = getFormatTags();
        Rectangle srcRect = mapDestRect(destRect, 0);
        RasterAccessor src = new RasterAccessor(sources[0], srcRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], this.getColorModel());

        if (src.getDataType() == dst.getDataType()) {
            transform.doTransform(src, formatTags[0], getSourceImage(0).getColorModel(),
                                  dst, formatTags[1], this.getColorModel());
        }
        else {
            throw new IllegalArgumentException("Input and output rasters don't match!");
        }
    }
}
