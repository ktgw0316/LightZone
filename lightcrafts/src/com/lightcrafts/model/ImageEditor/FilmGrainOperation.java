package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.opimage.OpenSimplexNoise;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

import lombok.val;

/**
 * Created by Masahiro Kitagawa on 2017/01/20.
 */
public class FilmGrainOperation extends BlendedOperation {
    static final OperationType type = new OperationTypeImpl("Film Grain");

    private static final String SIZE = "GrainSize"; // TODO: l10n
    private double featureSize = 1;

    private static final String BLUR = "Blur"; // TODO: l10n
    private double blur = 0.5;

    private static final String INTENSITY = "Intensity"; // TODO: l10n
    private double intensity = 0.5;

    private static final String COLOR = "Color"; // TODO: l10n
    private double color = 0;

    public FilmGrainOperation(Rendering rendering) {
        super(rendering, type);
        colorInputOnly = true; // TODO:

        addSliderKey(SIZE);
        setSliderConfig(SIZE, new SliderConfig(0.1, 2, featureSize, 0.1, false,
                new DecimalFormat("0.0")));

        addSliderKey(BLUR);
        setSliderConfig(BLUR, new SliderConfig(0.0, 1.0, blur, 0.1, false,
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
            featureSize = value;
        }
        else if (key.equals(BLUR) && blur != value) {
            blur = value;
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
            val grain = grainImage(back.getWidth(), back.getHeight(), featureSize, intensity);

            val pb = new ParameterBlock();
            pb.addSource(back)
                    .addSource(grain)
                    .add("Hard Light");
            return JAI.create("Blend", pb, null);
        }

        private RenderedImage grainImage(int width, int height,
                                         double featureSize, double intensity) {
            val numBands = back.getNumBands();
            val rawShorts = new short[numBands * width * height];

            val noise = new OpenSimplexNoise();
            int pos = 0;
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    for (int c = 0; c < numBands; ++c) {
                        val value = noise.eval(x / featureSize, y / featureSize, (c - 1) * color);
                        val rgb = (short) ((value * intensity + 1) * 32767.5);
                        rawShorts[pos++] = rgb;
                    }
                }
            }

            val dataBuffer = new DataBufferUShort(rawShorts, rawShorts.length);
            val raster = Raster.createInterleavedRaster(
                    dataBuffer, width, height, width * numBands, numBands, new int[]{0, 1, 2}, null);
            val cm = back.getColorModel();
            val front = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

            return Functions.gaussianBlur(front, rendering, op, blur * scale);
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
