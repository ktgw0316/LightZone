/*
 * $RCSfile: PointOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:15 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Vector;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An abstract base class for image operators that require only the
 * (x, y) pixel from each source image in order to compute the
 * destination pixel (x, y).
 *
 * <p> <code>PointOpImage</code> is intended as a convenient
 * superclass for <code>OpImage</code>s that only need to look at each
 * destination pixel's corresponding source pixels.  Some examples are
 * lookup, contrast adjustment, pixel arithmetic, and color space
 * conversion.
 *
 * @see OpImage
 */
public abstract class PointOpImage extends OpImage {

    /* Flag indicating that dispose() has been invoked. */
    private boolean isDisposed = false;

    /* Flag indicating whether the various flags have been set. */
    private boolean areFieldsInitialized = false;

    /* Flag indicating that in-place compatibility should be checked. */
    private boolean checkInPlaceOperation = false;

    /* Flag indicating whether in-place operation is enabled. */
    private boolean isInPlaceEnabled = false;

    /// BEGIN: Variable fields used only when in-place operation is enabled.
    /* The first source cast to a WritableRenderedImage. */
    private WritableRenderedImage source0AsWritableRenderedImage;

    /* The first source cast to an OpImage. */
    private OpImage source0AsOpImage;

    /* Flag indicating whether the first source is a WritableRenderedImage. */
    private boolean source0IsWritableRenderedImage;
    /// END: Variable fields use only when in-place operation is enabled.

    /* Flag indicating whether the bounds are the same as for all sources. */
    private boolean sameBounds;

    /* Flag indicating whether the tile grid is the same as for all sources. */
    private boolean sameTileGrid;
    // END in-place fields.

    /** Fills in the default layout settings. */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            Vector sources,
                                            Map config) {
        int numSources = sources.size();

        if(numSources < 1) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic5"));
        }

        RenderedImage source0 = (RenderedImage)sources.get(0);
        Rectangle isect =
            new Rectangle(source0.getMinX(), source0.getMinY(),
                          source0.getWidth(), source0.getHeight());

        Rectangle rect = new Rectangle();
        for (int i = 1; i < numSources; i++) {
            RenderedImage s = (RenderedImage)sources.get(i);
            rect.setBounds(s.getMinX(), s.getMinY(),
                           s.getWidth(), s.getHeight());
            isect = isect.intersection(rect);
        }

        if (isect.isEmpty()) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PointOpImage0"));
        }

        if(layout == null) {
            layout = new ImageLayout(isect.x, isect.y,
                                     isect.width, isect.height);
        } else {
            layout = (ImageLayout)layout.clone();
            if (!layout.isValid(ImageLayout.MIN_X_MASK)) {
                layout.setMinX(isect.x);
            }
            if (!layout.isValid(ImageLayout.MIN_Y_MASK)) {
                layout.setMinY(isect.y);
            }
            if (!layout.isValid(ImageLayout.WIDTH_MASK)) {
                layout.setWidth(isect.width);
            }
            if (!layout.isValid(ImageLayout.HEIGHT_MASK)) {
                layout.setHeight(isect.height);
            }

            Rectangle r = new Rectangle(layout.getMinX(null),
                                        layout.getMinY(null),
                                        layout.getWidth(null),
                                        layout.getHeight(null));
            if (r.isEmpty()) {
                throw new IllegalArgumentException(
                    JaiI18N.getString("PointOpImage1"));
            }

            if (!isect.contains(r)) {
                throw new IllegalArgumentException(
                    JaiI18N.getString("PointOpImage2"));
            }
        }

        // If no SampleModel is given, create a new SampleModel (and perhaps
        // ColorModel) corresponding to the minimum band count and maximum
        // data depth.
        if (numSources > 1 && !layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            // Determine the min number of bands and max range of data

            SampleModel sm = source0.getSampleModel();
            ColorModel cm = source0.getColorModel();
            int dtype0 = getAppropriateDataType(sm);
            int bands0 = getBandCount(sm, cm);
            int dtype = dtype0;
            int bands = bands0;

            for (int i = 1; i < numSources; i++) {
                RenderedImage source = (RenderedImage)sources.get(i);
                sm = source.getSampleModel();
                cm = source.getColorModel();
                int sourceBands = getBandCount(sm, cm);

                dtype = mergeTypes(dtype, getPixelType(sm));
                bands = Math.min(bands, sourceBands);
            }

            // Force data type to byte for multi-band bilevel data.
            if(dtype == PixelAccessor.TYPE_BIT && bands > 1) {
                dtype = DataBuffer.TYPE_BYTE;
            }

            // Set a new SampleModel if and only if that of source 0 is
            // not compatible.
            SampleModel sm0 = source0.getSampleModel();
            if(dtype != sm0.getDataType() || bands != sm0.getNumBands()) {
                int tw = layout.getTileWidth(source0);
                int th = layout.getTileHeight(source0);
                SampleModel sampleModel;
                if(dtype == PixelAccessor.TYPE_BIT) {
                    sampleModel =
                        new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                        tw, th, 1);
                } else {
                    sampleModel =
                        RasterFactory.createPixelInterleavedSampleModel(dtype,
                                                                        tw, th,
                                                                        bands);
                }

                layout.setSampleModel(sampleModel);

                // Set a new ColorModel only if this one is incompatible.
                if(cm != null &&
                   !JDKWorkarounds.areCompatibleDataModels(sampleModel, cm)) {
                    cm = ImageUtil.getCompatibleColorModel(sampleModel, config);
                    layout.setColorModel(cm);
                }
            }
        }

        return layout;
    }

    /**
     * Returns the pixel type.
     */
    private static int getPixelType(SampleModel sampleModel) {
        return ImageUtil.isBinary(sampleModel) ?
            PixelAccessor.TYPE_BIT : sampleModel.getDataType();
    }

    /**
     * Returns the number of bands.
     */
    private static int getBandCount(SampleModel sampleModel,
                                    ColorModel colorModel) {
        if(ImageUtil.isBinary(sampleModel)) {
            return 1;
        } else  if (colorModel instanceof IndexColorModel) {
            return colorModel.getNumComponents();
        } else {
            return sampleModel.getNumBands();
        }
    }

    /**
     * Determine the appropriate data type for the SampleModel.
     */
    private static int getAppropriateDataType(SampleModel sampleModel) {
        int dataType = sampleModel.getDataType();
        int retVal = dataType;

        if (ImageUtil.isBinary(sampleModel)) {
            retVal = PixelAccessor.TYPE_BIT;
        } else if (dataType == DataBuffer.TYPE_USHORT ||
            dataType == DataBuffer.TYPE_INT) {
            boolean canUseBytes = true;
            boolean canUseShorts = true;

            int[] ss = sampleModel.getSampleSize();
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] > 16) {
                    canUseBytes = false;
                    canUseShorts = false;
                    break;
                }
                if (ss[i] > 8) {
                    canUseBytes = false;
                }
            }

            if (canUseBytes) {
                retVal = DataBuffer.TYPE_BYTE;
            } else if (canUseShorts) {
                retVal = DataBuffer.TYPE_USHORT;
            }
        }

        return retVal;
    }

    /**
     * Returns a type (one of the enumerated constants from
     * DataBuffer) that has sufficent range to contain values from
     * either of two given types.  This corresponds to an upwards move
     * in the type lattice.
     *
     * <p> Note that the merge of SHORT and USHORT is INT, so it is not
     * correct to simply use the larger of the types.
     */
    private static int mergeTypes(int type0, int type1) {
        if (type0 == type1) {
            return type0;
        }

        // Default to second type.
        int type = type1;

        // Use switch logic to avoid depending on monotonicity of
        // DataBuffer.TYPE_*.
        switch(type0) {
        case PixelAccessor.TYPE_BIT:
        case DataBuffer.TYPE_BYTE:
            // Do nothing.
            break;
        case DataBuffer.TYPE_SHORT:
            if(type1 == DataBuffer.TYPE_BYTE) {
                type = DataBuffer.TYPE_SHORT;
            } else if(type1 == DataBuffer.TYPE_USHORT) {
                type = DataBuffer.TYPE_INT;
            }
            break;
        case DataBuffer.TYPE_USHORT:
            if(type1 == DataBuffer.TYPE_BYTE) {
                type = DataBuffer.TYPE_USHORT;
            } else if(type1 == DataBuffer.TYPE_SHORT) {
                type = DataBuffer.TYPE_INT;
            }
            break;
        case DataBuffer.TYPE_INT:
            if(type1 == DataBuffer.TYPE_BYTE ||
               type1 == DataBuffer.TYPE_SHORT ||
               type1 == DataBuffer.TYPE_USHORT) {
                type = DataBuffer.TYPE_INT;
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            if(type1 != DataBuffer.TYPE_DOUBLE) {
                type = DataBuffer.TYPE_FLOAT;
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            type = DataBuffer.TYPE_DOUBLE;
            break;
        }

        return type;
    }

    /**
     * Constructor.
     *
     * <p> There must be at least one valid source supplied via the
     * <code>sources</code> argument.  However, there is no upper limit
     * on the number of sources this image may have.
     *
     * <p> The image's layout is encapsulated in the <code>layout</code>
     * argument.  If the image bounds are supplied they must be contained
     * within the intersected source bounds which must be non-empty.
     * If the bounds are not supplied, they are calculated to be the
     * intersection of the bounds of all sources.
     *
     * <p> If no <code>SampleModel</code> is specified in the layout, a new
     * <code>SampleModel</code> will be created.  This <code>SampleModel</code>
     * will have a number of bands equal to the minimum band count of all
     * sources and a depth which can accomodate the data of all sources.
     * The band count of sources which have an <code>IndexColorModel</code>
     * will be set to the number of components of the
     * <code>IndexColorModel</code> instead of to the number of bands of the
     * <code>SampleModel</code>.
     *
     * <p> In all cases, the layout is forwarded to the <code>OpImage</code>
     * constructor which sets the default layout values in the standard way.
     *
     * @param layout  The layout parameters of the destination image.
     * @param sources  The source images.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources  <code>true</code> if computeRect() expects
     *        contiguous sources.
     *
     * @throws IllegalArgumentException  If <code>sources</code> or any
     *         object in <code>sources</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>sources</code> does not
     *         contain at least one element.
     * @throws ClassCastException  If any object in <code>sources</code>
     *         is not a <code>RenderedImage</code>.
     * @throws IllegalArgumentException  If combining the intersected
     *         source bounds with the user-specified bounds, if any,
     *         yields an empty rectangle, or the user-specified image bounds
     *         extends beyond the intersection of all the source bounds.
     *
     * @since JAI 1.1
     */
    public PointOpImage(Vector sources,
                        ImageLayout layout,
                        Map configuration,
                        boolean cobbleSources) {
        super(checkSourceVector(sources, true),
              layoutHelper(layout, sources, configuration),
              configuration,
              cobbleSources);
    }

    /**
     * Constructs a <code>PointOpImage</code> with one source image.
     * The image layout is computed as described in the constructor
     * taking a <code>Vector</code> of sources.
     *
     * @param layout  The layout parameters of the destination image.
     * @param source  The source image.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources  Indicates whether <code>computeRect()</code>
     *        expects contiguous sources.
     *
     * @throws IllegalArgumentException if <code>source</code>
     *         is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public PointOpImage(RenderedImage source,
                        ImageLayout layout,
                        Map configuration,
                        boolean cobbleSources) {
        this(vectorize(source), // vectorize() checks for null source.
             layout, configuration, cobbleSources);
    }

    /**
     * Constructs a <code>PointOpImage</code> with two source images.
     * The image layout is computed as described in the constructor
     * taking a <code>Vector</code> of sources.
     *
     * @param layout  The layout parameters of the destination image.
     * @param source0  The first source image.
     * @param source1  The second source image.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources  Indicates whether <code>computeRect()</code>
     *        expects contiguous sources.
     *
     * @throws IllegalArgumentException if <code>source0</code> or
     *         <code>source1</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public PointOpImage(RenderedImage source0,
                        RenderedImage source1,
                        ImageLayout layout,
                        Map configuration,
                        boolean cobbleSources) {
        this(vectorize(source0, source1), // vectorize() checks for null sources.
             layout, configuration, cobbleSources);
    }

    /**
     * Constructs a <code>PointOpImage</code> with three source
     * images.  The image layout is computed as described in the
     * constructor taking a <code>Vector</code> of sources.
     *
     * @param layout  The layout parameters of the destination image.
     * @param source0  The first source image.
     * @param source1  The second source image.
     * @param source2  The third source image.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources  Indicates whether <code>computeRect()</code>
     *        expects contiguous sources.
     *
     * @throws IllegalArgumentException if <code>source0</code> or
     *         <code>source1</code> or <code>source2</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public PointOpImage(RenderedImage source0,
                        RenderedImage source1,
                        RenderedImage source2,
                        ImageLayout layout,
                        Map configuration,
                        boolean cobbleSources) {
        this(vectorize(source0, source1, source2), // vectorize() checks null
             layout, configuration, cobbleSources);

    }

    /*
     * Initialize flags and instance variables.
     */
    private synchronized void initializeFields() {

        if (areFieldsInitialized)
	    return;

        PlanarImage source0 = getSource(0);

        if (checkInPlaceOperation) {
            // Set the in-place operation flag.
            // XXX: In-place operation could work equally well when source0
            // is a WritableRenderedImage. However this could produce
            // unexpected results so consequently it would be desirable if
            // some kind of hint could be set that in-place operation is to
            // be used. The following statement should then be changed such
            // that instead of
            //
            //     source0 instanceof OpImage &&
            //
            // we have
            //
            //     (source0 instanceof OpImage ||
            //      (source0 instanceof WritableRenderedImage &&
            //       isInPlaceHintSet))
            //
	    Vector source0Sinks = source0.getSinks();
            isInPlaceEnabled = source0 != null &&
                getTileGridXOffset() == source0.getTileGridXOffset() &&
                getTileGridYOffset() == source0.getTileGridYOffset() &&
                getBounds().equals(source0.getBounds()) &&
                source0 instanceof OpImage &&
                hasCompatibleSampleModel(source0) &&
		!(source0Sinks != null && source0Sinks.size() > 1);

            // Ensure that source0 computes unique tiles, i.e.,
            // computesUniqueTiles() returns false.  This disqualifies
            // for example in-place operations when source0 is an instance
            // of NullOpImage or of a subclass of NullOpImage or
            // SourcelessOpImage which does not override
            // computesUniqueTiles() to return true.
            if (isInPlaceEnabled &&
                !((OpImage)source0).computesUniqueTiles()) {
                isInPlaceEnabled = false;
            }

            // Unset the in-place flag if getTile() is overridden by the
            // class of which source0 is an instance.
            if (isInPlaceEnabled) {
                try {
                    Method getTileMethod =
                        source0.getClass().getMethod("getTile",
                                                     new Class[] {int.class,
                                                                  int.class});
                    Class opImageClass =
                        Class.forName("com.lightcrafts.mediax.jai.OpImage");
                    Class declaringClass = getTileMethod.getDeclaringClass();

                    // Unset in-place flag if getTile() is overridden.
                    if(!declaringClass.equals(opImageClass)) {
                        isInPlaceEnabled = false;
                    }
                } catch(ClassNotFoundException e) {
                    isInPlaceEnabled = false;
                } catch(NoSuchMethodException e) {
                    isInPlaceEnabled = false;
                }
            }

            // Set local fields as a function of the in-place operation flag.
            if (isInPlaceEnabled) {
                // Set the flag indicating source0's type.
                source0IsWritableRenderedImage =
                    source0 instanceof WritableRenderedImage;

                // Cast the first source to one of the cached image variables.
                if (source0IsWritableRenderedImage) {
                    source0AsWritableRenderedImage =
                        (WritableRenderedImage)source0;
                } else {
                    source0AsOpImage = (OpImage)source0;
                }
            }

            // Unset this flag.
            checkInPlaceOperation = false;
        }

        // Get the number of sources.
        int numSources = getNumSources();

        // Initialize the bounds and tile grid flags.
        sameBounds = true;
        sameTileGrid = true;

        // Loop over all sources or until both flags are false.
        for(int i = 0; i < numSources && (sameBounds || sameTileGrid); i++) {
            PlanarImage source = getSource(i);

            // Update the bounds flag.
            if (sameBounds) {
                sameBounds = sameBounds &&
                    minX == source.minX && minY == source.minY &&
                    width == source.width && height == source.height;
            }

            // Update the tile grid flag.
            if (sameTileGrid) {
                sameTileGrid = sameTileGrid &&
                    tileGridXOffset == source.tileGridXOffset &&
                    tileGridYOffset == source.tileGridYOffset &&
                    tileWidth == source.tileWidth &&
                    tileHeight == source.tileHeight;
            }
        }

        // Set this flag.
        areFieldsInitialized = true;
    }

    /*
     * Check whether the <code>SampleModel</code> of the argument
     * <code>PlanarImage</code>is compatible with that of this
     * <code>PointOpImage</code>.
     *
     * @param src The <code>PlanarImage</code> whose <code>SampleModel</code>
     * is to be checked.
     * @return Whether the parameter has a compatible <code>SampleModel</code>.
     */
    private boolean hasCompatibleSampleModel(PlanarImage src) {
        SampleModel srcSM = src.getSampleModel();
        int numBands = sampleModel.getNumBands();

        boolean isCompatible =
            srcSM.getTransferType() == sampleModel.getTransferType() &&
            srcSM.getWidth() == sampleModel.getWidth() &&
            srcSM.getHeight() == sampleModel.getHeight() &&
            srcSM.getNumBands() == numBands &&
            srcSM.getClass().equals(sampleModel.getClass());

        if (isCompatible) {
            if (sampleModel instanceof ComponentSampleModel) {
                ComponentSampleModel smSrc = (ComponentSampleModel)srcSM;
                ComponentSampleModel smDst = (ComponentSampleModel)sampleModel;
                isCompatible = isCompatible &&
                    smSrc.getPixelStride() == smDst.getPixelStride() &&
                    smSrc.getScanlineStride() == smDst.getScanlineStride();
                int[] biSrc = smSrc.getBankIndices();
                int[] biDst = smDst.getBankIndices();
                int[] boSrc = smSrc.getBandOffsets();
                int[] boDst = smDst.getBandOffsets();
                for(int b = 0; b < numBands && isCompatible; b++) {
                    isCompatible = isCompatible &&
                        biSrc[b] == biDst[b] &&
                        boSrc[b] == boDst[b];
                }
            } else if (sampleModel instanceof
                      SinglePixelPackedSampleModel) {
                SinglePixelPackedSampleModel smSrc =
                    (SinglePixelPackedSampleModel)srcSM;
                SinglePixelPackedSampleModel smDst =
                    (SinglePixelPackedSampleModel)sampleModel;
                isCompatible = isCompatible &&
                    smSrc.getScanlineStride() == smDst.getScanlineStride();
                int[] bmSrc = smSrc.getBitMasks();
                int[] bmDst = smDst.getBitMasks();
                for(int b = 0; b < numBands && isCompatible; b++) {
                    isCompatible = isCompatible &&
                        bmSrc[b] == bmDst[b];
                }
            } else if (sampleModel instanceof MultiPixelPackedSampleModel) {
                MultiPixelPackedSampleModel smSrc =
                    (MultiPixelPackedSampleModel)srcSM;
                MultiPixelPackedSampleModel smDst =
                    (MultiPixelPackedSampleModel)sampleModel;
                isCompatible = isCompatible &&
                    smSrc.getPixelBitStride() == smDst.getPixelBitStride() &&
                    smSrc.getScanlineStride() == smDst.getScanlineStride() &&
                    smSrc.getDataBitOffset() == smDst.getDataBitOffset();
            } else {
                isCompatible = false;
            }
        }

        return isCompatible;
    }

    /**
     * Causes a flag to be set to indicate that in-place operation should
     * be permitted if the image bounds, tile grid offset, tile dimensions,
     * and SampleModels of the source and destination images are compatible.
     * This method should be invoked in the constructor of the implementation
     * of a given operation only if that implementation is amenable to
     * in-place computation.  Invocation of this method is a necessary but
     * not a sufficient condition for in-place computation actually to occur.
     * If the system property "com.lightcrafts.mediax.jai.PointOpImage.InPlace" is equal
     * to the string "false" in a case-insensitive fashion then in-place
     * operation will not be permitted.
     */
    protected void permitInPlaceOperation() {
        // Retrieve the in-place property.
        Object inPlaceProperty = null;
        try {
            inPlaceProperty =
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        String name = "com.lightcrafts.mediax.jai.PointOpImage.InPlace";
                        return System.getProperty(name);
                    }
                });
        } catch (SecurityException se) {
            /// as if the property isn't set
        }
        // Set the flag to false if and only if the property is set to
        // the string "false" (case-insensitive).
        checkInPlaceOperation =
            !(inPlaceProperty != null &&
              inPlaceProperty instanceof String &&
              ((String)inPlaceProperty).equalsIgnoreCase("false"));
    }

    /**
     * Indicates whether the operation is being effected directly on the
     * associated colormap.  This method will in general return
     * <code>true</code> if the image is the destination of a unary,
     * shift-invariant operation with an <code>IndexColorModel</code> equal
     * to that of its unique source.
     *
     * <p> When this method returns <code>true</code> the
     * <code>computeTile()</code> method in this class will return either
     * a copy of the corresponding region of the first source image or,
     * if the operation is being performed in place, the corresponding
     * tile of the first source image.
     *
     * <p> The implementation in this class always returns <code>false</code>.
     *
     * @since JAI 1.1
     */
    protected boolean isColormapOperation() {
        return false;
    }

    /**
     * Computes a tile.  If source cobbling was requested at
     * construction time, the source tile boundaries are overlayed
     * onto the destination and <code>computeRect(Raster[],
     * WritableRaster, Rectangle)</code> is called for each of the
     * resulting regions.  Otherwise, <code>computeRect(PlanarImage[],
     * WritableRaster, Rectangle)</code> is called once to compute the
     * entire active area of the tile.
     *
     * <p> The image bounds may be larger than the bounds of the
     * source image.  In this case, samples for which there are no
     * corresponding sources are set to zero.
     *
     * @param tileX  The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        if (cobbleSources) {
            return super.computeTile(tileX, tileY);
        }

        // Make sure the fields are initialized.
	initializeFields();

        // Get a WritableRaster to represent this tile.
        WritableRaster dest = null;
        if (isInPlaceEnabled) {
            if (source0IsWritableRenderedImage) {
                // Check one out from the WritableRenderedImage source.
                dest = source0AsWritableRenderedImage.getWritableTile(tileX,
                                                                      tileY);
            } else { // source0 is OpImage
                // Re-use one from the OpImage source.
                // First check whether the source raster is cached.
                Raster raster =
                    source0AsOpImage.getTileFromCache(tileX, tileY);

                if (raster == null) {
                    // Compute the tile.
                    try {
                        raster = source0AsOpImage.computeTile(tileX, tileY);
                        if (raster instanceof WritableRaster) {
                            dest = (WritableRaster)raster;
                        }
                    } catch(Exception e) {
                        // Do nothing: this catch is simply in case the
                        // OpImage in question does not itself implement
                        // computeTile() in which case it may be resolved
                        // to OpImage.computeTile() which will throw an
                        // Exception.
                    }
                }
            }
        }

        // Set tile recycling flag.
        boolean recyclingSource0Tile = dest != null;

        if (!recyclingSource0Tile) {
            // Create a new WritableRaster.
            Point org = new Point(tileXToX(tileX), tileYToY(tileY));
            dest = createWritableRaster(sampleModel, org);
        }

        // Colormap operation: return the source Raster if operating
        // in place or a copy thereof otherwise.
        if(isColormapOperation()) {
            if(!recyclingSource0Tile) {
                PlanarImage src = getSource(0);
                Raster srcTile = null;
                Rectangle srcRect = null;
                Rectangle dstRect = dest.getBounds();

                // Confirm that the tile grids of the source and destination
                // are the same
                if (sameTileGrid) {
                    // Tile grids are aligned so the tile indices correspond
                    // to pixels at the same locations in source and destination
                    srcTile = getSource(0).getTile(tileX, tileY);
                }
                else if (dstRect.intersects(src.getBounds())) {
                    // Tile grids are not aligned but the destination rectangle
                    // intersects the source bounds so get the data using
                    // the destination rectangle
                    srcTile = src.getData(dstRect);
                }
                else {
                    // The destination rectangle does not interest the source
                    // bounds so just return the destination.
                    return dest;
                }

                srcRect = srcTile.getBounds();

                // Ensure that the source tile doesn't lie outside the
                // destination tile.
                if(!dstRect.contains(srcRect)) {
                    srcRect = dstRect.intersection(srcRect);
                    srcTile =
                        srcTile.createChild(srcTile.getMinX(),
                                            srcTile.getMinY(),
                                            srcRect.width,
                                            srcRect.height,
                                            srcRect.x,
                                            srcRect.y,
                                            null);
                }

                JDKWorkarounds.setRect(dest, srcTile, 0, 0);
            }
            return dest;
        }

        // Output bounds are initially equal to the tile bounds.
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destMaxX = destMinX + dest.getWidth();
        int destMaxY = destMinY + dest.getHeight();

        // Clip output bounds to the dest image bounds.
        Rectangle bounds = getBounds();
        if (destMinX < bounds.x) {
            destMinX = bounds.x;
        }
        int boundsMaxX = bounds.x + bounds.width;
        if (destMaxX > boundsMaxX) {
            destMaxX = boundsMaxX;
        }
        if (destMinY < bounds.y) {
            destMinY = bounds.y;
        }
        int boundsMaxY = bounds.y + bounds.height;
        if (destMaxY > boundsMaxY) {
            destMaxY = boundsMaxY;
        }

        // Get the number of sources.
        int numSrcs = getNumSources();

        // Branch to actual destination rectangle computation as a
        // function of in-place operation and layout compatibility.
        if (recyclingSource0Tile && numSrcs == 1) {
            // Recycling tile from a single source.
            Raster[] sources = new Raster[] {dest};
            Rectangle destRect = new Rectangle(destMinX, destMinY,
                                               destMaxX - destMinX,
                                               destMaxY - destMinY);
            computeRect(sources, dest, destRect);
        } else if (recyclingSource0Tile && sameBounds && sameTileGrid) {
            // Recycling tile from first of layout-compatible sources.
            Raster[] sources = new Raster[numSrcs];
            sources[0] = dest;
            for(int i = 1; i < numSrcs; i++) {
                sources[i] = getSource(i).getTile(tileX, tileY);
            }
            Rectangle destRect = new Rectangle(destMinX, destMinY,
                                               destMaxX - destMinX,
                                               destMaxY - destMinY);
            computeRect(sources, dest, destRect);
        } else {
            // Clip against source bounds only if necessary.
            if (!sameBounds) {
                // Clip output bounds to each source image bounds
                for (int i = recyclingSource0Tile ? 1 : 0; i < numSrcs; i++) {
                    bounds = getSource(i).getBounds();
                    if (destMinX < bounds.x) {
                        destMinX = bounds.x;
                    }
                    boundsMaxX = bounds.x + bounds.width;
                    if (destMaxX > boundsMaxX) {
                        destMaxX = boundsMaxX;
                    }
                    if (destMinY < bounds.y) {
                        destMinY = bounds.y;
                    }
                    boundsMaxY = bounds.y + bounds.height;
                    if (destMaxY > boundsMaxY) {
                        destMaxY = boundsMaxY;
                    }

                    if (destMinX >= destMaxX || destMinY >= destMaxY) {
                        return dest;	// no corresponding source region
                    }
                }
            }

            // Initialize the (possibly clipped) destination Rectangle.
            Rectangle destRect = new Rectangle(destMinX, destMinY,
                                               destMaxX - destMinX,
                                               destMaxY - destMinY);

            // Allocate memory for source Rasters.
            Raster[] sources = new Raster[numSrcs];

            if (sameTileGrid) {
                // All sources share the tile grid of the destination so
                // there is no need for splits.
                if (recyclingSource0Tile) {
                    sources[0] = dest;
                }
                for (int i = recyclingSource0Tile ? 1 : 0; i < numSrcs; i++) {
                    sources[i] = getSource(i).getTile(tileX, tileY);
                }

                computeRect(sources, dest, destRect);
            } else {
                //
                // The tileWidth and tileHeight of the source image
                // may differ from this tileWidth and tileHeight.
                //
                IntegerSequence xSplits =
                    new IntegerSequence(destMinX, destMaxX);
                xSplits.insert(destMinX);
                xSplits.insert(destMaxX);

                IntegerSequence ySplits =
                    new IntegerSequence(destMinY, destMaxY);
                ySplits.insert(destMinY);
                ySplits.insert(destMaxY);

                for (int i = recyclingSource0Tile ? 1 : 0; i < numSrcs; i++) {
                    PlanarImage s = getSource(i);
                    s.getSplits(xSplits, ySplits, destRect);
                }

                //
                // Divide destRect into sub rectangles based on the source
                // splits, and compute each sub rectangle separately.
                //
                int x1, x2, y1, y2, w, h;
                Rectangle subRect = new Rectangle();

                ySplits.startEnumeration();
                for (y1 = ySplits.nextElement(); ySplits.hasMoreElements(); y1 = y2) {
                    y2 = ySplits.nextElement();
                    h = y2 - y1;

                    xSplits.startEnumeration();
                    for (x1 = xSplits.nextElement();
                         xSplits.hasMoreElements(); x1 = x2) {
                        x2 = xSplits.nextElement();
                        w = x2 - x1;

                        // Get sources.
                        if (recyclingSource0Tile) {
                            sources[0] = dest;
                        }
                        for (int i = recyclingSource0Tile ? 1: 0;
                             i < numSrcs; i++) {
                            PlanarImage s = getSource(i);
                            int tx = s.XToTileX(x1);
                            int ty = s.YToTileY(y1);
                            sources[i] = s.getTile(tx, ty);
                        }

                        subRect.x = x1;
                        subRect.y = y1;
                        subRect.width = w;
                        subRect.height = h;
                        computeRect(sources, dest, subRect);
                    }
                }
            }
        }

        if (recyclingSource0Tile && source0IsWritableRenderedImage) {
            source0AsWritableRenderedImage.releaseWritableTile(tileX, tileY);
        }

        return dest;
    }

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source. The resulting <code>Rectangle</code> is <u>not</u>
     * clipped to the destination image bounds.
     *
     * @param sourceRect the <code>Rectangle</code> in source coordinates.
     * @param sourceIndex the index of the source image.
     * @return a <code>Rectangle</code> indicating the potentially affected
     *         destination region, or <code>null</code> if the region is unknown.
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public final Rectangle mapSourceRect(Rectangle sourceRect,
                                         int sourceIndex) {
        if ( sourceRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }
        return new Rectangle(sourceRect);
    }

    /**
     * Returns a conservative estimate of the region of a specific
     * source that is required in order to compute the pixels of a
     * given destination rectangle. The resulting <code>Rectangle</code>
     * is <u>not</u> clipped to the source image bounds.
     *
     * @param destRect the <code>Rectangle</code> in source coordinates.
     * @param sourceIndex the index of the source image.
     * @return a <code>Rectangle</code> indicating the potentially affected
     *         destination region.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is
     *         <code>null</code>.
     */
    public final Rectangle mapDestRect(Rectangle destRect,
                                       int sourceIndex) {
        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }
        return new Rectangle(destRect);
    }

    /**
     * Disposes of any remaining tiles in the <code>TileCache</code>.
     *
     * <p>If <code>cache</code> is non-<code>null</code>, in place operation
     * is enabled, and <code>tileRecycler</code> is non-<code>null</code>,
     * then all tiles owned by this specific image are removed from the cache.
     * Subsequent to this <code>super.dispose()</code> is invoked.</p>
     *
     * @since JAI 1.1.2
     */
    public synchronized void dispose() {
        if(isDisposed) {
            return;
        }

        isDisposed = true;

        if(cache != null && isInPlaceEnabled && tileRecycler != null) {
            cache.removeTiles(this);
        }

        super.dispose();
    }
}
