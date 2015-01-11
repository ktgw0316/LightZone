/* Copyright (C) 2015 Masahiro Kitagawa */
package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.NonLocalMeansFilterOpImage;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import java.text.DecimalFormat;

public class AdvancedNoiseReductionOperationV5 extends BlendedOperation {
    static final String COLOR_NOISE = "Color_Noise";
    static final String GRAIN_NOISE = "Grain_Noise";
    static final OperationType type = new OperationTypeImpl("Advanced Noise Reduction V5");
    private float chroma_domain = 2;
    private float luma_domain = 2;

    public AdvancedNoiseReductionOperationV5(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        DecimalFormat format = new DecimalFormat("0.0");

        this.addSliderKey(COLOR_NOISE);
        this.setSliderConfig(COLOR_NOISE, new SliderConfig(0, 20, chroma_domain, 1, false, format));

        this.addSliderKey(GRAIN_NOISE);
        this.setSliderConfig(GRAIN_NOISE, new SliderConfig(0, 20, luma_domain, 1, false, format));
    }

    public boolean neutralDefault() {
        return false;
    }

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key == COLOR_NOISE) {
            chroma_domain = (float) value;
        } else if (key == GRAIN_NOISE) {
            luma_domain = (float) value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class NoiseReduction extends BlendedTransform {
        NoiseReduction(PlanarImage source) {
            super(source);
        }

        public PlanarImage setFront() {
            if (chroma_domain == 0 && luma_domain == 0)
                return back;

            PlanarImage front = back;
            BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_REFLECT);
            front = new NonLocalMeansFilterOpImage(back, borderExtender, null, null,
                    luma_domain * scale, 0.02f,
                    chroma_domain * scale, 0.04f);
            front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            return front;
        }
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new NoiseReduction(source);
    }

    public OperationType getType() {
        return type;
    }
}
