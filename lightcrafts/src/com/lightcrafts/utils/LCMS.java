/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;
import java.awt.color.ICC_Profile;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;

import sun.awt.image.ShortInterleavedRaster;
import sun.awt.image.ByteInterleavedRaster;
import com.lightcrafts.jai.utils.Functions;

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
//            X: swap 16 bps endianess?
//            S: Do swap? ie, BGR, KYMC
//            E: Extra samples
//            C: Channels (Samples per pixel)
//            B: Bytes per sample
//            Y: Swap first - changes ABGR to BGRA and KCMY to CMYK

    private static final int COLORSPACE_SH(int s)       { return ((s) << 16); }
    private static final int SWAPFIRST_SH(int s)        { return ((s) << 14); }
    private static final int FLAVOR_SH(int s)           { return ((s) << 13); }
    private static final int PLANAR_SH(int p)           { return ((p) << 12); }
    private static final int ENDIAN16_SH(int e)         { return ((e) << 11); }
    private static final int DOSWAP_SH(int e)           { return ((e) << 10); }
    private static final int EXTRA_SH(int e)            { return ((e) << 7); }
    private static final int CHANNELS_SH(int c)         { return ((c) << 3); }
    private static final int BYTES_SH(int b)            { return (b); }

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

// Separations with more than 6 channels aren't very standarized,
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
    public static final int cmsFLAGS_HIGHRESPRECALC          = 0x0400;    // Use more memory to give better accurancy
    public static final int cmsFLAGS_LOWRESPRECALC           = 0x0800;    // Use less memory to minimize resouces


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

    private static final int cmsFLAGS_GRIDPOINTS(int n)       { return (((n) & 0xFF) << 16); }

// Native LCMS API Functions

    // NOTE: LCMS doesn't seem to be properly reentrant, make all native calls synchronized

    protected synchronized native static long cmsCreateLab2Profile();

    protected synchronized native static long cmsOpenProfileFromMem(byte data[], int size);

    protected synchronized native static long cmsCreateRGBProfile(double WhitePoint[],
                                                                  double Primaries[],
                                                                  double gamma);

    protected synchronized native static boolean cmsCloseProfile(long hProfile);

    private synchronized native static long cmsCreateTransform(long inputProfile, int inputFormat,
                                                               long outputProfile, int outputFormat,
                                                               int intent, int flags);

    private synchronized native static long cmsCreateProofingTransform(long inputProfile, int inputFormat,
                                                                       long outputProfile, int outputFormat,
                                                                       long proofingProfile,
                                                                       int Intent, int ProofingIntent, int dwFlags);

    private synchronized native static void cmsDeleteTransform(long hTransform);

    private native static void cmsDoTransform(long hTransform, byte[] InputBuffer, byte[] OutputBuffer, int size);

    private native static void cmsDoTransform(long hTransform, short[] InputBuffer, short[] OutputBuffer, int size);

    private native static void cmsDoTransform(long hTransform, double[] InputBuffer, double[] OutputBuffer, int size);

// Public Interface

    // TODO: exception handling

    private final static class LRUHashMap extends LinkedHashMap {
        LRUHashMap(int initialCapacity, float loadFactor, int max_entries) {
            super(initialCapacity, loadFactor, true);
            this.max_entries = max_entries;
        }

        public LRUHashMap(int initialCapacity, int max_entries) {
            super(initialCapacity);
            this.max_entries = max_entries;
        }

        public LRUHashMap(int max_entries) {
            super();
            this.max_entries = max_entries;
        }

        private final int max_entries;

        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size() > max_entries) {
                ((RCHandle) eldest.getValue()).decrement();
                return true;
            }
            return false;
        }
    }

    private static class RCHandle {
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

    private static class RGBProfileComponents {
        final double whitePoint[];
        final double primaries[];
        final double gamma;

        RGBProfileComponents(double whitePoint[], double primaries[], double gamma) {
            this.whitePoint = whitePoint;
            this.primaries = primaries;
            this.gamma = gamma;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RGBProfileComponents that = (RGBProfileComponents) o;

            if (Double.compare(that.gamma, gamma) != 0) return false;
            if (!Arrays.equals(primaries, that.primaries)) return false;
            if (!Arrays.equals(whitePoint, that.whitePoint)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            long temp;
            result = (whitePoint != null ? Arrays.hashCode(whitePoint) : 0);
            result = 31 * result + (primaries != null ? Arrays.hashCode(primaries) : 0);
            temp = gamma != +0.0d ? Double.doubleToLongBits(gamma) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    public static class Profile {
        private static Map profileCache = new LRUHashMap(20);

        protected RCHandle cmsProfile = null;

        protected Profile() { }

        public Profile(ICC_Profile iccProfile) {
            RCHandle handle = (RCHandle) profileCache.get(iccProfile);
            
            if (handle != null && handle.increment() > 1)
                cmsProfile = handle;
            else {
                byte data[] = iccProfile.getData();
                cmsProfile = new RCHandle(cmsOpenProfileFromMem(data, data.length));
                profileCache.put(iccProfile, cmsProfile);
                cmsProfile.increment(); // for the cache reference
            }
        }

        public Profile(double whitePoint[], double primaries[], double gamma) {
            RGBProfileComponents components = new RGBProfileComponents(whitePoint, primaries, gamma);
            RCHandle handle = (RCHandle) profileCache.get(components);

            if (handle != null && handle.increment() > 1)
                cmsProfile = handle;
            else {
                cmsProfile = new RCHandle(cmsCreateRGBProfile(whitePoint, primaries, gamma));
                profileCache.put(components, cmsProfile);
                cmsProfile.increment(); // for the cache reference
            }
        }

        public void dispose() {
            if (cmsProfile != null && cmsProfile.decrement() == 0) {
                cmsCloseProfile(cmsProfile.handle);
            }
            cmsProfile = null;
        }

        public void finalize() {
            dispose();
        }
    }

    public static class LABProfile extends Profile {
        private static long labProfileHandle = -1;
        private static RCHandle handle = null;

        public LABProfile() {
            if (labProfileHandle == -1) {
                labProfileHandle = cmsCreateLab2Profile();
                cmsProfile = handle = new RCHandle(labProfileHandle);
            } else {
                cmsProfile = handle;
            }
        }

        public void dispose() { }
    }

    private static ShortInterleavedRaster normalizeRaster(ShortInterleavedRaster raster) {
        boolean reallocBuffer = false;
        int scanLineStride = raster.getScanlineStride();
        int dataOffsets[] = raster.getDataOffsets();
        for (int i = 0; i < dataOffsets.length; i++)
            if (dataOffsets[i] != i) {
                reallocBuffer = true;
                break;
            }
        int bands = raster.getNumBands();
        if (!reallocBuffer && (raster.getPixelStride() != bands || scanLineStride != raster.getWidth() * bands))
            reallocBuffer = true;
        if (reallocBuffer) {
            PixelInterleavedSampleModel sm = (PixelInterleavedSampleModel) raster.getSampleModel();

            PixelInterleavedSampleModel newSM = new PixelInterleavedSampleModel(sm.getDataType(),
                                                                                raster.getWidth(),
                                                                                raster.getHeight(),
                                                                                bands, bands * raster.getWidth(),
                                                                                bands == 1
                                                                                ? new int[] {0}
                                                                                : new int[] {0, 1, 2});
            ShortInterleavedRaster newRaster = new ShortInterleavedRaster(newSM, new Point(raster.getMinX(),
                                                                                           raster.getMinY()));

            Functions.copyData(newRaster, raster);
            raster = newRaster;
        }
        return raster;
    }

    private static ByteInterleavedRaster normalizeRaster(ByteInterleavedRaster raster) {
        boolean reallocBuffer = false;
        int scanLineStride = raster.getScanlineStride();
        int dataOffsets[] = raster.getDataOffsets();
        for (int i = 0; i < dataOffsets.length; i++)
            if (dataOffsets[i] != i) {
                reallocBuffer = true;
                break;
            }
        int bands = raster.getNumBands();
        if (!reallocBuffer && (raster.getPixelStride() != bands || scanLineStride != raster.getWidth() * bands))
            reallocBuffer = true;
        if (reallocBuffer) {
            PixelInterleavedSampleModel sm = (PixelInterleavedSampleModel) raster.getSampleModel();

            PixelInterleavedSampleModel newSM = new PixelInterleavedSampleModel(sm.getDataType(),
                                                                                raster.getWidth(),
                                                                                raster.getHeight(),
                                                                                bands, bands * raster.getWidth(),
                                                                                bands == 1
                                                                                ? new int[] {0}
                                                                                : new int[] {0, 1, 2});

            ByteInterleavedRaster newRaster = new ByteInterleavedRaster(newSM, new Point(raster.getMinX(),
                                                                                           raster.getMinY()));
            Functions.copyData(newRaster, raster);
            raster = newRaster;
        }
        return raster;
    }

    public static class Transform {
        private static Map transformCache = new LRUHashMap(20);

        private RCHandle cmsTransform = null;

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

            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                TransformData that = (TransformData) o;

                if (flags != that.flags) return false;
                if (inputProfileHandle != that.inputProfileHandle) return false;
                if (inputType != that.inputType) return false;
                if (intent != that.intent) return false;
                if (outputProfileHandle != that.outputProfileHandle) return false;
                if (outputType != that.outputType) return false;
                if (proofIntent != that.proofIntent) return false;
                if (proofProfileHandle != that.proofProfileHandle) return false;

                return true;
            }

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
            RCHandle transformHandle = (RCHandle) transformCache.get(td);

            if (transformHandle != null && transformHandle.increment() > 1)
                cmsTransform = transformHandle;
            else {
                // Don't bother hires with 8bit to 8bit transforms
                if (inputType != TYPE_RGB_8 || outputType != TYPE_RGB_8)
                    flags |= cmsFLAGS_HIGHRESPRECALC;

                cmsTransform = new RCHandle(cmsCreateTransform(input.cmsProfile.handle, inputType,
                                                               output.cmsProfile.handle, outputType,
                                                               intent, flags));

                transformCache.put(td, cmsTransform);
                cmsTransform.increment(); // for the cache reference
            }
        }

        public Transform(Profile input, int inputType, Profile output, int outputType, Profile proof,
                         int intent, int proofIntent, int flags) {
            TransformData td = new TransformData(input, inputType, output, outputType, proof, intent, proofIntent, flags);
            RCHandle transformHandle = (RCHandle) transformCache.get(td);

            if (transformHandle != null && transformHandle.increment() > 1)
                cmsTransform = transformHandle;
            else {
                cmsTransform = new RCHandle(cmsCreateProofingTransform(input.cmsProfile.handle, inputType,
                                                                       output.cmsProfile.handle, outputType,
                                                                       proof.cmsProfile.handle,
                                                                       intent, proofIntent,
                                                                       flags
                                                                       | cmsFLAGS_NOTPRECALC
                                                                       | cmsFLAGS_SOFTPROOFING));

                transformCache.put(td, cmsTransform);
                cmsTransform.increment(); // for the cache reference
            }
        }

        public void doTransform(ByteInterleavedRaster input, ByteInterleavedRaster output) {
            if (cmsTransform != null) {
                ByteInterleavedRaster ri = normalizeRaster(input);
                ByteInterleavedRaster ro;
                int outBands = output.getNumBands();
                if (!input.getBounds().equals(output.getBounds())) {
                    int[] offsets = outBands == 1 ? new int[] {0} : new int[] {0, 1, 2};
                    SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                                     ri.getWidth(), ri.getHeight(),
                                                                     outBands, outBands * ri.getWidth(),
                                                                     offsets);
                    ro = new ByteInterleavedRaster(sm, new Point(ri.getMinX(), ri.getMinY()));
                } else
                    ro = normalizeRaster(output);
                int pixels = outBands == 1 ? ro.getDataStorage().length : ro.getDataStorage().length / outBands;
                cmsDoTransform(cmsTransform.handle, ri.getDataStorage(), ro.getDataStorage(), pixels);
                if (ro != output)
                    Functions.copyData(output, ro);
            }
        }

        public void doTransform(byte[] input, byte[] output) {
            cmsDoTransform(cmsTransform.handle, input, output, 1);
        }

        public void doTransform(double[] input, double[] output) {
            cmsDoTransform(cmsTransform.handle, input, output, 1);
        }

        public void doTransform(ShortInterleavedRaster input, ShortInterleavedRaster output) {
            if (cmsTransform != null) {
                ShortInterleavedRaster ri = normalizeRaster(input);
                ShortInterleavedRaster ro;
                int outBands = output.getNumBands();
                if (!input.getBounds().equals(output.getBounds())) {
                    int[] offsets = outBands == 1 ? new int[] {0} : new int[] {0, 1, 2};
                    SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_USHORT,
                                                                     ri.getWidth(), ri.getHeight(),
                                                                     outBands, outBands * ri.getWidth(),
                                                                     offsets);
                    ro = new ShortInterleavedRaster(sm, new Point(ri.getMinX(), ri.getMinY()));
                } else
                    ro = normalizeRaster(output);
                int pixels = outBands == 1 ? ro.getDataStorage().length : ro.getDataStorage().length / outBands;
                cmsDoTransform(cmsTransform.handle, ri.getDataStorage(), ro.getDataStorage(), pixels);
                if (ro != output)
                    Functions.copyData(output, ro);
            }
        }

        public void doTransform(short[] input, short[] output) {
            cmsDoTransform(cmsTransform.handle, input, output, 1);
        }

        public void dispose() {
            if (cmsTransform != null && cmsTransform.decrement() == 0) {
                cmsDeleteTransform(cmsTransform.handle);
            }
            cmsTransform = null;
        }

        public void finalize() {
            dispose();
        }
    }

    static {
        System.loadLibrary("LCLCMS");
    }

    public static void main(String args[]) {
        try {
            ICC_Profile inProfile = ICC_Profile.getInstance("/System/Library/ColorSync/Profiles/AdobeRGB1998.icc");
            ICC_Profile outProfile = ICC_Profile.getInstance("/Library/ColorSync/Profiles/CIE 1931 D50 Gamma 1.icm");

            Profile cmsOutProfile = new Profile(outProfile);
            Profile cmsInProfile = new Profile(inProfile);

            BufferedImage inputImage = ImageIO.read(new File("/Stuff/Reference/small-q60-adobergb.TIF"));
            ShortInterleavedRaster inputRaster = (ShortInterleavedRaster) inputImage.getTile(0, 0);

            ColorSpace outCS = new ICC_ColorSpace(outProfile);
            ColorModel outCM = new ComponentColorModel(outCS, false, false,
                                                       Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
            ShortInterleavedRaster outputRaster =
                    (ShortInterleavedRaster) outCM.createCompatibleWritableRaster(inputImage.getWidth(),
                                                                                  inputImage.getHeight());
            BufferedImage outputImage = new BufferedImage(outCM, outputRaster, false, null);

            Transform cmsTransform = new Transform(cmsInProfile, TYPE_RGB_16,
                                                   cmsOutProfile, TYPE_RGB_16,
                                                   INTENT_PERCEPTUAL,
                                                   0);

            cmsTransform.doTransform(inputRaster, outputRaster);

            ImageIO.write(outputImage, "TIF", new File("/Stuff/small-q60-CIED65.TIF"));

            cmsTransform.dispose();
            cmsOutProfile.dispose();
            cmsInProfile.dispose();
            // System.out.println("Profile: " + hProfile + ", " + );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
