/*
 * $RCSfile: EncodeRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:24 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.OutputStream;
import java.io.IOException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import com.lightcrafts.media.jai.codec.ImageEncoder;
import com.lightcrafts.media.jai.util.ImageUtil;


/**
 * @see com.lightcrafts.mediax.jai.operator.FileDescriptor
 *
 * @since EA4
 *
 */
public class EncodeRIF implements RenderedImageFactory {

    /** Constructor. */
    public EncodeRIF() {}

    /**
     * Stores an image to a stream.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {

        ImagingListener listener = ImageUtil.getImagingListener(renderHints);

        // Retrieve the OutputStream.
        OutputStream stream = (OutputStream)paramBlock.getObjectParameter(0);

        // Retrieve the format.
        String format = (String)paramBlock.getObjectParameter(1);

        // Retrieve the ImageEncodeParam (which may be null).
        ImageEncodeParam param = null;
        if(paramBlock.getNumParameters() > 2) {
            param = (ImageEncodeParam)paramBlock.getObjectParameter(2);
        }

        // Create an ImageEncoder.
        ImageEncoder encoder =
            ImageCodec.createImageEncoder(format, stream, param);

        // Check the ImageEncoder.
        if(encoder == null) {
            throw new RuntimeException(JaiI18N.getString("EncodeRIF0"));
        }

        // Store the data.
        RenderedImage im = (RenderedImage)paramBlock.getSource(0);
        try {
            encoder.encode(im);
            stream.flush();
	    // Fix 4665208: EncodeRIF closed the stream after flush
	    // User may put more into the stream
            //stream.close();
        } catch (IOException e) {
            String message = JaiI18N.getString("EncodeRIF1") + " " + format;
            listener.errorOccurred(message, e, this, false);
//            e.printStackTrace();
            return null;
        }

        return im;
    }
}
