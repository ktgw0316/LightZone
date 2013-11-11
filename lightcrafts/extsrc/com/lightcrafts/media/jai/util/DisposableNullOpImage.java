/*
 * $RCSfile: DisposableNullOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2006/06/17 00:02:27 $
 * $State: Exp $
 */

package com.lightcrafts.media.jai.util;

import java.awt.image.RenderedImage;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.NullOpImage;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RenderedImageAdapter;

/**
 * <code>NullOpImage</code> subclass which conditionally forwards
 * {@link #dispose()} to its source. The call will be forwarded if the
 * source is a <code>PlanarImage</code> or a <code>RenderedImage</code>
 * wrapped by <code>RenderedImageAdapter</code> and which has a
 * <code>dispose()</code> method with no parameters. In the former case
 * the call is forwarded directly, and in the latter via reflection.
 *
 * @since JAI 1.1.3
 */
public class DisposableNullOpImage extends NullOpImage {
    public DisposableNullOpImage(RenderedImage source,
                                 ImageLayout layout,
                                 Map configuration,
                                 int computeType) {
        super(source, layout, configuration, computeType);
    }

    public synchronized void dispose() {
        PlanarImage src = getSourceImage(0);
        if(src instanceof RenderedImageAdapter) {
            // Use relection to invoke dispose();
            RenderedImage trueSrc =
                ((RenderedImageAdapter)src).getWrappedImage();
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
        } else {
            // Invoke dispose() directly.
            src.dispose();
        }
    }
}
