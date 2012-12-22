/*
 * $RCSfile: ViewportListener.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:59 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.widget;

/**
 * An interface used by the <code>ScrollingImagePanel</code>
 * class to inform listeners of the current viewable area of the image.
 *
 * @see ScrollingImagePanel
 *
 * <p>
 * This class has been deprecated.  The source
 * code has been moved to the samples/widget
 * directory.  These widgets are no longer
 * supported.
 *
 * @deprecated as of JAI 1.1
 */
public interface ViewportListener {
    
    /**
     * Called to inform the listener of the currently viewable area od
     * the source image.
     *
     * @param x The X coordinate of the upper-left corner of the current 
     *          viewable area.
     * @param y The Y coordinate of the upper-left corner of the current 
     *          viewable area.
     * @param width The width of the current viewable area in pixels.
     * @param height The height of the current viewable area in pixels.
     */
    void setViewport(int x, int y, int width, int height);
}
