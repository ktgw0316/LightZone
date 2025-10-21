/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;
import com.lightcrafts.jai.opimage.RedMaskBlackener;
import com.lightcrafts.jai.opimage.RedMaskOpImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.RedEyeOperation;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.utils.LCMS_ColorSpace;

import org.eclipse.imagen.JAI;
import org.eclipse.imagen.KernelJAI;
import org.eclipse.imagen.PlanarImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_RED_EYE;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Apr 7, 2005
 * Time: 7:41:47 AM
 */

public class RedEyesOperation extends BlendedOperation implements RedEyeOperation {
    private static final String TOLERANCE =  "Tolerance";

    public RedEyesOperation(Rendering rendering) {
        super(rendering, type);
        setHelpTopic(HELP_TOOL_RED_EYE);

        addSliderKey(TOLERANCE);

        DecimalFormat format = new DecimalFormat("0.00");

        double step = 0.01;
        setSliderConfig(TOLERANCE, new SliderConfig(0.5, 1.5, tolerance, step, false, format));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    static final OperationType type = new OperationTypeImpl("Red Eyes");

    private double tolerance = 1;

    @Override
    public EditorMode getPreferredMode() {
        return EditorMode.REGION;
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (!key.equals(TOLERANCE) || tolerance == value)
            return;

        tolerance = value;
        super.setSliderValue(key, value);
    }

    private class GaussMask extends BlendedTransform {
        Operation op;

        GaussMask(PlanarImage source, Operation op) {
            super(source);
            this.op = op;
        }

        @Override
        public PlanarImage setFront() {
            if (hasMask()) {
                PlanarImage labImage = Functions.toColorSpace(back, new LCMS_ColorSpace(new LCMS.LABProfile()),
                                                              LCMSColorConvertDescriptor.RELATIVE_COLORIMETRIC, null);

                RenderedImage redMask = new RedMaskOpImage(labImage, tolerance, null);

                KernelJAI morphKernel = new KernelJAI(3, 3, new float[] {1, 1, 1, 1, 0, 1, 1, 1, 1});
                final ParameterBlock pb = new ParameterBlock()
                        .addSource(redMask)
                        .add(morphKernel);
                redMask = Functions.fastGaussianBlur(JAI.create("dilate", pb, null), 4 * scale);

                return new RedMaskBlackener(back, redMask, null);
            } else {
                return back;
            }
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new GaussMask(source, this);
    }

    @Override
    public OperationType getType() {
        return type;
    }

    @Override
    public boolean hasFooter() {
        return false;
    }
}
