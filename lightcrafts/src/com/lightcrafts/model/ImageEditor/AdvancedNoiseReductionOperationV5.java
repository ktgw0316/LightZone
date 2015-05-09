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
    static final String COLOR_RADIUS = "Color_Radius";
    static final String COLOR_INTENSITY = "Color_Intensity";
    static final String GRAIN_RADIUS = "Grain_Radius";
    static final String GRAIN_INTENSITY = "Grain_Intensity";
    static final OperationType type = new OperationTypeImpl("Advanced Noise Reduction V5");
    private float chroma_domain = 2;
    private float chroma_range = 3;
    private float luma_domain = 2;
    private float luma_range = 3;

    public AdvancedNoiseReductionOperationV5(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        DecimalFormat format = new DecimalFormat("0.0");

        this.addSliderKey(COLOR_RADIUS);
        this.setSliderConfig(COLOR_RADIUS, new SliderConfig(0, 5, chroma_domain, 1, false, format));

        this.addSliderKey(COLOR_INTENSITY);
        this.setSliderConfig(COLOR_INTENSITY, new SliderConfig(0, 10, chroma_range, 0.1, false, format));

        this.addSliderKey(GRAIN_RADIUS);
        this.setSliderConfig(GRAIN_RADIUS, new SliderConfig(0, 5, luma_domain, 1, false, format));

        this.addSliderKey(GRAIN_INTENSITY);
        this.setSliderConfig(GRAIN_INTENSITY, new SliderConfig(0, 10, luma_range, 0.1, false, format));
    }

    public boolean neutralDefault() {
        return false;
    }

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key == COLOR_RADIUS && chroma_domain != value) {
            chroma_domain = (float) value;
        } else if (key == COLOR_INTENSITY && chroma_range != value) {
            chroma_range = (float) value;
        } else if (key == GRAIN_RADIUS && luma_domain != value) {
            luma_domain = (float) value;
        } else if (key == GRAIN_INTENSITY && luma_range != value) {
            luma_range = (float) value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class NoiseReduction extends BlendedTransform {
        NoiseReduction(PlanarImage source) {
            super(source);
        }

        public PlanarImage setFront() {
            if (chroma_domain == 0 && chroma_range == 0 && luma_domain == 0 && luma_range == 0)
                return back;

            PlanarImage front = back;
            BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_REFLECT);
            front = new NonLocalMeansFilterOpImage(back, borderExtender, null, null,
                    luma_domain * scale, luma_range * scale * 0.02f,
                    chroma_domain * scale, chroma_range * scale * 0.02f);
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
