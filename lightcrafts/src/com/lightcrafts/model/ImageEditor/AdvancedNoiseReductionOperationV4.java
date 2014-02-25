package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.BilateralFilterRGBOpImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import java.awt.*;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

/**
 * Copyright (C) 2010 Light Crafts, Inc.
 * Author: fabio
 * 12/22/10 @ 12:38 PM
 */

public class AdvancedNoiseReductionOperationV4 extends BlendedOperation {
    static final String COLOR_NOISE = "Color_Noise";
    static final String GRAIN_NOISE = "Grain_Noise";
    static final OperationType type = new OperationTypeImpl("Advanced Noise Reduction V4");
    private float chroma_domain = 2;
    private float luma_domain = 0;

    public AdvancedNoiseReductionOperationV4(Rendering rendering, OperationType type) {
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

            ColorScience.LinearTransform transform = new ColorScience.YST();

            double[][] rgb2yst = transform.fromRGB(back.getSampleModel().getDataType());
            double[][] yst2rgb = transform.toRGB(back.getSampleModel().getDataType());

            if (true) {
                BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                front = new BilateralFilterRGBOpImage(back, borderExtender, null, null,
                        luma_domain * scale, 0.02f,
                        chroma_domain * scale, 0.04f);

                /* front = new O1BilateralFilterOpImage(back, JAIContext.fileCacheHint,
                                                     luma_domain * scale, 0.04f,
                                                     chroma_domain * scale, 0.04f); */

                front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            } else {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource( back );
                pb.add( rgb2yst );
                RenderedOp ystImage = JAI.create("BandCombine", pb, null);

                RenderingHints mfHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

                if (chroma_domain != 0) {
                    pb = new ParameterBlock();
                    pb.addSource(ystImage);
                    pb.add(chroma_domain * scale);
                    // pb.add(0.02f + 0.001f * chroma_domain);
                    pb.add(0.04f);
                    ystImage = JAI.create("BilateralFilter", pb, mfHints);
                    ystImage.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                }

                if (luma_domain != 0) {
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
                    // pb.add((2 + luma_domain / 10f)* scale);
                    // pb.add(0.005f * luma_domain);
                    pb.add(luma_domain* scale);
                    pb.add(0.04f);
                    y = JAI.create("BilateralFilter", pb, mfHints);

                    RenderingHints layoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, Functions.getImageLayout(ystImage));
                    pb = new ParameterBlock();
                    pb.addSource(y);
                    pb.addSource(cc);
                    // layoutHints.add(JAIContext.noCacheHint);
                    ystImage = JAI.create("BandMerge", pb, layoutHints);
                }

                pb = new ParameterBlock();
                pb.addSource( ystImage );
                pb.add( yst2rgb );
                front = JAI.create("BandCombine", pb, null);
                front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            }
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
