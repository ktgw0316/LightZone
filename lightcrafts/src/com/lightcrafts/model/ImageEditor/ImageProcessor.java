/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import javax.media.jai.RenderedOp;
import java.awt.image.RenderedImage;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Oct 12, 2006
 * Time: 8:21:09 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ImageProcessor {
    RenderedOp process(RenderedImage source);
}
