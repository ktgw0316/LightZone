/* Copyright (C) 2015 Masahiro Kitagawa */
package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.NonLocalMeansFilterOpImage;
import com.lightcrafts.jai.utils.Transform;
import javax.media.jai.BorderExtender;
import javax.media.jai.PlanarImage;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import java.text.DecimalFormat;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_NOISE_REDUCTION;

public class AdvancedNoiseReductionOperationV5 extends BlendedOperation {
    private static final String COLOR_RADIUS = "Color_Radius";
    private static final String COLOR_INTENSITY = "Color_Intensity";
    private static final String GRAIN_RADIUS = "Grain_Radius";
    private static final String GRAIN_INTENSITY = "Grain_Intensity";
    static final OperationType type = new OperationTypeImpl("Advanced Noise Reduction V5");

    private int chroma_radius = 2;
    private float chroma_intensity = 3;
    private int luma_radius = 2;
    private float luma_intensity = 3;

    public AdvancedNoiseReductionOperationV5(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        setHelpTopic(HELP_TOOL_NOISE_REDUCTION);

        DecimalFormat format = new DecimalFormat("0.0");

        addSliderKey(COLOR_RADIUS);
        setSliderConfig(COLOR_RADIUS, new SliderConfig(0, 5,  chroma_radius, 1, false, format));

        addSliderKey(COLOR_INTENSITY);
        setSliderConfig(COLOR_INTENSITY, new SliderConfig(0, 10, chroma_intensity, 0.1, false, format));

        addSliderKey(GRAIN_RADIUS);
        setSliderConfig(GRAIN_RADIUS, new SliderConfig(0, 5, luma_radius, 1, false, format));

        addSliderKey(GRAIN_INTENSITY);
        setSliderConfig(GRAIN_INTENSITY, new SliderConfig(0, 10, luma_intensity, 0.1, false, format));
    }

    public boolean neutralDefault() {
        return false;
    }

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (COLOR_RADIUS.equals(key) && chroma_radius != value) {
             chroma_radius = (int) value;
        } else if (COLOR_INTENSITY.equals(key) && chroma_intensity != value) {
            chroma_intensity = (float) value;
        } else if (GRAIN_RADIUS.equals(key) && luma_radius != value) {
            luma_radius = (int) value;
        } else if (GRAIN_INTENSITY.equals(key) && luma_intensity != value) {
            luma_intensity = (float) value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class NoiseReduction extends BlendedTransform {
        NoiseReduction(PlanarImage source) {
            super(source);
        }

        public PlanarImage setFront() {
            if ( chroma_radius == 0 && chroma_intensity == 0 && luma_radius == 0 && luma_intensity == 0)
                return back;

            final int luma_patch_radius   =  (int) (luma_radius * scale);
            final int chroma_patch_radius =  (int) (chroma_radius * scale);
            final int luma_search_radius   = 2 * luma_patch_radius;
            final int chroma_search_radius = 2 * chroma_patch_radius;
            final float luma_h   = 0.01f * luma_intensity; // * scale;
            final float chroma_h = 0.01f * chroma_intensity; // * scale;

            BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_REFLECT);
            PlanarImage  front = new NonLocalMeansFilterOpImage(back, borderExtender, null, null,
                    luma_search_radius, luma_patch_radius, luma_h,
                    chroma_search_radius, chroma_patch_radius, chroma_h);
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
