/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.utils;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;
import com.lightcrafts.model.ImageEditor.ImageProcessor;
import com.lightcrafts.model.ImageEditor.Rendering;
import com.lightcrafts.model.Operation;
import com.sun.media.jai.util.ImageUtil;

import javax.media.jai.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 7, 2005
 * Time: 8:00:25 AM
 */
public class Functions {
    public static boolean DEBUG = false;

    public static LookupTableJAI computeGammaTable(int dataType, double gamma) {
        if (dataType == DataBuffer.TYPE_BYTE) {
            byte[] tableDataByte = new byte[0x100];
            for (int i = 0; i < tableDataByte.length; i++) {
                tableDataByte[i] = (byte) (0xFF * Math.pow(i / (double) 0xFF, gamma) + 0.5);
            }
            return new LookupTableJAI(tableDataByte);
        } else {
            short[] tableDataUShort = new short[0x10000];
            for (int i = 0; i < tableDataUShort.length; i++) {
                tableDataUShort[i] = (short) (0xFFFF * Math.pow(i / (double) 0xFFFF, gamma) + 0.5);
            }
            return new LookupTableJAI(tableDataUShort, true);
        }
    }

    static public RenderedOp crop(RenderedImage image, float x, float y, float width, float height, RenderingHints hints) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(x);
        pb.add(y);
        pb.add(width);
        pb.add(height);
        return JAI.create("Crop", pb, hints);
    }

    static public PlanarImage scaledRendering(Rendering rendering, Operation op, float scale, boolean cheap) {
        Rendering newRendering = rendering.clone();
        float oldScale = rendering.getScaleFactor();
        newRendering.cheapScale = cheap;
        newRendering.setScaleFactor(scale * oldScale);
        return newRendering.getRendering(rendering.indexOf(op));
    }

    static public RenderedOp gaussianBlur(RenderedImage image, Rendering rendering,
                                          Operation op, double radius) {
        return gaussianBlur(image, rendering, op, null, radius);
    }

    static public RenderedOp gaussianBlur(RenderedImage image, Rendering rendering,
                                          Operation op, ImageProcessor processor, double radius) {
        double newRadius = radius;
        float rescale = 1;

        final int size = Math.min(image.getWidth(), image.getHeight());
        final int tileSize = Math.max(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);

        if (size > tileSize) {
            while (newRadius > 32) {
                newRadius /= 2;
                rescale /= 2;
            }
        }

        RenderedImage scaleDown = (rescale != 1) ? scaledRendering(rendering, op, rescale, true) : image;
        if (processor != null) {
            scaleDown = processor.process(scaleDown);
        }

        final RenderedOp blur = fastGaussianBlur(scaleDown, newRadius);

        if (rescale != 1) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(blur);
            pb.add(AffineTransform.getScaleInstance(image.getWidth() / (double) blur.getWidth(),
                                                    image.getHeight() / (double) blur.getHeight()));
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
            RenderingHints sourceLayoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                                  new ImageLayout(0, 0,
                                                                                  JAIContext.TILE_WIDTH,
                                                                                  JAIContext.TILE_HEIGHT,
                                                                                  null, null));
            RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                    BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            sourceLayoutHints.add(extenderHints);
            // sourceLayoutHints.add(JAIContext.noCacheHint);
            return JAI.create("Affine", pb, sourceLayoutHints);
        } else {
            return blur;
        }
    }

    public static RenderedOp fastGaussianBlur(RenderedImage image, double radius) {
        // TODO: Make this fast
        RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        KernelJAI kernel = getGaussKernel(radius);
        ParameterBlock pb = new ParameterBlock()
                .addSource(image)
                .add(kernel);
        return JAI.create("LCSeparableConvolve", pb, extenderHints);
    }

    public static ImageLayout getImageLayout(RenderedImage image) {
        return getImageLayout(image.getSampleModel().getDataType(),
                              image.getColorModel().getColorSpace());
    }

    public static ImageLayout getImageLayout(RenderedImage image, int tileWidth, int tileHeight) {
        return getImageLayout(image.getSampleModel().getDataType(),
                              image.getColorModel().getColorSpace(),
                              tileWidth, tileHeight);
    }

    public static ImageLayout getImageLayout(RenderedImage image, int dataType) {
        return getImageLayout(dataType,
                              image.getColorModel().getColorSpace());
    }

    public static ImageLayout getImageLayout(int dataType, ColorSpace cs) {
        return getImageLayout(dataType, cs, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);
    }

    public static ImageLayout getImageLayout(int dataType, ColorSpace cs, int tileWidth, int tileHeight) {
        ColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE, dataType);
        return new ImageLayout(0, 0, tileWidth, tileHeight, cm.createCompatibleSampleModel(tileWidth, tileHeight), cm);
    }

    public static float[] fromLinearToCS(ColorSpace target, float[] color) {
        synchronized (ColorSpace.class) {
            return target.fromCIEXYZ(JAIContext.linearColorSpace.toCIEXYZ(color));
        }
    }

    public static int[] fromLinearToCS(ColorSpace target, int[] color) {
        float[] converted;
        synchronized (ColorSpace.class) {
            converted = target.fromCIEXYZ(JAIContext.linearColorSpace.toCIEXYZ(
                    new float[]{color[0] / 255.0f, color[1] / 255.0f, color[2] / 255.0f})
            );
        }
        return new int[] {(int) (255 * converted[0]), (int) (255 * converted[1]), (int) (255 * converted[2])};
    }

    public static double gauss(double x, double s) {
        return Math.exp(-x * x / (2 * s * s));
    }

    public static double LoG(double x, double y, double s) {
        double exp = (x * x + y * y) / (2 * s * s);
        return - Math.exp(-exp) * (1 - exp) /*/ (Math.PI * Math.pow(s, 4))*/;
    }

    public static double LoG(double x, double s) {
        double exp = (x * x) / (2 * s * s);
        return - Math.exp(-exp) * (1 - exp) /*/ (Math.PI * Math.pow(s, 4))*/;
    }

    /**
     * Generates the kernel from the current theta and kernel size.
     */
    public static float[] generateLoGKernel(double theta, int kernelSize) {
        float[] logKernel = new float[kernelSize * kernelSize];
        int k = 0;
        double scale = 0;
        for (int j = 0; j < kernelSize; ++j) {
            for (int i = 0; i < kernelSize; ++i) {
                int x = (-kernelSize / 2) + i;
                int y = (-kernelSize / 2) + j;
                double value = LoG(x, y, theta);
                scale += value;
                logKernel[k++] = (float) value;
            }
        }
        for (int i = 0; i < logKernel.length; i++)
            logKernel[i] /= scale;
        return logKernel;
    }

    static public double logScale(double value, double max) {
        assert value >= 0 && value <= 1.0;
        return Math.pow(max + 1, value) - 1;
    }

    static NumberFormat fmt = DecimalFormat.getInstance();

    static public KernelJAI LoGSharpenKernel(double radius, double gain) {
        if (radius < 0.00001)
            radius = 0.00001;

        int size = 5;

        float[] data = generateLoGKernel(radius, size);

        if (DEBUG) System.out.println("kernel data: (" + radius + ") ");
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                data[i + size * j] *= gain;
                if (i == size / 2 && j == size / 2)
                    data[i + size * j] += (1 - gain);
                if (DEBUG) System.out.print(fmt.format(data[i + size * j]) + " ");
            }
            if (DEBUG) System.out.println();
        }

        return new KernelJAI(size, size, data);
    }

    static public KernelJAI LoGSharpenKernel2(double radius, double gain) {
        if (radius < 0.00001)
            radius = 0.00001;

        int size = 5;

        float[] data = generateLoGKernel(radius, size);

        if (DEBUG) System.out.println("kernel data: (" + radius + ") ");
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == size / 2 && j == size / 2)
                    data[i + size * j] = (float) (1 + gain * (1 - data[i + size * j]));
                else
                    data[i + size * j] *= -gain;
                if (DEBUG) System.out.print(fmt.format(data[i + size * j]) + " ");
            }
            if (DEBUG) System.out.println();
        }

        return new KernelJAI(size, size, data).getRotatedKernel();
    }

    static public KernelJAI getLoGKernel(double radius) {
        // boolean DEBUG = true;

        if (radius < 0.00001)
            radius = 0.00001;

        int size = (int) (6 * radius + 0.5);
        size += 1 - size & 1;

        if (size < 3)
            size = 3;
        float[] data = new float[size];
        if (DEBUG) System.out.print("Radius: " + radius + ", kernel size: " + size + ", kernel data: ");
        float positive = 0;
        float negative = 0;
        // float scale = 0;
        for (int x = -size/2, j = 0; x <= size/2; x++, j++) {
            data[j] = (float) LoG(x, radius);
            if (data[j] > 0)
                positive += data[j];
            else
                negative += data[j];
            // scale += data[j];
            if (DEBUG) System.out.print(", " + data[j]);
        }
        if (DEBUG) System.out.println();
        for (int i = 0; i < data.length; i++) {
            if (data[i] > 0)
                data[i] *= (-negative/positive);
        }
        return new KernelJAI(size, size, size/2, size/2, data, data);
    }

    static public KernelJAI getLoGKernel(double radius, double gain) {
        // boolean DEBUG = true;

        if (radius < 0.00001)
            radius = 0.00001;

        int size = (int) (8 * radius + 0.5);
        size += 1 - size & 1;

        if (size < 3)
            size = 3;
        float[] data = new float[size];
        if (DEBUG) System.out.print("Radius: " + radius + ", kernel size: " + size + ", kernel data: ");
        float positive = 0;
        float negative = 0;
        float scale = 0;
        for (int x = -size/2, j = 0; x <= size/2; x++, j++) {
            data[j] = (float) LoG(x, radius);
            if (data[j] > 0)
                positive += data[j];
            else
                negative += data[j];
            scale += data[j];
            if (DEBUG) System.out.print(", " + data[j]);
        }
        if (DEBUG) System.out.println();
        if (false) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] > 0)
                data[i] *= -negative/positive;
            data[i] *= -gain;
            if (i == size / 2)
                data[i] += (1 + gain);
        }
        } else {
        for (int i = 0; i < data.length; i++)
            data[i] /= scale;
        }
        return new KernelJAI(size, size, size/2, size/2, data, data);
    }

    static public KernelJAI getGaussKernel(double sigma) {
        if (sigma < 0.001)
            sigma = 0.001;

        int size = 2 * (int) Math.ceil(sigma) + 1;

        float[] data = new float[size];
        int j = 0;
        float scale = 0;

        for (int x = -size/2; x <= size/2; x++) {
            data[j++] = (float) gauss(x, sigma);
            scale += data[j - 1];
        }

        for (int i = 0; i < data.length; i++)
            data[i] /= scale;

        return new KernelJAI(size, size, size/2, size/2, data, data);
    }

    static public KernelJAI getSincKernel(double sigma) {
        // boolean DEBUG = true;

        if (sigma < 0.00001)
            sigma = 0.00001;

        int size = 4 * (int) Math.round(sigma) + 1;

        if (size < 3)
            size = 3;
        float[] data = new float[size];
        if (DEBUG) System.out.print("Radius: " + sigma + ", kernel size: " + size + ", kernel data: ");
        int j = 0;
        float scale = 0;
        for (int x = -size/2; x <= size/2; x++) {
            data[j++] = x == 0 ? 1 : (float) Math.sin(x * sigma) / x;
            scale += data[j - 1];
            if (DEBUG) System.out.print(", " + data[j - 1]);
        }
        if (DEBUG) System.out.println();

        for (int i = 0; i < data.length; i++)
            data[i] /= scale;
        return new KernelJAI(size, size, size/2, size/2, data, data);
    }

    static public double lanczos2(double x) {
        if (x == 0)
            return 1;
        else if (x > -2 && x < 2)
            return Math.sin(Math.PI * x) * Math.sin(Math.PI * x / 2) / (Math.PI * Math.PI * x * x / 2);
        else
            return 0;
    }

    static public double lanczos3(double x) {
        if (x == 0)
            return 1;
        else if (x > -3 && x < 3)
            return Math.sin(Math.PI * x) * Math.sin(Math.PI * x / 3) / (Math.PI * Math.PI * x * x / 3);
        else
            return 0;
    }

    static public KernelJAI getLanczos2Kernel(int ratio) {
        /*
         * To decimate a signal we have to sample with a frequency
         * of 1/ratio inside the support of the filter function.
         *
         * The lanczos2 has a support [-2, 2] so we need 4 * ratio + 1
         * points for a zero phase filter.
         *
         */
        
        int samples = 4 * ratio + 1;
        float[] data = new float[samples];
        float sum = 0;
        for (int i = 0; i < samples; i++)
            sum += data[i] = (float) lanczos2(i / (double) ratio - 2.);
        for (int i = 0; i < samples; i++)
            data[i] /= sum;
        return new KernelJAI(samples, samples, samples/2, samples/2, data, data);
    }

    static public KernelJAI getHighPassKernel(double ratio) {
        /*
         * To decimate a signal we have to sample with a frequency
         * of 1/ratio inside the support of the filter function.
         *
         * The lanczos2 has a support [-2, 2] so we need 4 * ratio + 1
         * points for a zero phase filter.
         *
         */

        int samples = 4 * (int) (ratio+0.5) + 1;
        float[] data = new float[samples];
        float sum = 0;
        for (int i = 0; i < samples; i++)
            sum += data[i] = - (float) lanczos2(i / ratio - 2.);
        for (int i = 0; i < samples; i++)
            data[i] /= sum;
        data[samples/2] += 1;
        return new KernelJAI(samples, samples, samples/2, samples/2, data, data);
    }

    /*
        Build an ImageLayout that works well with the underlaying OS X Core Graphics engine based on RGB buffers.
        For some reason Java uses BGR buffers by default that require expensive translations at draw time on the Mac.
    */

    public static ImageLayout getDirectImageLayout(int width, int height, ColorSpace cs) {
        ImageLayout layout = new ImageLayout();
        ColorModel cm = new DirectColorModel(cs,
                                    32,
                                    0x00ff0000, // Red
                                    0x0000ff00, // Green
                                    0x000000ff, // Blue
                                    0x00000000, // Alpha
                                    false,
                                    DataBuffer.TYPE_INT);

        layout.setColorModel(cm);
        layout.setSampleModel(cm.createCompatibleSampleModel(width, height));
        layout.setTileWidth(JAIContext.TILE_WIDTH);
        layout.setTileHeight(JAIContext.TILE_HEIGHT);
        return layout;
    }

    public static RenderedImage systemColorSpaceImage(RenderedImage image) {
        ColorModel colors = image.getColorModel();
        ColorSpace space = colors.getColorSpace();
        if (space != null && !space.equals(JAIContext.systemColorSpace)) {
            image = toColorSpace(image, JAIContext.systemColorSpace, null);
        }
        return new sRGBWrapper(image);
    }

    public static class sRGBWrapper extends PlanarImage {
        final RenderedImage source;

        static ImageLayout patchColorModel(ImageLayout layout, ColorModel cm) {
            layout.setColorModel(cm);
            return layout;
        }

        public sRGBWrapper(RenderedImage source) {
            super(patchColorModel(new ImageLayout(source),
                                  new ComponentColorModel(source.getSampleModel().getNumBands() == 3
                                                          ? JAIContext.sRGBColorSpace
                                                          : source.getSampleModel().getNumBands() == 4
                                                            ? JAIContext.CMYKColorSpace
                                                            : JAIContext.gray22ColorSpace,
                                                          false, false,
                                                          Transparency.OPAQUE, DataBuffer.TYPE_BYTE)), null, null);
            this.source = source;
        }

        public Raster getTile(int tileX, int tileY) {
            return source.getTile(tileX, tileY);
        }
    }

    public static class CSWrapper extends PlanarImage {
        final RenderedImage source;

        static ImageLayout patchColorModel(ImageLayout layout, ColorModel cm) {
            layout.setColorModel(cm);
            return layout;
        }

        public CSWrapper(RenderedImage source, ColorSpace cs) {
            super(patchColorModel(new ImageLayout(source),
                                  new ComponentColorModel(cs, false, false,
                                                          Transparency.OPAQUE,
                                                          source.getColorModel().getTransferType())), null, null);
            this.source = source;
        }

        public Raster getTile(int tileX, int tileY) {
            return source.getTile(tileX, tileY);
        }
    }

    private static final ColorModel sRGBColorModel = new ComponentColorModel(
            JAIContext.sRGBColorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    public static BufferedImage toFastBufferedImage(RenderedImage image) {
        // Note: we could use opImage.getAsBufferedImage(), but images thus produced
        // are awfully inefficient for drawing which would be bad thing for thumbs
        if (!(image instanceof BufferedImage)
            || ((BufferedImage) image).getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage goodImage = new BufferedImage(image.getWidth(), image.getHeight(),
                                                        image.getSampleModel().getNumBands() == 1
                                                        ? BufferedImage.TYPE_BYTE_GRAY
                                                        : BufferedImage.TYPE_INT_RGB);
            Graphics2D big = (Graphics2D) goodImage.getGraphics();
            if (image instanceof PlanarImage) {
                PlanarImage opImage = image.getSampleModel().getNumBands() == 3
                                      ? new sRGBWrapper(image)
                                      : PlanarImage.wrapRenderedImage(image);
                big.drawRenderedImage(opImage, AffineTransform.getTranslateInstance(-image.getMinX(), -image.getMinY()));
                // opImage.copyData(goodImage.getRaster());
                opImage.dispose();
            } else if (image instanceof BufferedImage) {
                BufferedImage srgbImage = new BufferedImage(sRGBColorModel,
                                                            ((BufferedImage) image).getRaster(), false, null);
                big.drawRenderedImage(srgbImage, new AffineTransform());
                // copyData(goodImage.getRaster(), ((BufferedImage) image).getRaster());
            }
            big.dispose();
            return goodImage;
        }
        return (BufferedImage) image;
    }

    public static RenderedOp fromByteToUShort(RenderedImage source, RenderingHints hints) {
        // NOTE: Specifying the ImageLayout forces rescale to also perform the Format operation

        ComponentColorModel cm = new ComponentColorModel(source.getColorModel().getColorSpace(), false, false,
                                                         Transparency.OPAQUE, DataBuffer.TYPE_USHORT);

        RenderingHints formatHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                        new ImageLayout(0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                                        cm.createCompatibleSampleModel(source.getWidth(),
                                                                                                       source.getHeight()),
                                                                        cm));

        if (hints != null)
            formatHints.add(hints);

        final double C0 = 0;
        final double C1 = 256.0;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(new double[]{C1});
        pb.add(new double[]{C0});
        return JAI.create("Rescale", pb, formatHints);
    }

    public static RenderedOp fromShortToUShort(RenderedImage source, RenderingHints hints) {
        // NOTE: Specifying the ImageLayout forces rescale to also perform the Format operation

        ComponentColorModel cm = new ComponentColorModel(source.getColorModel().getColorSpace(), false, false,
                                                         Transparency.OPAQUE, DataBuffer.TYPE_USHORT);

        RenderingHints formatHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                        new ImageLayout(0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                                        cm.createCompatibleSampleModel(source.getWidth(),
                                                                                                       source.getHeight()),
                                                                        cm));

        if (hints != null)
            formatHints.add(hints);

        final double C0 = 0;
        final double C1 = 1;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(new double[]{C1});
        pb.add(new double[]{C0});
        return JAI.create("Rescale", pb, formatHints);
    }

    public static RenderedOp fromUShortToByte(RenderedImage source, RenderingHints hints) {
        // NOTE: Specifying the ImageLayout forces rescale to also perform the Format operation

        ComponentColorModel cm = new ComponentColorModel(source.getColorModel().getColorSpace(), false, false,
                                                         Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        RenderingHints formatHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                        new ImageLayout(0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                                        cm.createCompatibleSampleModel(source.getWidth(),
                                                                                                       source.getHeight()),
                                                                        cm));

        if (hints != null)
            formatHints.add(hints);

        final double C0 = 0;
        final double C1 = 1.0/256.0;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(new double[]{C1});
        pb.add(new double[]{C0});
        return JAI.create("Rescale", pb, formatHints);
    }

    public static PlanarImage toColorSpace(RenderedImage source, ColorSpace cs, ICC_Profile proof,
                                           LCMSColorConvertDescriptor.RenderingIntent intent,
                                           LCMSColorConvertDescriptor.RenderingIntent proofIntent,
                                           RenderingHints hints) {
        if (source.getColorModel().getColorSpace().equals(cs))
            return PlanarImage.wrapRenderedImage(source);

        // NOTE: specifying the ColorModel alone is not sufficient since
        // the new image might have a different number of components

        ColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE,
                                                source.getColorModel().getTransferType());

        RenderingHints formatHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                        new ImageLayout(0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                                        cm.createCompatibleSampleModel(source.getWidth(),
                                                                                                       source.getHeight()),
                                                                        cm));

        if (hints != null)
            formatHints.add(hints);

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(cm);
        if (intent != null)
            pb.add(intent);
        else
            pb.add(LCMSColorConvertDescriptor.PERCEPTUAL);
        if (proof != null) {
            pb.add(proof);
            if (proofIntent != null)
                pb.add(proofIntent);
        }
        return JAI.create("LCMSColorConvert", pb, formatHints);
    }

    public static PlanarImage toColorSpace(RenderedImage source, ColorSpace cs,
                                           LCMSColorConvertDescriptor.RenderingIntent intent,
                                           RenderingHints hints) {
        return toColorSpace(source, cs, null, intent, null, hints);
    }

    public static PlanarImage toColorSpace(RenderedImage source, ColorSpace cs, RenderingHints hints) {
        return toColorSpace(source, cs, null, null, null, hints);
    }

    public static PlanarImage toUShortLinear(PlanarImage image, RenderingHints hints) {
        // int numComponents = image.getColorModel().getNumComponents();

        ColorSpace linearCS = /* numComponents == 1 ?
                              JAIContext.linearGrayColorSpace : */
                              JAIContext.linearColorSpace;

        if (image.getColorModel().getColorSpace().equals(linearCS)
            && image.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT)
            return image;

        if (image.getColorModel().getColorSpace() == linearCS)
            return image;

        if (image.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE)
            return toColorSpace(fromByteToUShort(image, JAIContext.noCacheHint), linearCS, hints);
        else
            return toColorSpace(image, linearCS, hints);
    }

    public static WritableRaster copyData(WritableRaster raster, Raster source) {
        Rectangle region;               // the region to be copied
        if (raster == null) {           // copy the entire image
            region = source.getBounds();

            SampleModel sm = source.getSampleModel();
            if(sm.getWidth() != region.width ||
               sm.getHeight() != region.height) {
                sm = sm.createCompatibleSampleModel(region.width,
                                                    region.height);
            }
            raster = Raster.createWritableRaster(sm, region.getLocation());
        } else {
            region = raster.getBounds().intersection(source.getBounds());

            if (region.isEmpty()) {     // Raster is outside of image's boundary
                return raster;
            }
        }

        SampleModel[] sampleModels = { source.getSampleModel() };
        int tagID = RasterAccessor.findCompatibleTag(sampleModels,
                                                     raster.getSampleModel());

        RasterFormatTag srcTag = new RasterFormatTag(source.getSampleModel(),tagID);
        RasterFormatTag dstTag =
            new RasterFormatTag(raster.getSampleModel(),tagID);

        Rectangle subRegion = region.intersection(source.getBounds());

        RasterAccessor s = new RasterAccessor(source, subRegion,
                                              srcTag, null);
        RasterAccessor d = new RasterAccessor(raster, subRegion,
                                              dstTag, null);

        if (source.getSampleModel() instanceof ComponentSampleModel &&
            raster.getSampleModel() instanceof ComponentSampleModel) {
            ComponentSampleModel ssm = (ComponentSampleModel) source.getSampleModel();

            if (ssm.getPixelStride() == ssm.getNumBands() &&
                source.getSampleModel().getNumBands() == raster.getSampleModel().getNumBands())
                fastCopyRaster(s, d);
            else
                ImageUtil.copyRaster(s, d);
        } else
            ImageUtil.copyRaster(s, d);

        return raster;
    }

    private static void fastCopyRaster(RasterAccessor src,
                                        RasterAccessor dst) {
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth() * dstPixelStride;
        int height = dst.getHeight() * dstLineStride;

        int dataType = src.getDataType();

        final Object s, d;

        if (dataType == DataBuffer.TYPE_BYTE) {
            s = src.getByteDataArray(0);
            d = dst.getByteDataArray(0);
        } else if (dataType == DataBuffer.TYPE_SHORT ||
                   dataType == DataBuffer.TYPE_USHORT) {
            s = src.getShortDataArray(0);
            d = dst.getShortDataArray(0);
        } else if (dataType == DataBuffer.TYPE_INT) {
            s = src.getIntDataArray(0);
            d = dst.getIntDataArray(0);
        } else if (dataType == DataBuffer.TYPE_FLOAT) {
            s = src.getFloatDataArray(0);
            d = dst.getFloatDataArray(0);
        } else if (dataType == DataBuffer.TYPE_DOUBLE) {
            s = src.getDoubleDataArray(0);
            d = dst.getDoubleDataArray(0);
        } else
            throw new IllegalArgumentException();

        int srcOffset = Integer.MAX_VALUE;
        for (int offset : srcBandOffsets)
            if (offset < srcOffset)
                srcOffset = offset;
        int dstOffset = Integer.MAX_VALUE;
        for (int offset : dstBandOffsets)
            if (offset < dstOffset)
                dstOffset = offset;

        int heightEnd = dstOffset + height;

        for (int dstLineOffset = dstOffset,
             srcLineOffset = srcOffset;
             dstLineOffset < heightEnd;
             dstLineOffset += dstLineStride,
             srcLineOffset += srcLineStride) {

            System.arraycopy(s, srcLineOffset, d, dstLineOffset, width);
        }
    }
}
