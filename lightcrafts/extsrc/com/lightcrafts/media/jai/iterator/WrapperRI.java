/*
 * $RCSfile: WrapperRI.java,v $
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
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;


/**
 * @since EA2
 */
public class WrapperRI implements RenderedImage {

    Raster ras;

    public WrapperRI(Raster ras) {
        this.ras = ras;
    }

    public Vector getSources() {
        return null;
    }

    public Object getProperty(String name) {
        return null;
    }

    public String[] getPropertyNames() {
        return null;
    }
    
    public ColorModel getColorModel() {
        return null;
    }

    public SampleModel getSampleModel() {
        return ras.getSampleModel();
    }

    public int getWidth() {
        return ras.getWidth();
    }

    public int getHeight() {
        return ras.getHeight();
    }
    
    public int getMinX() {
        return ras.getMinX();
    }

    public int getMinY() {
        return ras.getMinY();
    }

    public int getNumXTiles() {
        return 1;
    }

    public int getNumYTiles() {
        return 1;
    }

    public int getMinTileX() {
        return 0;
    }

    public int getMinTileY() {
        return 0;
    }

    public int getTileWidth() {
        return ras.getWidth();
    }

    public int getTileHeight() {
        return ras.getHeight();
    }

    public int getTileGridXOffset() {
        return ras.getMinX();
    }

    public int getTileGridYOffset() {
        return ras.getMinY();
    }

    public Raster getTile(int tileX, int tileY) {
        return ras;
    }

    public Raster getData() {
        throw new RuntimeException(JaiI18N.getString("WrapperRI0"));
    }

    public Raster getData(Rectangle rect) {
        throw new RuntimeException(JaiI18N.getString("WrapperRI0"));
    }

    public WritableRaster copyData(WritableRaster raster) {
        throw new RuntimeException(JaiI18N.getString("WrapperRI0"));
    }
}
