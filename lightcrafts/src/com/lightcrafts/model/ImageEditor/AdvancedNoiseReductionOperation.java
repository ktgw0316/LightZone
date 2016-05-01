/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.utils.ColorScience;

import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.text.DecimalFormat;

public class AdvancedNoiseReductionOperation extends BlendedOperation {
    private static final String COLOR_RADIUS = "Color_Radius";
    private static final String COLOR_INTENSITY = "Color_Intensity";
    private static final String GRAIN_RADIUS = "Grain_Radius";
    private static final String GRAIN_INTENSITY = "Grain_Intensity";
    private static final String COLOR_NOISE = "Color_Noise";
    private static final String GRAIN_NOISE = "Grain_Noise";
    static final OperationType typeV1 = new OperationTypeImpl("Advanced Noise Reduction");
    static final OperationType typeV2 = new OperationTypeImpl("Advanced Noise Reduction V2");
    static final OperationType typeV3 = new OperationTypeImpl("Advanced Noise Reduction V3");
    private float chroma_domain = 2;
    private float chroma_range = 4;
    private float luma_domain = 3;
    private float luma_range = 0;

    public AdvancedNoiseReductionOperation(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        DecimalFormat format = new DecimalFormat("0.0");

        if (type == typeV3) {
            this.addSliderKey(COLOR_NOISE);
            this.setSliderConfig(COLOR_NOISE, new SliderConfig(0, 20, chroma_domain, 1, false, format));

            this.addSliderKey(GRAIN_NOISE);
            this.setSliderConfig(GRAIN_NOISE, new SliderConfig(0, 20, luma_range, 1, false, format));
        } else {
            this.addSliderKey(COLOR_RADIUS);
            this.setSliderConfig(COLOR_RADIUS, new SliderConfig(0, 10, chroma_domain, 1, false, format));

            this.addSliderKey(COLOR_INTENSITY);
            this.setSliderConfig(COLOR_INTENSITY, new SliderConfig(0, 20, chroma_range, 1, false, format));

            if (type == typeV1) {
                this.addSliderKey(GRAIN_RADIUS);
                this.setSliderConfig(GRAIN_RADIUS, new SliderConfig(0, 10, luma_domain, 0.1, false, format));
            }

            this.addSliderKey(GRAIN_INTENSITY);
            this.setSliderConfig(GRAIN_INTENSITY, new SliderConfig(0, 10, luma_range, 0.1, false, format));
        }
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(COLOR_NOISE) && chroma_domain != value) {
            chroma_domain = (float) value;
            chroma_range = (float) (2 * value);
        } else if (key.equals(GRAIN_NOISE) && luma_range != value) {
            luma_range = (float) value;
            luma_domain = (float) (value / 2);
        } else if (key.equals(COLOR_RADIUS) && chroma_domain != value) {
            chroma_domain = (float) value;
        } else if (key.equals(COLOR_INTENSITY) && chroma_range != value) {
            chroma_range = (float) value;
        } else if (key.equals(GRAIN_RADIUS) && luma_domain != value) {
            luma_domain = (float) value;
        } else if (key.equals(GRAIN_INTENSITY) && luma_range != value) {
            luma_range = (float) value;
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
            if (chroma_domain == 0 && chroma_range == 0 && luma_domain == 0 && luma_range == 0)
                return back;

            ColorScience.LinearTransform transform = new ColorScience.YST();

            double[][] rgb2yst = transform.fromRGB(back.getSampleModel().getDataType());
            double[][] yst2rgb = transform.toRGB(back.getSampleModel().getDataType());

            ParameterBlock pb = new ParameterBlock();
            pb.addSource( back );
            pb.add( rgb2yst );
            RenderedOp ystImage = JAI.create("BandCombine", pb, null);

            RenderingHints mfHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

            if (chroma_domain != 0 && chroma_range != 0) {
                pb = new ParameterBlock();
                pb.addSource(ystImage);
                pb.add(chroma_domain * scale);
                pb.add(0.02f + 0.001f * chroma_domain);
                // pb.add(0.1f);
                ystImage = JAI.create("BilateralFilter", pb, mfHints);
                ystImage.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            }

            if (luma_domain != 0 && luma_range != 0) {
                pb = new ParameterBlock();
                pb.addSource(ystImage);
                pb.add(new int[]{0});
                RenderedOp y = JAI.create("bandselect", pb, null);

                pb = new ParameterBlock();
                pb.addSource(ystImage);
                pb.add(new int[]{1, 2});
                RenderedOp cc = JAI.create("bandselect", pb, JAIContext.noCacheHint);

                pb = new ParameterBlock();
                pb.addSource( y );
                pb.add((2 + luma_domain / 10f)* scale);
                pb.add(0.005f * luma_domain);
                y = JAI.create("BilateralFilter", pb, mfHints);

                RenderingHints layoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, Functions.getImageLayout(ystImage));
                pb = new ParameterBlock();
                pb.addSource(y);
                pb.addSource(cc);
                layoutHints.add(JAIContext.noCacheHint);
                ystImage = JAI.create("BandMerge", pb, layoutHints);
            }

            pb = new ParameterBlock();
            pb.addSource( ystImage );
            pb.add( yst2rgb );
            PlanarImage front = JAI.create("BandCombine", pb, null);
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
