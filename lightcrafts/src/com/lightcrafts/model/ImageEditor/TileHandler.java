/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 28, 2005
 * Time: 4:08:47 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TileHandler {
    public void handle(int tileX, int tileY, PaintContext ctx);
}
