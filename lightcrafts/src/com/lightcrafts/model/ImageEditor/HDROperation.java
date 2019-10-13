/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.LayerConfig;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_RELIGHT;

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

    private final static String RADIUS = "Radius";
    private final static String GAMMA = "Gamma";
    private final static String DETAIL = "Detail";

    public HDROperation(Rendering rendering, OperationType type) {
        super(rendering, type);

        setHelpTopic(HELP_TOOL_RELIGHT);

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
        } else if (key.equals(DETAIL) && detail != value) {
            detail = value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class DesaturateInvertProcessor implements ImageProcessor {
        @Override
        public RenderedOp process(RenderedImage source) {
            final RenderedImage singleChannel = createSingleChannel(source);
            RenderedOp invert = JAI.create("Not", singleChannel, JAIContext.noCacheHint);       // Invert
            LookupTableJAI table = Functions.computeGammaTable(invert.getColorModel().getTransferType(), gamma);
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
                final RenderedImage singleChannel = createSingleChannel(back);

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(singleChannel);
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
