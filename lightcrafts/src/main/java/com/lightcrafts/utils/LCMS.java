/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2025-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.eclipse.imagen.media.util.ImageUtil;
import org.eclipse.imagen.RasterAccessor;
import org.eclipse.imagen.RasterFormatTag;

import java.awt.*;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.ref.Cleaner;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jul 23, 2006
 * Time: 12:47:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class LCMS {

// Format of pixel is defined by one DWORD, using bit fields as follows
//
//            TTTTT U Y F P X S EEE CCCC BBB
//
//            T: Pixeltype
//            F: Flavor  0=MinIsBlack(Chocolate) 1=MinIsWhite(Vanilla)
//            P: Planar? 0=Chunky, 1=Planar
//            X: swap 16 bps endianness?
//            S: Do swap? ie, BGR, KYMC
//            E: Extra samples
//            C: Channels (Samples per pixel)
//            B: Bytes per sample
//            Y: Swap first - changes ABGR to BGRA and KCMY to CMYK

    private static int COLORSPACE_SH(int s)       { return ((s) << 16); }
    private static int SWAPFIRST_SH(int s)        { return ((s) << 14); }
    private static int FLAVOR_SH(int s)           { return ((s) << 13); }
    private static int PLANAR_SH(int p)           { return ((p) << 12); }
    private static int ENDIAN16_SH(int e)         { return ((e) << 11); }
    private static int DOSWAP_SH(int e)           { return ((e) << 10); }
    private static int EXTRA_SH(int e)            { return ((e) << 7); }
    private static int CHANNELS_SH(int c)         { return ((c) << 3); }
    private static int BYTES_SH(int b)            { return (b); }

// Pixel types

    public static final int PT_ANY     =  0;    // Don't check colorspace
                              // 1 & 2 are reserved
    public static final int PT_GRAY   =   3;
    public static final int PT_RGB    =   4;
    public static final int PT_CMY    =   5;
    public static final int PT_CMYK   =   6;
    public static final int PT_YCbCr  =   7;
    public static final int PT_YUV    =   8;     // Lu'v'
    public static final int PT_XYZ    =   9;
    public static final int PT_Lab    =   10;
    public static final int PT_YUVK   =   11;    // Lu'v'K
    public static final int PT_HSV    =   12;
    public static final int PT_HLS    =   13;
    public static final int PT_Yxy    =   14;
    public static final int PT_HiFi   =   15;
    public static final int PT_HiFi7  =   16;
    public static final int PT_HiFi8  =   17;
    public static final int PT_HiFi9  =   18;
    public static final int PT_HiFi10 =   19;
    public static final int PT_HiFi11 =   20;
    public static final int PT_HiFi12 =   21;
    public static final int PT_HiFi13 =   22;
    public static final int PT_HiFi14 =   23;
    public static final int PT_HiFi15 =   24;

    public static final int TYPE_GRAY_8          = (COLORSPACE_SH(PT_GRAY)|CHANNELS_SH(1)|BYTES_SH(1));
    public static final int TYPE_GRAY_8_REV      = (COLORSPACE_SH(PT_GRAY)|CHANNELS_SH(1)|BYTES_SH(1)|FLAVOR_SH(1));
    public static final int TYPE_GRAY_16         = (COLORSPACE_SH(PT_GRAY)|CHANNELS_SH(1)|BYTES_SH(2));
    public static final int TYPE_GRAY_16_REV     = (COLORSPACE_SH(PT_GRAY)|CHANNELS_SH(1)|BYTES_SH(2)|FLAVOR_SH(1));
    public static final int TYPE_GRAY_16_SE      = (COLORSPACE_SH(PT_GRAY)|CHANNELS_SH(1)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_GRAYA_8         = (COLORSPACE_SH(PT_GRAY)|EXTRA_SH(1)|CHANNELS_SH(1)|BYTES_SH(1));
    public static final int TYPE_GRAYA_16        = (COLORSPACE_SH(PT_GRAY)|EXTRA_SH(1)|CHANNELS_SH(1)|BYTES_SH(2));
    public static final int TYPE_GRAYA_16_SE     = (COLORSPACE_SH(PT_GRAY)|EXTRA_SH(1)|CHANNELS_SH(1)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_GRAYA_8_PLANAR  = (COLORSPACE_SH(PT_GRAY)|EXTRA_SH(1)|CHANNELS_SH(1)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_GRAYA_16_PLANAR = (COLORSPACE_SH(PT_GRAY)|EXTRA_SH(1)|CHANNELS_SH(1)|BYTES_SH(2)|PLANAR_SH(1));

    public static final int TYPE_RGB_8           = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_RGB_8_PLANAR    = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_BGR_8           = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_BGR_8_PLANAR    = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(1)|DOSWAP_SH(1)|PLANAR_SH(1));
    public static final int TYPE_RGB_16          = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_RGB_16_PLANAR   = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_RGB_16_SE       = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_BGR_16          = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_BGR_16_PLANAR   = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(2)|DOSWAP_SH(1)|PLANAR_SH(1));
    public static final int TYPE_BGR_16_SE       = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));

    public static final int TYPE_RGBA_8          = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_RGBA_8_PLANAR   = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_RGBA_16         = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_RGBA_16_PLANAR  = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_RGBA_16_SE      = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1));

    public static final int TYPE_ARGB_8          = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(1)|SWAPFIRST_SH(1));
    public static final int TYPE_ARGB_16         = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|SWAPFIRST_SH(1));

    public static final int TYPE_ABGR_8          = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_ABGR_16         = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_ABGR_16_PLANAR  = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|DOSWAP_SH(1)|PLANAR_SH(1));
    public static final int TYPE_ABGR_16_SE      = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));

    public static final int TYPE_BGRA_8          = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(1)|DOSWAP_SH(1)|SWAPFIRST_SH(1));
    public static final int TYPE_BGRA_16         = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|DOSWAP_SH(1)|SWAPFIRST_SH(1));
    public static final int TYPE_BGRA_16_SE      = (COLORSPACE_SH(PT_RGB)|EXTRA_SH(1)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1)|SWAPFIRST_SH(1));

    public static final int TYPE_CMY_8           = (COLORSPACE_SH(PT_CMY)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_CMY_8_PLANAR    = (COLORSPACE_SH(PT_CMY)|CHANNELS_SH(3)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_CMY_16          = (COLORSPACE_SH(PT_CMY)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_CMY_16_PLANAR   = (COLORSPACE_SH(PT_CMY)|CHANNELS_SH(3)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_CMY_16_SE       = (COLORSPACE_SH(PT_CMY)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1));

    public static final int TYPE_CMYK_8          = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(1));
    public static final int TYPE_CMYKA_8         = (COLORSPACE_SH(PT_CMYK)|EXTRA_SH(1)|CHANNELS_SH(4)|BYTES_SH(1));
    public static final int TYPE_CMYK_8_REV      = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(1)|FLAVOR_SH(1));
    public static final int TYPE_YUVK_8          = TYPE_CMYK_8_REV;
    public static final int TYPE_CMYK_8_PLANAR   = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_CMYK_16         = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2));
    public static final int TYPE_CMYK_16_REV     = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|FLAVOR_SH(1));
    public static final int TYPE_YUVK_16         = TYPE_CMYK_16_REV;
    public static final int TYPE_CMYK_16_PLANAR  = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_CMYK_16_SE      = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|ENDIAN16_SH(1));

    public static final int TYPE_KYMC_8          = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC_16         = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC_16_SE      = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));

    public static final int TYPE_KCMY_8          = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(1)|SWAPFIRST_SH(1));
    public static final int TYPE_KCMY_8_REV      = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(1)|FLAVOR_SH(1)|SWAPFIRST_SH(1));
    public static final int TYPE_KCMY_16         = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|SWAPFIRST_SH(1));
    public static final int TYPE_KCMY_16_REV     = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|FLAVOR_SH(1)|SWAPFIRST_SH(1));
    public static final int TYPE_KCMY_16_SE      = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(2)|ENDIAN16_SH(1)|SWAPFIRST_SH(1));


// HiFi separations, Thanks to Steven Greaves for providing the code,
// the colorspace is not checked
    public static final int TYPE_CMYK5_8         = (CHANNELS_SH(5)|BYTES_SH(1));
    public static final int TYPE_CMYK5_16        = (CHANNELS_SH(5)|BYTES_SH(2));
    public static final int TYPE_CMYK5_16_SE     = (CHANNELS_SH(5)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_KYMC5_8         = (CHANNELS_SH(5)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC5_16        = (CHANNELS_SH(5)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC5_16_SE     = (CHANNELS_SH(5)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));

    public static final int TYPE_CMYKcm_8        = (CHANNELS_SH(6)|BYTES_SH(1));
    public static final int TYPE_CMYKcm_8_PLANAR = (CHANNELS_SH(6)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_CMYKcm_16       = (CHANNELS_SH(6)|BYTES_SH(2));
    public static final int TYPE_CMYKcm_16_PLANAR= (CHANNELS_SH(6)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_CMYKcm_16_SE    = (CHANNELS_SH(6)|BYTES_SH(2)|ENDIAN16_SH(1));

// Separations with more than 6 channels aren't very standardized,
// Except most start with CMYK and add other colors, so I just used
// then total number of channels after CMYK i.e CMYK8_8

    public static final int TYPE_CMYK7_8         = (CHANNELS_SH(7)|BYTES_SH(1));
    public static final int TYPE_CMYK7_16        = (CHANNELS_SH(7)|BYTES_SH(2));
    public static final int TYPE_CMYK7_16_SE     = (CHANNELS_SH(7)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_KYMC7_8         = (CHANNELS_SH(7)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC7_16        = (CHANNELS_SH(7)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC7_16_SE     = (CHANNELS_SH(7)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));
    public static final int TYPE_CMYK8_8         = (CHANNELS_SH(8)|BYTES_SH(1));
    public static final int TYPE_CMYK8_16        = (CHANNELS_SH(8)|BYTES_SH(2));
    public static final int TYPE_CMYK8_16_SE     = (CHANNELS_SH(8)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_KYMC8_8         = (CHANNELS_SH(8)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC8_16        = (CHANNELS_SH(8)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC8_16_SE     = (CHANNELS_SH(8)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));
    public static final int TYPE_CMYK9_8         = (CHANNELS_SH(9)|BYTES_SH(1));
    public static final int TYPE_CMYK9_16        = (CHANNELS_SH(9)|BYTES_SH(2));
    public static final int TYPE_CMYK9_16_SE     = (CHANNELS_SH(9)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_KYMC9_8         = (CHANNELS_SH(9)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC9_16        = (CHANNELS_SH(9)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC9_16_SE     = (CHANNELS_SH(9)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));
    public static final int TYPE_CMYK10_8        = (CHANNELS_SH(10)|BYTES_SH(1));
    public static final int TYPE_CMYK10_16       = (CHANNELS_SH(10)|BYTES_SH(2));
    public static final int TYPE_CMYK10_16_SE    = (CHANNELS_SH(10)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_KYMC10_8        = (CHANNELS_SH(10)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC10_16       = (CHANNELS_SH(10)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC10_16_SE    = (CHANNELS_SH(10)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));
    public static final int TYPE_CMYK11_8        = (CHANNELS_SH(11)|BYTES_SH(1));
    public static final int TYPE_CMYK11_16       = (CHANNELS_SH(11)|BYTES_SH(2));
    public static final int TYPE_CMYK11_16_SE    = (CHANNELS_SH(11)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_KYMC11_8        = (CHANNELS_SH(11)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC11_16       = (CHANNELS_SH(11)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC11_16_SE    = (CHANNELS_SH(11)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));
    public static final int TYPE_CMYK12_8        = (CHANNELS_SH(12)|BYTES_SH(1));
    public static final int TYPE_CMYK12_16       = (CHANNELS_SH(12)|BYTES_SH(2));
    public static final int TYPE_CMYK12_16_SE    = (CHANNELS_SH(12)|BYTES_SH(2)|ENDIAN16_SH(1));
    public static final int TYPE_KYMC12_8        = (CHANNELS_SH(12)|BYTES_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_KYMC12_16       = (CHANNELS_SH(12)|BYTES_SH(2)|DOSWAP_SH(1));
    public static final int TYPE_KYMC12_16_SE    = (CHANNELS_SH(12)|BYTES_SH(2)|DOSWAP_SH(1)|ENDIAN16_SH(1));

// Colorimetric

    public static final int TYPE_XYZ_16          = (COLORSPACE_SH(PT_XYZ)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_Lab_8           = (COLORSPACE_SH(PT_Lab)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_ALab_8          = (COLORSPACE_SH(PT_Lab)|CHANNELS_SH(3)|BYTES_SH(1)|EXTRA_SH(1)|DOSWAP_SH(1));
    public static final int TYPE_Lab_16          = (COLORSPACE_SH(PT_Lab)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_Yxy_16          = (COLORSPACE_SH(PT_Yxy)|CHANNELS_SH(3)|BYTES_SH(2));

// YCbCr

    public static final int TYPE_YCbCr_8         = (COLORSPACE_SH(PT_YCbCr)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_YCbCr_8_PLANAR  = (COLORSPACE_SH(PT_YCbCr)|CHANNELS_SH(3)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_YCbCr_16        = (COLORSPACE_SH(PT_YCbCr)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_YCbCr_16_PLANAR = (COLORSPACE_SH(PT_YCbCr)|CHANNELS_SH(3)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_YCbCr_16_SE     = (COLORSPACE_SH(PT_YCbCr)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1));

// YUV

    public static final int TYPE_YUV_8         = (COLORSPACE_SH(PT_YUV)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_YUV_8_PLANAR  = (COLORSPACE_SH(PT_YUV)|CHANNELS_SH(3)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_YUV_16        = (COLORSPACE_SH(PT_YUV)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_YUV_16_PLANAR = (COLORSPACE_SH(PT_YUV)|CHANNELS_SH(3)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_YUV_16_SE     = (COLORSPACE_SH(PT_YUV)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1));

// HLS

    public static final int TYPE_HLS_8         = (COLORSPACE_SH(PT_HLS)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_HLS_8_PLANAR  = (COLORSPACE_SH(PT_HLS)|CHANNELS_SH(3)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_HLS_16        = (COLORSPACE_SH(PT_HLS)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_HLS_16_PLANAR = (COLORSPACE_SH(PT_HLS)|CHANNELS_SH(3)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_HLS_16_SE     = (COLORSPACE_SH(PT_HLS)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1));


// HSV

    public static final int TYPE_HSV_8         = (COLORSPACE_SH(PT_HSV)|CHANNELS_SH(3)|BYTES_SH(1));
    public static final int TYPE_HSV_8_PLANAR  = (COLORSPACE_SH(PT_HSV)|CHANNELS_SH(3)|BYTES_SH(1)|PLANAR_SH(1));
    public static final int TYPE_HSV_16        = (COLORSPACE_SH(PT_HSV)|CHANNELS_SH(3)|BYTES_SH(2));
    public static final int TYPE_HSV_16_PLANAR = (COLORSPACE_SH(PT_HSV)|CHANNELS_SH(3)|BYTES_SH(2)|PLANAR_SH(1));
    public static final int TYPE_HSV_16_SE     = (COLORSPACE_SH(PT_HSV)|CHANNELS_SH(3)|BYTES_SH(2)|ENDIAN16_SH(1));

// Named color index. Only 16 bits allowed (don't check colorspace)

    public static final int TYPE_NAMED_COLOR_INDEX = (CHANNELS_SH(1)|BYTES_SH(2));

// Double values. Painful slow, but sometimes helpful. NOTE THAT 'BYTES' FIELD IS SET TO ZERO!

    public static final int TYPE_XYZ_DBL      = (COLORSPACE_SH(PT_XYZ)|CHANNELS_SH(3)|BYTES_SH(0));
    public static final int TYPE_Lab_DBL      = (COLORSPACE_SH(PT_Lab)|CHANNELS_SH(3)|BYTES_SH(0));
    public static final int TYPE_GRAY_DBL     = (COLORSPACE_SH(PT_GRAY)|CHANNELS_SH(1)|BYTES_SH(0));
    public static final int TYPE_RGB_DBL      = (COLORSPACE_SH(PT_RGB)|CHANNELS_SH(3)|BYTES_SH(0));
    public static final int TYPE_CMYK_DBL     = (COLORSPACE_SH(PT_CMYK)|CHANNELS_SH(4)|BYTES_SH(0));

// Intents

    public static final int INTENT_PERCEPTUAL                = 0;
    public static final int INTENT_RELATIVE_COLORIMETRIC     = 1;
    public static final int INTENT_SATURATION                = 2;
    public static final int INTENT_ABSOLUTE_COLORIMETRIC     = 3;

// Flags

    public static final int cmsFLAGS_MATRIXINPUT             = 0x0001;
    public static final int cmsFLAGS_MATRIXOUTPUT            = 0x0002;
    public static final int cmsFLAGS_MATRIXONLY              = (cmsFLAGS_MATRIXINPUT|cmsFLAGS_MATRIXOUTPUT);

    public static final int cmsFLAGS_NOWHITEONWHITEFIXUP     = 0x0004;    // Don't hot fix scum dot
    public static final int cmsFLAGS_NOPRELINEARIZATION      = 0x0010;    // Don't create prelinearization tables
                                                        // on precalculated transforms (internal use)

    public static final int cmsFLAGS_GUESSDEVICECLASS        = 0x0020;    // Guess device class (for transform2devicelink)

    public static final int cmsFLAGS_NOTCACHE                = 0x0040;    // Inhibit 1-pixel cache

    public static final int cmsFLAGS_NOTPRECALC              = 0x0100;
    public static final int cmsFLAGS_NULLTRANSFORM           = 0x0200;    // Don't transform anyway
    public static final int cmsFLAGS_HIGHRESPRECALC          = 0x0400;    // Use more memory to give better accuracy
    public static final int cmsFLAGS_LOWRESPRECALC           = 0x0800;    // Use less memory to minimize resources


    public static final int cmsFLAGS_WHITEBLACKCOMPENSATION  = 0x2000;
    public static final int cmsFLAGS_BLACKPOINTCOMPENSATION  = cmsFLAGS_WHITEBLACKCOMPENSATION;

// Proofing flags

    public static final int cmsFLAGS_GAMUTCHECK              = 0x1000;    // Out of Gamut alarm
    public static final int cmsFLAGS_SOFTPROOFING            = 0x4000;    // Do softproofing


// Black preservation

    public static final int cmsFLAGS_PRESERVEBLACK           = 0x8000;

// CRD special

    public static final int cmsFLAGS_NODEFAULTRESOURCEDEF    = 0x00010000;

// Gridpoints

    private static int cmsFLAGS_GRIDPOINTS(int n)       { return (((n) & 0xFF) << 16); }

// Public Interface

    // TODO: exception handling

    private final static class RCHandleHashMap<K,V extends RCHandle> extends LRUHashMap<K,V> {

        public RCHandleHashMap(int max_entries) {
            super(max_entries);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            if (size() > m_maxEntries) {
                eldest.getValue().decrement();
                return true;
            }
            return false;
        }
    }

    protected static class RCHandle {
        final long handle;
        private int refcount = 1;

        RCHandle(long handle) {
            this.handle = handle;
        }

        synchronized int increment() {
            refcount++;
            return refcount;
        }

        synchronized int decrement() {
            refcount--;
            return refcount;
        }
    }

    private record RGBProfileComponents(double[] whitePoint, double[] primaries, double gamma) {
    }

    public static class Profile {
        private static Map<Object, RCHandle> profileCache = new RCHandleHashMap<>(20);

        protected RCHandle cmsProfile = null;

        protected Profile() { }

        public Profile(ICC_Profile iccProfile) {
            RCHandle handle = profileCache.get(iccProfile);

            if (handle != null && handle.increment() > 1)
                cmsProfile = handle;
            else {
                byte[] data = iccProfile.getData();
                cmsProfile = new RCHandle(LCMSNative.cmsOpenProfileFromMem(data, data.length));
                profileCache.put(iccProfile, cmsProfile);
                cmsProfile.increment(); // for the cache reference
            }

            cleaner.register(this, cleanup(this));
        }

        public Profile(double[] whitePoint, double[] primaries, double gamma) {
            RGBProfileComponents components = new RGBProfileComponents(whitePoint, primaries, gamma);
            RCHandle handle = profileCache.get(components);

            if (handle != null && handle.increment() > 1)
                cmsProfile = handle;
            else {
                cmsProfile = new RCHandle(LCMSNative.cmsCreateRGBProfile(whitePoint, primaries, gamma));
                profileCache.put(components, cmsProfile);
                cmsProfile.increment(); // for the cache reference
            }

            cleaner.register(this, cleanup(this));
        }

        public void dispose() {
            if (cmsProfile != null && cmsProfile.decrement() == 0) {
                LCMSNative.cmsCloseProfile(cmsProfile.handle);
            }
            cmsProfile = null;
        }

        private static final Cleaner cleaner = Cleaner.create();

        @Contract(pure = true)
        private static @NotNull Runnable cleanup(@NotNull Profile instance) {
            return instance::dispose;
        }
    }

    public static class LABProfile extends Profile {
        private static long labProfileHandle = -1;
        private static RCHandle handle = null;

        public LABProfile() {
            if (labProfileHandle == -1) {
                labProfileHandle = LCMSNative.cmsCreateLab2Profile();
                cmsProfile = handle = new RCHandle(labProfileHandle);
            } else {
                cmsProfile = handle;
            }
        }

        @Override
        public void dispose() { }
    }

    private static RasterAccessor normalizeRaster(RasterAccessor src, RasterFormatTag rft, ColorModel cm) {
        boolean reallocBuffer = false;
        final int[] dataOffsets = src.getBandOffsets();
        for (int i = 0; i < dataOffsets.length; i++) {
            if (dataOffsets[i] != i) {
                reallocBuffer = true;
                break;
            }
        }
        final int bands = src.getNumBands();
        final int scanLineStride = src.getScanlineStride();
        if (reallocBuffer || src.getPixelStride() != bands || scanLineStride != src.getWidth() * bands) {
            final int[] offsets = bands == 1 ? new int[] {0} : new int[] {0, 1, 2};
            WritableRaster newRaster = Raster.createInterleavedRaster(
                    src.getDataType(),
                    src.getWidth(), src.getHeight(),
                    src.getWidth() * bands, bands, offsets,
                    new Point(src.getX(), src.getY())
            );
            RasterAccessor newSrc = new RasterAccessor(newRaster, newRaster.getBounds(), rft, cm);

            ImageUtil.copyRaster(src, newSrc);
            src = newSrc;
        }
        return src;
    }

    public static class Transform {
        private static Map<Object, RCHandle> transformCache = new RCHandleHashMap<Object, RCHandle>(20);

        private RCHandle cmsTransform;

        private static class TransformData {
            final long inputProfileHandle;
            final int inputType;
            final long outputProfileHandle;
            final int outputType;
            final long proofProfileHandle;
            final int intent;
            final int proofIntent;
            final int flags;

            TransformData(Profile input, int inputType, Profile output, int outputType, int intent, int flags) {
                inputProfileHandle = input.cmsProfile.handle;
                this.inputType = inputType;
                outputProfileHandle = output.cmsProfile.handle;
                this.outputType = outputType;
                this.intent = intent;
                proofProfileHandle = 0;
                this.proofIntent = 0;
                this.flags = flags;
            }

            TransformData(Profile input, int inputType, Profile output, int outputType, Profile proof,
                          int intent, int proofIntent, int flags) {
                inputProfileHandle = input.cmsProfile.handle;
                this.inputType = inputType;
                outputProfileHandle = output.cmsProfile.handle;
                this.outputType = outputType;
                this.intent = intent;
                proofProfileHandle = proof.cmsProfile.handle;
                this.proofIntent = proofIntent;
                this.flags = flags;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                TransformData that = (TransformData) o;

                return ((flags == that.flags)
                        && (inputProfileHandle == that.inputProfileHandle)
                        && (inputType == that.inputType)
                        && (intent == that.intent)
                        && (outputProfileHandle == that.outputProfileHandle)
                        && (outputType == that.outputType)
                        && (proofIntent == that.proofIntent)
                        && (proofProfileHandle == that.proofProfileHandle));
            }

            @Override
            public int hashCode() {
                int result;
                result = (int) (inputProfileHandle ^ (inputProfileHandle >>> 32));
                result = 31 * result + inputType;
                result = 31 * result + (int) (outputProfileHandle ^ (outputProfileHandle >>> 32));
                result = 31 * result + outputType;
                result = 31 * result + (int) (proofProfileHandle ^ (proofProfileHandle >>> 32));
                result = 31 * result + intent;
                result = 31 * result + proofIntent;
                result = 31 * result + flags;
                return result;
            }
        }

        public Transform(Profile input, int inputType, Profile output, int outputType, int intent, int flags) {
            TransformData td = new TransformData(input, inputType, output, outputType, intent, flags);
            RCHandle transformHandle = transformCache.get(td);

            if (transformHandle != null && transformHandle.increment() > 1)
                cmsTransform = transformHandle;
            else {
                // Don't bother hires with 8bit to 8bit transforms
                if (inputType != TYPE_RGB_8 || outputType != TYPE_RGB_8)
                    flags |= cmsFLAGS_HIGHRESPRECALC;

                cmsTransform = new RCHandle(LCMSNative.cmsCreateTransform(input.cmsProfile.handle, inputType,
                        output.cmsProfile.handle, outputType,
                        intent, flags));

                transformCache.put(td, cmsTransform);
                cmsTransform.increment(); // for the cache reference
            }

            cleaner.register(this, cleanup(this));
        }

        public Transform(Profile input, int inputType, Profile output, int outputType, Profile proof,
                         int intent, int proofIntent, int flags) {
            TransformData td = new TransformData(input, inputType, output, outputType, proof, intent, proofIntent, flags);
            RCHandle transformHandle = transformCache.get(td);

            if (transformHandle != null && transformHandle.increment() > 1)
                cmsTransform = transformHandle;
            else {
                cmsTransform = new RCHandle(LCMSNative.cmsCreateProofingTransform(input.cmsProfile.handle, inputType,
                        output.cmsProfile.handle, outputType,
                        proof.cmsProfile.handle,
                        intent, proofIntent,
                        flags
                                | cmsFLAGS_NOTPRECALC
                                | cmsFLAGS_SOFTPROOFING));

                transformCache.put(td, cmsTransform);
                cmsTransform.increment(); // for the cache reference
            }

            cleaner.register(this, cleanup(this));
        }

        public void doTransform(RasterAccessor src, RasterFormatTag srcRft, ColorModel srcCm,
                                RasterAccessor dst, RasterFormatTag dstRft, ColorModel dstCm) {
            if (cmsTransform == null) {
                return;
            }
            RasterAccessor ri = normalizeRaster(src, srcRft, srcCm);
            RasterAccessor ro;
            final int bands = dst.getNumBands();

            if (src.getX() != dst.getX() || src.getY() != dst.getY() ||
                    src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight()) {
                int[] offsets = bands == 1 ? new int[] {0} : new int[] {0, 1, 2};
                WritableRaster output = Raster.createInterleavedRaster(
                        ri.getDataType(),
                        ri.getWidth(), ri.getHeight(),
                        ri.getWidth() * bands, bands, offsets,
                        new Point(ri.getX(), ri.getY())
                );
                ro = new RasterAccessor(output, output.getBounds(), dstRft, dstCm);
                System.out.println("*** A");
            }
            else {
                ro = normalizeRaster(dst, dstRft, dstCm);
            }
            switch (ro.getDataType()) {
                case DataBuffer.TYPE_BYTE: {
                    final int pixels = ro.getByteDataArray(0).length / bands;
                    LCMSNative.cmsDoTransform(cmsTransform.handle, ri.getByteDataArray(0), ro.getByteDataArray(0), pixels);
                    break;
                }
                case DataBuffer.TYPE_USHORT: {
                    final int pixels = ro.getShortDataArray(0).length / bands;
                    LCMSNative.cmsDoTransform(cmsTransform.handle, ri.getShortDataArray(0), ro.getShortDataArray(0), pixels);
                    break;
                }
                default:
            }
            if (ro != dst) {
                ImageUtil.copyRaster(ro, dst);
            }
        }

        public void doTransform(byte[] input, byte[] output) {
            LCMSNative.cmsDoTransform(cmsTransform.handle, input, output, 1);
        }

        public void doTransform(double[] input, double[] output) {
            LCMSNative.cmsDoTransform(cmsTransform.handle, input, output, 1);
        }

        public void doTransform(short[] input, short[] output) {
            LCMSNative.cmsDoTransform(cmsTransform.handle, input, output, 1);
        }

        public void dispose() {
            if (cmsTransform != null && cmsTransform.decrement() == 0) {
                LCMSNative.cmsDeleteTransform(cmsTransform.handle);
            }
            cmsTransform = null;
        }

        private static final Cleaner cleaner = Cleaner.create();

        @Contract(pure = true)
        private static @NotNull Runnable cleanup(@NotNull Transform instance) {
            return instance::dispose;
        }
    }
}
