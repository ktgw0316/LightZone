/*
 * $RCSfile: ColormapOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:06 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;

/**
 * A class to be used to implement an operation which may conditionally
 * be accelerated by transforming the colormap of the source image instead
 * of its data.  An instance of this class will represent the destination
 * of a single-source point operation which may be effected by a simple
 * transformation of the source image colormap if the <code>ColorModel</code>s
 * of the source and destination images are both instances of
 * <code>IndexColorModel</code>.  A subclass may take advantage of this
 * capability merely by implementing <code>transformColormap()</code> and
 * invoking <code>initializeColormapOperation()</code> as the last
 * statement of its constructor.  If the <code>ColorModel</code>s are not
 * <code>IndexColorModel</code>s, the behavior is the same as if the
 * superclass <code>PointOpImage</code> had been extended directly.
 *
 * <p> The default behavior for a <code>ColormapOpImage</code> is to
 * do the transform only on the color map in order to accelerate processing
 * when the source and destination images are all color-indexed.
 * However, in some situations it may be desirable to transform the
 * pixel (index) data directly instead of transforming the colormap.
 * To suppress the acceleration, a mapping of the key
 * <code>JAI.KEY_TRANSFORM_ON_COLORMAP</code> with value
 * <code>Boolean.FALSE</code> should be added to the configuration
 * map (or the <code>RenderingHints</code> provided to the
 * <code>create</code> methods in the class <code>JAI</code>) supplied to
 * the corresponding operation when it is created.
 *
 * <p> Transforming on the pixel (index) data is only meaningful
 * when the transform maps all the possible index values of the source image
 * into the index value set of the destination image.  Otherwise,
 * it may generate pixel (index) values without color definitions, and
 * cause problems when computing the color components from pixel values.
 * In addition, the colormaps should be ordered in a useful way
 * for the transform in question, e.g. a smooth grayscale.</p>
 *
 * @see java.awt.image.IndexColorModel
 * @see PointOpImage
 *
 * @since JAI 1.1
 */
public abstract class ColormapOpImage extends PointOpImage {

    /** Whether the colormap acceleration flag has been initialized. */
    private boolean isInitialized = false;

    /** Whether the operation is effected via colormap transformation. */
    private boolean isColormapAccelerated;

    /**
     * Constructs a <code>ColormapOpImage</code> with one source image.
     * The parameters are forwarded unmodified to the equivalent
     * superclass constructor.
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
     */
    public ColormapOpImage(RenderedImage source,
                           ImageLayout layout,
                           Map configuration,
                           boolean cobbleSources) {
        super(source, // tested for null in superclass
              layout, configuration, cobbleSources);

        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.

	// Fix 4706651: ColormapOpImage should not force destination to 
	// have an IndexColorModel 
	/*
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
             sampleModel = source.getSampleModel().createCompatibleSampleModel(
                                                   tileWidth, tileHeight);
             colorModel = srcColorModel;
        }
	*/
	isColormapAccelerated = true;
	Boolean value =
            configuration == null ? Boolean.TRUE :
	    (Boolean)configuration.get(JAI.KEY_TRANSFORM_ON_COLORMAP);
	if (value != null)
	    isColormapAccelerated = value.booleanValue();
    }

    /**
     * Whether the operation is performed on the colormap directly.
     */
    protected final boolean isColormapOperation() {
        return isColormapAccelerated;
    }

    /**
     * Method to be invoked as the last statement of a subclass constructor.
     * The colormap acceleration flag is set appropriately and if
     * <code>true</code> a new <code>ColorModel</code> is calculated by
     * transforming the source colormap.  This method must be invoked in
     * the subclass constructor as it calls <code>transformColormap()</code>
     * which is abstract and therefore requires that the implementing class be
     * constructed before it may be invoked.
     */
    protected final void initializeColormapOperation() {
        // Retrieve the ColorModels
        ColorModel srcCM = getSource(0).getColorModel();
        ColorModel dstCM = super.getColorModel();

        // Set the acceleration flag.
        isColormapAccelerated &=
            srcCM != null && dstCM != null &&
            srcCM instanceof IndexColorModel &&
            dstCM instanceof IndexColorModel;

        // Set the initialization flag.
        isInitialized = true;

        // Transform the colormap if the operation is accelerated.
        if(isColormapAccelerated) {
            // Cast the target ColorModel.
            IndexColorModel icm = (IndexColorModel)dstCM;

            // Get the size and allocate the array.
            int mapSize = icm.getMapSize();
            byte[][] colormap = new byte[3][mapSize];

            // Load the colormap.
            icm.getReds(colormap[0]);
            icm.getGreens(colormap[1]);
            icm.getBlues(colormap[2]);

            // Transform the colormap.
            transformColormap(colormap);

            // Clamp the colormap if necessary. In the Sun implementation
            // of IndexColorModel, getComponentSize() always returns 8 so
            // no clamping will be performed.
            for(int b = 0; b < 3; b++) {
                int maxComponent = 0xFF >> (8 - icm.getComponentSize(b));
                if(maxComponent < 255) {
                    byte[] map = colormap[b];
                    for(int i = 0; i < mapSize; i++) {
                        if((map[i] & 0xFF) > maxComponent) {
                            map[i] = (byte)maxComponent;
                        }
                    }
                }
            }

            // Cache references to individual color component arrays.
            byte[] reds = colormap[0];
            byte[] greens = colormap[1];
            byte[] blues = colormap[2];

            // Create the RGB array.
            int[] rgb = new int[mapSize];

            // Copy the colormap into the RGB array.
            if(icm.hasAlpha()) {
                byte[] alphas = new byte[mapSize];
                icm.getAlphas(alphas);
                for(int i = 0; i < mapSize; i++) {
                    rgb[i] =
                        ((alphas[i] & 0xFF) << 24) |
                        ((reds[i] & 0xFF)   << 16) |
                        ((greens[i] & 0xFF) <<  8) |
                        (blues[i] & 0xFF);
                }
            } else {
                for(int i = 0; i < mapSize; i++) {
                    rgb[i] =
                        ((reds[i] & 0xFF)   << 16) |
                        ((greens[i] & 0xFF) <<  8) |
                        (blues[i] & 0xFF);
                }
            }

            // Create the new ColorModel.
            colorModel= new IndexColorModel(icm.getPixelSize(), mapSize,
                                            rgb, 0, icm.hasAlpha(),
                                            icm.getTransparentPixel(),
                                            sampleModel.getTransferType());
        }
    }

    /**
     * Transform the colormap according to the specific operation.
     * The modification is done in place so that the parameter array
     * will be modified.  The format of the parameter array is
     * <code>byte[3][]</code> wherein <code>colormap[0]</code>,
     * <code>colormap[1]</code>, and <code>colormap[2]</code>
     * represent the red, green, and blue colormaps, respectively.
     */
    protected abstract void transformColormap(byte[][] colormap);
}
