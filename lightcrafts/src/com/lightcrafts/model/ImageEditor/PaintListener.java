/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.mediax.jai.PlanarImage;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 10, 2005
 * Time: 4:53:10 PM
 * To change this template use File | Settings | File Templates.
 */
interface PaintListener {
    void paintDone(PlanarImage image, Rectangle visibleRect, boolean synchronous, long time);
}
