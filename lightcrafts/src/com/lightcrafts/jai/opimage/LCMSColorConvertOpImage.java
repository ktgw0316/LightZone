/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.utils.LCMS_LabColorSpace;
import com.lightcrafts.utils.LCMS_ColorSpace;

import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import java.awt.image.*;
import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.Map;

import sun.awt.image.ShortInterleavedRaster;
import sun.awt.image.ByteInterleavedRaster;

public class LCMSColorConvertOpImage extends PointOpImage {
    final LCMSColorConvertDescriptor.RenderingIntent intent;
    final LCMSColorConvertDescriptor.RenderingIntent proofIntent;
    final ColorModel targetColorModel;
    final ICC_Profile proof;
    LCMS.Transform transform = null;
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
        if (sources[0] instanceof ByteInterleavedRaster && dest instanceof ByteInterleavedRaster) {
            ByteInterleavedRaster source = (ByteInterleavedRaster) sources[0];
            ByteInterleavedRaster destination = (ByteInterleavedRaster) dest;

            transform.doTransform(source, destination);
        } else if (sources[0] instanceof ShortInterleavedRaster && dest instanceof ShortInterleavedRaster) {
            ShortInterleavedRaster source = (ShortInterleavedRaster) sources[0];
            ShortInterleavedRaster destination = (ShortInterleavedRaster) dest;

            transform.doTransform(source, destination);
        } else
            throw new IllegalArgumentException( "Input and output rasters don't match!" );
    }
}
