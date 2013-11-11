/*
 * $RCSfile: FileLoadRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2006/06/17 00:02:28 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.RenderedImageAdapter;
import com.lightcrafts.mediax.jai.registry.RIFRegistry;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.codec.FileSeekableStream;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;
import com.lightcrafts.media.jai.util.ImageUtil;

/*
 * Package-scope class which merely adds a finalize() method to close
 * the associated stream and a dispose() method to forward the dispose()
 * call if possible.
 */
class StreamImage extends RenderedImageAdapter {
    private InputStream stream;

    /*
     * Create the object and cache the stream.
     */
    public StreamImage(RenderedImage image,
                       InputStream stream) {
        super(image);
        this.stream = stream;
        if(image instanceof OpImage) {
            // Set the properties related to TileCache key as used in
            // RenderedOp.
            setProperty("tile_cache_key", image);
            Object tileCache = ((OpImage)image).getTileCache();
            setProperty("tile_cache",
                        tileCache == null ?
                        java.awt.Image.UndefinedProperty : tileCache);
        }
    }

    public void dispose() {
        // Use relection to invoke dispose();
        RenderedImage trueSrc = getWrappedImage();
        Method disposeMethod = null;
        try {
            Class<? extends RenderedImage> cls = trueSrc.getClass();
            disposeMethod = cls.getMethod("dispose", (Class<?>[]) null);
            if(!disposeMethod.isAccessible()) {
                AccessibleObject.setAccessible(new AccessibleObject[] {
                    disposeMethod
                }, true);
            }
            disposeMethod.invoke(trueSrc, (Object[]) null);
        } catch(Exception e) {
            // Ignore it.
        }
    }

    /*
     * Close the stream.
     */
    protected void finalize() throws Throwable {
        stream.close();
        super.finalize();
    }
}

/**
 * @see com.lightcrafts.mediax.jai.operator.FileDescriptor
 *
 * @since EA3
 *
 */
public class FileLoadRIF implements RenderedImageFactory {

    /** Constructor. */
    public FileLoadRIF() {}

    /**
     * Creates an image from a String containing a file name.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        ImagingListener listener = ImageUtil.getImagingListener(hints);

        try {
            // Create a SeekableStream from the file name (first parameter).
            String fileName = (String)args.getObjectParameter(0);

            SeekableStream src = null;
            try {
                src = new FileSeekableStream(fileName);
            } catch (FileNotFoundException fnfe) {
                // Try to get the file as an InputStream resource. This would
                // happen when the application and image file are packaged in
                // a JAR file
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
                if (is != null)
                    src = SeekableStream.wrapInputStream(is, true);
            }

            ImageDecodeParam param = null;
            if (args.getNumParameters() > 1) {
                param = (ImageDecodeParam)args.getObjectParameter(1);
            }

            ParameterBlock newArgs = new ParameterBlock();
            newArgs.add(src);
            newArgs.add(param);

            RenderingHints.Key key = JAI.KEY_OPERATION_BOUND;
            int bound = OpImage.OP_IO_BOUND;
            if (hints == null) {
                hints = new RenderingHints(key, new Integer(bound));
            } else if (!hints.containsKey(key)) {
                hints = (RenderingHints)hints.clone();
                hints.put(key, new Integer(bound));
            }

            // Get the registry from the hints, if any.
            // Don't check for null hints as it cannot be null here.
            OperationRegistry registry =
                (OperationRegistry)hints.get(JAI.KEY_OPERATION_REGISTRY);

            // Create the image using the most preferred RIF for "stream".
            RenderedImage image =
                RIFRegistry.create(registry, "stream", newArgs, hints);

            return image == null ? null : new StreamImage(image, src);

        } catch (FileNotFoundException e) {
            String message =
                JaiI18N.getString("FileLoadRIF0") + args.getObjectParameter(0);
            listener.errorOccurred(message, e, this, false);
//            e.printStackTrace();
            return null;
        } catch (Exception e) {
            String message = JaiI18N.getString("FileLoadRIF1");
            listener.errorOccurred(message, e, this, false);
//            e.printStackTrace();
            return null;
        }
    }
}
