/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.Operation;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.utils.Functions;

import javax.media.jai.PlanarImage;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 7, 2005
 * Time: 7:41:47 AM
 * To change this template use File | Settings | File Templates.
 */

public class GaussianBlurOperation extends BlendedOperation {
    private final double step = 0.1;

    public GaussianBlurOperation(Rendering rendering) {
        super(rendering, type);
        addSliderKey("Radius");

        DecimalFormat format = new DecimalFormat("0.0");

        setSliderConfig("Radius", new SliderConfig(0.1, 500, radius, step, true, format));
    }

    public boolean neutralDefault() {
        return false;
    }

    static final OperationType type = new OperationTypeImpl("Gaussian Blur");

    private double radius = 5.0;

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key == "Radius" && radius != value) {
            radius = value;
        } else
            return;
        
        super.setSliderValue(key, value);
    }

    private class GaussMask extends BlendedTransform {
        Operation op;

        GaussMask(PlanarImage source, Operation op) {
            super(source);
            this.op = op;
        }

        public PlanarImage setFront() {
            return Functions.gaussianBlur(back, rendering, op, radius * scale);
        }
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new GaussMask(source, this);
    }

    public OperationType getType() {
        return type;
    }
}

