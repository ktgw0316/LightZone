/*
 * $RCSfile: CodecRIFUtil.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/06/17 00:02:28 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageDecoder;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;
import com.lightcrafts.media.jai.util.DisposableNullOpImage;
import com.lightcrafts.media.jai.util.ImageUtil;

public class CodecRIFUtil {

    private CodecRIFUtil() {}

    public static RenderedImage create(String type,
                                       ParameterBlock paramBlock,
                                       RenderingHints renderHints) {
        ImagingListener listener = ImageUtil.getImagingListener(renderHints);

        SeekableStream source =
            (SeekableStream)paramBlock.getObjectParameter(0);

        ImageDecodeParam param = null;
        if (paramBlock.getNumParameters() > 1) {
            param = (ImageDecodeParam)paramBlock.getObjectParameter(1);
        }
        int page = 0;
        if (paramBlock.getNumParameters() > 2) {
            page = paramBlock.getIntParameter(2);
        }

        ImageDecoder dec = ImageCodec.createImageDecoder(type, source, param);
        try {
            int bound = OpImage.OP_IO_BOUND;
            ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

            if (renderHints != null) {
                RenderingHints.Key key;

                key = JAI.KEY_OPERATION_BOUND;
                if (renderHints.containsKey(key)) {
                    bound = ((Integer)renderHints.get(key)).intValue();
                }
            }

            // Set flag indicating that a recovery may be attempted if
            // an OutOfMemoryError occurs during the decodeAsRenderedImage()
            // call - which is only possible if the stream can seek backwards.
            boolean canAttemptRecovery = source.canSeekBackwards();

            // Save the stream position prior to decodeAsRenderedImage().
            long streamPosition = Long.MIN_VALUE;
            if(canAttemptRecovery) {
                try {
                    streamPosition = source.getFilePointer();
                } catch(IOException ioe) {
                    listener.errorOccurred(JaiI18N.getString("StreamRIF1"),
                                           ioe, CodecRIFUtil.class, false);
                    // Unset the recovery attempt flag but otherwise
                    // ignore the exception.
                    canAttemptRecovery = false;
                }
            }

            OpImage image = null;
            try {
                // Attempt to create an OpImage from the decoder image.
                image = new DisposableNullOpImage(dec.decodeAsRenderedImage(page),
                                                  layout,
                                                  renderHints,
                                                  bound);
            } catch(OutOfMemoryError memoryError) {
                // Ran out of memory - may be due to the decoder being
                // obliged to read the entire image when it creates the
                // RenderedImage it returns.
                if(canAttemptRecovery) {
                    // First flush the cache if one is defined.
                    TileCache cache = image != null ?
                        image.getTileCache() :
                        RIFUtil.getTileCacheHint(renderHints);
                    if(cache != null) {
                        cache.flush();
                    }

                    // Force garbage collection.
                    System.gc(); //slow

                    // Reposition the stream before the previous decoding.
                    source.seek(streamPosition);

                    // Retry image decoding.
                    image = new DisposableNullOpImage(dec.decodeAsRenderedImage(page),
                                                      layout,
                                                      renderHints,
                                                      bound);
                } else {
                    // Re-throw the error.
                    String message = JaiI18N.getString("CodecRIFUtil0");
                    listener.errorOccurred(message,
                                           new ImagingException(message,
                                                                memoryError),
                                           CodecRIFUtil.class, false);
//                    throw memoryError;
                }
            }

            return image;
        } catch (Exception e) {
            listener.errorOccurred(JaiI18N.getString("CodecRIFUtil1"),
                                   e, CodecRIFUtil.class, false);
//            e.printStackTrace();
            return null;
        }
    }
}
