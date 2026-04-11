/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import org.eclipse.imagen.PlanarImage;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 3, 2005
 * Time: 4:39:28 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PaintContext {
    boolean isCancelled();

    boolean isSynchronous();

    boolean isPrefetch();

    PlanarImage getImage();
}
