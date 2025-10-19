/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.Preview;
import com.lightcrafts.model.Region;
import com.lightcrafts.model.ZoneOperation;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.Segment;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileRGB;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.lightcrafts.model.ImageEditor.Locale.LOCALE;

public class ZoneFinder extends Preview implements PaintListener {
    private static final boolean ADJUST_GRAYSCALE = true;
    private static final int MAX_PREVIEW_DIMENSION = 512; // Maximum dimension for preview

    private final boolean colorMode;
    final ImageEditorEngine engine;

    // Caching for performance
    private RenderedImage cachedSegmentedImage = null;
    private RenderedImage cachedInputImage = null;
    private long cachedImageHash = 0;
    
    @Override
    public String getName() {
        return LOCALE.get( colorMode ? "ColorZones_Name" : "Zones_Name" );
    }

    @Override
    public void setDropper(Point p) {
        if (p == null || engine == null)
            return;

        final var sample = engine.getAveragedPixelValue(p.x, p.y);
        final var zone = (sample != null) ? (int) Math.round(calcZone(sample)) : -1;
        setFocusedZone(zone);
        // repaint();
    }

    @Override
    public void addNotify() {
        // This method gets called when this Preview is added.
        engine.update(null, false);
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        // This method gets called when this Preview is removed.
        stopSegmenter();
        super.removeNotify();
    }

    @Override
    public void setRegion(Region region) {
        // Fabio: only draw yellow inside the region?
    }

    @Override
    public void setSelected(Boolean selected) {
        if (!selected) {
            zones = null;
            cachedSegmentedImage = null;
            cachedInputImage = null;
            cachedImageHash = 0;
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (zones == null)
            engine.update(null, false);

        // Fill in the background:
        Graphics2D g = (Graphics2D) graphics;
        Shape clip = g.getClip();
        g.setColor(LightZoneSkin.Colors.NeutralGray);
        g.fill(clip);

        if (zones != null) {
            int dx, dy;
            AffineTransform transform = new AffineTransform();
            if (getSize().width > zones.getWidth())
                dx = (getSize().width - zones.getWidth()) / 2;
            else
                dx = 0;
            if (getSize().height > zones.getHeight())
                dy = (getSize().height - zones.getHeight()) / 2;
            else
                dy = 0;
            transform.setToTranslation(dx, dy);
            try {
                g.drawRenderedImage(zones, transform);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private int currentFocusZone = -1;

    private BufferedImage lastPreview = null;

    void setFocusedZone(int index) {
        if (currentFocusZone == index) {
            return; // No change, avoid unnecessary work
        }
        
        currentFocusZone = index;

        if (!colorMode && ADJUST_GRAYSCALE && lastPreview != null) {
            // Use cached segmented image if available
            if (cachedSegmentedImage != null) {
                zones = requantize(cachedSegmentedImage, currentFocusZone);
            } else {
                zones = requantize(lastPreview, currentFocusZone);
            }
            repaint();
        }
    }

    private RenderedImage zones;

    ZoneFinder(ImageEditorEngine engine) {
        this(engine, false);
    }

    ZoneFinder(final ImageEditorEngine engine, boolean colorMode) {
        this.engine = engine;
        this.colorMode = colorMode;

        addComponentListener(
            new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    if (isShowing()) {
                        // Clear cache on resize
                        cachedSegmentedImage = null;
                        cachedInputImage = null;
                        cachedImageHash = 0;
                        engine.update(null, false);
                    }
                }
            }
        );
    }

    private RenderedImage cropScaleGrayscale(Rectangle visibleRect, RenderedImage image) {
        Rectangle bounds = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());

        visibleRect = bounds.intersection(visibleRect);

        if (bounds.contains(visibleRect)) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add((float) visibleRect.x);
            pb.add((float) visibleRect.y);
            pb.add((float) visibleRect.width);
            pb.add((float) visibleRect.height);
            image = JAI.create("Crop", pb, JAIContext.noCacheHint);
        }

        Dimension previewSize = getSize();

         // Limit maximum preview size for performance
        int maxDim = Math.max(previewSize.width, previewSize.height);
        if (maxDim > MAX_PREVIEW_DIMENSION) {
            float scaleFactor = MAX_PREVIEW_DIMENSION / (float) maxDim;
            previewSize = new Dimension(
                (int) (previewSize.width * scaleFactor),
                (int) (previewSize.height * scaleFactor)
            );
        }

        if (visibleRect.width > previewSize.width || visibleRect.height > previewSize.height) {
            final float scale = Math.min(previewSize.width / (float) visibleRect.width, previewSize.height / (float) visibleRect.height);

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(scale);
            pb.add(scale);
            image = JAI.create("Scale", pb, JAIContext.noCacheHint);
        }

        // avoid keeping references to the input image
        if (image instanceof RenderedOp) {
            RenderedOp ropImage = (RenderedOp) image;

            SampleModel sm = ropImage.getSampleModel().createCompatibleSampleModel(image.getWidth(), image.getHeight());

            WritableRaster wr = Raster.createWritableRaster(sm, new Point(ropImage.getMinX(), ropImage.getMinY()));
            ropImage.copyData(wr);
            image = new BufferedImage(ropImage.getColorModel(), wr.createWritableTranslatedChild(0, 0), false, null);
            ropImage.dispose();
        }

        /* image = Functions.toColorSpace(image, JAIContext.sRGBColorSpace, null);

        if (((PlanarImage) image).getSampleModel().getDataType() == DataBuffer.TYPE_USHORT)
            image = Functions.fromUShortToByte(image, null); */

        if (!colorMode && image.getColorModel().getNumColorComponents() == 3) {
            ICC_Profile profile = ((ICC_ColorSpace) (image.getColorModel().getColorSpace())).getProfile();

            if (!(profile instanceof ICC_ProfileRGB)) {
                image = Functions.toColorSpace(image, JAIContext.sRGBColorSpace, null);
                profile = ((ICC_ColorSpace) (image.getColorModel().getColorSpace())).getProfile();
            }

            ICC_ProfileRGB rgb_profile = (ICC_ProfileRGB) profile;

            ColorScience.ICC_ProfileParameters pp = new ColorScience.ICC_ProfileParameters(rgb_profile);

            double[][] transform = {
                {pp.W[0], pp.W[1], pp.W[2], 0}
            };

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(transform);
            image = JAI.create("BandCombine", pb, JAIContext.noCacheHint); // Desaturate, single banded
        }

        return image;
    }

    static private final int steps = 16;

    /**
     * the same lightness scale used in the zone mapper
     */
    static private final int[] colors = new int[steps + 1];
    static {
        for (int i = 0; i < steps; i++) {
            final var color = (float) ((Math.pow(2, i * 8.0 / (steps - 1)) - 1) / 255.);
            final var srgbColor = Functions.fromLinearToCS(JAIContext.systemColorSpace, new float[] {color, color, color});
            colors[i] = (int) (255 * srgbColor[0]);
        }
        colors[steps] = colors[steps - 1];
    }

    private static int zoneFrom(int lightness) {
        for (int i = 1; i <= steps; i++) {
            if (lightness < colors[i]) {
                return i - 1;
            }
        }
        return steps;
    }

    // Cache the lookup table to avoid recreating it every time
    private static byte[][][] cachedLUTs = new byte[steps + 1][][];
    
    // requantize the segmented image to match the same lightness scale used in the zone mapper
    private static RenderedImage requantize(RenderedImage image, int focusZone) {
        byte[][] lut;
        
        // Use cached LUT if available
        if (focusZone >= 0 && focusZone < cachedLUTs.length && cachedLUTs[focusZone] != null) {
            lut = cachedLUTs[focusZone];
        } else {
            lut = new byte[3][256];
            int step = 0;
            for (int i = 0; i < colors[steps]; i++) {
                if (i > colors[step])
                    step++;
                if (i < (colors[step] + colors[step + 1]) / 2) {
                    if (focusZone >= 0 && step ==  focusZone) {
                        lut[0][i] = (byte) Color.yellow.getRed();
                        lut[1][i] = (byte) Color.yellow.getGreen();
                        lut[2][i] = (byte) Color.yellow.getBlue();
                    } else
                        lut[0][i] = lut[1][i] = lut[2][i] = (byte) (colors[step] & 0xFF);
                } else
                    lut[0][i] = lut[1][i] = lut[2][i] = (byte) (colors[step + 1] & 0xFF);
            }
            for (int i = colors[steps]; i < 256; i++) {
                lut[0][i] = lut[1][i] = lut[2][i] = (byte) colors[steps];
            }
            
            // Cache the LUT for future use
            if (focusZone >= 0 && focusZone < cachedLUTs.length) {
                cachedLUTs[focusZone] = lut;
            }
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(new LookupTableJAI(lut));

        return JAI.create("lookup", pb, JAIContext.noCacheHint);
    }

    private RenderedImage segment_bah(RenderedImage image) {
        image = Functions.fromByteToUShort(image, null);

        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                  BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(4f);
        pb.add(20f);
        RenderedOp filtered = JAI.create("BilateralFilter", pb, hints);

        filtered = Functions.fromUShortToByte(filtered, null);

        RenderedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        filtered.copyData(((BufferedImage) result).getRaster());
        lastPreview = (BufferedImage) result;

        if (!colorMode && ADJUST_GRAYSCALE)
            result = requantize(result, currentFocusZone);

        return result;
    }

    // Compute a simple hash of the image for cache comparison
    private long computeImageHash(RenderedImage image) {
        long hash = image.getWidth();
        hash = 31 * hash + image.getHeight();
        hash = 31 * hash + image.getMinX();
        hash = 31 * hash + image.getMinY();
        hash = 31 * hash + System.identityHashCode(image);
        return hash;
    }

    private RenderedImage segment(RenderedImage image) {
        // Check cache first
        long imageHash = computeImageHash(image);
        if (cachedSegmentedImage != null && cachedImageHash == imageHash && cachedInputImage == image) {
            return cachedSegmentedImage;
        }

        Rectangle bounds = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());

        byte[] pixels = ((DataBufferByte) image.getData(bounds).getDataBuffer()).getData();
        if (pixels.length != bounds.height * bounds.width * image.getSampleModel().getNumBands()) {
            pixels = (byte[]) image.getData(bounds).getDataElements(bounds.x, bounds.y, bounds.width, bounds.height, null);
        }

        if (pixels.length <= 0 || bounds.height <= 15 || bounds.width <= 15)
            return null;

        pixels = Segment.segmentImage(pixels, colorMode ? 3 : 1, bounds.height, bounds.width);

        DataBufferByte data = new DataBufferByte(pixels, pixels.length);

        WritableRaster raster;
        ColorModel colorModel;
        if (colorMode) {
            colorModel = image.getColorModel();
            raster = Raster.createInterleavedRaster(data, bounds.width, bounds.height, 3 * bounds.width, 3, new int[]{0, 1, 2}, null);
        } else {
            raster = Raster.createInterleavedRaster(data, bounds.width, bounds.height, bounds.width, 1, new int[]{0}, null);
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            colorModel = new ComponentColorModel(cs, new int[]{8}, false, true,
                                                 Transparency.OPAQUE,
                                                 DataBuffer.TYPE_BYTE);
        }

        RenderedImage result = lastPreview = new BufferedImage(colorModel, raster, false, null);

        // Cache the segmented image before requantization
        cachedSegmentedImage = result;
        cachedInputImage = image;
        cachedImageHash = imageHash;

        // requantize the segmented image to match the same lightness scale used in the zone mapper
        if (!colorMode && ADJUST_GRAYSCALE)
            result = requantize(result, currentFocusZone);

        return result;
    }

    // Improved thread management with proper queue
    class Segmenter extends Thread {
        private final BlockingQueue<SegmentRequest> requestQueue = new LinkedBlockingQueue<>(1);
        private final AtomicBoolean running = new AtomicBoolean(true);

        Segmenter(Rectangle visibleRect, PlanarImage image) {
            super("ZoneFinder Segmenter");
            setDaemon(true);
            requestQueue.offer(new SegmentRequest(visibleRect, image));
        }

        void nextView(Rectangle visibleRect, PlanarImage image) {
            // Clear old requests and add new one
            requestQueue.clear();
            requestQueue.offer(new SegmentRequest(visibleRect, image));
        }

        void shutdown() {
            running.set(false);
            interrupt();
        }

        @Override
        public void run() {
            while (running.get()) {
                try {
                    SegmentRequest request = requestQueue.poll();
                    if (request == null) {
                        Thread.sleep(50); // Brief wait if no work
                        continue;
                    }

                    if (getSize().width > 0 && getSize().height > 0) {
                        RenderedImage processedImage = cropScaleGrayscale(request.visibleRect, request.image);
                        RenderedImage newZones = segment(processedImage);
                        if (newZones != null) {
                            zones = newZones;
                            SwingUtilities.invokeLater(() -> repaint());
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class SegmentRequest {
        final Rectangle visibleRect;
        final PlanarImage image;

        SegmentRequest(Rectangle visibleRect, PlanarImage image) {
            this.visibleRect = visibleRect;
            this.image = image;
        }
    }

    private Segmenter segmenter = null;

    private void stopSegmenter() {
        if (segmenter != null) {
            segmenter.shutdown();
            segmenter = null;
        }
    }

    /*
        BIG NOTE: JAI has all sorts of deadlocks in its notification management,
        we just avoid doing any pipeline setup off the main event thread.
        This code sets the pipeline on the main thread but performs the actual computation on a worker thread
    */

    @Override
    public void paintDone(PlanarImage image, Rectangle visibleRect, boolean synchronous, long time) {
        Dimension previewDimension = getSize();

        assert (image.getColorModel().getColorSpace().isCS_sRGB()
                || image.getColorModel().getColorSpace() == JAIContext.systemColorSpace)
                && image.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;

        if (previewDimension.getHeight() > 1 && previewDimension.getWidth() > 1) {
            Operation op = engine.getSelectedOperation();
            if (op != null && op instanceof ZoneOperation /* && op.isActive() */ ) {
                PlanarImage processedImage = engine.getRendering(engine.getSelectedOperationIndex() + 1);
                image = Functions.fromUShortToByte(Functions.toColorSpace(processedImage,
                                                                          JAIContext.systemColorSpace,
                                                                          engine.getProofProfile(),
                                                                          null,
                                                                          engine.getProofIntent(),
                                                                          null),
                                                   null);

                if (image.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT)
                    image = Functions.fromUShortToByte(image, null);
            }

            if (segmenter == null || !segmenter.isAlive()) {
                segmenter = new Segmenter(visibleRect, image);
                segmenter.start();
            } else
                segmenter.nextView(visibleRect, image);
        }
    }
}
