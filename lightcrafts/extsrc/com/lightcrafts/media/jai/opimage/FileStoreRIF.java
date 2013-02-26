/*
 * $RCSfile: FileStoreRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/02 01:51:26 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.OutputStream;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RenderedImageAdapter;
import com.lightcrafts.mediax.jai.registry.RIFRegistry;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.codec.SeekableOutputStream;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * @see com.lightcrafts.mediax.jai.operator.FileDescriptor
 *
 * @since EA4
 *
 */
public class FileStoreRIF implements RenderedImageFactory {
    /** The default file format. */
    private static String DEFAULT_FORMAT = "tiff";

    /** Constructor. */
    public FileStoreRIF() {}

    /*
     * Private class which merely adds a finalize() method to close
     * the associated stream.
     */
    private class FileStoreImage extends RenderedImageAdapter {
        private OutputStream stream;

        /*
         * Create the object and cache the stream.
         */
        public FileStoreImage(RenderedImage image,
                              OutputStream stream) {
            super(image);
            this.stream = stream;
        }

        /*
         * Close the stream.
         */
        public void dispose() {
            try {
                stream.close();
            } catch(IOException e) {
                // Ignore it ...
            }
            super.dispose();
        }
    }

    /**
     * Stores an image to a file.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        ImagingListener listener = ImageUtil.getImagingListener(renderHints);

        // Retrieve the file path.
        String fileName = (String)paramBlock.getObjectParameter(0);

        // Retrieve the file format preference.
        String format = (String)paramBlock.getObjectParameter(1);

        // TODO: If format is null get format name from file extension.

        // If the format is still null use the default format.
        if(format == null) {
            format = DEFAULT_FORMAT;
        }

        // Retrieve the ImageEncodeParam (which may be null).
        ImageEncodeParam param = null;
        if(paramBlock.getNumParameters() > 2) {
            param = (ImageEncodeParam)paramBlock.getObjectParameter(2);
        }

        // Create a FileOutputStream from the file name.
        OutputStream stream = null;
        try {
            if(param == null) {
                // Use a BufferedOutputStream for greater efficiency
                // since no compression is occurring.
                stream =
                    new BufferedOutputStream(new FileOutputStream(fileName));
            } else {
                // Use SeekableOutputStream to avoid temp cache file
                // in case of compression.
                stream =
                    new SeekableOutputStream(new RandomAccessFile(fileName,
                                                                  "rw"));
            }
        } catch (FileNotFoundException e) {
            String message = JaiI18N.getString("FileLoadRIF0") + fileName;
            listener.errorOccurred(message, e, this, false);
//            e.printStackTrace();
            return null;
        } catch (SecurityException e) {
            String message = JaiI18N.getString("FileStoreRIF0");
            listener.errorOccurred(message, e, this, false);
//            e.printStackTrace();
            return null;
        }

        // Add the operation to the DAG.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(paramBlock.getSource(0));
	pb.add(stream).add(format).add(param);

        // Get the default registry.
        OperationRegistry registry = (renderHints == null) ? null :
	    (OperationRegistry)renderHints.get(JAI.KEY_OPERATION_REGISTRY);

        PlanarImage im = new FileStoreImage(RIFRegistry.create
			    (registry, "encode", pb, renderHints), stream);

        return im;
    }
}
