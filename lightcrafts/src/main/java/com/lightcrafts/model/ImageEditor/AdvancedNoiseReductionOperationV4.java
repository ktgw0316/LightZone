package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.BilateralFilterRGBOpImage;
import com.lightcrafts.jai.utils.Transform;
import javax.media.jai.BorderExtender;
import javax.media.jai.PlanarImage;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import java.text.DecimalFormat;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_NOISE_REDUCTION;

/**
 * Copyright (C) 2010 Light Crafts, Inc.
 * Author: fabio
 * 12/22/10 @ 12:38 PM
 */

public class AdvancedNoiseReductionOperationV4 extends BlendedOperation {
    static final OperationType type = new OperationTypeImpl("Advanced Noise Reduction V4");
    private static final String COLOR_NOISE = "Color_Noise";
    private static final String GRAIN_NOISE = "Grain_Noise";
    private float chroma_domain = 2;
    private float luma_domain = 0;

    public AdvancedNoiseReductionOperationV4(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        setHelpTopic(HELP_TOOL_NOISE_REDUCTION);

        DecimalFormat format = new DecimalFormat("0.0");

        this.addSliderKey(COLOR_NOISE);
        this.setSliderConfig(COLOR_NOISE, new SliderConfig(0, 20, chroma_domain, 1, false, format));

        this.addSliderKey(GRAIN_NOISE);
        this.setSliderConfig(GRAIN_NOISE, new SliderConfig(0, 20, luma_domain, 1, false, format));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(COLOR_NOISE)) {
            chroma_domain = (float) value;
        } else if (key.equals(GRAIN_NOISE)) {
            luma_domain = (float) value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class NoiseReduction extends BlendedTransform {
        NoiseReduction(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            if (chroma_domain == 0 && luma_domain == 0)
                return back;

            BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
            PlanarImage front = new BilateralFilterRGBOpImage(back, borderExtender, null, null,
                                                              luma_domain * scale, 0.02f,
                                                              chroma_domain * scale, 0.04f);
            front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            return front;
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new NoiseReduction(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
