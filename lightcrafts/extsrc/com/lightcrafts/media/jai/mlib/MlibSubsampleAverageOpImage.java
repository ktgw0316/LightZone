/*
 * $RCSfile: MlibSubsampleAverageOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:06 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;

import com.lightcrafts.media.jai.opimage.SubsampleAverageOpImage;
import com.sun.medialib.mlib.Image;
import com.sun.medialib.mlib.mediaLibImage;

public class MlibSubsampleAverageOpImage extends SubsampleAverageOpImage {
    /* XXX
    public static void main(String[] args) throws Throwable {
        RenderedImage source =
            com.lightcrafts.mediax.jai.JAI.create("fileload", args[0]);
        double scaleX = args.length > 1 ?
            Double.valueOf(args[1]).doubleValue() : 0.25;
        double scaleY = args.length > 2 ?
            Double.valueOf(args[2]).doubleValue() : scaleX;

        RenderedImage dest =
            new MlibSubsampleAverageOpImage(source, null, null,
                                            scaleX, scaleY);

        System.out.println(source.getClass().getName()+": "+
                           new ImageLayout(source));
        System.out.println(dest.getClass().getName()+": "+
                           new ImageLayout(dest));

        java.awt.Frame frame = new java.awt.Frame("Mlib Sub-average Test");
        frame.setLayout(new java.awt.GridLayout(1, 2));
        com.lightcrafts.mediax.jai.widget.ScrollingImagePanel ps =
            new com.lightcrafts.mediax.jai.widget.ScrollingImagePanel(source,
                                                           512, 512);
        com.lightcrafts.mediax.jai.widget.ScrollingImagePanel pd =
            new com.lightcrafts.mediax.jai.widget.ScrollingImagePanel(dest,
                                                           512, 512);
        frame.add(ps);
        frame.add(pd);
        frame.pack();
        frame.show();
    }
    */

    public MlibSubsampleAverageOpImage(RenderedImage source,
                                       ImageLayout layout,
                                       Map config,
                                       double scaleX,
                                       double scaleY) {
        super(source,
              layout,
              config,
              scaleX,
              scaleY);
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        Raster source = sources[0];
        Rectangle srcRect = source.getBounds();

        int formatTag = MediaLibAccessor.findCompatibleTag(sources,dest);

        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(source,srcRect,formatTag);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest,destRect,formatTag);

	mediaLibImage srcML[], dstML[];

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            srcML = srcAccessor.getMediaLibImages();
            dstML = dstAccessor.getMediaLibImages();

            Image.SubsampleAverage(dstML[0],
                                   srcML[0],
                                   scaleX,
                                   scaleY);
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
	    srcML = srcAccessor.getMediaLibImages();
            dstML = dstAccessor.getMediaLibImages();

            Image.SubsampleAverage_Fp(dstML[0],
                                      srcML[0],
                                      scaleX,
                                      scaleY);
	    break;

        default:
            // XXX?
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.copyDataToRaster();
        }
    }
}
