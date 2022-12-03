/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.LCROIShape;
import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;
import com.lightcrafts.jai.opimage.BlendOpImage;
import com.lightcrafts.jai.opimage.RGBColorSelectionMaskOpImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.*;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.utils.LCMS_ColorSpace;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public abstract class BlendedOperation extends GenericOperationImpl implements Cloneable {
    private double opacity = 1.0;
    private String blendingMode = "Normal";

    private LCROIShape mask = null;

    private PlanarImage colorSelectionMask = null;
    private RGBColorSelection lastColorSelection = null;

    protected boolean colorInputOnly = false;

    protected AffineTransform lastTransform = null;

    public BlendedOperation clone(Rendering rendering) {
        try {
            BlendedOperation object = (BlendedOperation) this.clone();
            object.rendering = rendering;
            // object.mask = null;
            object.operation = null;
            object.lastTransform = null;
            return object;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean validRegion(Region region) {
        return region != null
               && region.getOuterShape().getBounds().height > 0
               && region.getOuterShape().getBounds().width > 0;
    }

    public boolean hasMask() {
        return mask != null;
    }

    @Override
    public void setRegion(Region region) {
        if (validRegion(region)) {
            mask = new LCROIShape(region, rendering.getInputTransform());
        } else if (region == null || region.getContours().size() == 0)
            mask = null;
        lastTransform = rendering.getInputTransform();
        super.setRegion(region);
    }

    @Override
    public void setRegionInverted(boolean inverted) {
        super.setRegionInverted(inverted);
    }

    // Define the List of LayerModes statically so they can be vended
    // by ImageEditorEngine:
    static List<LayerMode> blendingModes;

    static {
        blendingModes = new ArrayList<LayerMode>();

        for (BlendOpImage.BlendingMode b : BlendOpImage.BlendingMode.values())
            blendingModes.add(new LayerModeImpl(b.getName()));
    }

    BlendedOperation(Rendering rendering, OperationType type) {
        super(rendering, type);
    }

    // Support for color picker in color based selections

    private Point2D clickPoint = null;
    private Color selectedColor = null;

    static float arctan2(float y, float x) {
        final float coeff_1 = (float) Math.PI / 4;
        final float coeff_2 = 3 * coeff_1;
        final float abs_y = Math.abs(y) + 1e-10f;      // kludge to prevent 0/0 condition
        float angle;

        if (x >= 0) {
            float r = (x - abs_y) / (x + abs_y);
            angle = coeff_1 - coeff_1 * r;
        } else {
            float r = (x + abs_y) / (abs_y - x);
            angle = coeff_2 - coeff_1 * r;
        }

        return y < 0 ? -angle : angle;
    }

    public static float hue(float r, float g, float b) {
        float x = r - (g+b) / 2;
        float y = ((g-b) * (float) Math.sqrt(3) / 2);
        return arctan2(y, x) + (float) Math.PI;
    }

    @Override
    public void setColorSelection(RGBColorSelection selection) {
        super.setColorSelection(selection);
    }

    public PlanarImage getColorSelectionMask() {
        return colorSelectionMask;
    }

    @Override
    public RGBColorSelection getColorSelectionAt(Point2D p) {
        this.clickPoint = p;
        settingsChanged();

        if (selectedColor != null) {
            float r = selectedColor.getRed() / (float) 0xff;
            float g = selectedColor.getGreen() / (float) 0xff;
            float b = selectedColor.getBlue() / (float) 0xff;

            selectedColor = null;

            float feather = 0.1f;

            float luminosity = (float) (Math.log1p(0xff * ColorScience.Wr * r +
                                                   0xff * ColorScience.Wg * g +
                                                   0xff * ColorScience.Wb * b) / (8 * Math.log(2)));

            float minLuminosity = Math.max(luminosity-feather, 0);
            float minLuminosityFeather = Math.min(minLuminosity, feather);

            float maxLuminosity = Math.min(luminosity+feather, 1);
            float maxLuminosityFeather = Math.min(1-maxLuminosity, feather);

            return new RGBColorSelection(r, g, b, 0.4f,
                                         minLuminosity, minLuminosityFeather,
                                         maxLuminosity, maxLuminosityFeather,
                                         false, true, true);
        }
        return new RGBColorSelection();
    }

    abstract class BlendedTransform extends Transform {
        PlanarImage back;
        SoftReference<PlanarImage> softFront = new SoftReference<PlanarImage>(null);
        SoftReference<PlanarImage> softBlender = new SoftReference<PlanarImage>(null);
        SoftReference<PlanarImage> softResult = new SoftReference<PlanarImage>(null);

        int[] pointToPixel(Point2D p) {
            if (p == null)
                return null;

            Point2D pp = rendering.getTransform().transform(p, null);

            int x = (int) pp.getX();
            int y = (int) pp.getY();

            if (rendering.getScaleFactor() > 1) {
                x /= rendering.getScaleFactor();
                y /= rendering.getScaleFactor();
            }

            if (!back.getBounds().contains(x, y))
                return null;

            int tx = back.XToTileX(x);
            int ty = back.YToTileY(y);

            Raster tile = back.getTile(tx, ty);

            int[] pixel;

            final int averagePixels = 3;

            // if (averagePixels <= 1)
            //     return tile.getPixel(x, y, pixel);

            Rectangle tileBounds = tile.getBounds();
            Rectangle sampleRect = new Rectangle(x - averagePixels / 2,
                    y - averagePixels / 2,
                    averagePixels,
                    averagePixels);

            Rectangle intersection = tileBounds.intersection(sampleRect);

            pixel = new int[]{0, 0, 0};
            int[] currentPixel = new int[3];

            for (int i = intersection.x; i < intersection.x + intersection.width; i++) {
                for (int j = intersection.y; j < intersection.y + intersection.height; j++) {
                    currentPixel = tile.getPixel(i, j, currentPixel);
                    for (int k = 0; k < 3; k++)
                        pixel[k] = (pixel[k] + currentPixel[k]) / 2;
                }
            }

            return pixel;
        }

        abstract public PlanarImage setFront();

        @Override
        public void dispose() {
            back.removeSinks();
            back.dispose();
            back = null;
            mask = null;
            cachedImage = null;
        }

        private RenderedOp createBlender(PlanarImage front) {
            // Overlay result on the original image
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(front);
            pb.addSource(back);
            pb.add(blendingMode);
            pb.add(new Double(invertedRegion ? -opacity : opacity));
            pb.add(mask);
            pb.add(colorSelectionMask);

            // we don't know what front might generate, specify the output format to be the same as the input
            RenderingHints formatHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, Functions.getImageLayout(back));
            RenderedOp blender = JAI.create("Blend", pb, formatHints);
            // blender.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            return blender;
        }

        BlendedTransform(PlanarImage source) {
            back = source;
        }

        @Override
        public void setSource(Object source) {
            if (source != back) {
                back.removeSinks();
                back.dispose();
                back = (PlanarImage) source;
                changed = true;
            }
        }

        private CachedImage cachedImage = null;

        @Override
        public PlanarImage render() {
            /*
                we have to update before any render, this is necessary otherwise
                we would not propagate changes from one resolution to the other

                NB: make sure that operations cache their state efficiently
            */
            if (colorInputOnly && back.getColorModel().getNumComponents() != 3)
                return back;

            PlanarImage newRendering = update();

            if (cachedImage == null || newRendering != cachedImage.getRendering())
                cachedImage = new CachedImage(newRendering, scale);

            return cachedImage;
        }

        public class CachedImage extends PlanarImage {
            private final TileCache cache = JAIContext.fileCache;
            private SoftReference<PlanarImage> softRendering = new SoftReference<PlanarImage>(null);
            final float scale;

            public CachedImage(PlanarImage rendering, float scale) {
                super(new ImageLayout(rendering), null, null);
                setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                softRendering = new SoftReference<PlanarImage>(rendering);
                this.scale = scale;
                // System.out.println("new CachedImage " + BlendedOperation.this.getClass());
            }

            public PlanarImage getRendering() {
                return softRendering != null ? softRendering.get() : null;
            }

            @Override
            public Raster getTile(int tileX, int tileY) {
                Raster tile = cache.getTile(this, tileX, tileY);

                if (tile == null) {
                    PlanarImage rendering = softRendering.get();

                    if (rendering == null) {
                        System.out.println("rendering null..." + BlendedOperation.this.getClass());
                        rendering = update();
                        softRendering = new SoftReference<PlanarImage>(rendering);
                    }

                    tile = rendering.getTile(tileX, tileY);
                    cache.add(this, tileX, tileY, tile);
                }

                return tile;
            }
        }

        /*
            Note: ever change sources or parameters in JAI pipelines, it is the slowest thing of all,
            just rebuild everything from scratch
        */

        @Override
        public PlanarImage update() {
            if (clickPoint != null) {
                int[] pixel = pointToPixel(clickPoint);
                if (pixel != null) {
                    int r = pixel[0] / 256;
                    int g = pixel[1] / 256;
                    int b = pixel[2] / 256;

                    selectedColor = new Color(r, g, b);
                }
                clickPoint = null;
            }

            if (colorInputOnly && back.getColorModel().getNumComponents() != 3)
                return back;

            boolean newFront = false;

            PlanarImage front = softFront.get();

            if (front == null || changed) {
                front = setFront();
                softFront = new SoftReference<PlanarImage>(front);
                newFront = true;
            }

            PlanarImage result = softResult.get();

            if (!newFront && result != null)
                return result;

            RGBColorSelection colorSelection = getColorSelection();

            if (opacity == 1 && blendingMode.equals("Normal") && !validRegion(getRegion())
                    && (colorSelection == null || colorSelection.isAllSelected())) {
                softResult = softFront;
                return front;
            }

            RenderedOp blender = (RenderedOp) softBlender.get();

            if (validRegion(getRegion())
                    && (!rendering.getInputTransform().equals(lastTransform)
                    || (blender != null && blender.getParameters().get(2) != mask))) {
                mask = new LCROIShape(getRegion(), rendering.getInputTransform());
                blender = null;
            } else if (getRegion() == null) {
                mask = null;
            }

            if (colorSelection != null && !colorSelection.isAllSelected()
                    && (newFront
                    || !colorSelection.equals(lastColorSelection)
                    || !rendering.getInputTransform().equals(lastTransform)
                    || (blender != null && blender.getParameters().get(3) != colorSelectionMask))) {

                PlanarImage labImage = Functions.toColorSpace(back, new LCMS_ColorSpace(new LCMS.LABProfile()),
                        LCMSColorConvertDescriptor.RELATIVE_COLORIMETRIC, null);
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(labImage);
                pb.add(new int[]{1, 2});
                RenderedOp abImage = JAI.create("bandselect", pb, null);

                pb = new ParameterBlock();
                pb.addSource(back);
                pb.add(new double[][]{{ColorScience.Wr, ColorScience.Wg, ColorScience.Wb, 0}});
                PlanarImage monochrome = JAI.create("BandCombine", pb, null);

                RenderingHints layoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, Functions.getImageLayout(labImage));
                // layoutHints.add(JAIContext.noCacheHint);
                pb = new ParameterBlock();
                pb.addSource(monochrome);
                pb.addSource(abImage);
                PlanarImage maskImage = JAI.create("BandMerge", pb, layoutHints);

                colorSelectionMask = Functions.fastGaussianBlur(
                        new RGBColorSelectionMaskOpImage(maskImage, getColorSelection(), null), 0.5 * scale);
                lastColorSelection = colorSelection;
            } else if (colorSelection == null || colorSelection.isAllSelected()) {
                colorSelectionMask = null;
            }

            lastTransform = rendering.getInputTransform();

            softResult = softBlender = new SoftReference<PlanarImage>(blender = createBlender(front));
            return blender;
        }
    }

    abstract protected BlendedTransform createBlendedOp(PlanarImage source);

    @Override
    protected Transform createOp(PlanarImage source) {
        return createBlendedOp(source);
    }

    @Override
    public void setLayerConfig(LayerConfig layer) {
        if (!blendingMode.equals(layer.getMode().getName()) || opacity != layer.getOpacity()) {
            blendingMode = layer.getMode().getName();
            opacity = layer.getOpacity();
            settingsChanged();
        }
    }

    @Override
    public LayerConfig getDefaultLayerConfig() {
        return new LayerConfig(new LayerModeImpl("Normal"), 1.);
    }

    static RenderedImage createSingleChannel(RenderedImage source) {
        if (source.getColorModel().getNumComponents() != 3) {
            return source;
        }

        final double[][] yChannel = new double[][]{{ColorScience.Wr, ColorScience.Wg, ColorScience.Wb, 0}};
        ParameterBlock pb = new ParameterBlock()
                .addSource(source)
                .add(yChannel);
        return JAI.create("BandCombine", pb, null);
    }
}
