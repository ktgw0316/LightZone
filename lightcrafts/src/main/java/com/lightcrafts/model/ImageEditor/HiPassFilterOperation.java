/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;

import javax.media.jai.KernelJAI;
import javax.media.jai.JAI;
import javax.media.jai.BorderExtender;
import javax.media.jai.PlanarImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 16, 2005
 * Time: 11:30:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class HiPassFilterOperation extends BlendedOperation {
    public HiPassFilterOperation(Rendering rendering) {
        super(rendering, type);
        addSliderKey("Gain");
        addSliderKey("Radius");

        DecimalFormat format = new DecimalFormat("0.000");

        setSliderConfig("Gain", new SliderConfig(0.01, 1, gain, .001, true, format));
        setSliderConfig("Radius", new SliderConfig(0.2, 5, radius, .001, true, format));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    static final OperationType type = new OperationTypeImpl("Hi Pass Filter");

    private double gain = 0.02;
    private double radius = 0.8;

    private KernelJAI kernel = null;

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals("Gain") && gain != value) {
            gain = value;
            kernel = null;
        } else if (key.equals("Radius") && radius != value) {
            radius = value;
            kernel = null;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class HiPassFilter extends BlendedTransform {
        HiPassFilter(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            kernel = Functions.LoGSharpenKernel(radius * scale, gain);
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add(kernel);
            RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                    BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            hints.add(JAIContext.noCacheHint);
            return JAI.create("convolve", pb, hints);
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new HiPassFilter(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
