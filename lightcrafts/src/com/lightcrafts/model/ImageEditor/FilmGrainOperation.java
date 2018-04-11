package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.FilmGrainOpImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import lombok.val;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

/**
 * Created by Masahiro Kitagawa on 2017/01/20.
 */
public class FilmGrainOperation extends BlendedOperation {
    static final OperationType type = new OperationTypeImpl("Film Grain");

    private static final String SIZE = "Grain_Size";
    private double featureSize = 1.0;

    private static final String SHARPNESS = "Sharpness";
    private double sharpness = 0.5;

    private static final String COLOR = "Color";
    private double color = 0.0;

    private static final String INTENSITY = "Intensity";
    private double intensity = 0.5;

    public FilmGrainOperation(Rendering rendering) {
        super(rendering, type);

        addSliderKey(SIZE);
        setSliderConfig(SIZE, new SliderConfig(1, 3, featureSize, 0.1, false,
                new DecimalFormat("0.0")));

        addSliderKey(SHARPNESS);
        setSliderConfig(SHARPNESS, new SliderConfig(0.0, 1.0, sharpness, 0.1, false,
                new DecimalFormat("0.0")));

        addSliderKey(COLOR);
        setSliderConfig(COLOR, new SliderConfig(0.0, 1.0, color, 0.1, false,
                new DecimalFormat("0.0")));

        addSliderKey(INTENSITY);
        setSliderConfig(INTENSITY, new SliderConfig(0.1, 1.0, intensity, 0.1, false,
                new DecimalFormat("0.0")));
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(SIZE) && featureSize != value) {
            // Add small number (< 0.1) to avoid disappearance of the grain
            // when zoom scale is 1:3, 1:6, etc.
            featureSize = value + 0.01;
        }
        else if (key.equals(SHARPNESS) && sharpness != value) {
            sharpness = value;
        }
        else if (key.equals(COLOR) && color != value) {
            color = value;
        }
        else if (key.equals(INTENSITY) && intensity != value) {
            intensity = value;
        }
        else {
            return;
        }
        super.setSliderValue(key, value);
    }

    private class FilmGrain extends BlendedTransform {
        Operation op;

        FilmGrain(PlanarImage source, Operation op) {
            super(source);
            this.op = op;
        }

        @Override
        public PlanarImage setFront() {
            val grain0 = new FilmGrainOpImage(back, featureSize * scale, color, intensity);
            grain0.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);

            // TODO: Do this in FilmGrainOpImage
            val grain = Functions.gaussianBlur(grain0, rendering, op, (1.0 - sharpness) * scale);

            val pb = new ParameterBlock();
            pb.addSource(back)
                    .addSource(grain)
                    .add("Hard Light");
            return JAI.create("Blend", pb, null);
        }
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new FilmGrain(source, this);
    }
}
