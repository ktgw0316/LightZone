/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2015-     Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.model.CloneContour;
import com.lightcrafts.model.Region;
import com.lightcrafts.model.Contour;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.LCROIShape;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.utils.SoftValueHashMap;
import javax.media.jai.*;

import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.color.ColorSpace;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: May 24, 2005
 * Time: 4:20:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShapedMask extends PlanarImage {
    private Region region;
    private LCROIShape shape;

    public static Rectangle getOuterBounds(Region region, AffineTransform transform) {
        Rectangle outerBounds = null;
        for (final Contour c : region.getContours()) {
            AffineTransform combined = transform;
            if (c.getTranslation() != null) {
                combined = AffineTransform.getTranslateInstance(c.getTranslation().getX(), c.getTranslation().getY());
                combined.preConcatenate(transform);
            }

            Rectangle cBounds = new Rectangle(c.getOuterShape().getBounds());
            cBounds = combined.createTransformedShape(cBounds).getBounds();

            if (outerBounds == null)
                outerBounds = cBounds;
            else
                outerBounds = outerBounds.union(cBounds);
        }

        return outerBounds;
    }

    private static ImageLayout createLayout(Region region, AffineTransform transform) {
        Rectangle regionBounds = getOuterBounds(region, transform);

        SampleModel graySm = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, regionBounds.width, regionBounds.height, 1);
        ColorModel grayCm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        return new ImageLayout(regionBounds.x, regionBounds.y, regionBounds.width, regionBounds.height,
                               regionBounds.x, regionBounds.y, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                               graySm, grayCm);
    }

    static ImageLayout createLayout(Rectangle regionBounds) {
        SampleModel graySm = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, regionBounds.width, regionBounds.height, 1);
        ColorModel grayCm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        return new ImageLayout(regionBounds.x, regionBounds.y, regionBounds.width, regionBounds.height,
                               regionBounds.x, regionBounds.y, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                               graySm, grayCm);
    }

    public ShapedMask(Region region, LCROIShape shape) {
        super(createLayout(region, shape.getTransform()), null, null);

        this.region = region;
        this.shape = shape;
    }

    static private Shape[] createBlurs(Shape shape, int width, float padding) {
        java.util.List<Shape> blurs = new LinkedList<Shape>();
        int feathering = (int) (width - 2 * padding);
        do {
            Stroke stroke = new BasicStroke(2 * (padding + feathering),
                                            BasicStroke.CAP_ROUND,
                                            BasicStroke.JOIN_ROUND);
            Shape blur = stroke.createStrokedShape(shape);
            blurs.add(blur);
            feathering /= 2;
        } while (feathering > 0);

        int size = blurs.size();
        Shape[] result = new Shape[size];
        int i = 0;
        for (final Shape b : blurs)
            result[size - 1 - i++] = b;
        return result;
    }

    private static Map<Contour, ScaledImage> bitmaps =
            Collections.synchronizedMap(new WeakHashMap<Contour, ScaledImage>());

    private static BorderExtender extender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

    private static class ScaledImage {
        float scale = 1;
        float tx = 0, ty = 0;
        PlanarImage image = null;
    }

    private static void drawBlurs(Graphics2D g2d, Shape shape, float contourWidth, float paddingWidth) {
        g2d.setColor(Color.white);
        g2d.fill(shape);

        if (contourWidth <= 1)
            return;

        // Draw the blurs in shades of gray:
        if (contourWidth > 1) {
            int width = (int) Math.floor(contourWidth);
            Shape[] blurs = createBlurs(shape, width, paddingWidth);

            int count = blurs.length;
            Area shapeArea = new Area(shape);
            for (int n = count - 1; n >= 0; n--) {
                Shape blur = blurs[n];
                Area semiBlur = new Area(blur);
                semiBlur.intersect(shapeArea);
                float value = n / (float) (count + 1);
                Color color = new Color(value, value, value);
                g2d.setColor(color);
                g2d.fill(semiBlur);
            }
        }
    }

    private static ScaledImage createContourImage(Contour contour) {
        Shape shape = contour.getOuterShape();

        // limit the blur radius to 7 pixels, use intelligent rescaling to emulate larger blurs
        float contourWidth = contour.getWidth();
        float widthScale = 1;
        int divideByTwo = 1;
        while (contourWidth > 7 * 4) {
            contourWidth /= 2;
            widthScale /= 2;
            divideByTwo *= 2;
        }

        if (widthScale < 1)
            shape = AffineTransform.getScaleInstance(widthScale, widthScale).createTransformedShape(shape);

        // We create the supporting BufferedImage contourWidth larger than
        // the outer shape bounds to allow for proper tapering of the blur

        Rectangle bounds = shape.getBounds();
        bounds.grow((int) contourWidth, (int) contourWidth);

        RenderedImage supportImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2d = ((BufferedImage) supportImage).createGraphics();

        g2d.setTransform(AffineTransform.getTranslateInstance(-bounds.x, -bounds.y));

        final float paddingWidth;
        final float kernelWidth;
        if (contour instanceof CloneContour && ((CloneContour)contour).getVersion() != null) {
            paddingWidth = contourWidth/6;
            kernelWidth = paddingWidth;
        }
        else {
            // make it backward compatible to LightZone v4.1.3 or earlier
            paddingWidth = 0;
            kernelWidth = contourWidth/4;
        }

        drawBlurs(g2d, shape, contourWidth, paddingWidth);

        g2d.dispose();

        supportImage = new TiledImage(supportImage,
                                      Math.max(JAIContext.TILE_WIDTH / divideByTwo, 8),
                                      Math.max(JAIContext.TILE_HEIGHT / divideByTwo, 8));

        ScaledImage contourImage = new ScaledImage();

        if (contourWidth > 1) {
            contourImage.image = Functions.fastGaussianBlur(supportImage, kernelWidth);
        } else {
            contourImage.image = (PlanarImage) supportImage;
        }

        contourImage.scale = widthScale;
        contourImage.tx = bounds.x;
        contourImage.ty = bounds.y;

        return contourImage;
    }

    private static synchronized ScaledImage getContourImage(Contour contour) {
        ScaledImage contourImage = bitmaps.get(contour);

        if (contourImage == null) {
            if ((contourImage = bitmaps.get(contour)) == null) {
                contourImage = createContourImage(contour);
                bitmaps.put(contour, contourImage);
            }
        }

        return contourImage;
    }

    static class RasterImage extends PlanarImage {
        private Raster raster;

        RasterImage(Raster r, ColorModel colorModel) {
            super(new ImageLayout(r.getMinX(),
                                  r.getMinY(),
                                  r.getWidth(),
                                  r.getHeight(),
                                  r.getMinX(),
                                  r.getMinY(),
                                  r.getWidth(),
                                  r.getHeight(),
                                  r.getSampleModel(),
                                  colorModel), null, null);
            raster = r;
        }

        @Override
        public Raster getTile(int tileX, int tileY) {
            return raster;
        }
    }

    private static final Map<AffinedImage, PlanarImage> expandedMasks =
            new SoftValueHashMap<AffinedImage, PlanarImage>();

    private static class AffinedImage {
        PlanarImage image;
        AffineTransform transform;

        AffinedImage(PlanarImage image, AffineTransform transform) {
            this.image = image;
            this.transform = transform;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AffinedImage))
                return false;

            final AffinedImage affinedImage = (AffinedImage) o;

            if (image != null ? !image.equals(affinedImage.image) : affinedImage.image != null)
                return false;
            if (transform != null ? !transform.equals(affinedImage.transform) : affinedImage.transform != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (image != null ? image.hashCode() : 0);
            result = 29 * result + (transform != null ? transform.hashCode() : 0);
            return result;
        }
    }

    @Override
    public Raster getData(Rectangle rect) {
        // SampleModel sampleModel = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, rect.width, rect.height, 1);
        ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                        false, false,
                                                        Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(rect.width, rect.height);

        TiledImage ti = new TiledImage(rect.x, rect.y, rect.width, rect.height,
                                       rect.x, rect.y, sampleModel, colorModel);

        WritableRaster result = (WritableRaster) ti.getData();

        boolean overlay = false;

        for (final Contour c : region.getContours()) {
            AffineTransform combined = shape.getTransform();
            if (c.getTranslation() != null) {
                combined = AffineTransform.getTranslateInstance(c.getTranslation().getX(),
                                                                c.getTranslation().getY());
                combined.preConcatenate(shape.getTransform());
            }

            // Take the blur tapering into account
            Rectangle bounds = c.getOuterShape().getBounds();
            bounds.grow((int) c.getWidth(), (int) c.getWidth());

            if (!combined.createTransformedShape(bounds).intersects(rect))
                continue;

            ScaledImage scaledImage = getContourImage(c);
            PlanarImage maskImage = scaledImage.image;

            if (!combined.isIdentity() || scaledImage.scale < 1 || scaledImage.tx != 0 || scaledImage.ty != 0) {
                AffineTransform transform = new AffineTransform(combined);

                if (scaledImage.scale < 1) {
                    float scaleX = (float) Math.floor(maskImage.getWidth() / scaledImage.scale) / (float) maskImage.getWidth();
                    float scaleY = (float) Math.floor(maskImage.getHeight() / scaledImage.scale) / (float) maskImage.getHeight();
                    transform.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
                }

                if (scaledImage.tx != 0 || scaledImage.ty != 0)
                    transform.concatenate(AffineTransform.getTranslateInstance(scaledImage.tx, scaledImage.ty));

                Rectangle scaledBounds = transform.createTransformedShape(maskImage.getBounds()).getBounds();

                // Avoid scaling underflows resulting into exeptions
                if (scaledBounds.width < 3 || scaledBounds.height < 3)
                    continue;

                synchronized (expandedMasks) {
                    AffinedImage key = new AffinedImage(maskImage, transform);
                    PlanarImage affinedImage = expandedMasks.get(key);
                    if (affinedImage == null) {
                        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                BorderExtender.createInstance(BorderExtender.BORDER_COPY));
                        // hints.add(JAIContext.noCacheHint);
                        Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                        ParameterBlock params = new ParameterBlock();
                        params.addSource(maskImage);
                        params.add(transform);
                        params.add(interp);
                        maskImage = JAI.create("Affine", params, hints);
                        expandedMasks.put(key, maskImage);
                    } else {
                        maskImage = affinedImage;
                    }
                }
            }

            if (!maskImage.getBounds().intersects(rect))
                continue;

            Rectangle itx = maskImage.getBounds().intersection(rect);

            byte[] resultData;
            if (!overlay) {
                resultData = (byte[]) maskImage.getData(itx).getDataElements(itx.x, itx.y, itx.width, itx.height, null);
                overlay = true;
            } else {
                resultData = (byte[]) result.getDataElements(itx.x, itx.y, itx.width, itx.height, null);
                byte[] currentData = (byte[]) maskImage.getData(itx).getDataElements(itx.x, itx.y, itx.width, itx.height, null);

                // blend overlapping regions using Porter-Duff alpha compositing: ar = a1 * (1 - a2) + a2
                for (int i = 0; i < resultData.length; i++) {
                    int current = currentData[i] & 0xFF;
                    if (current != 0) {
                        int cumulative = resultData[i] & 0xFF;
                        if (cumulative != 0) {
                            resultData[i] = (byte) ((cumulative * (0xff - current)) / 0x100 + current);
                        } else
                            resultData[i] = (byte) current;
                    }
                }
            }

            result.setDataElements(itx.x, itx.y, itx.width, itx.height, resultData);
        }

        return result;
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        return getData(new Rectangle(tileXToX(tileX), tileYToY(tileY), getTileWidth(), getTileHeight()));
    }
}
