/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.image.libs.LCTIFFReader;
import com.lightcrafts.image.metadata.DCRawMetadataReader;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.jai.opimage.RGBDemosaicOpImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.utils.DCRaw;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

import javax.media.jai.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

/**
 * A <code>RawImageType</code> is-an {@link ImageType} that is the base class
 * for raw images.  It factors out common code that uses Dave Coffin's
 * <i>dcraw</i> to extract information from camera raw image files.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class RawImageType extends ImageType {

    private static final boolean CACHE_CONVERSION = true;

    static final boolean USE_EMBEDDED_PREVIEW = false;

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getDimension( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        val auxInfo = imageInfo.getAuxiliaryInfo();
        assert auxInfo instanceof RawImageInfo;
        val dcRaw = ((RawImageInfo)auxInfo).getDCRaw();
        return new Dimension( dcRaw.getImageWidth(), dcRaw.getImageHeight() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized PlanarImage getImage( ImageInfo imageInfo, ProgressThread thread )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException, UserCanceledException
    {
        long startTime = System.currentTimeMillis();

        val rawInfo = (RawImageInfo)imageInfo.getAuxiliaryInfo();
        val dcRaw = rawInfo.getDCRaw();

        if (!dcRaw.decodable() || dcRaw.rawColors() != 3)
            throw new UnknownImageTypeException("Unsupported Camera");

        String cacheKey = null;
        File imageFile = null;
        val fileCache = RawImageCache.getCacheFor( imageInfo );

        if ( CACHE_CONVERSION && fileCache != null ) {
            System.out.println("Checking cache for: " + imageInfo);
            val t1 = System.currentTimeMillis();
            cacheKey = RawImageCache.getCacheKeyFor( imageInfo );
            if ( cacheKey != null )
                imageFile = RawImageCache.getCachedImageFileFor( cacheKey );
            if (imageFile != null) {
                val fileName = imageFile.getAbsolutePath();
                try {
                    /* val reader = new LCTIFFReader( fileName, true );
                    val image = reader.getImage( thread ); */
                    val image = new LCTIFFReader.TIFFImage(fileName);
                    val t2 = System.currentTimeMillis();
                    System.out.println("Retrieved Cached image in " + (t2 - t1) + "ms");
                    return image;
                } catch (LCImageLibException e) {
                    // never mind, don't use the cache
                    e.printStackTrace();
                }
            } else
                System.out.println("File not in cache.");
        }

        ProgressIndicator indicator = null;
        if ( thread != null )
            indicator = thread.getProgressIndicator();
        if ( indicator != null ) {
            indicator.setMinimum( 0 );
            indicator.setMaximum( 3 );
        }

        val filters = dcRaw.getFilters();

        val colorModel = RasterFactory.createComponentColorModel(
                DataBuffer.TYPE_USHORT, JAIContext.linearColorSpace, false, false,
                Transparency.OPAQUE
        );

        val dcrawImage = (BufferedImage) dcRaw.runDCRaw(DCRaw.dcrawMode.full, false);
        val dcrawTime = System.currentTimeMillis();

        val metadata = imageInfo.getMetadata();
        val imageWidth = metadata.getImageWidth();
        val imageHeight = metadata.getImageHeight();
        System.out.println("metadata width: " + imageWidth + ", height: " + imageHeight);

        val dcrawWidth  = dcrawImage.getWidth();
        val dcrawHeight = dcrawImage.getHeight();
        System.out.println("dcraw    width: " + dcrawWidth + ", height: " + dcrawHeight);

        if ( thread != null && thread.isCanceled() )
            return null;

        if ( indicator != null )
            indicator.incrementBy(1);

        val dcrawLayout = new ImageLayout(
                0, 0, dcrawWidth, dcrawHeight,
                0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                colorModel.createCompatibleSampleModel(
                        JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT),
                colorModel);

        PlanarImage rgbImage;

        if (dcrawImage.getSampleModel().getNumBands() == 1 && filters != 0 && filters != -1) {
            rgbImage = new RGBDemosaicOpImage(dcrawImage, null, dcrawLayout, filters);

            val make = dcRaw.getCameraMake(false);
            val cameraMake = make == null ? "" : make;

            if ((this instanceof RAFImageType
                 || (this instanceof DNGImageType && cameraMake.startsWith("FUJI")))
                && dcRaw.getImageWidth() != dcRaw.getRawWidth()) {

                val angle =  (dcRaw.getModel().equals("FinePix S2Pro"))
                        ? 3 * Math.PI / 4
                        : Math.PI / 4;
                val width  = dcRaw.getImageWidth();
                val height = dcRaw.getImageHeight();

                rgbImage = FujiRotatedImage(rgbImage, colorModel, width, height, angle);
            } else if (cameraMake.equals("NIKON") && dcRaw.getModel().equals("D1X")) {
                rgbImage = nikonD1XImage(rgbImage, colorModel);
            } else {
                rgbImage = Functions.crop(rgbImage,
                                          rgbImage.getMinX() + 5,
                                          rgbImage.getMinY() + 5,
                                          rgbImage.getWidth() - 10,
                                          rgbImage.getHeight() - 10, JAIContext.noCacheHint);
            }

            val cacheLayout = new ImageLayout(
                    0, 0, rgbImage.getWidth(), rgbImage.getHeight(),
                    0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                    colorModel.createCompatibleSampleModel(
                            JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT),
                    colorModel);
            val cache = new CachedImage(cacheLayout, JAIContext.fileCache);

            retile(rgbImage, cache);
            rgbImage = cache;
        } else {
            val cache = new CachedImage(dcrawLayout, JAIContext.fileCache);

            for (int x = 0; x <= cache.getMaxTileX(); x++) {
                for (int y = 0; y <= cache.getMaxTileY(); y++) {
                    Functions.copyData(cache.getWritableTile(x, y), dcrawImage.getRaster());
                }
            }
            rgbImage = cache;
        }

        if (indicator != null)
            indicator.incrementBy(1);

        val demosaicTime = System.currentTimeMillis();

        if (indicator != null)
            indicator.incrementBy(1);

        System.out.println("dcraw: " + (dcrawTime - startTime)
                + "ms, demosaic: " + (demosaicTime - dcrawTime) + "ms");

        if (CACHE_CONVERSION && fileCache != null && imageFile == null && cacheKey != null) {
            RawImageCache.add(cacheKey, rgbImage);
        }

        return rgbImage;
    }

    private PlanarImage FujiRotatedImage(PlanarImage rgbImage, ComponentColorModel colorModel,
                                         int width, int height, double angle) {
        val interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        val rotation = AffineTransform.getRotateInstance(
                angle, rgbImage.getWidth() / 2, rgbImage.getHeight() / 2);
        val sm = colorModel.createCompatibleSampleModel(
                JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);

        val pb2 = new ParameterBlock();
        pb2.addSource(rgbImage);
        pb2.add(rotation);
        pb2.add(interp);
        RenderedOp rotated = JAI.create("Affine", pb2, null);

        val rotatedLayout = new ImageLayout(
                rotated.getBounds().x, rotated.getBounds().y,
                rotated.getBounds().width, rotated.getBounds().height,
                rotated.getBounds().x, rotated.getBounds().y,
                JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                sm, colorModel);
        val hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, rotatedLayout);

        rotated = JAI.create("Affine", pb2, hints);

        return Functions.crop(
                rotated,
                rotated.getMinX() + (rotated.getWidth()  - width)  / 2 + 2,
                rotated.getMinY() + (rotated.getHeight() - height) / 2 + 2,
                width - 4, height - 4, JAIContext.noCacheHint);
    }

    private PlanarImage nikonD1XImage(PlanarImage rgbImage, ComponentColorModel colorModel) {
        rgbImage = new TiledImage(rgbImage, true).getSubImage(
                rgbImage.getMinX() + 5,
                rgbImage.getMinY() + 5,
                rgbImage.getWidth() - 10,
                rgbImage.getHeight() - 10);

        val hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        val interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

        val sm = colorModel.createCompatibleSampleModel(
                JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);

        val newLayout = new ImageLayout(
                0, 0, 3 * rgbImage.getWidth() / 4, 3 * rgbImage.getHeight() / 2,
                0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                sm, colorModel);

        hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, newLayout));

        val pb2 = new ParameterBlock();
        pb2.addSource(rgbImage);
        pb2.add(AffineTransform.getScaleInstance(3.0 / 4.0, 3.0 / 2.0));
        pb2.add(interp);
        return JAI.create("Affine", pb2, hints);
    }

    private void retile(PlanarImage rgbImage, final CachedImage cache) {
        long tilingTime = System.currentTimeMillis();

        Rectangle bounds = cache.getBounds();
        bounds.translate(rgbImage.getMinX(), rgbImage.getMinY());
        val tileIndices = rgbImage.getTileIndices(bounds);

        val processors = Runtime.getRuntime().availableProcessors();

        val image = rgbImage;

        if (processors > 1) {
            val threads = new Thread[processors];
            int k = 0;
            for (int i = 0; i < processors; ++i) {
                int jobSize = tileIndices.length / processors;
                if (i == processors - 1)
                    jobSize += tileIndices.length % processors;

                val job = new Point[jobSize];
                for (int j = 0; j < jobSize; ++j) {
                    job[j] = tileIndices[k++];
                }

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        setTileData(job, image, cache);
                    }
                };

                threads[i] = new Thread(runnable, "RAW Processor " + i);
                threads[i].start();
            }
            for (val t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            setTileData(tileIndices, image, cache);
        }

        System.out.println("retiling: " + (System.currentTimeMillis() - tilingTime) + "ms");
    }

    private void setTileData(Point[] tileIndices, PlanarImage image, CachedImage cache) {
        for (val ti : tileIndices) {
            Raster tile = image.getTile(ti.x, ti.y);
            tile = tile.createTranslatedChild(tile.getMinX() - image.getMinX(),
                                              tile.getMinY() - image.getMinY());
            cache.setData(tile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        val rawInfo = (RawImageInfo)imageInfo.getAuxiliaryInfo();
        val dcRaw = rawInfo.getDCRaw();

        if (dcRaw.getThumbHeight() >= 400 && dcRaw.getThumbWidth() >= 600)
            return dcRaw.runDCRaw(DCRaw.dcrawMode.thumb);
        else
            return dcRaw.runDCRaw(DCRaw.dcrawMode.preview);
    }

    @Override
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        val rawInfo = (RawImageInfo)imageInfo.getAuxiliaryInfo();
        val dcRaw = rawInfo.getDCRaw();
        return dcRaw.runDCRaw(DCRaw.dcrawMode.thumb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RawImageInfo newAuxiliaryInfo( ImageInfo imageInfo ) {
        return new RawImageInfo( imageInfo );
    }

    /**
     * Reads all the image metadata provided by dcraw.
     *
     * @param imageInfo The image to read the metadata from.
     */
    @Override
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        new DCRawMetadataReader( imageInfo ).readMetadata();
    }
}
/* vim:set et sw=4 ts=4: */
