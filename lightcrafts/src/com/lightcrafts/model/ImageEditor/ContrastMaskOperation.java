/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.model.LayerConfig;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.utils.ColorScience;

import com.lightcrafts.mediax.jai.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 30, 2005
 * Time: 9:55:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class ContrastMaskOperation extends BlendedOperation {
    private double radius = 128;
    private double gamma = 2.2;
    private KernelJAI kernel = null;
    private short[] tableDataUShort = new short[0x10000];
    private byte[] tableDataByte = new byte[0x100];
    private LookupTableJAI byteLut = null;
    private LookupTableJAI ushortLut = null;

    public ContrastMaskOperation(Rendering rendering) {
        super(rendering, type);

        DecimalFormat format = new DecimalFormat("0.0");

        addSliderKey("Radius");
        setSliderConfig("Radius", new SliderConfig(0.1, 500, radius, .1, true, format));
        addSliderKey("Gamma");
        setSliderConfig("Gamma", new SliderConfig(0.2, 5, gamma, .1, false, format));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    static final OperationType type = new OperationTypeImpl("Contrast Mask");

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals("Radius") && radius != value) {
            radius = value;
            kernel = null;
        } else if (key.equals("Gamma") && gamma != value) {
            gamma = value;
            byteLut = null;
            ushortLut = null;
        } else {
            return;
        }

        super.setSliderValue(key, value);
    }

    private LookupTableJAI computeGammaTable(int dataType) {
        if (dataType == DataBuffer.TYPE_BYTE) {
            if (byteLut != null)
                return byteLut;
            for (int i = 0; i < tableDataByte.length; i++) {
                tableDataByte[i] = (byte) (0xFF * Math.pow(i / (double) 0xFF, gamma) + 0.5);
            }
            return byteLut = new LookupTableJAI(tableDataByte);
        } else {
            if (ushortLut != null)
                return ushortLut;
            for (int i = 0; i < tableDataUShort.length; i++) {
                tableDataUShort[i] = (short) (0xFFFF * Math.pow(i / (double) 0xFFFF, gamma) + 0.5);
            }
            return ushortLut = new LookupTableJAI(tableDataUShort, true);
        }
    }

    private class ContrastMask extends BlendedTransform {
        RenderedOp gammaCurve;

        ContrastMask(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            double[][] transform = {
                { ColorScience.Wr, ColorScience.Wg, ColorScience.Wb, 0 }
            };

            // Calculate a blurred desautuated inverted version of the source as a mask
            double newRadius = radius * scale;
            double rescale = 1;
            int divideByTwo = 1;

            while (newRadius > 7) {
                newRadius /= 2;
                rescale /= 2;
                divideByTwo *= 2;
            }

            RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                      BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

            PlanarImage scaleDown;
            if (rescale != 1) {
                float scaleX = (float) Math.floor(rescale * back.getWidth()) / (float) back.getWidth();
                float scaleY = (float) Math.floor(rescale * back.getHeight()) / (float) back.getHeight();

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(back);
                pb.add(AffineTransform.getScaleInstance(scaleX, scaleY));
                pb.add(interp);
                RenderingHints layoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                            new ImageLayout(0, 0,
                                                                            Math.max(JAIContext.TILE_WIDTH/divideByTwo, 8),
                                                                            Math.max(JAIContext.TILE_HEIGHT/divideByTwo, 8),
                                                                            null, null));
                layoutHints.add(hints);
                layoutHints.add(JAIContext.noCacheHint);
                scaleDown = JAI.create("Affine", pb, layoutHints);
            } else {
                scaleDown = back;
            }

            if (scaleDown.getColorModel().getNumComponents() == 3) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(scaleDown);
                pb.add(transform);
                scaleDown = JAI.create("BandCombine", pb, JAIContext.noCacheHint);  // Desaturate, single banded
            }

            scaleDown = JAI.create("Not", scaleDown, JAIContext.noCacheHint);       // Invert
            LookupTableJAI table = computeGammaTable(scaleDown.getSampleModel().getDataType());
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(scaleDown);
            pb.add(table);
            // we cache this since convolution scans its input multiple times
            gammaCurve = JAI.create("lookup", pb, null /*JAIContext.noCacheHint*/);

            kernel = Functions.getGaussKernel(newRadius);
            pb = new ParameterBlock();
            pb.addSource(gammaCurve);
            pb.add(kernel);
            // RenderingHints convolveHints = new RenderingHints(hints);
            // convolveHints.add(JAIContext.noCacheHint);
            RenderedOp blur = JAI.create("LCSeparableConvolve", pb, hints);    // Gaussian Blur

            if (rescale != 1) {
                pb = new ParameterBlock();
                pb.addSource(blur);
                pb.add(AffineTransform.getScaleInstance(back.getWidth() / (double) scaleDown.getWidth(),
                                                        back.getHeight() / (double) scaleDown.getHeight()));
                pb.add(interp);
                RenderingHints resultLayoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                                  new ImageLayout(0, 0,
                                                                                  JAIContext.TILE_WIDTH,
                                                                                  JAIContext.TILE_HEIGHT,
                                                                                  null, null));
                resultLayoutHints.add(hints);
                resultLayoutHints.add(JAIContext.noCacheHint);
                return JAI.create("Affine", pb, resultLayoutHints);
            } else {
                return blur;
            }
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ContrastMaskOperation.ContrastMask(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }

    @Override
    public LayerConfig getDefaultLayerConfig() {
        return new LayerConfig(new LayerModeImpl("Soft Light"), .5);
    }
}
