/*
 * $RCSfile: OpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:12 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel; // 3-22-00 used in deprecated methods only
import java.awt.image.IndexColorModel; // 3-22-00 used in deprecated mthds only
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel; // 3-22-00 used in deprecated methods only
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.Vector;

/**
 * This is the base class for all image operations.  It provides a home
 * for information and functionalities common to all the op-image classes,
 * and implements various utility methods that may be useful to a specific
 * operation.  Image operations may be divided into different categories
 * based on their characteristics.  A subclass, extending
 * <code>OpImage</code>, represents a category and implements methods
 * unique and common to those operations.  Each individual operator
 * should extend the subclass that represents the specific
 * category that operator belongs to.
 *
 * <p> The layout variables of an <code>OpImage</code> are inherited from the
 * <code>PlanarImage</code> superclass.  The layout should be set when the
 * <code>OpImage</code> is constructed.  Each subclass must set
 * the appropriate layout variables and supply them via the
 * <code>ImageLayout</code> argument at construction time.  This class
 * simply modifies these settings as described in the <code>OpImage</code>
 * constructor comments before forwarding the layout to the
 * <code>PlanarImage</code> constructor.  If a subclass needs to modify
 * any of the layout settings subsequent to invoking its superclass
 * constructors it should use the <code>setImageLayout()</code> method
 * defined in <code>PlanarImage</code> in preference to setting the
 * layout variables directly.
 *
 * <p> A <code>RenderedImage</code>'s pixel data type and number of bands
 * are defined by its <code>SampleModel</code>, while the
 * <code>ColorModel</code> translates the pixel data into color/alpha
 * components in the specific <code>ColorSpace</code> that is associated
 * with the <code>ColorModel</code>.
 *
 * <p> By default, the operators provided by Java Advanced Imaging (JAI)
 * operate on the image's pixel data only.  That is, the computations
 * are performed on the data described by the image's
 * <code>SampleModel</code>.  No color translation is performed prior
 * to the actual computation by the operator, regardless of the type of
 * the <code>ColorModel</code> an image has.  If a user intends to have
 * an operation performed on the color data, he must perform the
 * color translation explicitly prior to invoking the operation.
 *
 * <p> There are those operators that specifically deal with the
 * color/alpha data of an image.  Such an operator must state its
 * behavior in its <code>OperationDescriptor</code> explicitly and
 * explain its intended usage of the image's color/alpha component data.
 * In such cases, the image's <code>ColorModel</code> as well as the
 * associated <code>ColorSpace</code> should be considered.
 *
 * <p> However there are certain operations, the results of which are
 * incorrect when the source has colormapped imagery, i.e. the source
 * has an <code>IndexColorModel</code>, and the computations are
 * performed on the image's non color transformed pixel data. In JAI,
 * such operations are those that are implemented as subclasses of
 * {@link AreaOpImage}, {@link GeometricOpImage}, and the "format" 
 * operation. These operations set the 
 * {@link JAI#KEY_REPLACE_INDEX_COLOR_MODEL} <code>RenderingHint</code>
 * to true, thus ensuring that the operations are performed correctly 
 * on the colormapped imagery, not treating the indices into the color
 * map as pixel data. 
 *
 * <p> The tile cache and scheduler are handled by this class.  JAI
 * provides a default implementation for <code>TileCache</code> and
 * <code>TileScheduler</code>.  However, they may be overriden by
 * each application.  An <code>OpImage</code> may share a common cache
 * with other <code>OpImage</code>s, or it may have a private cache of
 * its own.  To override an existing cache, use the
 * <code>setTileCache</code> method; an input argument of <code>null</code>
 * indicates that this image should not have a tile cache.
 *
 * <p> The <code>getTile</code> method may be used to request a tile
 * of the image.  The default implementation of this method in this
 * class first checks whether the requested tile is in the tile cache,
 * and if not, uses the default <code>TileScheduler</code> to schedule
 * the tile for computation.  Once the tile has been computed, it is
 * added to the cache and returned as a <code>Raster</code>.
 *
 * <p> The JAI tile scheduler assumes that when a request is made to
 * schedule a tile for computation via the <code>scheduleTile</code>
 * method, that tile is not currently in the cache.  To avoid a cycle,
 * it calls <code>OpImage.computeTile</code> for the actual tile
 * computation.
 *
 * <p> The default implementation of the <code>computeTile</code> method 
 * in this class first creates a new <code>Raster</code> to represent
 * the requested tile, then calls one of the two <code>computeRect</code>
 * methods to compute the actual pixel values and store the result in
 * the <code>DataBuffer</code> of the <code>Raster</code>.
 *
 * <p> Two variants of the <code>computeRect</code> method exist.
 *
 * <p> The first (with input arguments <code>Raster[]</code>,
 * <code>WritableRaster</code>, and <code>Rectangle</code>) is used when
 * the <code>OpImage</code> is constructed with the
 * <code>cobbleSources</code> argument set to <code>true</code>.
 * This indicates that the source data must be cobbled into a single
 * <code>Raster</code> and that all the necessary source data are provided
 * in order to compute the rectangular region of the destination image.
 * The source <code>Raster</code> array contains one entry for each
 * source image.
 *
 * <p> The second (with input arguments <code>PlanarImage[]</code>,
 * <code>WritableRaster</code>, and <code>Rectangle</code>) is used when
 * the <code>OpImage</code> is constructed with the
 * <code>cobbleSources</code> argument set to <code>false</code>.
 * This indicates that the source data are not cobbled into a single
 * <code>Raster</code>; instead an array of <code>PlanarImage</code>s,
 * one for each source, supply the source data and each image is
 * responsible for performing its own data accesses.  This variant is
 * generally useful if iterators are to be used for the underlying
 * implementation of accessing the image data.
 *
 * <p> The two <code>computeRect</code> methods are not abstract because
 * normally only one needs to be implemented by the subclass depending on
 * the <code>cobbleSources</code> value.  The default implementation of
 * these two methods in this class throws a <code>RuntimeException</code>.
 *
 * <p> Every operator who follows the above default implementation must
 * supply an overridden version of at least one of the
 * <code>computeRect</code> method variants, and specify which one is
 * to be called via the <code>cobbleSources</code> argument of the
 * constructor, or an exception will be thrown at run time.
 *
 * <p> If a subclass overrides <code>getTile</code> not to call
 * <code>computeTile</code>, does not use the JAI implementation of
 * <code>TileScheduler</code>, overrides <code>computeTile</code> not to
 * call <code>computeRect</code>, or does not follow the above default
 * implementation in any way, then it may need to handle issues such as
 * tile caching, multi-threading, and etc. by itself and may not need to
 * override some of the methods described above.  In some cases, some of
 * the methods or variables are even irrelevant.  However, subclasses
 * should be careful when not following the default path for computing
 * a tile.  Most importantly, when a subclass overrides
 * <code>getTile</code>, it should also override <code>computeTile</code>.
 *
 * <p> To request multiple tiles at a time, it is preferable to
 * call the <code>getTiles</code> method with a complete list of the
 * requested tiles' indices, than to call <code>getTile</code> once
 * per tile.  The implementation of <code>getTiles</code> in this class
 * is optimized using multi-threading so that multiple tiles are
 * computed simultaneously.
 *
 * @see PlanarImage
 * @see AreaOpImage
 * @see GeometricOpImage
 * @see PointOpImage
 * @see StatisticsOpImage
 * @see SourcelessOpImage
 *
 */
public abstract class OpImage extends PlanarImage {

    /**
     * A constant indicating that an operation is likely to
     * spend its time mainly performing computation.
     */
    public static final int OP_COMPUTE_BOUND = 1;

    /**
     * A constant indicating that an operation is likely to
     * spend its time mainly performing local I/O.
     */
    public static final int OP_IO_BOUND = 2;

    /**
     * A constant indicating that an operation is likely to
     * spend its time mainly performing network I/O.
     */
    public static final int OP_NETWORK_BOUND = 3;

    /**
     * A constant equal to what would be returned by
     * <code>ImageLayout.getValidMask()</code> if all fields were set.
     */
    private static final int LAYOUT_MASK_ALL =
        ImageLayout.MIN_X_MASK | ImageLayout.MIN_Y_MASK |
        ImageLayout.WIDTH_MASK | ImageLayout.HEIGHT_MASK |
        ImageLayout.TILE_GRID_X_OFFSET_MASK |
        ImageLayout.TILE_GRID_Y_OFFSET_MASK |
        ImageLayout.TILE_WIDTH_MASK | ImageLayout.TILE_HEIGHT_MASK |
        ImageLayout.SAMPLE_MODEL_MASK | ImageLayout.COLOR_MODEL_MASK;


    /**
     * The cache object used to cache this image's tiles.  It may refer
     * to a common cache shared by many <code>OpImage</code>s or a private
     * cache for this image only.  If it is <code>null</code>, it
     * indicates that this image does not have a tile cache.
     */
    protected transient TileCache cache;

    /**
     * Metric used to produce an ordered list of tiles.  This determines
     * which tiles are removed from the cache first if a memory control
     * operation is required.
     *
     * @since JAI 1.1
     */
    protected Object tileCacheMetric;

    /**
     * The scheduler to be used to schedule tile computation.
     */
    private transient TileScheduler scheduler =
        JAI.getDefaultInstance().getTileScheduler();

    /**
     * Variable indicating whether the TileScheduler is the Sun implementation.
     */
    private boolean isSunTileScheduler = false;

    /**
     * Indicates which one of the two <code>computeRect</code> variants
     * should be called by the <code>computeTile</code> method.  If it
     * is <code>true</code>, <code>computeRect</code> expects
     * contiguous sources.
     */
    protected boolean cobbleSources;

    /**
     * Whether dispose() has been invoked.
     */
    private boolean isDisposed = false;

    /**
     * Flag indicating that tile recycling is enabled for tiles which
     * may be referenced outside the API.
     */
    private boolean isCachedTileRecyclingEnabled = false;

    /**
     * A <code>TileRecycler</code> for use in <code>createTile()</code>.
     * May be <code>null</code>. This field is set by the configuration
     * map passed to {@link #OpImage(Vector,ImageLayout,Map,boolean}.
     *
     * @since JAI 1.1.2
     */
    protected TileRecycler tileRecycler;

    /** The default RasterAccessor format tags. */
    // XXX This variable should be removed if we stop using RasterAccessor.
    private RasterFormatTag[] formatTags = null;

    /**
     * Creates a new <code>ImageLayout</code> or forwards the argument
     * layout with or without modifications.
     *
     * <p> If the <code>layout</code> parameter is non-<code>null</code>
     * and all its fields are set then it is cloned and returned.
     *
     * <p> If the <code>layout</code> parameter is non-<code>null</code>
     * but some of its fields are not set and there is at least one source
     * available, then all fields of the layout which are not set are
     * copied from the corresponding attributes of the first source except
     * possibly the <code>ColorModel</code>.  The <code>ColorModel</code>
     * is copied if and only if the destination <code>SampleModel</code> is
     * non-<code>null</code> and the <code>ColorModel</code> and
     * <code>SampleModel</code> are compatible.
     *
     * The image's tile dimensions will be set by the first applicable 
     * means in the following priority-ordered list. Note that each tile
     * dimension, the <code>tileWidth</code> and the
     * <code>tileHeight</code>, is considered independently :
     * <ol>
     * <li>Tile dimension set in the <code>ImageLayout</code> (either by
     * the user or the operator itself);</li>
     * <li>Tile dimension of source, if source is non-<code>null</code>.
     * The tile dimension will be clamped to the minimum of that of the
     * source tile dimension and the image's corresponding dimension;</li>
     * <li>Non-<code>null</code> default tile size returned by 
     * <code>JAI.getDefaultTileSize()</code>, if the corresponding
     * image dimension is at least double the default tile size;</li>
     * <li>The dimensions of the image itself;</li>
     * </ol>
     *
     * <p> If the <code>layout</code> parameter is <code>null</code> and
     * there is at least one source available, a new layout is created from
     * the first source and returned directly.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            Vector sources,
                                            Map config) {
        // Initialize to a reference to the layout passed in.
        ImageLayout il = layout;

        // Check the source Vector elements for nulls.
        if(sources != null) {
            checkSourceVector(sources, true);
        }

        // Check the class of the first source to avoid an exception here; a
        // class cast exception will be thrown by the PlanarImage constructor.
        RenderedImage im =
            sources != null && sources.size() > 0 &&
            sources.firstElement() instanceof RenderedImage ?
            (RenderedImage)sources.firstElement() : null;

        // Update some or all of the layout if a source is available.
        if(im != null) {
            // Create a new layout with the source as fallback.
            // The ColorModel field is intentionally NOT set.
            if(layout == null) {
                // Copy entirety of source image layout.
                il = layout = new ImageLayout(im);

                // Invalidate the ColorModel setting.
                il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            } else {
                // Set all fields except ColorModel.
                il = new ImageLayout(layout.getMinX(im),
                                     layout.getMinY(im),
                                     layout.getWidth(im),
                                     layout.getHeight(im),
                                     layout.getTileGridXOffset(im),
                                     layout.getTileGridYOffset(im),
                                     layout.getTileWidth(im),
                                     layout.getTileHeight(im),
                                     layout.getSampleModel(im),
                                     null);
            }

            // At this point "layout" and "il" are non-null with "layout"
            // representing the ImageLayout originally passed in.  "il" does
            // not yet have its ColorModel field set.

            // Set the ColorModel.
            if(layout.isValid(ImageLayout.COLOR_MODEL_MASK) &&
               layout.getColorModel(null) == null) {
                // User wants to force a null ColorModel.
                il.setColorModel(null);

            } else if(il.getSampleModel(null) != null) {

                // Target SampleModel is available.

                // Get SampleModel from "il"; guaranteed to be non-null.
                SampleModel sm = il.getSampleModel(null);

                // Get ColorModel from "layout".
                ColorModel cmLayout = layout.getColorModel(null);

                // First attempt to set the ColorModel to that specified
                // in the original layout, if any.
                if(cmLayout != null) {
                    // ColorModel is set in original layout.
                    if(JDKWorkarounds.areCompatibleDataModels(sm, cmLayout)) {
                        // ColorModel is compatible with target SampleModel.
                        il.setColorModel(cmLayout);

                    } else if(layout.getSampleModel(null) == null) {
                        // SampleModel not set in original layout so
                        // ColorModel must be incompatible with source
                        // SampleModel: create a compatible SampleModel.

                        // Set the ColorModel to that desired.
                        il.setColorModel(cmLayout);

                        // Derive a new SampleModel from the desired ColorModel
                        SampleModel derivedSM =
                            cmLayout.createCompatibleSampleModel(
                                il.getTileWidth(null),
                                il.getTileHeight(null));

                        // Set the SampleModel to that derived from the CM.
                        il.setSampleModel(derivedSM);

                    }
                }

                // If ColorModel not set from ImageLayout, attempt to set
                // using a ColorModelFactory and if that fails, attempt to
                // use the ColorModel of the source.
                if(!il.isValid(ImageLayout.COLOR_MODEL_MASK) &&
                   !setColorModelFromFactory(sm, sources, config, il)) {
                    // Get ColorModel from "im", i.e., the source.
                    ColorModel cmSource = im.getColorModel();
                    if(cmSource != null &&
                       JDKWorkarounds.areCompatibleDataModels(sm, cmSource)) {
                        // Set to source ColorModel.
			if (cmSource != null &&
			    cmSource instanceof IndexColorModel &&
			    config != null &&
			    config.containsKey(
					 JAI.KEY_REPLACE_INDEX_COLOR_MODEL) &&
			    ((Boolean)config.get(JAI.KEY_REPLACE_INDEX_COLOR_MODEL)).booleanValue()) {
			    
			    ColorModel newCM = 
				PlanarImage.getDefaultColorModel(
						  sm.getDataType(),
						  cmSource.getNumComponents());

			    SampleModel newSM;
			    if (newCM != null) {
				newSM = 
				    newCM.createCompatibleSampleModel(
						       il.getTileWidth(null),
						       il.getTileHeight(null));
			    } else {
				newSM = 
			      RasterFactory.createPixelInterleavedSampleModel(
						  sm.getDataType(),
						  il.getTileWidth(null),
						  il.getTileHeight(null),
						  cmSource.getNumComponents());
			    }

			    il.setSampleModel(newSM);			    
			    if (newCM != null) 
				il.setColorModel(newCM);
			} else {
			    il.setColorModel(cmSource);
			}
                    }
                }
            } else if(il.getSampleModel(null) == null) { // null SampleModel
                // Set to whatever is available.
                il.setColorModel(layout.getColorModel(im));
            }
            // end of block if(im != null)
        } else if(il != null) {
            // Can only get here if im == null && layout != null.
            // Make sure that il is a clone of layout.
            il = (ImageLayout)layout.clone();

            // If the ColorModel is set but the SampleModel is not,
            // derive a SampleModel from the ColorModel.
            if(il.getColorModel(null) != null &&
               il.getSampleModel(null) == null) {
                // Set SampleModel dimensions.
                int smWidth = il.getTileWidth(null);
                if(smWidth == 0) {
                    smWidth = 512;
                }
                int smHeight = il.getTileHeight(null);
                if(smHeight == 0) {
                    smHeight = 512;
                }

                // Derive a new SampleModel from the desired ColorModel
                SampleModel derivedSM =
                    il.getColorModel(null).createCompatibleSampleModel(smWidth,
                                                                       smHeight);

                // Set the SampleModel to that derived from the CM.
                il.setSampleModel(derivedSM);
            }
        } // end of block if(il != null)

        // If no ColorModel is set, then first attempt to set a ColorModel
        // using the ColorModelFactory; otherwise set a default ColorModel
        // if either the configuration mapping is null, it does not contain
        // a mapping of the key KEY_DEFAULT_COLOR_MODEL_ENABLED, or this key
        // is mapped to Boolean.TRUE.
        if(il != null &&
           !il.isValid(ImageLayout.COLOR_MODEL_MASK) &&
           il.getSampleModel(null) != null &&
           !setColorModelFromFactory(il.getSampleModel(null),
                                     sources, config, il)) {

	    ColorModel cm = null;
	    SampleModel srcSM = il.getSampleModel(null);
	    
	    // If it is not a byte image, then probably all the above did not
	    // manage to set a ColorModel and ImageUtil.getCompatibleColorModel
	    // will be used to get the ColorModel. However we want to ensure
	    // that the destination ColorModel is expanded if the ColorModel
	    // of the source is an IndexColorModel and we've been asked to
	    // do IndexColorModel expansion
	    if (im != null &&
		im.getColorModel() != null && 
		im.getColorModel() instanceof IndexColorModel &&
		config != null &&
		config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL) &&
		((Boolean)config.get(JAI.KEY_REPLACE_INDEX_COLOR_MODEL)).booleanValue()) {

		IndexColorModel icm = (IndexColorModel)im.getColorModel();

		// We need to change the ColorModel to a non IndexColorModel
		// so that operations such as geometric can take place
		// correctly. If the ColorModel is changed the SampleModel
		// needs to be changed too, so that they are compatible
		cm = PlanarImage.getDefaultColorModel(srcSM.getDataType(),
						      icm.getNumComponents());
		
		SampleModel newSM;
		if (cm != null) {
		    newSM = cm.createCompatibleSampleModel(srcSM.getWidth(),
							   srcSM.getHeight());
		} else {
		    newSM = RasterFactory.createPixelInterleavedSampleModel(
						      srcSM.getDataType(),
						      srcSM.getWidth(),
						      srcSM.getHeight(),
						      icm.getNumComponents());
		}

		il.setSampleModel(newSM);

	    } else {

		cm = ImageUtil.getCompatibleColorModel(il.getSampleModel(null),
						       config);
	    }

            // Set ColorModel only if method succeeded.
            if(cm != null)
		il.setColorModel(cm);
        }

        // If the tile dimensions were not set in "layout" and are either
        // not set in "il" or would yield an untiled image then reset them to
        // the global tile size default if each image dimension is at least
        // double the respective default tile dimension.
        if(layout != null && il != null &&
           !layout.isValid(ImageLayout.TILE_WIDTH_MASK|
                           ImageLayout.TILE_HEIGHT_MASK)) {
            Dimension defaultTileSize = JAI.getDefaultTileSize();
            if(defaultTileSize != null) {
                if(!layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
                    if(il.getTileWidth(null) <= 0) {
                        il.setTileWidth(defaultTileSize.width);
                    } else {
                        // Calculate number of tiles along X.
                        int numX =
                            XToTileX(il.getMinX(null) + il.getWidth(null) - 1,
                                     il.getTileGridXOffset(null),
                                     il.getTileWidth(null)) -
                            XToTileX(il.getMinX(null),
                                     il.getTileGridXOffset(null),
                                     il.getTileWidth(null)) + 1;
                    
                        // Reset if single-tiled and image is big enough in X.
                        if(numX <= 1
                           && il.getWidth(null) >= 2*defaultTileSize.width) {
                            il.setTileWidth(defaultTileSize.width);
                        }
                    }
                }

                if(!layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
                    if(il.getTileHeight(null) <= 0) {
                        il.setTileHeight(defaultTileSize.height);
                    } else {
                        // Calculate number of tiles along Y.
                        int numY =
                            YToTileY(il.getMinY(null) + il.getHeight(null) - 1,
                                     il.getTileGridYOffset(null),
                                     il.getTileHeight(null)) -
                            YToTileY(il.getMinY(null),
                                     il.getTileGridYOffset(null),
                                     il.getTileHeight(null)) + 1;

                        // Reset if single-tiled and image is big enough in Y.
                        if(numY <= 1
                           && il.getHeight(null) >= 2*defaultTileSize.height) {
                            il.setTileHeight(defaultTileSize.height);
                        }
                    }
                }
            }
        }

	// Independently clamp each tile dimension to the respective image
	// dimension, if the tile dimensions are not set in any supplied
	// ImageLayout, and the tile and image dimensions are both set in
	// the ImageLayout to be returned.

	// Tile width
	if ((layout == null ||
	     !layout.isValid(ImageLayout.TILE_WIDTH_MASK)) &&
	    il.isValid(ImageLayout.TILE_WIDTH_MASK | 
		       ImageLayout.WIDTH_MASK) &&
	    il.getTileWidth(null) > il.getWidth(null)) {
	    il.setTileWidth(il.getWidth(null));
	}
	
	// Tile height
	if ((layout == null || 
	     !layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) &&
	    il.isValid(ImageLayout.TILE_HEIGHT_MASK | 
		       ImageLayout.HEIGHT_MASK) &&
	    il.getTileHeight(null) > il.getHeight(null)) {
	    il.setTileHeight(il.getHeight(null));
	}

        return il;
    }

    /**
     * Set the <code>ColorModel</code> in <code>layout</code> if
     * <code>config</code> is non-<code>null</code> and contains a mapping
     * for <code>JAI.KEY_COLOR_MODEL_FACTORY</code>.  If the
     * <code>ColorModelFactory</code> returns a non-<code>null</code>
     * <code>ColorModel</code> which is compatible with
     * <code>sampleModel</code> it is used to set the <code>ColorModel</code>
     * in <code>layout</code>.
     *
     * @param sampleModel The <code>SampleModel</code> to which the
     *        <code>ColorModel</code> to be created must correspond;
     *        may <b>not</b> be <code>null</code>.
     * @param sources A <code>List</code> of <code>RenderedImage</code>s;
     *        may be <code>null</code>.
     * @param config A configuration mapping; may be
     *        <code>null</code>.
     * @param layout The image layout; will be updated with the
     *        <code>ColorModel</code>created by the
     *        <code>ColorModelFactory</code>
     *
     * @return Whether the <code>ColorModel</code> in <code>layout</code>
     *         has been set.
     * @exception IllegalArgumentException if <code>sampleModel</code>
     *            is <code>null</code>.
     */
    private static boolean setColorModelFromFactory(SampleModel sampleModel,
                                                    Vector sources,
                                                    Map config,
                                                    ImageLayout layout) {
        boolean isColorModelSet = false;

        if(config != null &&
           config.containsKey(JAI.KEY_COLOR_MODEL_FACTORY)) {
            ColorModelFactory cmf =
                (ColorModelFactory)config.get(JAI.KEY_COLOR_MODEL_FACTORY);
            ColorModel cm = cmf.createColorModel(sampleModel,
                                                 sources,
                                                 config);
            if(cm != null &&
               JDKWorkarounds.areCompatibleDataModels(sampleModel, cm)) {
                layout.setColorModel(cm);
                isColorModelSet = true;
            }
        }

        return isColorModelSet;
    }

    // XXX Note: ColorModel.isCompatibleRaster() is mentioned below but it
    // should be ColorModel.isCompatibleSampleModel(). This has a bug
    // however (4326636) which is worked around within JAI. The other method
    // is mentioned as it does NOT have a bug.
    /**
     * Constructor.
     *
     * <p> The image's layout is encapsulated in the <code>layout</code>
     * argument.  The variables of the image layout which are not set in
     * the <code>layout</code> parameter are copied from the first source
     * if sources are available.  In the case of the <code>ColorModel</code>,
     * the copy is performed if and only if the <code>ColorModel</code> is
     * compatible with the destination <code>SampleModel</code> and is not
     * set by another higher priority mechanism as described presently.</p>
     *
     * <p> Assuming that there is at least one source, the image's
     * <code>ColorModel</code> will be set by the first applicable means
     * in the following priority-ordered list:
     * <ol>
     * <li><code>null</code> <code>ColorModel</code> from
     * <code>ImageLayout</code>;</li>
     * <li>Non-<code>null</code> <code>ColorModel</code> from
     * <code>ImageLayout</code> if compatible with
     * <code>SampleModel</code> in <code>ImageLayout</code> or if
     * <code>SampleModel</code> in <code>ImageLayout</code> is
     * <code>null</code>;</li>
     * <li>Value returned by <code>ColorModelFactory</code> set via the
     * <code>JAI.KEY_COLOR_MODEL_FACTORY</code> configuration variable if
     * compatible with <code>SampleModel</code>;</li>
     * <li>An instance of a non-<code>IndexColorModel</code> (or
     * <code>null</code> if no compatible non-<code>IndexColorModel</code> 
     * could be generated), if the source has an <code>IndexColorModel</code> 
     * and <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code>
     * is <code>Boolean.TRUE</code>;</li>
     * <li><code>ColorModel</code> of first source if compatible with
     * <code>SampleModel</code>;</li>
     * <li>Value returned by default method specified by the
     * <code>JAI.KEY_DEFAULT_COLOR_MODEL_METHOD</code> configuration variable
     * if <code>JAI.KEY_DEFAULT_COLOR_MODEL_ENABLED</code> is
     * <code>Boolean.TRUE</code>.</li>
     * </ol>
     * If it is not possible to set the <code>ColorModel</code> by any of
     * these means it will remain <code>null</code>.</p>
     *
     * The image's tile dimensions will be set by the first applicable 
     * means in the following priority-ordered list. Note that each tile
     * dimension, the <code>tileWidth</code> and the
     * <code>tileHeight</code>, is considered independently :
     * <ol>
     * <li>Tile dimension set in the <code>ImageLayout</code> (either by
     * the user or the operator itself);</li>
     * <li>Tile dimension of source, if source is non-<code>null</code>.
     * The tile dimension will be clamped to the minimum of that of the
     * source tile dimension and the image's corresponding dimension;</li>
     * <li>Non-<code>null</code> default tile size returned by 
     * <code>JAI.getDefaultTileSize()</code>, if the corresponding
     * image dimension is at least double the default tile size;</li>
     * <li>The dimensions of the image itself;</li>
     * </ol>
     *
     * <p> The <code>sources</code> contains a list of immediate sources
     * of this image.  Elements in the list may not be <code>null</code>.
     * If this image has no sources this argument should be <code>null</code>.
     * This parameter is forwarded unmodified to the <code>PlanarImage</code>
     * constructor.
     *
     * <p> The <code>configuration</code> contains a mapping of configuration
     * variables and image properties.  Entries which have keys of type
     * <code>RenderingHints.Key</code> are taken to be configuration variables.
     * Entries with a key which is either a <code>String</code> or a
     * <code>CaselessStringKey</code> are interpreted as image properties.
     * This parameter is forwarded unmodified to the <code>PlanarImage</code>
     * constructor.
     *
     * <p> This image class recognizes the configuration variables referenced
     * by the following keys:
     *
     * <ul>
     * <li> <code>JAI.KEY_TILE_CACHE</code>: specifies the
     * <code>TileCache</code> in which to store the image tiles;
     * if this key is not supplied no tile caching will be performed.
     * <li> <code>JAI.KEY_TILE_CACHE_METRIC</code>: establishes an
     * ordering of tiles stored in the tile cache.  This ordering
     * is used to determine which tiles will be removed first, if
     * a condition causes tiles to be removed from the cache.
     * <li> <code>JAI.KEY_TILE_SCHEDULER</code>: specifies the
     * <code>TileScheduler</code> to use to schedule tile computation;
     * if this key is not supplied the default scheduler will be used.
     * <li> <code>JAI.KEY_COLOR_MODEL_FACTORY</code>: specifies a
     * <code>ColorModelFactory</code> to be used to generate the
     * <code>ColorModel</code> of the image.  If such a callback is
     * provided it will be invoked if and only if either no
     * <code>ImageLayout</code> hint is given, or an <code>ImageLayout</code>
     * hint is given but contains a non-<code>null</code>
     * <code>ColorModel</code> which is incompatible with the image
     * <code>SampleModel</code>.  In other words, such a callback provides
     * the second priority mechanism for setting the <code>ColorModel</code>
     * of the image.</li>
     * <li> <code>JAI.KEY_DEFAULT_COLOR_MODEL_ENABLED</code>: specifies whether
     * a default <code>ColorModel</code> will be derived
     * if none is specified and one cannot be inherited from the first source;
     * if this key is not supplied a default <code>ColorModel</code> will be
     * computed if necessary.
     * <li> <code>JAI.KEY_DEFAULT_COLOR_MODEL_METHOD</code>: specifies the
     * method to be used to compute the default <code>ColorModel</code>;
     * if this key is not supplied and a default <code>ColorModel</code> is
     * required, <code>PlanarImage.createColorModel()</code> will be used to
     * compute it.
     * <li> <code>JAI.KEY_TILE_FACTORY</code>: specifies a
     * {@link TileFactory} to be used to generate the tiles of the
     * image via {@link TileFactory#createTile(SampleModel,Point)}.  If
     * no such configuration variable is given, a new <code>Raster</code>
     * will be created for each image tile.  This behavior may be
     * overridden by subclasses which have alternate means of saving
     * memory, for example as in the case of point operations which
     * may overwrite a source image not referenced by user code. Note
     * that the corresponding instance variable is actually set by
     * the superclass constructor.</li>
     * <li> <code>JAI.KEY_TILE_RECYCLER</code>: specifies a
     * {@link TileRecycler} to be used to recycle the tiles of the
     * image when the <code>dispose()</code> method is invoked.  If
     * such a configuration variable is set, the image has a
     * non-<code>null</code> <code>TileCache</code>, and tile recycling
     * is enabled, then invoking <code>dispose()</code> will cause each
     * of the tiles of this image currently in the cache to be passed to
     * the configured <code>TileRecycler</code></li> via
     * {@link TileRecycler#recycleTile(Raster)}.</li>
     * <li> <code>JAI.KEY_CACHED_TILE_RECYCLING_ENABLED</code>: specifies a
     * <code>Boolean</code> value which indicates whether {#dispose()}
     * should pass to <code>tileRecycler.recycleTile()</code> any image
     * tiles remaining in the cache.</li>
     * </ul>
     *
     * <p> The <code>cobbleSources</code> indicates which one of the two
     * variants of the <code>computeRect</code> method should be called.
     * If a subclass does not follow the default tile computation scheme,
     * then this argument may be irrelevant.
     *
     * @param layout  The layout of this image.
     * @param sources  The immediate sources of this image.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This parameter may be <code>null</code>.
     * @param cobbleSources  Indicates which variant of the
     *        <code>computeRect</code> method should be called.
     *
     * @throws IllegalArgumentException  If <code>sources</code>
     *         is non-<code>null</code> and any object in
     *         <code>sources</code> is <code>null</code>.
     * @throws RuntimeException If default <code>ColorModel</code> setting
     *         is enabled via a hint in the configuration <code>Map</code>
     *         and the supplied <code>Method</code> does not conform to the
     *         requirements stated in the <code>JAI</code> class for the
     *         hint key <code>KEY_DEFAULT_COLOR_MODEL_METHOD</code>.
     *
     * @since JAI 1.1
     */
    public OpImage(Vector sources,
                   ImageLayout layout,
                   Map configuration,
                   boolean cobbleSources) {
        super(layoutHelper(layout, sources, configuration),
              sources, configuration);

        if(configuration != null) {
            // Get the cache from the configuration map.
            Object cacheConfig = configuration.get(JAI.KEY_TILE_CACHE);

            // Ensure that it is a TileCache instance with positive capacity.
            if(cacheConfig != null &&
               cacheConfig instanceof TileCache &&
               ((TileCache)cacheConfig).getMemoryCapacity() > 0) {
                cache = (TileCache)cacheConfig;
            }

            // Get the scheduler from the configuration map.
            Object schedulerConfig = configuration.get(JAI.KEY_TILE_SCHEDULER);

            // Ensure that it is a TileScheduler instance.
            if(schedulerConfig != null &&
               schedulerConfig instanceof TileScheduler) {
                scheduler = (TileScheduler)schedulerConfig;
            }

            try {
                // Test whether the TileScheduler is the default type.
                Class sunScheduler =
                    Class.forName("com.lightcrafts.media.jai.util.SunTileScheduler");

                isSunTileScheduler = sunScheduler.isInstance(scheduler);
            } catch(Exception e) {
                // Deliberately ignore any Exceptions.
            }

            // Get the tile metric (cost or priority, for example)
            tileCacheMetric = configuration.get(JAI.KEY_TILE_CACHE_METRIC);

            // Set up cached tile recycling flag.
            Object recyclingEnabledValue =
                configuration.get(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED);
            if(recyclingEnabledValue instanceof Boolean) {
                isCachedTileRecyclingEnabled =
                    ((Boolean)recyclingEnabledValue).booleanValue();
            }

            // Set up the TileRecycler.
            Object recyclerValue = configuration.get(JAI.KEY_TILE_RECYCLER);
            if(recyclerValue instanceof TileRecycler) {
                tileRecycler = (TileRecycler)recyclerValue;
            }
        }

        this.cobbleSources = cobbleSources;
    }

    /**
     * A <code>TileComputationListener</code> to pass to the
     * <code>TileScheduler</code> to intercept method calls such that the
     * computed tiles are added to the <code>TileCache</code> of the image.
     */
    private class TCL implements TileComputationListener {
        OpImage opImage;

        private TCL(OpImage opImage) {
            this.opImage = opImage;
        }

        public void tileComputed(Object eventSource,
                                 TileRequest[] requests,
                                 PlanarImage image, int tileX, int tileY,
                                 Raster tile) {
            if(image == opImage) {
                // Cache the tile.
                addTileToCache(tileX, tileY, tile);
            }
        }

        public void tileCancelled(Object eventSource,
                                  TileRequest[] requests,
                                  PlanarImage image, int tileX, int tileY) {
            // Do nothing.
        }

        public void tileComputationFailure(Object eventSource,
                                           TileRequest[] requests,
                                           PlanarImage image,
                                           int tileX, int tileY,
                                           Throwable situation) {
            // Do nothing.
        }
    }

    /**
     * Stores a <code>RenderedImage</code> in a <code>Vector</code>.
     *
     * @param image The image to be stored in the <code>Vector</code>.
     *
     * @return A <code>Vector</code> containing the image.
     *
     * @throws IllegalArgumentException if <code>image</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1
     */
    protected static Vector vectorize(RenderedImage image) {
        if(image == null) {
            throw new IllegalArgumentException(JaiI18N.getString("OpImage3"));
        }
        Vector v = new Vector(1);
        v.addElement(image);
        return v;
    }

    /**
     * Stores two <code>RenderedImage</code>s in a <code>Vector</code>.
     *
     * @param image1 The first image to be stored in the <code>Vector</code>.
     * @param image2 The second image to be stored in the <code>Vector</code>.
     *
     * @return A <code>Vector</code> containing the images.
     *
     * @throws IllegalArgumentException if <code>image1</code> or
     * <code>image2</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    protected static Vector vectorize(RenderedImage image1,
                                      RenderedImage image2) {
        if(image1 == null || image2 == null) {
            throw new IllegalArgumentException(JaiI18N.getString("OpImage3"));
        }
        Vector v = new Vector(2);
        v.addElement(image1);
        v.addElement(image2);
        return v;
    }

    /**
     * Stores three <code>RenderedImage</code>s in a <code>Vector</code>.
     *
     * @param image1 The first image to be stored in the <code>Vector</code>.
     * @param image2 The second image to be stored in the <code>Vector</code>.
     * @param image3 The third image to be stored in the <code>Vector</code>.
     *
     * @return A <code>Vector</code> containing the images.
     *
     * @throws IllegalArgumentException if <code>image1</code> or
     * <code>image2</code> or <code>image3</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    protected static Vector vectorize(RenderedImage image1,
                                      RenderedImage image2,
                                      RenderedImage image3) {
        if(image1 == null || image2 == null || image3 == null) {
            throw new IllegalArgumentException(JaiI18N.getString("OpImage3"));
        }
        Vector v = new Vector(3);
        v.addElement(image1);
        v.addElement(image2);
        v.addElement(image3);
        return v;
    }

    /**
     * Checks the source <code>Vector</code>.
     *
     * <p>Checks whether the <code>sources</code> parameter is <code>null</code>
     * and optionally whether all elements are non-<code>null</code>.
     *
     * @param sources The source <code>Vector</code>.
     * @param checkElements Whether the elements are to be checked.
     *
     * @return The <code>sources</code> parameter unmodified.
     *
     * @throws IllegalArgumentException  If <code>sources</code>
     *         is <code>null</code>.
     * @throws IllegalArgumentException  If <code>checkElements</code>
     *         is <code>true</code>, <code>sources</code>
     *         is non-<code>null</code> and any object in
     *         <code>sources</code> is <code>null</code>.
     */
    static Vector checkSourceVector(Vector sources, boolean checkElements) {
        // Check for null source Vector.
        if(sources == null) {
            throw new IllegalArgumentException(JaiI18N.getString("OpImage2"));
        }

        if(checkElements) {
            // Check Vector elements.
            int numSources = sources.size();
            for(int i = 0; i < numSources; i++) {
                // Check for null element.
                if(sources.get(i) == null) {
                    throw new
                        IllegalArgumentException(JaiI18N.getString("OpImage3"));
                }
            }
        }
        
        return sources;
    }

    /**
     * Returns the tile cache object of this image by reference.
     * If this image does not have a tile cache, this method returns
     * <code>null</code>.
     *
     * @since JAI 1.1
     */
    public TileCache getTileCache() {
        return cache;
    }

    /**
     * Sets the tile cache object of this image.  A <code>null</code>
     * input indicates that this image should have no tile cache and
     * subsequently computed tiles will not be cached.
     *
     * <p> The existing cache object is informed to release all the
     * currently cached tiles of this image.
     *
     * @param cache  A cache object to be used for caching this image's
     *        tiles, or <code>null</code> if no tile caching is desired.
     */
    public void setTileCache(TileCache cache) {
        if (this.cache != null) {
            this.cache.removeTiles(this);
        }
        this.cache = cache;
    }
    
    /**
     * Retrieves a tile from the tile cache.  If this image does not
     * have a tile cache, or the requested tile is not currently in
     * the cache, this method returns <code>null</code>.
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     *
     * @return The requested tile as a <code>Raster</code> or
     *         <code>null</code>.
     */
    protected Raster getTileFromCache(int tileX, int tileY) {
        return cache != null ? cache.getTile(this, tileX, tileY) : null;
    }
    
    /**
     * Adds a tile to the tile cache.  If this image does not have
     * a tile cache, this method does nothing.
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     * @param tile  The tile to be added to the cache.
     */
    protected void addTileToCache(int tileX,
                                  int tileY,
                                  Raster tile) {
        if (cache != null) {
            cache.add(this, tileX, tileY, tile, tileCacheMetric);
        }
    }

    /**
     * Returns the <code>tileCacheMetric</code> instance variable by reference.
     *
     * @since JAI 1.1
     */
    public Object getTileCacheMetric() {
        return tileCacheMetric;
    }

    /**
     * Returns a tile of this image as a <code>Raster</code>.  If the
     * requested tile is completely outside of this image's bounds,
     * this method returns <code>null</code>.
     *
     * <p> This method attempts to retrieve the requested tile from the
     * cache.  If the tile is not currently in the cache, it schedules
     * the tile for computation and adds it to the cache once the tile
     * has been computed.
     *
     * <p> If a subclass overrides this method, then it needs to handle
     * tile caching and scheduling.  It should also override
     * <code>computeTile()</code> which may be invoked directly by the
     * <code>TileScheduler</code>.
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     */
    public Raster getTile(int tileX, int tileY) {
        Raster tile = null;	// the requested tile, to be returned

        // Make sure the requested tile is inside this image's boundary.
        if (tileX >= getMinTileX() && tileX <= getMaxTileX() &&
            tileY >= getMinTileY() && tileY <= getMaxTileY()) {
            // Check if tile is available in the cache.
            tile = getTileFromCache(tileX, tileY);

            if (tile == null) {         // tile not in cache
                try {
                    tile = scheduler.scheduleTile(this, tileX, tileY);
                } catch (OutOfMemoryError e) {
                    // Free some space in cache
                    if(cache != null) {
                        cache.removeTiles(this);
                    }
                    try {
                        // Re-attempt to compute the tile.
                        tile = scheduler.scheduleTile(this, tileX, tileY);
                    } catch (OutOfMemoryError e1){
                        // Empty the cache
                        if(cache != null) {
                            cache.flush();
                        }
                    }

                    // Need to reissue the tile scheduling.
                    tile = scheduler.scheduleTile(this, tileX, tileY);
                }

                // Cache the result tile.
                addTileToCache(tileX, tileY, tile);
            }
        }

        return tile;
    }

    /**
     * Computes the image data of a tile.
     *
     * <p> When a tile is requested via the <code>getTile</code> method
     * and that tile is not in this image's tile cache, this method is
     * invoked by the <code>TileScheduler</code> to compute the data of
     * the new tile.  Even though this method is marked <code>public</code>,
     * it should not be called by the applications directly.  Rather, it
     * is meant to be called by the <code>TileScheduler</code> for the
     * actual computation.
     *
     * <p> The implementation of this method in this class assumes that
     * the requested tile either intersects the image, or is within the
     * image's bounds.  It creates a new <code>Raster</code> to
     * represent the requested tile, then calls one of the two variants
     * of <code>computeRect</code> to calculate the pixels of the
     * tile that are within the image's bounds.  The value of
     * <code>cobbleSources</code> determines which variant of
     * <code>computeRect</code> is invoked, as described in the class
     * comments.
     *
     * <p> Subclasses may provide a more optimized implementation of this
     * method.  If they override this method not to call either variant of
     * <code>computeRect</code>, then neither variant of
     * <code>computeRect</code> needs to be implemented.
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        // Create a new Raster.
        WritableRaster dest = createWritableRaster(sampleModel,
                                                   new Point(tileXToX(tileX),
                                                             tileYToY(tileY)));
            
        // Determine the active area; tile intersects with image's bounds.
        Rectangle destRect = getTileRect(tileX, tileY);

        int numSources = getNumSources();

        if (cobbleSources) {
            Raster[] rasterSources = new Raster[numSources];
            // Cobble areas
            for (int i = 0; i < numSources; i++) {
                PlanarImage source = getSource(i);
                Rectangle srcRect = mapDestRect(destRect, i);

                // If srcRect is empty, set the Raster for this source to
                // null; otherwise pass srcRect to getData(). If srcRect
                // is null, getData() will return a Raster containing the
                // data of the entire source image.
                rasterSources[i] = srcRect != null && srcRect.isEmpty() ?
                    null : source.getData(srcRect);
            }
            computeRect(rasterSources, dest, destRect);

            for (int i = 0; i < numSources; i++) {
                Raster sourceData = rasterSources[i];
                if(sourceData != null) {
                    PlanarImage source = getSourceImage(i);

                    // Recycle the source tile
                    if(source.overlapsMultipleTiles(sourceData.getBounds())) {
                        recycleTile(sourceData);
                    }
                }
            }
        } else {
            PlanarImage[] imageSources = new PlanarImage[numSources];
            for (int i = 0; i < numSources; i++) {
                imageSources[i] = getSource(i);
            }
            computeRect(imageSources, dest, destRect);
        }

        return dest;
    }

    /**
     * Computes a rectangle of output, given <code>Raster</code>
     * sources.  This method should be overridden by
     * <code>OpImage</code> subclasses that make use of cobbled
     * sources, as determined by the setting of the
     * <code>cobbleSources</code> constructor argument to this class.
     *
     * <p> The source <code>Raster</code>s are guaranteed to include
     * at least the area specified by <code>mapDestRect(destRect)</code>
     * unless this area is empty or does not intersect the corresponding
     * source in which case the source <code>Raster</code>
     * will be <code>null</code>.  Only the specified destination region
     * should be written.</p>
     *
     * <p> Since the subclasses of <code>OpImage</code> may choose
     * between the cobbling and non-cobbling versions of
     * <code>computeRect</code>, it is not possible to leave this
     * method abstract in <code>OpImage</code>.  Instead, a default
     * implementation is provided that throws a
     * <code>RuntimeException</code>.</p>
     *
     * @param sources an array of source <code>Raster</code>s, one per
     *        source image.
     * @param dest a <code>WritableRaster</code> to be filled in.
     * @param destRect the <code>Rectangle</code> within the
     *        destination to be written.
     *
     * @throws RuntimeException  If this method is invoked on the subclass
     *         that sets <code>cobbleSources</code> to <code>true</code>
     *         but does not supply an implementation of this method.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        String className = this.getClass().getName();
        throw new RuntimeException(className + " " +
                                   JaiI18N.getString("OpImage0"));
    }

    /**
     * Computes a rectangle of output, given <code>PlanarImage</code>
     * sources.  This method should be overridden by
     * <code>OpImage</code> subclasses that do not require cobbled
     * sources; typically they will instantiate iterators to perform
     * source access, but they may access sources directly (via the
     * <code>SampleModel</code>/<code>DataBuffer</code> interfaces) if
     * they wish.
     *
     * <p> Since the subclasses of <code>OpImage</code> may choose
     * between the cobbling and non-cobbling versions of
     * <code>computeRect</code>, it is not possible to leave this
     * method abstract in <code>OpImage</code>.  Instead, a default
     * implementation is provided that throws a
     * <code>RuntimeException</code>.
     *
     * @param sources an array of <code>PlanarImage</code> sources.
     * @param dest a <code>WritableRaster</code> to be filled in.
     * @param destRect the <code>Rectangle</code> within the
     * destination to be written.
     *
     * @throws RuntimeException  If this method is invoked on the subclass
     *         that sets <code>cobbleSources</code> to <code>false</code>
     *         but does not supply an implementation of this method.
     */
    protected void computeRect(PlanarImage[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        String className = this.getClass().getName();
        throw new RuntimeException(className + " " +
                                   JaiI18N.getString("OpImage1"));
    }

    /**
     * Returns a list of indices of the tiles of a given source image
     * that may be required in order to compute a given tile.
     * Ideally, only tiles that will be requested by means of calls to
     * the source's <code>getTile()</code> method should be reported.
     * The default implementation uses <code>mapDestRect()</code> to
     * obtain a conservative estimate.
     *
     * <p> If no dependencies exist, this method returns
     * <code>null</code>.
     *
     * <p> This method may be used by optimized implementations of JAI
     * in order to predict future work and create an optimized
     * schedule for performing it.
     *
     * <p> A given <code>OpImage</code> may mix calls to
     * <code>getTile()</code> with calls to other methods such as
     * <code>getData()</code> and <code>copyData()</code> in order to
     * avoid requesting entire tiles where only a small portion is
     * needed.  In such a case, this method may be overridden to
     * provide a more accurate estimate of the set of
     * <code>getTile()</code> calls that will actually be performed.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     * @param sourceIndex the index of the source image.
     *
     * @return An array of <code>Point</code>s indicating the source
     *         tile dependencies.
     *
     * @throws IllegalArgumentException  If <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     */
    public Point[] getTileDependencies(int tileX, int tileY,
                                       int sourceIndex) {
        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            // Specified source does not exist for this image.
            throw new IllegalArgumentException(
                JaiI18N.getString("Generic1"));
        }

        Rectangle rect = getTileRect(tileX, tileY);
        if (rect.isEmpty()) {
            // The tile is outside of the image bounds.
            return null;
        }

        // Returns a list of tiles that belong to the source specified by
        // the <code>sourceIndex</code> argument, which are need to compute
        // the pixels within the rectangle region specified by the
        // <code>rect</code> argument of this image.
        //
        // This method uses <code>mapDestRect</code> to conservatively
        // determine the source region required.  However, only those tiles
        // actually inside the source image bound are returned.  If the
        // region of interest maps completely outside of the source image,
        // <code>null</code> is returned.
        PlanarImage src = getSource(sourceIndex);
        Rectangle srcRect = mapDestRect(rect, sourceIndex);

        int minTileX = src.XToTileX(srcRect.x);
        int maxTileX = src.XToTileX(srcRect.x + srcRect.width - 1);

        int minTileY = src.YToTileY(srcRect.y);
        int maxTileY = src.YToTileY(srcRect.y + srcRect.height - 1);

        // Make sure the tiles are really inside the source image.
        minTileX = Math.max(minTileX, src.getMinTileX());
        maxTileX = Math.min(maxTileX, src.getMaxTileX());

        minTileY = Math.max(minTileY, src.getMinTileY());
        maxTileY = Math.min(maxTileY, src.getMaxTileY());

        int numXTiles = maxTileX - minTileX + 1;
        int numYTiles = maxTileY - minTileY + 1;
        if (numXTiles <= 0 || numYTiles <= 0) {
            // The tile maps outside of source image bound.
            return null;
        }

        Point[] ret = new Point[numYTiles*numXTiles];
        int i = 0;

        for (int y = minTileY; y <= maxTileY; y++) {
            for (int x = minTileX; x <= maxTileX; x++) {
                ret[i++] = new Point(x, y);
            }
        }

        return ret;
    }

    /**
     * Computes the tiles indicated by the given tile indices.  This
     * call is preferable to a series of <code>getTile()</code> calls
     * because certain implementations can make optimizations based on
     * the knowledge that multiple tiles are being asked for at once.
     *
     * <p> The implementation of this method in this class uses multiple
     * threads to compute multiple tiles at a time.
     *
     * @param tileIndices An array of <code>Point</code>s representing
     *        tile indices.
     *
     * @return An array of <code>Raster</code>s containing the tiles
     *         corresponding to the given tile indices.
     *
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     */
    public Raster[] getTiles(Point[] tileIndices) {
        if (tileIndices == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int numTiles = tileIndices.length;	// number of tiles requested

        // The requested tiles, to be returned.
        Raster[] tiles = new Raster[numTiles];

        // Indicator for those tiles that actually need to be computed.
        boolean[] computeTiles = new boolean[numTiles];

        int minTileX = getMinTileX();
        int maxTileX = getMaxTileX();
        int minTileY = getMinTileY();
        int maxTileY = getMaxTileY();

        int count = 0;	// number of tiles need to be computed

        for (int i = 0; i < numTiles; i++) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;

            // Make sure the tile is inside image boundary.
            if (tileX >= minTileX && tileX <= maxTileX &&
                tileY >= minTileY && tileY <= maxTileY) {
                // Check if tile is available in the cache.
                tiles[i] = getTileFromCache(tileX, tileY);

                if (tiles[i] == null) {
                    // Tile not in cache. needs computation.
                    computeTiles[i] = true;
                    count++;
                }
            }
        }

        if (count > 0) {	// need to compute some tiles
            if (count == numTiles) {
                // None of the tiles is in cache.
                tiles = scheduler.scheduleTiles(this, tileIndices);

                if (cache != null) {	// cache these tiles
                    if(cache != null) {
                        for (int i = 0; i < numTiles; i++) {
                            cache.add(this,
                                      tileIndices[i].x,
                                      tileIndices[i].y,
                                      tiles[i],
                                      tileCacheMetric);
                        }
                    }
                }

            } else {
                // Only schedule those tiles not in cache for computation.
                Point[] indices = new Point[count];
                count = 0;
                for (int i = 0; i < numTiles; i++) {
                    if (computeTiles[i]) {
                        indices[count++] = tileIndices[i];
                    }
                }

                // Schedule needed tiles and return.
                Raster[] newTiles = scheduler.scheduleTiles(this, indices);

                count = 0;
                for (int i = 0; i < numTiles; i++) {
                    if (computeTiles[i]) {
                        tiles[i] = newTiles[count++];
                        addTileToCache(tileIndices[i].x, tileIndices[i].y,
                                       tiles[i]);
                    }
                }
            }
        }

        return tiles;
    }

    private static TileComputationListener[]
        prependListener(TileComputationListener[] listeners,
                        TileComputationListener listener) {
        if(listeners == null) {
            return new TileComputationListener[] {listener};
        }

        TileComputationListener[] newListeners =
            new TileComputationListener[listeners.length+1];
        newListeners[0] = listener;
        System.arraycopy(listeners, 0, newListeners, 1, listeners.length);

        return newListeners;
    }

    /**
     * Returns an array of indices of tiles which are not cached or
     * <code>null</code> if all are cached.
     */
    /* XXX
    private Point[] pruneIndices(Point[] tileIndices) {
        if(true)return tileIndices;//XXX
        int numIndices = tileIndices.length;

        ArrayList uncachedIndices = new ArrayList(numIndices);

        for(int i = 0; i < numIndices; i++) {
            Point p = tileIndices[i];
            if(getTileFromCache(p.x, p.y) == null) {
                uncachedIndices.add(p);
            }
        }

        int numUncached = uncachedIndices.size();
        return numUncached > 0 ?
            (Point[])uncachedIndices.toArray(new Point[numUncached]) : null;
    }
    */

    /**
     * Queues a list of tiles for computation.  Registered listeners will
     * be notified after each tile has been computed.  The event source
     * parameter passed to such listeners will be the <code>TileScheduler</code>
     * and the image parameter will be this image.
     *
     * @param tileIndices A list of tile indices indicating which tiles
     *        to schedule for computation.
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public TileRequest queueTiles(Point[] tileIndices) {
        if (tileIndices == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        /* XXX bad idea probably
        // Remove any tile indices corresponding to cached tiles.
        tileIndices = pruneIndices(tileIndices);

        // Return if no tiles remain, i.e., all are cached.
        if(tileIndices == null) {
            return;
        }
        */

        // Get registered listeners.
        TileComputationListener[] tileListeners = getTileComputationListeners();

        // Add a listener to cache tiles only if not a SunTileScheduler.
        // The SunTileScheduler caches tiles generated by an OpImage but
        // this is not a requirement of the specification.
        if(!isSunTileScheduler) {
            // Create a local listener.
            TileComputationListener localListener = new TCL(this);

            // Prepend local listener to array.
            tileListeners = prependListener(tileListeners, localListener);
        }

        // Queue the tiles to the scheduler.
        return scheduler.scheduleTiles(this, tileIndices, tileListeners);
    }

    /**
     * Issue an advisory cancellation request to nullify processing of
     * the indicated tiles via the TileScheduler for this image.  This
     * method should merely forward the request to the associated
     * <code>TileScheduler</code>.
     *
     * @param request The request for which tiles are to be cancelled.
     * @param tileIndices The tiles to be cancelled; may be <code>null</code>.
     *        Any tiles not actually in the <code>TileRequest</code> will be
     *        ignored.
     * @throws IllegalArgumentException  If <code>request</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void cancelTiles(TileRequest request, Point[] tileIndices) {
        if (request == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic4"));
        }
        scheduler.cancelTiles(request, tileIndices);
    }

    /**
     * Hints that the given tiles might be needed in the near future.
     * Some implementations may spawn one or more threads
     * to compute the tiles, while others may ignore the hint.
     *
     * @param tileIndices A list of tile indices indicating which tiles
     *        to prefetch.
     *
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     */
    public void prefetchTiles(Point[] tileIndices) {
        if (tileIndices == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        /* XXX bad idea probably
        // Remove any tile indices corresponding to cached tiles.
        tileIndices = pruneIndices(tileIndices);
        */

        // Return if no tiles remain, i.e., all are cached.
        if(tileIndices == null) {
            return;
        }

        // Prefetch any remaining tiles.
        scheduler.prefetchTiles(this, tileIndices);
    }

    /**
     * Computes the position in the specified source that best
     * matches the supplied destination image position. If it
     * is not possible to compute the requested position,
     * <code>null</code> will be returned. If the point is mapped
     * outside the source bounds, the coordinate value or <code>null</code>
     * may be returned at the discretion of the implementation.
     *
     * <p>Floating-point input and output coordinates are supported,
     * and recommended when possible.  Subclass implementations may
     * however use integer computation if necessary for simplicity.</p>
     *
     * <p>The implementation in this class returns the value of
     * <code>pt</code> in the following code snippet:
     *
     * <pre>
     * Rectangle destRect = new Rectangle((int)destPt.getX(),
     *                                    (int)destPt.getY(),
     *                                    1, 1);
     * Rectangle sourceRect = mapDestRect(destRect, sourceIndex);
     * Point2D pt = (Point2D)destPt.clone();
     * pt.setLocation(sourceRect.x + (sourceRect.width - 1.0)/2.0,
     *                sourceRect.y + (sourceRect.height - 1.0)/2.0);
     * </pre>
     *
     * Subclasses requiring different behavior should override this
     * method.</p>
     *
     * @param destPt the position in destination image coordinates
     * to map to source image coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>destPt</code> or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is
     * negative or greater than or equal to the number of sources.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt, int sourceIndex) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        Rectangle destRect = new Rectangle((int)destPt.getX(),
                                           (int)destPt.getY(),
                                           1, 1);

        Rectangle sourceRect = mapDestRect(destRect, sourceIndex);

        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation(sourceRect.x + (sourceRect.width - 1.0)/2.0,
                       sourceRect.y + (sourceRect.height - 1.0)/2.0);

        return pt;
    }

    /**
     * Computes the position in the destination that best
     * matches the supplied source image position. If it
     * is not possible to compute the requested position,
     * <code>null</code> will be returned. If the point is mapped
     * outside the destination bounds, the coordinate value or
     * <code>null</code> may be returned at the discretion of the
     * implementation.
     *
     * <p>Floating-point input and output coordinates are supported,
     * and recommended when possible.  Subclass implementations may
     * however use integer computation if necessary for simplicity.</p>
     *
     * <p>The implementation in this class returns the value of
     * <code>pt</code> in the following code snippet:
     *
     * <pre>
     * Rectangle sourceRect = new Rectangle((int)sourcePt.getX(),
     *                                      (int)sourcePt.getY(),
     *                                      1, 1);
     * Rectangle destRect = mapSourceRect(sourceRect, sourceIndex);
     * Point2D pt = (Point2D)sourcePt.clone();
     * pt.setLocation(destRect.x + (destRect.width - 1.0)/2.0,
     *                destRect.y + (destRect.height - 1.0)/2.0);
     * </pre>
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>sourcePt</code> or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>sourcePt</code> is
     * <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is
     * negative or greater than or equal to the number of sources.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt, int sourceIndex) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        Rectangle sourceRect = new Rectangle((int)sourcePt.getX(),
                                             (int)sourcePt.getY(),
                                             1, 1);

        Rectangle destRect = mapSourceRect(sourceRect, sourceIndex);

        // Return null of destination rectangle is not computable.
        if(destRect == null) {
            return null;
        }

        Point2D pt = (Point2D)sourcePt.clone();
        pt.setLocation(destRect.x + (destRect.width - 1.0)/2.0,
                       destRect.y + (destRect.height - 1.0)/2.0);

        return pt;
    }

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source.  An empty <code>Rectangle</code> may be returned
     * if the destination is unaffected by the contents of the source
     * rectangle.  This is distinct from a <code>null</code> return value
     * which serves rather to indicate that it is not possible to
     * determine the bounds of the affected region.  The safest
     * interpretation of a <code>null</code> return value is that the
     * entire destination might be affected by any pixel within the
     * given source rectangle.
     *
     * @param sourceRect  The <code>Rectangle</code> in source coordinates.
     * @param sourceIndex  The index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the potentially
     *         affected destination region, or <code>null</code> if
     *         the region is unknown.
     *
     * @throws IllegalArgumentException  If the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException  If <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public abstract Rectangle mapSourceRect(Rectangle sourceRect,
                                            int sourceIndex);
    
    /**
     * Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle.  The computation may as appropriate
     * clip the mapped <code>Rectangle</code> to the actual bounds of the
     * source or may treat the source as having infinite extent.
     * It is therefore the responsibility of the invoking
     * object to constrain the region in accordance with its needs.
     * Returning an empty <code>Rectangle</code> should indicate that
     * the data of the source image in question are not required for the
     * computation of the specified destination region.  If the entire
     * source image might be required to compute this destination
     * region, then <code>getSourceImage(sourceIndex).getBounds()</code>
     * should be returned.
     *
     * <p> To illustrate the issue of whether the source should be thought
     * to have infinite extent, consider the case wherein computing a
     * destination pixel requires multiple source pixels of context.  At
     * the source image boundary, these pixels might only be available if the
     * source data were extrapolated, e.g., using a {@link BorderExtender}.
     * If such an extender were available, destination pixels could be
     * computed even if they mapped to a region on the source boundary so
     * in this case the source could be considered to have infinite extent.
     * If no such extender were available, only destination pixels with
     * source context contained within the source image bounds could be
     * considered so that it might be preferable to clip the rectangle to
     * the source bounds.</p>
     *
     * @param destRect  The <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex  The index of the source image.
     *
     * @return A non-<code>null</code> <code>Rectangle</code> indicating
     *         the required source region.
     *
     * @throws IllegalArgumentException  If the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException  If <code>destRect</code> is
     *         <code>null</code>.
     */
    public abstract Rectangle mapDestRect(Rectangle destRect,
                                          int sourceIndex);

    /**
     * Returns one of <code>OP_COMPUTE_BOUND</code>,
     * <code>OP_IO_BOUND</code>, or <code>OP_NETWORK_BOUND</code> to
     * indicate how the operation is likely to spend its time.  The
     * answer does not affect the output of the operation, but may
     * allow a scheduler to parallelize the computation of multiple
     * operations more effectively.
     *
     * <p> The implementation of this method in this class
     * returns <code>OP_COMPUTE_BOUND</code>.
     */
    public int getOperationComputeType() {
        return OP_COMPUTE_BOUND;
    }
    
    /**
     * Returns <code>true</code> if the <code>OpImage</code> returns an
     * unique <code>Raster</code> object every time <code>computeTile</code>
     * is called.  <code>OpImage</code>s that internally cache
     * <code>Raster</code>s and return them via <code>computeTile</code>
     * should return <code>false</code> for this method.  
     *
     * <p> The implementation of this method in this class always returns
     * <code>true</code>.
     */
    public boolean computesUniqueTiles() {
        return true;
    }

    /**
     * Uncaches all tiles and calls <code>super.dispose()</code>.
     * If a <code>TileRecycler</code> was defined via the configuration
     * variable <code>JAI.KEY_TILE_RECYCLER</code> when this image was
     * constructed and tile recycling was enabled via the configuration
     * variable <code>JAI.KEY_CACHED_TILE_RECYCLING_ENABLED</code>, then each
     * of this image's tiles which is currently in the cache will be
     * recycled.  This method may be invoked more than once although
     * invocations after the first one may do nothing.
     *
     * <p> The results of referencing an image after a call to
     * <code>dispose()</code> are undefined.</p>
     *
     * @since JAI 1.1.2
     */
    public synchronized void dispose() {
        if(isDisposed) {
            return;
        }

        isDisposed = true;

        if (cache != null) {
            if(isCachedTileRecyclingEnabled && tileRecycler != null) {
                Raster[] tiles = cache.getTiles(this);
                if(tiles != null) {
                    int numTiles = tiles.length;
                    for(int i = 0; i < numTiles; i++) {
                        tileRecycler.recycleTile(tiles[i]);
                    }
                }
            }
            cache.removeTiles(this);
        }
        super.dispose();
    }

    /**
     * Indicates whether the source with the given index has a
     * <code>BorderExtender</code>. If the source index is out of bounds
     * for the source vector of this <code>OpImage</code> then an
     * <code>ArrayIndexOutOfBoundsException</code> may be thrown.
     *
     * @param sourceIndex The index of the source in question.
     * @return <code>true</code> if the indicated source has an extender.
     *
     * @deprecated as of JAI 1.1.
     */
    public boolean hasExtender(int sourceIndex) {
        if(sourceIndex != 0) {
            throw new ArrayIndexOutOfBoundsException();
        } else if(this instanceof AreaOpImage) {
            return ((AreaOpImage)this).getBorderExtender() != null;
        } else if(this instanceof GeometricOpImage) {
            return ((GeometricOpImage)this).getBorderExtender() != null;
        }
        return false;
    }

    /**
     * Returns the effective number of bands of an image with a given
     * <code>SampleModel</code> and <code>ColorModel</code>.
     * Normally, this is given by
     * <code>sampleModel.getNumBands()</code>, but for images with an
     * <code>IndexColorModel</code> the effective number of bands is
     * given by <code>colorModel.getNumComponents()</code>, since
     * a single physical sample represents multiple color components.
     *
     * @deprecated as of JAI 1.1.
     */
    public static int getExpandedNumBands(SampleModel sampleModel,
                                          ColorModel colorModel) {
        if (colorModel instanceof IndexColorModel) {
            return colorModel.getNumComponents();
        } else {
            return sampleModel.getNumBands();
        }
    }

    /**
     * Returns the image's format tags to be used with
     * a <code>RasterAccessor</code>.
     *
     * <p> This method will compute and cache the tags the first time
     * it is called on a particular image.  The image's
     * <code>SampleModel</code> and <code>ColorModel</code> must be
     * set to their final values before calling this method.
     *
     * @return An array containing <code>RasterFormatTag</code>s for the
     * sources in the first <code>getNumSources()</code> elements and a
     * <code>RasterFormatTag</code> for the destination in the last element.
     */
    // XXX This method should be removed if we stop using RasterAccessor.
    protected synchronized RasterFormatTag[] getFormatTags() {
        if (formatTags == null) {
            RenderedImage[] sourceArray = new RenderedImage[getNumSources()];
            if(sourceArray.length > 0) {
                getSources().toArray(sourceArray);
            }
            formatTags = RasterAccessor.findCompatibleTags(sourceArray, this);
        }

        return formatTags;
    }

    /**
     * Returns the value of the instance variable <code>tileRecycler</code>.
     *
     * @since JAI 1.1.2
     */
    public TileRecycler getTileRecycler() {
        return tileRecycler;
    }

    /**
     * Creates a <code>WritableRaster</code> at the given tile grid position.
     * The superclass method {@link #createWritableRaster(SampleModel,Point)}
     * will be invoked with this image's <code>SampleModel</code> and the
     * location of the specified tile.
     *
     * <p>Subclasses should ideally use this method to create destination
     * tiles as this method will take advantage of any
     * <code>TileFactory</code> specified to the <code>OpImage</code> at
     * construction.</p>
     *
     * @since JAI 1.1.2
     */
    protected final WritableRaster createTile(int tileX, int tileY) {
        return createWritableRaster(sampleModel,
                                    new Point(tileXToX(tileX),
                                              tileYToY(tileY)));
    }

    /**
     * A tile recycling convenience method.
     *
     * <p>If <code>tileRecycler</code> is non-<code>null</code>, the call
     * is forwarded to {@link TileRecycler.recycleTile(Raster)}; otherwise
     * the method does nothing.</p>
     *
     * <p>This method is for use by subclasses which create
     * <code>Raster</code>s with limited scope which therefore may easily
     * be identified as safe candidates for recycling.  This might occur
     * for example within
     * {@link #computeRect(Raster[],WritableRaster,Rectangle)} or
     * {@link #computeTile(int,int)} wherein <code>Raster</code>s may be
     * created for use within the method but be eligible for garbage
     * collection once the method is exited.</p>
     *
     * @throws IllegalArgumentException if <code>tile</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    protected void recycleTile(Raster tile) {
	if (tile == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        if(tileRecycler != null) {
            tileRecycler.recycleTile(tile);
        }
    }
}
