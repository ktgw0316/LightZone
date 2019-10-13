/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.FastBilateralFilterOpImage;
import com.lightcrafts.jai.opimage.HDROpImage2;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.lang.ref.SoftReference;
import java.text.DecimalFormat;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_RELIGHT;

/**
 * Copyryght (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Apr 18, 2007
 * Time: 12:02:23 PM
 */
public class HDROperationV3 extends BlendedOperation {
    private double depth = 64;
    private double highlights = 0.2;
    private double detail = 1.5;
    private double shadows = 3.0;
    private double fuzz = 0.1;

    private final static String HIGHLIGHTS = "Highlights";
    private final static String DETAIL = "Detail";
    private final static String SHADOWS = "Shadows";
    private final static String DEPTH = "Depth";
    private final static String FUZZ = "Fuzz";

    public HDROperationV3(Rendering rendering, OperationType type) {
        super(rendering, type);

        setHelpTopic(HELP_TOOL_RELIGHT);

        DecimalFormat format = new DecimalFormat("0.00");

        addSliderKey(SHADOWS);
        setSliderConfig(SHADOWS, new SliderConfig(0, 10, shadows, .05, false, format));

        addSliderKey(HIGHLIGHTS);
        setSliderConfig(HIGHLIGHTS, new SliderConfig(0, 1, highlights, .05, false, format));

        addSliderKey(DETAIL);
        setSliderConfig(DETAIL, new SliderConfig(0, 10, detail, .05, false, format));

        addSliderKey(DEPTH);
        setSliderConfig(DEPTH, new SliderConfig(8, 64, depth, .05, false, format));

        addSliderKey(FUZZ);
        setSliderConfig(FUZZ, new SliderConfig(0.1, 1, fuzz, .05, false, format));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    static final OperationType typeV5 = new OperationTypeImpl("Tone Mapper V5");

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(FUZZ) && fuzz != value) {
            fuzz = value;
        } else if (key.equals(DEPTH) && depth != value) {
            depth = value;
        } else if (key.equals(HIGHLIGHTS) && highlights != value) {
            highlights = value;
        } else if (key.equals(DETAIL) && detail != value) {
            detail = value;
        } else if (key.equals(SHADOWS) && shadows != value) {
            shadows = value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class ToneMaperTransform extends BlendedTransform {
        ToneMaperTransform(PlanarImage source) {
            super(source);
        }

        SoftReference<PlanarImage> lastBack = new SoftReference<PlanarImage>(null);
        SoftReference<PlanarImage> mask = new SoftReference<PlanarImage>(null);

        private double last_radius = 0;
        private double last_fuzz = 0;

        @Override
        public PlanarImage setFront() {
            if (lastBack.get() != back || mask.get() == null || depth != last_radius || fuzz != last_fuzz) {
                final RenderedImage singleChannel = createSingleChannel(back);

                PlanarImage maskImage = new FastBilateralFilterOpImage(singleChannel,
                                                                       JAIContext.fileCacheHint,
                                                                       (float) (depth * scale), 0.1f);

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(maskImage);
                pb.add(new int[]{0});
                RenderedOp bfMask = JAI.create("bandselect", pb, null);

                RenderedOp blurredMask = Functions.fastGaussianBlur(bfMask, 10 * fuzz * scale);

                pb = new ParameterBlock();
                pb.addSource( maskImage );
                pb.addSource( blurredMask );
                maskImage = JAI.create("BandMerge", pb, null);

                last_radius = fuzz;
                last_fuzz = detail;

                mask = new SoftReference<PlanarImage>(maskImage);
                lastBack = new SoftReference<PlanarImage>(back);
            }

            return new HDROpImage2(back, mask.get(), shadows, highlights, detail, null);
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ToneMaperTransform(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
