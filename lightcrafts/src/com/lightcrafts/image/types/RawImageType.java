/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.File;

import Jama.Matrix;

import sun.awt.image.ShortInterleavedRaster;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.libs.LCTIFFReader;
import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.jai.opimage.RGBDemosaicOpImage;
import javax.media.jai.*;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.*;
import com.lightcrafts.utils.filecache.FileCache;

/**
 * A <code>RawImageType</code> is-an {@link ImageType} that is the base class
 * for raw images.  It factors out common code that uses Dave Coffin's
 * <i>dcraw</i> to extract information from camera raw image files.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class RawImageType extends ImageType {

    private static final boolean CACHE_CONVERSION = true;

    static final boolean USE_EMBEDDED_PREVIEW = false;

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public Dimension getDimension( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final AuxiliaryImageInfo auxInfo = imageInfo.getAuxiliaryInfo();
        assert auxInfo instanceof RawImageInfo;
        final DCRaw dcRaw = ((RawImageInfo)auxInfo).getDCRaw();
        return new Dimension( dcRaw.getImageWidth(), dcRaw.getImageHeight() );
    }

    static Matrix pseudoinverse(Matrix m) {
        Matrix t = m.transpose();
        return t.times(m).inverse().times(t).transpose();
    }

    // specialized to 3 x 3 so we can use the real inverse matrix instead of a pseudoinverse
    // some day we should go back to the more generic 3 x 4 case
    static void cam_xyz_coeff(float cam_xyz[][], float rgb_cam[][], float pre_mul[]) {
        double cam_rgb[][] = new double[3][3];

        for (int i = 0; i < 3; i++)             /* Multiply out XYZ colorspace */
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 3; k++)
                    cam_rgb[i][j] += cam_xyz[i][k] * ColorScience.XYZToRGBMat[k][j]; // xyz_rgb[k][j];

        for (int i = 0; i < 3; i++) {           /* Normalize cam_rgb so that */
            double num = 0;                     /* cam_rgb * (1,1,1) is (1,1,1,1) */
            for (int j = 0; j < 3; j++)
                num += cam_rgb[i][j];
            for (int j = 0; j < 3; j++)
                cam_rgb[i][j] /= num;
            pre_mul[i] = (float) (1 / num);
        }

        float inverse[][] = new Matrix(cam_rgb).inverse().transpose().getArrayFloat();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                rgb_cam[i][j] = inverse[j][i];
    }

    /**
     * {@inheritDoc}
     */
    public synchronized PlanarImage getImage( ImageInfo imageInfo, ProgressThread thread )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException, UserCanceledException
    {
        long startTime = System.currentTimeMillis();

        final RawImageInfo rawInfo = (RawImageInfo)imageInfo.getAuxiliaryInfo();
        final DCRaw dcRaw = rawInfo.getDCRaw();

        if (!dcRaw.decodable() || dcRaw.rawColors() != 3)
            throw new UnknownImageTypeException("Unsupported Camera");

        final ColorModel colorModel = RasterFactory.createComponentColorModel(
            DataBuffer.TYPE_USHORT, JAIContext.linearColorSpace, false, false,
            Transparency.OPAQUE
        );

        final ImageMetadata metadata = imageInfo.getMetadata();
        final int imageWidth = metadata.getImageWidth();
        final int imageHeight = metadata.getImageHeight();

        String cacheKey = null;
        File imageFile = null;
        final FileCache fileCache = RawImageCache.getCacheFor( imageInfo );

        if ( CACHE_CONVERSION && fileCache != null ) {
            System.out.println("Checking cache for: " + imageInfo);
            final long t1 = System.currentTimeMillis();
            cacheKey = RawImageCache.getCacheKeyFor( imageInfo );
            if ( cacheKey != null )
                imageFile = RawImageCache.getCachedImageFileFor( cacheKey );
            if (imageFile != null) {
                final String fileName = imageFile.getAbsolutePath();
                try {
                    /* final LCTIFFReader reader = new LCTIFFReader( fileName, true );
                    final PlanarImage image = reader.getImage( thread ); */
                    final PlanarImage image = new LCTIFFReader.TIFFImage(fileName);
                    final long t2 = System.currentTimeMillis();
                    System.out.println("Retrieved Cached image in " + (t2 - t1) + "ms");
                    return image;
                } catch (LCImageLibException e) {
                    // never mind, don't use the cache
                    e.printStackTrace();
                }
            } else
                System.out.println("File not in cache.");
        }

        System.out.println("metadata width: " + imageWidth + ", height: " + imageHeight);

        ProgressIndicator indicator = null;
        if ( thread != null )
            indicator = thread.getProgressIndicator();
        if ( indicator != null ) {
            indicator.setMinimum( 0 );
            indicator.setMaximum( 3 );
        }

        final int filters = dcRaw.getFilters();

        final BufferedImage dcrawImage = (BufferedImage) dcRaw.runDCRaw(DCRaw.dcrawMode.full, false);

        System.out.println("dcraw width: " + dcrawImage.getWidth() + ", height: " + dcrawImage.getHeight());

        // Merge the secondary pixels of Fuji S3 and S5
        if (this instanceof RAFImageType && dcRaw.getSecondaryPixels() &&
            (dcRaw.getCameraMake(true).endsWith("S3PRO") || dcRaw.getCameraMake(true).endsWith("S5PRO")))
        {
            final int rawWidth = dcrawImage.getWidth();
            final int rawHeight = dcrawImage.getHeight();

            int rx, ry, bx, by;

            System.out.println("Filters: 0x" + Long.toString(0xffffffffL & filters, 16));

            switch (filters) {
                case 0x16161616:
                    rx=1; ry=1; bx=0; by=0;
                    break;
                case 0x61616161:
                    rx=1; ry=0; bx=0; by=1;
                    break;
                case 0x49494949:
                    rx=0; ry=1; bx=1; by=0;
                    break;
                case 0x94949494:
                    rx=0; ry=0; bx=1; by=1;
                    break;
                case 0:
                case -1:
                    rx=0; ry=0; bx=0; by=0;
                    break;
                default:
                    throw new UnknownImageTypeException("Unknown Bayer filter pattern.");
            }

            final ShortInterleavedRaster dcrawRaster = (ShortInterleavedRaster) dcrawImage.getRaster();

            final BufferedImage dcrawImage2 = (BufferedImage) dcRaw.runDCRaw(DCRaw.dcrawMode.full, true);
            final ShortInterleavedRaster dcrawRaster2 = (ShortInterleavedRaster) dcrawImage2.getRaster();

            short data[] = dcrawRaster.getDataStorage();
            short data2[] = dcrawRaster2.getDataStorage();

            float[] cameraMultipliers = dcRaw.getCameraMultipliers();
            float[] secondaryCameraMultipliers = dcRaw.getSecondaryCameraMultipliers();

            float redRatio = secondaryCameraMultipliers[0] / cameraMultipliers[0];
            float blueRatio = secondaryCameraMultipliers[2] / cameraMultipliers[2];

            final int tl = (int) (0x6000/12.73f); // 3.5 stops
            final int th = (int) (0x8000/12.73f);

            for (int y = 0; y < rawHeight; y++) {
                for (int x = 0; x < rawWidth; x++) {
                    int d = (int) ((0xffff & data[y * rawWidth + x]) / 12.73f);
                    if (x >= 0 && y > 0 && x < rawWidth - 1 && y < rawHeight - 1 && d > tl) {
                        int d2 = 0xffff & data2[(y-1) * (rawWidth-1) + x];
                        if ((x&1) == rx && (y&1) == ry)
                            d2 = (int) (redRatio * d2);
                        if ((x&1) == bx && (y&1) == by)
                            d2 = (int) (blueRatio * d2);

                        if (d < th) {
                            float m = (th - d) / (float) (th - tl);
                            d = (int) (m * d + (1 - m) * d2);
                        } else
                            d = d2;
                    }

                    data[y * rawWidth + x] = (short) (0xffff & d);
                }
            }
        }

        long dcrawTime = System.currentTimeMillis();

        if ( thread != null && thread.isCanceled() )
            return null;

        if ( indicator != null )
            indicator.incrementBy(1);

        PlanarImage rgbImage;

        if (dcrawImage.getSampleModel().getNumBands() == 1 && filters != 0 && filters != -1) {
            ImageLayout demosaicLayout = new ImageLayout(0, 0, dcrawImage.getWidth(), dcrawImage.getHeight(),
                                                 0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                 colorModel.createCompatibleSampleModel(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT),
                                                 colorModel);

            rgbImage = new RGBDemosaicOpImage(dcrawImage, null, demosaicLayout, filters);

            if ((this instanceof RAFImageType
                 || (this instanceof DNGImageType && dcRaw.getCameraMake(false).startsWith("FUJI")))
                && dcRaw.getImageWidth() != dcRaw.getRawWidth()) {
                final Interpolation interp =
                        Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

                double angle = Math.PI / 4;

                if (dcRaw.getModel().equals("FinePix S2Pro"))
                    angle = 3 * Math.PI / 4;

                AffineTransform rotation = AffineTransform.getRotateInstance(angle,
                                                                             rgbImage.getWidth() / 2,
                                                                             rgbImage.getHeight() / 2);

                SampleModel sm = colorModel.createCompatibleSampleModel(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);

                final ParameterBlock pb2 = new ParameterBlock();
                pb2.addSource(rgbImage);
                pb2.add(rotation);
                pb2.add(interp);
                RenderedOp rotated = JAI.create("Affine", pb2, null);

                ImageLayout rotatedLayout = new ImageLayout(rotated.getBounds().x, rotated.getBounds().y,
                                                            rotated.getBounds().width, rotated.getBounds().height,
                                                            rotated.getBounds().x, rotated.getBounds().y,
                                                            JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                            sm, colorModel);

                final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, rotatedLayout);

                rotated = JAI.create("Affine", pb2, hints);

                int width = dcRaw.getImageWidth();
                int height = dcRaw.getImageHeight();

                rgbImage = Functions.crop(rotated,
                                          rotated.getMinX() + (rotated.getWidth() - width) / 2 + 2,
                                          rotated.getMinY() + (rotated.getHeight() - height) / 2 + 2,
                                          width - 4, height - 4, JAIContext.noCacheHint);
            } else if (dcRaw.getMake().equals("NIKON") && dcRaw.getModel().equals("D1X")) {
//                rgbImage = Functions.crop(rgbImage,
//                                          rgbImage.getMinX() + 2,
//                                          rgbImage.getMinY() + 2,
//                                          rgbImage.getWidth() - 6,
//                                          rgbImage.getHeight() - 6, null);
                rgbImage = new TiledImage(rgbImage, true).getSubImage(
                        rgbImage.getMinX() + 5,
                        rgbImage.getMinY() + 5,
                        rgbImage.getWidth() - 10,
                        rgbImage.getHeight() - 10);

                final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                                BorderExtender.createInstance(BorderExtender.BORDER_COPY));
                final Interpolation interp =
                        Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

                SampleModel sm = colorModel.createCompatibleSampleModel(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);

                ImageLayout newLayout = new ImageLayout(0, 0, 3 * rgbImage.getWidth() / 4, 3 * rgbImage.getHeight() / 2,
                                                        0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                        sm, colorModel);

                hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, newLayout));

                final ParameterBlock pb2 = new ParameterBlock();
                pb2.addSource(rgbImage);
                pb2.add(AffineTransform.getScaleInstance(3.0 / 4.0, 3.0 / 2.0));
                pb2.add(interp);
                rgbImage = JAI.create("Affine", pb2, hints);
            } else {
                rgbImage = Functions.crop(rgbImage,
                                          rgbImage.getMinX() + 5,
                                          rgbImage.getMinY() + 5,
                                          rgbImage.getWidth() - 10,
                                          rgbImage.getHeight() - 10, JAIContext.noCacheHint);
            }

            ImageLayout cacheLayout = new ImageLayout(0, 0, rgbImage.getWidth(), rgbImage.getHeight(),
                                     0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                     colorModel.createCompatibleSampleModel(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT),
                                     colorModel);

            final CachedImage cache = new CachedImage(cacheLayout, JAIContext.fileCache);

            long tilingTime = System.currentTimeMillis();

            Rectangle bounds = cache.getBounds();
            bounds.translate(rgbImage.getMinX(), rgbImage.getMinY());
            final Point[] tileIndices = rgbImage.getTileIndices(bounds);

            // rgbImage.queueTiles(tileIndices);

            int processors = Runtime.getRuntime().availableProcessors();

            if (processors > 1) {
                Thread threads[] = new Thread[processors];

                final PlanarImage image = rgbImage;

                final Point jobs[][] = new Point[processors][];
                int k = 0;

                for (int i = 0; i < processors; i++) {
                    if (i < processors - 1)
                        jobs[i] = new Point[tileIndices.length/processors];
                    else
                        jobs[i] = new Point[tileIndices.length - (processors - 1) * (tileIndices.length/processors)];

                    for (int j = 0; j < jobs[i].length; j++)
                        jobs[i][j] = tileIndices[k++];
                }

                for (int i = 0; i < processors; i++) {
                    final Point job[] = jobs[i];
                    Runnable runnable = new Runnable() {
                        public void run() {
                            for (Point ti : job) {
                                Raster tile = image.getTile(ti.x, ti.y);
                                tile = tile.createTranslatedChild(tile.getMinX()-image.getMinX(),
                                                                  tile.getMinY()-image.getMinY());
                                cache.setData(tile);
                            }
                        }
                    };

                    threads[i] = new Thread(runnable, "RAW Processor " + i);
                    threads[i].start();
                }
                for (int i = 0; i < processors; i++) {
                    try {
                        threads[i].join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                for (Point ti : tileIndices) {
                    Raster tile = rgbImage.getTile(ti.x, ti.y);
                    tile = tile.createTranslatedChild(tile.getMinX()-rgbImage.getMinX(), tile.getMinY()-rgbImage.getMinY());
                    cache.setData(tile);
                }
            }

            System.out.println("retiling: " + (System.currentTimeMillis() - tilingTime));

            rgbImage = cache;
        } else {
            ImageLayout layout = new ImageLayout(0, 0, dcrawImage.getWidth(), dcrawImage.getHeight(),
                                     0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                     colorModel.createCompatibleSampleModel(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT),
                                     colorModel);

            CachedImage cache = new CachedImage(layout, JAIContext.fileCache);

            for (int x = 0; x <= cache.getMaxTileX(); x++)
                for (int y = 0; y <= cache.getMaxTileY(); y++) {
                    Functions.copyData(cache.getWritableTile(x, y), dcrawImage.getRaster());
                }

             rgbImage = cache;
        }

        if (indicator != null)
            indicator.incrementBy(1);

        long demosaicTime = System.currentTimeMillis();

        if (indicator != null)
            indicator.incrementBy(1);

        System.out.println("dcraw: " + (dcrawTime - startTime) + ", demosaic: " + (demosaicTime - dcrawTime));

        if (CACHE_CONVERSION && fileCache != null && imageFile == null) {
            if ( rgbImage != null && cacheKey != null)
                RawImageCache.add(cacheKey,  rgbImage);
        }

        return  rgbImage;
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final RawImageInfo rawInfo = (RawImageInfo)imageInfo.getAuxiliaryInfo();
        final DCRaw dcRaw = rawInfo.getDCRaw();

        if (dcRaw.getThumbHeight() >= 400 && dcRaw.getThumbWidth() >= 600)
            return dcRaw.runDCRaw(DCRaw.dcrawMode.thumb);
        else
        return dcRaw.runDCRaw(DCRaw.dcrawMode.preview);
    }

    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final RawImageInfo rawInfo = (RawImageInfo)imageInfo.getAuxiliaryInfo();
        final DCRaw dcRaw = rawInfo.getDCRaw();
        return dcRaw.runDCRaw(DCRaw.dcrawMode.thumb);
    }

    /**
     * {@inheritDoc}
     */
    public RawImageInfo newAuxiliaryInfo( ImageInfo imageInfo ) {
        return new RawImageInfo( imageInfo );
    }

    /**
     * Reads all the image metadata provided by dcraw.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        new DCRawMetadataReader( imageInfo ).readMetadata();
    }

    /**
     * Writes the metadata for raw files to an XMP sidecar file.
     *
     * @param imageInfo The image to write the metadata for.
     */
    public void writeMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final File xmpFile = new File( imageInfo.getXMPFilename() );
        final ImageMetadata metadata = imageInfo.getCurrentMetadata();
        XMPMetadataWriter.mergeInto( metadata, xmpFile );
        metadata.clearEdited();
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>RawImageType</code>.
     */
    protected RawImageType() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
