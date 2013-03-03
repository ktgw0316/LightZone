/*
 * $RCSfile: WrapperWRI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:44 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;


/**
 * @since EA2
 */
public class WrapperWRI extends WrapperRI implements WritableRenderedImage {

    WritableRaster wras;

    public WrapperWRI(WritableRaster wras) {
        super(wras);
        this.wras = wras;
    }

    public void addTileObserver(TileObserver to) {
        throw new RuntimeException(JaiI18N.getString("WrapperWRI0"));
    }

    public void removeTileObserver(TileObserver to) {
        throw new RuntimeException(JaiI18N.getString("WrapperWRI0"));
    }
    
    public WritableRaster getWritableTile(int tileX, int tileY) {
        if ((tileX != 0) || (tileY != 0)) {
            throw new IllegalArgumentException();
        }
        return wras;
    }

    public void releaseWritableTile(int tileX, int tileY) {
        if ((tileX != 0) || (tileY != 0)) {
            throw new IllegalArgumentException();
        }
    }

    public boolean isTileWritable(int tileX, int tileY) {
        return true;
    }

    public Point[] getWritableTileIndices() {
        Point[] p = new Point[1];
        p[0] = new Point(0, 0);
        return p;
    }

    public boolean hasTileWriters() {
        return true;
    }

    public void setData(Raster r) {
        throw new RuntimeException(JaiI18N.getString("WrapperWRI0"));
    }
}
