/*
 * $RCSfile: URLRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:46 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.registry.RIFRegistry;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * @see com.lightcrafts.mediax.jai.operator.URLDescriptor
 *
 * @since EA4
 *
 */
public class URLRIF implements RenderedImageFactory {

    /** Constructor. */
    public URLRIF() {}

    /**
     * Creates an image from a URL.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        try {
            // Create a SeekableStream from the URL (first parameter).
            URL url = (URL)paramBlock.getObjectParameter(0);
            InputStream stream = url.openStream();
            SeekableStream src = SeekableStream.wrapInputStream(stream, true);

            ImageDecodeParam param = null;
            if (paramBlock.getNumParameters() > 1) {
                param = (ImageDecodeParam)paramBlock.getObjectParameter(1);
            }

            ParameterBlock newParamBlock = new ParameterBlock();
            newParamBlock.add(src);
            newParamBlock.add(param);

            RenderingHints.Key key = JAI.KEY_OPERATION_BOUND;
            int bound = OpImage.OP_NETWORK_BOUND;
            if (renderHints == null) {
                renderHints = new RenderingHints(key, new Integer(bound));
            } else if (!renderHints.containsKey(key)) {
                renderHints.put(key, new Integer(bound));
            }

            // Get the registry from the hints, if any.
            // Don't check for null hints as it cannot be null here.
            OperationRegistry registry =
                (OperationRegistry)renderHints.get(JAI.KEY_OPERATION_REGISTRY);

            // Create the image using the most preferred RIF for "stream".
            RenderedImage image =
                RIFRegistry.create(registry, "stream", newParamBlock, renderHints);

            // NB: StreamImage is defined in FileLoadRIF.java.
            return image == null ? null : new StreamImage(image, src);
        } catch (IOException e) {
            ImagingListener listener =
                ImageUtil.getImagingListener(renderHints);
            String message = JaiI18N.getString("URLRIF0");
            listener.errorOccurred(message, e, this, false);
//            e.printStackTrace();
            return null;
        }
    }
}
