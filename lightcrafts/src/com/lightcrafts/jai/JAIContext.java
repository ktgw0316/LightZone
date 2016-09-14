/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai;

import com.lightcrafts.jai.operator.*;
import com.lightcrafts.jai.opimage.*;
import com.lightcrafts.jai.utils.LCTileCache;
import com.lightcrafts.jai.utils.LCRecyclingTileFactory;
import com.lightcrafts.utils.ColorScience;
import com.lightcrafts.utils.ColorProfileInfo;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.media.jai.util.SunTileCache;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.mediax.jai.registry.CRIFRegistry;
import com.lightcrafts.mediax.jai.registry.RIFRegistry;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.*;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Feb 27, 2005
 * Time: 6:33:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class JAIContext {
    public static final Collection<ColorProfileInfo> systemProfiles;

    public static final ICC_Profile linearProfile;
    public static final ICC_ColorSpace linearColorSpace;
    public static final ICC_Profile labProfile;
    public static final ICC_ColorSpace labColorSpace;
    public static final ICC_Profile gray22Profile;
    public static final ICC_ColorSpace gray22ColorSpace;
    public static final ICC_Profile oldLinearProfile;
    public static final ICC_ColorSpace oldLinearColorSpace;

    public static final ICC_Profile CMYKProfile;
    public static final ICC_ColorSpace CMYKColorSpace;

    public static final ICC_Profile systemProfile;
    public static final ColorSpace systemColorSpace;

    public static final ICC_ColorSpace linearGrayColorSpace = (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);

    public static final ICC_ColorSpace sRGBColorSpace;
    public static final ICC_Profile sRGBColorProfile;
    public static final ICC_Profile sRGBExportColorProfile;

    public static final ICC_Profile adobeRGBProfile;
    public static final ColorSpace adobeRGBColorSpace;

    public static final ColorModel colorModel_linear16;
    public static final ColorModel colorModel_linear8;
    public static final ColorModel colorModel_sRGB16;
    public static final ColorModel colorModel_sRGB8;
    public static final ColorModel systemColorModel;
    public static final RenderingHints noCacheHint;
    public static final String PERSISTENT_CACHE_TAG = "LCPersistentCache";
    public static final TileCache noTileCache = new SunTileCache(0);
    public static final RenderingHints fileCacheHint;
    public static final TileCache fileCache;
    public static final TileCache defaultTileCache;

    /** Tile dimensions. */
    public static final int TILE_WIDTH = 512;
    public static final int TILE_HEIGHT = 512;

    // public static final int fastMode = DataBuffer.TYPE_BYTE;
    // public static final int preciseMode = DataBuffer.TYPE_USHORT;

    static void dumpProperty(ICC_Profile profile, int tag, String name)
    {
        byte[] data = profile.getData(tag);
        if (data != null) {
            System.out.print(name + " (" + data.length + ") :");
            for (byte aData : data)
                System.out.print(" " + (aData & 0xFF));
            System.out.println();

            for (byte aData : data)
                System.out.print((char) (aData & 0xFF));
            System.out.println();
        } else {
            System.out.println("no " + name + " info");
        }
    }

    /**
     * Given a {@link ColorSpace} and a {@link Raster}, get the
     * {@link ColorModel} for them.
     *
     * @param colorSpace The {@link ColorSpace}.
     * @param raster The {@link Raster}.
     * @return Returns the relevant {@link ColorModel} or <code>null</code> if
     * none can be determined.
     */
    public static ColorModel getColorModelFrom( ColorSpace colorSpace,
                                                Raster raster ) {
        return new ComponentColorModel(
            colorSpace, false, false, Transparency.OPAQUE,
            raster.getSampleModel().getDataType()
        );
    }

    /**
     * Given an {@link ICC_Profile} and a {@link Raster}, get the
     * {@link ColorModel} for them.
     *
     * @param profile The {@link ICC_Profile}.
     * @param raster The {@link Raster}.
     * @return Returns the relevant {@link ColorModel} or <code>null</code> if
     * none can be determined.
     */
    public static ColorModel getColorModelFrom( ICC_Profile profile,
                                                Raster raster ) {
        return getColorModelFrom(
            getColorSpaceFrom( profile, raster ), raster
        );
    }


    /**
     * Given an {@link ICC_Profile} and a {@link Raster}, get the
     * {@link ColorSpace} for them.
     *
     * @param profile The {@link ICC_Profile}.
     * @param raster The {@link Raster}.
     * @return Returns the relevant {@link ColorSpace} or <code>null</code> if
     * none can be determined.
     */
    public static ColorSpace getColorSpaceFrom( ICC_Profile profile,
                                                Raster raster ) {
        if ( profile != null )
            return new ICC_ColorSpace( profile );
        switch ( raster.getSampleModel().getNumBands() ) {
            case 1:
                return gray22ColorSpace;
            case 3:
                return sRGBColorSpace;
            case 4:
                return CMYKColorSpace;
            default:
                return null;
        }
    }

    static void zlum(ICC_ColorSpace cs) {
        float[] zero;
        synchronized (ColorSpace.class) {
            zero = cs.fromCIEXYZ(new float[] {0, 0, 0});
        }
        System.out.println("zero: "  + zero[0] + " : " + zero[1] + " : " + zero[2]);
        double zlum = ColorScience.Wr * zero[0] + ColorScience.Wg * zero[1] + ColorScience.Wb * zero[2];
        System.out.println("zero lum: " + zlum);
    }

    static {
        final long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.printf("Max Memory:   %11d%n", maxMemory);
        System.out.printf("Total Memory: %11d%n", Runtime.getRuntime().totalMemory());

        JAI jaiInstance = JAI.getDefaultInstance();

        // Use our own Tile Scheduler -- TODO: Needs more testing
        // jaiInstance.setTileScheduler(new LCTileScheduler());

        int processors = Runtime.getRuntime().availableProcessors();

        String VMVersion[] = System.getProperty("java.version").split("[._]");
        String OSArch = System.getProperty("os.arch");

        System.out.println("Running on " + processors + " processors");

        if (OSArch.equals("ppc") && Integer.parseInt(VMVersion[3]) < 7) {
            processors = Math.min(processors, 2);
            System.out.println("Old PPC Java, limiting to " + processors + " processors");
        }

        // don't use more than 2 processors, it uses too much memory,
        // and use 2 procs only if we have more than 750MB of heap

        final int MB = 1024 * 1024;

        if (maxMemory >= 400 * MB)
            jaiInstance.getTileScheduler().setParallelism(processors);
        else
            jaiInstance.getTileScheduler().setParallelism(1);

        fileCache = new LCTileCache(maxMemory <= 1024 * MB ? maxMemory/2 : maxMemory -  512 * MB, true);
        // fileCache.setMemoryThreshold(0.5f);
        jaiInstance.setTileCache(fileCache);
        fileCacheHint = new RenderingHints(JAI.KEY_TILE_CACHE, fileCache);
        defaultTileCache = jaiInstance.getTileCache();

        TileFactory rtf = new LCRecyclingTileFactory();
        jaiInstance.setRenderingHint(JAI.KEY_TILE_FACTORY, rtf);
        jaiInstance.setRenderingHint(JAI.KEY_TILE_RECYCLER, rtf);
        // TODO: causes rendering artifacts
        // jaiInstance.setRenderingHint(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED, Boolean.TRUE);

        OperationRegistry or = jaiInstance.getOperationRegistry();

        // register LCColorConvert
        OperationDescriptor desc = new LCColorConvertDescriptor();
        or.registerDescriptor(desc);
        ContextualRenderedImageFactory crif = new LCColorConvertCRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", crif);
        CRIFRegistry.register(or, desc.getName(), crif);

        // register LCMSColorConvert
        desc = new LCMSColorConvertDescriptor();
        or.registerDescriptor(desc);
        crif = new LCMSColorConvertCRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", crif);
        CRIFRegistry.register(or, desc.getName(), crif);

        // register BlendOp
        desc = new BlendDescriptor();
        or.registerDescriptor(desc);
        crif = new BlendCRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", crif);
        CRIFRegistry.register(or, desc.getName(), crif);

        // register LCSeparableConvolve
        desc = new LCSeparableConvolveDescriptor();
        or.registerDescriptor(desc);
        RenderedImageFactory rif = new LCSeparableConvolveRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", rif);

        // register NOPOp
        desc = new NOPDescriptor();
        or.registerDescriptor(desc);
        crif = new NOPCRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", crif);
        CRIFRegistry.register(or, desc.getName(), crif);

        // register UnSharpMaskOp
        desc = new UnSharpMaskDescriptor();
        or.registerDescriptor(desc);
        crif = new UnSharpMaskCRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", crif);
        CRIFRegistry.register(or, desc.getName(), crif);

        // register LCErode
        desc = new LCErodeDescriptor();
        or.registerDescriptor(desc);
        rif = new LCErodeRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", rif);

        // register RawAdjustments
        desc = new RawAdjustmentsDescriptor();
        or.registerDescriptor(desc);
        rif = new RawAdjustmentsCRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", rif);

        // register LCBandCombine
        desc = new LCBandCombineDescriptor();
        or.registerDescriptor(desc);
        rif = new LCBandCombineCRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", rif);

        // register LCBandCombine
        desc = new BilateralFilterDescriptor();
        or.registerDescriptor(desc);
        rif = new BilateralFilterRIF();
        RIFRegistry.register(or, desc.getName(), "com.lightcrafts", rif);

        JAI.setDefaultTileSize(new Dimension(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT));

        systemProfiles = new ArrayList<ColorProfileInfo>();
        final Collection<ColorProfileInfo> exportProfiles =
            Platform.getPlatform().getExportProfiles();
        if (exportProfiles != null) {
            systemProfiles.addAll(exportProfiles);
        }
        final Collection<ColorProfileInfo> printerProfiles =
            Platform.getPlatform().getPrinterProfiles();
        if (printerProfiles != null) {
            systemProfiles.addAll(printerProfiles);
        }

        ICC_Profile _sRGBColorProfile = null;
        for (ColorProfileInfo cpi : systemProfiles) {
            if ((cpi.getName().equals("sRGB Profile") || cpi.getName().equals("sRGB IEC61966-2.1"))) {
                try {
                    _sRGBColorProfile = ICC_Profile.getInstance(cpi.getPath());
                    System.out.println("found " + cpi.getName());
                    if (_sRGBColorProfile != null)
                        break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (_sRGBColorProfile == null) {
            InputStream in = JAIContext.class.getResourceAsStream("resources/sRGB.icc");
            try {
                _sRGBColorProfile = ICC_Profile.getInstance(in);
            } catch (IOException e) {
                System.err.println("Can't load resource sRGB profile, defaulting on Java's");
                _sRGBColorProfile = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
            }
        }
        sRGBExportColorProfile = _sRGBColorProfile;

        sRGBColorProfile = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        sRGBColorSpace = (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);

        ICC_Profile _linearProfile;
        ICC_Profile _oldLinearProfile;
        ICC_Profile _labProfile;
        // ICC_Profile _uniformLabProfile;
        ICC_Profile _gray22Profile;
        ICC_Profile _adobeRGBProfile;
        ICC_ColorSpace _linearColorSpace;
        ICC_ColorSpace _oldLinearColorSpace;
        ICC_ColorSpace _labColorSpace;
        // ICC_ColorSpace _uniformLabColorSpace;
        ICC_ColorSpace _gray22ColorSpace;
        ICC_ColorSpace _adobeRGBColorSpace;
        ColorModel _colorModel_linear16;
        ColorModel _colorModel_linear8;
        ColorModel _colorModel_sRGB16;
        ColorModel _colorModel_sRGB8;

        ICC_Profile _CMYKProfile;
        ICC_ColorSpace _CMYKColorSpace;

        RenderingHints _noCacheHint;

        ICC_Profile _systemProfile;
        ICC_ColorSpace _systemColorSpace;
        ColorModel _systemColorModel;

        try {
            InputStream in = JAIContext.class.getResourceAsStream("resources/rimm.icm");
            _linearProfile = ICC_Profile.getInstance(in);
            _linearColorSpace = new ICC_ColorSpace(_linearProfile);

            in = JAIContext.class.getResourceAsStream("resources/CIE 1931 D50 Gamma 1.icm");
            _oldLinearProfile = ICC_Profile.getInstance(in);
            _oldLinearColorSpace = new ICC_ColorSpace(_linearProfile);

            in = JAIContext.class.getResourceAsStream("resources/Generic CMYK Profile.icc");
            _CMYKProfile = ICC_Profile.getInstance(in);
            _CMYKColorSpace = new ICC_ColorSpace(_CMYKProfile);

            in = JAIContext.class.getResourceAsStream("resources/Generic Lab Profile.icm");
            _labProfile = ICC_Profile.getInstance(in);
            _labColorSpace = new ICC_ColorSpace(_labProfile);

            in = JAIContext.class.getResourceAsStream("resources/Gray Gamma 2.2.icc");
            _gray22Profile = ICC_Profile.getInstance(in);
            _gray22ColorSpace = new ICC_ColorSpace(_gray22Profile);

            in = JAIContext.class.getResourceAsStream("resources/compatibleWithAdobeRGB1998.icc");
            _adobeRGBProfile = ICC_Profile.getInstance(in);
            _adobeRGBColorSpace = new ICC_ColorSpace(_adobeRGBProfile);

            _colorModel_linear16 =
                    RasterFactory.createComponentColorModel(DataBuffer.TYPE_USHORT,
                            _linearColorSpace,
                            false,
                            false,
                            Transparency.OPAQUE);
            _colorModel_linear8 =
                    RasterFactory.createComponentColorModel(DataBuffer.TYPE_BYTE,
                            _linearColorSpace,
                            false,
                            false,
                            Transparency.OPAQUE);
            _colorModel_sRGB16 =
                    RasterFactory.createComponentColorModel(DataBuffer.TYPE_USHORT,
                            JAIContext.sRGBColorSpace,
                            false,
                            false,
                            Transparency.OPAQUE);
            _colorModel_sRGB8 =
                    RasterFactory.createComponentColorModel(DataBuffer.TYPE_BYTE,
                            JAIContext.sRGBColorSpace,
                            false,
                            false,
                            Transparency.OPAQUE);

            _systemColorSpace = sRGBColorSpace;
            _systemColorModel = _colorModel_sRGB8;
            _systemProfile = _sRGBColorProfile;

            try {
                final ICC_Profile displayProfile = Platform.getPlatform().getDisplayProfile();
                if (displayProfile != null) {
                    _systemProfile = displayProfile;
                    _systemColorSpace = new ICC_ColorSpace(_systemProfile);
                    _systemColorModel = RasterFactory.createComponentColorModel(DataBuffer.TYPE_BYTE,
                                                                                _systemColorSpace,
                                                                                false,
                                                                                false,
                                                                                Transparency.OPAQUE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            _noCacheHint = new RenderingHints(JAI.KEY_TILE_CACHE, noTileCache);
            // _noCacheHint = new RenderingHints(JAI.KEY_TILE_CACHE, defaultTileCache);
        } catch (IOException e) {
            e.printStackTrace();
            // Rethrow so the Application will notice the problem:
            throw new RuntimeException(
                "Couldn't access color space resource",
                e
            );
        }
        linearProfile = _linearProfile;
        linearColorSpace = _linearColorSpace;

        oldLinearProfile = _oldLinearProfile;
        oldLinearColorSpace = _oldLinearColorSpace;

        colorModel_linear16 = _colorModel_linear16;
        colorModel_linear8 = _colorModel_linear8;
        colorModel_sRGB16 = _colorModel_sRGB16;
        colorModel_sRGB8 = _colorModel_sRGB8;

        CMYKProfile = _CMYKProfile;
        CMYKColorSpace = _CMYKColorSpace;

        labProfile = _labProfile;
        labColorSpace = _labColorSpace;

        gray22Profile = _gray22Profile;
        gray22ColorSpace = _gray22ColorSpace;

        adobeRGBProfile = _adobeRGBProfile;
        adobeRGBColorSpace = _adobeRGBColorSpace;

        systemProfile = _systemProfile;

        systemColorSpace = _systemColorSpace;
        systemColorModel = _systemColorModel;

        noCacheHint = _noCacheHint;
    }
}
/* vim:set et sw=4 ts=4: */
