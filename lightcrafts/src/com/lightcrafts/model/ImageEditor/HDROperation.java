/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.model.LayerConfig;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.model.Operation;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.utils.ColorScience;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 30, 2005
 * Time: 9:55:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class HDROperation extends BlendedOperation {
    private double radius = 250;
    private double gamma = 2.2;
    private double detail = 0.8;
    private short[] tableDataUShort = new short[0x10000];
    private byte[] tableDataByte = new byte[0x100];
    private LookupTableJAI byteLut = null;
    private LookupTableJAI ushortLut = null;

    private final static String RADIUS = "Radius";
    private final static String GAMMA = "Gamma";
    private final static String DETAIL = "Detail";

    public HDROperation(Rendering rendering, OperationType type) {
        super(rendering, type);

        DecimalFormat format = new DecimalFormat("0.00");

        addSliderKey(RADIUS);
        setSliderConfig(RADIUS, new SliderConfig(100, 500, radius, 10, true, format));
        addSliderKey(GAMMA);
        setSliderConfig(GAMMA, new SliderConfig(0.1, 10, gamma, .1, true, format));

        addSliderKey(DETAIL);
        setSliderConfig(DETAIL, new SliderConfig(0, 1, detail, .1, false, format));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    static final OperationType type = new OperationTypeImpl("Tone Mapper");

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(RADIUS) && radius != value) {
            radius = value;
        } else if (key.equals(GAMMA) && gamma != value) {
            gamma = value;
            byteLut = null;
            ushortLut = null;
        } else if (key.equals(DETAIL) && detail != value) {
            detail = value;
        } else
            return;

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

    private class DesaturateInvertProcessor implements ImageProcessor {
        @Override
        public RenderedOp process(RenderedImage source) {
            RenderedImage singleChannel;
            if (source.getColorModel().getNumComponents() == 3) {
                double[][] yChannel = new double[][]{{ColorScience.Wr, ColorScience.Wg, ColorScience.Wb, 0}};

                ParameterBlock pb = new ParameterBlock();
                pb.addSource( source );
                pb.add( yChannel );
                singleChannel = JAI.create("BandCombine", pb, null);
            } else {
                singleChannel = source;
            }

            RenderedOp invert = JAI.create("Not", singleChannel, JAIContext.noCacheHint);       // Invert
            LookupTableJAI table = computeGammaTable(invert.getColorModel().getTransferType());
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(invert);
            pb.add(table);
            // we cache this since convolution scans its input multiple times
            return JAI.create("lookup", pb, null);
        }
    }

    private DesaturateInvertProcessor desaturateInvert = new DesaturateInvertProcessor();

    private class ToneMaperTransform extends BlendedTransform {
        Operation op;

        ToneMaperTransform(PlanarImage source, Operation op) {
            super(source);
            this.op = op;
        }

        @Override
        public PlanarImage setFront() {
            // Calculate a blurred desautuated inverted version of the source as a mask
            PlanarImage front = Functions.gaussianBlur(back, rendering, op, desaturateInvert, radius * scale);

            if (detail > 0) {
                RenderedImage singleChannel;
                if (back.getColorModel().getNumComponents() == 3) {
                    double[][] yChannel = new double[][]{{ColorScience.Wr, ColorScience.Wg, ColorScience.Wb, 0}};

                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource( back );
                    pb.add( yChannel );
                    singleChannel = JAI.create("BandCombine", pb, null);
                } else {
                    singleChannel = back;
                }

                ParameterBlock pb = new ParameterBlock();
                pb.addSource( singleChannel );
                pb.add(2f * scale);
                pb.add(20f);
                RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                          BorderExtender.createInstance(BorderExtender.BORDER_COPY));
                RenderedOp bilateral = JAI.create("BilateralFilter", pb, hints);

                pb = new ParameterBlock();
                pb.addSource(bilateral);
                pb.addSource(front);
                pb.add("Overlay");
                pb.add(detail);
                front = JAI.create("Blend", pb, null);
            }

            return front;
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new HDROperation.ToneMaperTransform(source, this);
    }

    @Override
    public OperationType getType() {
        return type;
    }

    @Override
    public LayerConfig getDefaultLayerConfig() {
        return new LayerConfig(new LayerModeImpl("Soft Light"), .75);
    }
}
