/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;
import java.util.LinkedList;

public class UnSharpMaskOperation extends BlendedOperation {
    private static final String AMOUNT = "Amount";
    private static final String RADIUS = "Radius";
    private static final String THRESHOLD = "Threshold";
    private static final String RGB = "RGB";

    static final OperationType typeV1 = new OperationTypeImpl("UnSharp Mask");
    static final OperationType typeV2 = new OperationTypeImpl("UnSharp Mask V2");
    static final OperationType typeV3 = new OperationTypeImpl("UnSharp Mask V3");

    public UnSharpMaskOperation(Rendering rendering, OperationType type) {
        super(rendering, type);

        addSliderKey(AMOUNT);
        addSliderKey(RADIUS);
        if (type != typeV1)
            addSliderKey(THRESHOLD);

        setSliderConfig(AMOUNT, new SliderConfig(1, 500, amount, 1, true, new DecimalFormat("0")));
        setSliderConfig(RADIUS, new SliderConfig(0.1, 500, radius, .1, true, new DecimalFormat("0.0")));
        if (type != typeV1)
            setSliderConfig(THRESHOLD, new SliderConfig(0, 100, threshold, 1, false, new DecimalFormat("0")));

        if (type == typeV2) {
            java.util.List<String> cks = new LinkedList<String>();
            cks.add(RGB);
            this.setCheckboxKeys(cks);
        }
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    private double amount = 100;
    private double radius = 1.0;
    private double threshold = type != typeV1 ? 20 : 0;

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(AMOUNT) && amount != value) {
            amount = value;
        } else if (key.equals(RADIUS) && radius != value) {
            radius = value;
        } else if (key.equals(THRESHOLD) && threshold != value) {
            threshold = value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private boolean rgb = false;

    @Override
    public void setCheckboxValue(String key, boolean value) {
        if (key.equals(RGB)) {
            rgb = value;
        }
        super.setCheckboxValue(key, value);
    }

    // Precondition the input signal to avoid shadows blocking

    private static double f (double x) {
        x = x * 10.0;
        return x * (1 - 1 / Math.exp(x * x / 5.0)) / 10.0;
    }

    private static short[] tableData = new short[0x10000];
    private static short[] invTableData = new short[0x10000];
    private static LookupTableJAI table, invTable;

    private static synchronized LookupTableJAI getTable() {
        if (table == null) {
            for (int i = 0; i < tableData.length; i++) {
                tableData[i] = (short) (0xFFFF & (int) (0xFFFF * f(i / (double) 0xFFFF) + 0.5));
            }
            table = new LookupTableJAI(tableData, true);
        }
        return table;
    }

    private static int binSearch(int x) {
        int low = 0;
        int high = tableData.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;

            if (x < (0xFFFF & tableData[mid]))
                high = mid - 1;
            else if (x > (0xFFFF & tableData[mid]))
                low = mid + 1;
            else
                return mid;
        }
        return low;
    }

    private static synchronized LookupTableJAI invertTable() {
        if (invTable == null) {
            getTable();
            for (int i = 0; i < invTableData.length; i++) {
                int p = binSearch(i);

                // We could interpolate, but it is not really worth it...
                invTableData[i] = (short) (p & 0xFFFF);
            }
            invTable = new LookupTableJAI(invTableData, true);
        }
        return invTable;
    }

    static class GammaUSMProcessor implements ImageProcessor {
        @Override
        public RenderedOp process(RenderedImage source) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(source);
            pb.add(invertTable());
            return JAI.create("lookup", pb, null);
        }
    }

    static class LuminanceUSMProcessor implements ImageProcessor {
        @Override
        public RenderedOp process(RenderedImage source) {
            double[][] yChannel = new double[][]{{ColorScience.Wr, ColorScience.Wg, ColorScience.Wb, 0}};

            ParameterBlock pb = new ParameterBlock();
            pb.addSource( source );
            pb.add( yChannel );
            RenderedOp y = JAI.create("BandCombine", pb, null);

            pb = new ParameterBlock();
            pb.addSource(y);
            pb.add(invertTable());
            return JAI.create("lookup", pb, null);
        }
    }

    private static GammaUSMProcessor GammaUSMProcessorInstance = new GammaUSMProcessor();
    private static LuminanceUSMProcessor LuminanceUSMProcessorInstance = new LuminanceUSMProcessor();

    private class UnSharpMask extends BlendedTransform {
        Operation op;

        UnSharpMask(PlanarImage source, Operation op) {
            super(source);
            this.op = op;
        }

        public PlanarImage setFrontPlain() {
            RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                              BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.addSource(Functions.gaussianBlur(back, rendering, op, radius * scale));
            pb.add(amount/100.0);
            pb.add((int) threshold);
            return JAI.create("LCUnSharpMask", pb, extenderHints);
        }

        @Override
        public void dispose() {
            super.dispose();
            op = null;
        }

        public PlanarImage setFrontGamma() {
            double blurRadius = radius * scale;
            RenderedOp blur = Functions.gaussianBlur(back, rendering, op, GammaUSMProcessorInstance, blurRadius);

            RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                              BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(GammaUSMProcessorInstance.process(back));
            pb.addSource(blur);
            pb.add(amount/100.0);
            pb.add((int) threshold);
            RenderedOp usm = JAI.create("LCUnSharpMask", pb, extenderHints);
            usm.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);

            pb = new ParameterBlock();
            pb.addSource(usm);
            pb.add(getTable());
            return JAI.create("lookup", pb, JAIContext.noCacheHint);
        }

        public PlanarImage setFrontLuminance() {
            ColorScience.YST yst = new ColorScience.YST();

            double[][] rgb2yst = yst.fromRGB(back.getSampleModel().getDataType());
            double[][] yst2rgb = yst.toRGB(back.getSampleModel().getDataType());

            ParameterBlock pb = new ParameterBlock();
            pb.addSource( back );
            pb.add( rgb2yst );
            RenderedOp ystImage = JAI.create("BandCombine", pb, null);

            pb = new ParameterBlock();
            pb.addSource(ystImage);
            pb.add(new int[]{1, 2});
            RenderedOp cc = JAI.create("bandselect", pb, JAIContext.noCacheHint);

            RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                              BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            pb = new ParameterBlock();
            pb.addSource(LuminanceUSMProcessorInstance.process(back));
            pb.addSource(Functions.gaussianBlur(back, rendering, op, LuminanceUSMProcessorInstance, radius * scale));
            pb.add(amount/100.0);
            pb.add((int) threshold);
            RenderedOp usm = JAI.create("LCUnSharpMask", pb, extenderHints);
            usm.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);

            pb = new ParameterBlock();
            pb.addSource(usm);
            pb.add(getTable());
            RenderedOp invLookup = JAI.create("lookup", pb, JAIContext.noCacheHint);

            RenderingHints layoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, Functions.getImageLayout(ystImage));
            pb = new ParameterBlock();
            pb.addSource(invLookup);
            pb.addSource(cc);
            layoutHints.add(JAIContext.noCacheHint);
            RenderedOp denoisedyst = JAI.create("BandMerge", pb, layoutHints);

            pb = new ParameterBlock();
            pb.addSource( denoisedyst );
            pb.add( yst2rgb );
            return JAI.create("BandCombine", pb, JAIContext.noCacheHint);
        }

        @Override
        public PlanarImage setFront() {
            if (type != typeV1 && !rgb) {
                return setFrontLuminance();
            } else
                return setFrontGamma();
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new UnSharpMask(source, this);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}

