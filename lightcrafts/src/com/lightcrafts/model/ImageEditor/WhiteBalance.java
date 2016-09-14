/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.ColorScience;

import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.PlanarImage;

import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: May 31, 2005
 * Time: 7:08:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class WhiteBalance extends BlendedOperation {
    static final String ORIGINAL = "Temperature";
    // static final String TARGET = "Target";

    public WhiteBalance(Rendering rendering) {
        super(rendering, type);
        colorInputOnly = true;

        addSliderKey(ORIGINAL);
        // addSliderKey(TARGET);

        DecimalFormat format = new DecimalFormat("0");

        setSliderConfig(ORIGINAL, new SliderConfig(2000, 50000, original, 10, true, format));
        // setSliderConfig(TARGET, new SliderConfig(1000, 20000, target, true, format));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    static final OperationType type = new OperationTypeImpl("White Point");

    private float original = 6500;
    private float target = 6500;
    private float Wt[] = null;

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(ORIGINAL) && original != value) {
            original = (float) value;
            Wt = null;
        } /* else if (key == TARGET) {
            target = value;
            Wt = null;
        } */
        else
            return;

        super.setSliderValue(key, value);
    }

    static float[] W(float original, float target) {
        float[] originalW = ColorScience.W(original);
        float[] targetW = ColorScience.W(target);
        return new float[]{originalW[0] / targetW[0], originalW[1] / targetW[1], originalW[2] / targetW[2]};
    }

    private class WhiteBalanceTransform extends BlendedTransform {
        WhiteBalanceTransform(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            Wt = W(original, target);
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add(Wt);
            return JAI.create("MultiplyConst", pb, JAIContext.noCacheHint);
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new WhiteBalanceTransform(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
