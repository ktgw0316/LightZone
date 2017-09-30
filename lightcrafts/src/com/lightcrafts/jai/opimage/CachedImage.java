/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;

import javax.media.jai.PlanarImage;
import javax.media.jai.TileCache;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterFactory;
import java.awt.image.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Oct 25, 2005
 * Time: 8:43:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class CachedImage extends PlanarImage {
    private final TileCache cache;

    // Provide an easy mechanism to generate sRGB images, workaround for a rendering bug on windogs
    static ImageLayout getsRGBImageLayout(PlanarImage image) {
        ImageLayout layout = new ImageLayout(image);
        layout.setColorModel(new ComponentColorModel(JAIContext.sRGBColorSpace, false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_BYTE));
        return layout;
    }

    // never create instances directly, go through cacheImage()
    public CachedImage(PlanarImage image, TileCache cache) {
        this(image, cache, false);
        setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
    }

    public CachedImage(PlanarImage image, TileCache cache, boolean castTosRGB) {
        super(castTosRGB ? getsRGBImageLayout(image) : new ImageLayout(image), null, null);
        setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
        this.cache = cache;
        cache.addTiles(this, image.getTileIndices(image.getBounds()), image.getTiles(), null);
    }

    public CachedImage(ImageLayout layout, TileCache cache) {
        super(layout, null, null);
        setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
        this.cache = cache;
    }

    private synchronized WritableRaster addTile(int tileX, int tileY) {
        WritableRaster raster = RasterFactory.createWritableRaster(getSampleModel(), new Point(tileX * getTileWidth(),
                                                                                               tileY * getTileHeight()));
        cache.add(this, tileX, tileY, raster);
        return raster;
    }

    public Raster getTile(int tileX, int tileY) {
        return cache.getTile(this, tileX, tileY);
    }

    public synchronized WritableRaster getWritableTile(int tileX, int tileY) {
        Raster raster = cache.getTile(this, tileX, tileY);
        if (raster == null)
            raster = addTile(tileX, tileY);
        return (WritableRaster) raster;
    }

    public synchronized void setData(Raster r) {
        // Return if the intersection of the image and Raster bounds is empty.
        Rectangle rBounds = r.getBounds();
        if((rBounds = rBounds.intersection(getBounds())).isEmpty()) {
            return;
        }

        // Set tile index limits.
        int txMin = XToTileX(rBounds.x);
        int tyMin = YToTileY(rBounds.y);
        int txMax = XToTileX(rBounds.x + rBounds.width - 1);
        int tyMax = YToTileY(rBounds.y + rBounds.height - 1);

        for(int ty = tyMin; ty <= tyMax; ty++) {
            for(int tx = txMin; tx <= txMax; tx++) {
                WritableRaster wr = getWritableTile(tx, ty);
                if(wr != null)
                    Functions.copyData(wr, r);
            }
        }
    }
/*
    public static PlanarImage cacheImage(PlanarImage image, TileCache cache) {
        return cacheImage(image, cache, false);
    }

    public static PlanarImage cacheImage(PlanarImage image, TileCache cache, boolean castTosRGB) {
        if (image instanceof CachedImage)
            return image;
        else
            return new CachedImage(image, cache, castTosRGB);
    }
*/
}
