/*
 * $RCSfile: FormatCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/12 18:24:32 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.NullOpImage;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * A <code>CRIF</code> supporting the "Format" operation in the rendered
 * and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.FormatDescriptor
 * @see FormatOpImage
 *
 *
 * @since EA4
 */
public class FormatCRIF extends CRIFImpl {

    /** Constructor. */
    public FormatCRIF() {
        super("format");
    }

    /**
     * Creates a new instance of <code>FormatOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and data type
     * @param hints  Contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        
        // Get the source image and the data type parameter.
        RenderedImage src = args.getRenderedSource(0);
        Integer datatype = (Integer)args.getObjectParameter(0);
        int type = datatype.intValue();

        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // If there is no change return the source image directly.
        if(layout == null && type == src.getSampleModel().getDataType()) {
            return src;
        }

        // Create or clone the ImageLayout.
        if(layout == null) {
            layout = new ImageLayout(src);
        } else {
            layout = (ImageLayout)layout.clone();
        }

	boolean isDataTypeChange = false;

        // Get prospective destination SampleModel.
        SampleModel sampleModel = layout.getSampleModel(src);

        // Create a new SampleModel if the type is not as desired.
        if (sampleModel.getDataType() != type) {
            int tileWidth = layout.getTileWidth(src);
            int tileHeight = layout.getTileHeight(src);
            int numBands = src.getSampleModel().getNumBands();

            SampleModel csm =
                RasterFactory.createComponentSampleModel(sampleModel,
                                                         type,
                                                         tileWidth,
                                                         tileHeight,
                                                         numBands);

            layout.setSampleModel(csm);
	    isDataTypeChange = true;
        }


        // Check ColorModel.
        ColorModel colorModel = layout.getColorModel(null);
        if(colorModel != null &&
           !JDKWorkarounds.areCompatibleDataModels(layout.getSampleModel(src),
                                                   colorModel)) {
            // Clear the mask bit if incompatible.
            layout.unsetValid(ImageLayout.COLOR_MODEL_MASK);
        }

        // Check whether anything but the ColorModel is changing.
        if (layout.getSampleModel(src) == src.getSampleModel() &&
            layout.getMinX(src) == src.getMinX() &&
            layout.getMinY(src) == src.getMinY() &&
            layout.getWidth(src) == src.getWidth() &&
            layout.getHeight(src) == src.getHeight() &&
            layout.getTileWidth(src) == src.getTileWidth() &&
            layout.getTileHeight(src) == src.getTileHeight() &&
            layout.getTileGridXOffset(src) == src.getTileGridXOffset() &&
            layout.getTileGridYOffset(src) == src.getTileGridYOffset()) {

            if(layout.getColorModel(src) == src.getColorModel()) {
                // Nothing changed: return the source directly.
                return src;
            } else {
                // Remove TileCache hint from RenderingHints if present.
                RenderingHints hints = renderHints;
                if(hints != null && hints.containsKey(JAI.KEY_TILE_CACHE)) {
                    hints = new RenderingHints((Map)renderHints);
                    hints.remove(JAI.KEY_TILE_CACHE);
                }

                // Only the ColorModel is changing.
                return new NullOpImage(src, layout, hints,
                                       OpImage.OP_IO_BOUND);
            }
        }

	if (isDataTypeChange == true) {

	    // Add JAI.KEY_REPLACE_INDEX_COLOR_MODEL hint to renderHints
	    if (renderHints == null) {
		renderHints = 
		    new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
				       Boolean.TRUE);
		
	    } else if (!renderHints.containsKey(
					JAI.KEY_REPLACE_INDEX_COLOR_MODEL)) {
		// If the user specified a value for this hint, we don't
		// want to change that
		renderHints.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, 
				Boolean.TRUE);
	    }
	}

        return new CopyOpImage(src, renderHints, layout);
    }
}
