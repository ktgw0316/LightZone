/*
 * $RCSfile: HistogramRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:28 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * A <code>RIF</code> supporting the "Histogram" operation in the
 * rendered image layer.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.HistogramDescriptor
 * @see HistogramOpImage
 *
 */
public class HistogramRIF implements RenderedImageFactory {

    /** Constructor. */
    public HistogramRIF() {}

    /**
     * Creates a new instance of <code>HistogramOpImage</code>
     * in the rendered layer. Any image layout information in
     * <code>RenderingHints</code> is ignored.
     * This method satisfies the implementation of RIF.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        RenderedImage src = args.getRenderedSource(0);

        int xStart = src.getMinX();	// default values
        int yStart = src.getMinY();

        int maxWidth = src.getWidth();
        int maxHeight = src.getHeight();

        ROI roi = (ROI)args.getObjectParameter(0);
        int xPeriod = args.getIntParameter(1);
        int yPeriod = args.getIntParameter(2);
        int[] numBins = (int[])args.getObjectParameter(3);
        double[] lowValue = (double[])args.getObjectParameter(4);
        double[] highValue = (double[])args.getObjectParameter(5);

        HistogramOpImage op = null;
        try {
            op = new HistogramOpImage(src,
                                      roi,
                                      xStart, yStart,
                                      xPeriod, yPeriod,
                                      numBins, lowValue, highValue);
        } catch (Exception e) {
            ImagingListener listener = ImageUtil.getImagingListener(hints);
            String message = JaiI18N.getString("HistogramRIF0");
            listener.errorOccurred(message, e, this, false);
        }

        return op;
    }
}
