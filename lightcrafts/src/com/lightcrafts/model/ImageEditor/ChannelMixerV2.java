/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;

import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.utils.ColorScience;

import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.util.Map;
import java.util.Collections;
import java.text.DecimalFormat;

public class ChannelMixerV2 extends BlendedOperation implements com.lightcrafts.model.ColorPickerOperation {
    private static final String Strenght = "Strength";

    private Color color = Color.white;

    public ChannelMixerV2(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        if (type != typeV2)
            addSliderKey(Strenght);

        DecimalFormat format = new DecimalFormat("0.00");

        if (type != typeV2)
            setSliderConfig(Strenght, new SliderConfig(0, 10, strenght, .5, false, format));
    }

    private double strenght = 1;

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(Strenght) && strenght != value) {
            strenght = value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    static final OperationType typeV2 = new OperationTypeImpl("Channel Mixer V2");
    static final OperationType typeV3 = new OperationTypeImpl("Channel Mixer V3");
    static final OperationType typeV4 = new OperationTypeImpl("Channel Mixer V4");

    @Override
    public Map<String, Double> setColor(Color color) {
        this.color = color;
        settingsChanged();
        return Collections.emptyMap();
    }

    @Override
    public Color getColor() {
        return color;
    }

    private class ChannelMixerTransform extends BlendedTransform {
        ChannelMixerTransform(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            if (type == typeV4)
                return setFrontV4();
            else
                return setFrontV3();
        }

//        public PlanarImage setFrontV4a() {
//            float filter[] = {color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
//            return new FilteredGrayscaleOpImage(back, filter, (float) (Math.PI), (float) strenght, null);
//        }

        public PlanarImage setFrontV4() {
            float filter[] = {color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
            filter = JAIContext.linearColorSpace.fromRGB(filter);

            float red = 1 - filter[0];
            float green = 1 - filter[1];
            float blue = 1 - filter[2];

            double tred = ColorScience.Wr, tgreen = ColorScience.Wg, tblue = ColorScience.Wb;

            if (red != 0) {
                tred -= strenght * red/2;
                tgreen += strenght * red/4;
                tblue += strenght * red/4;
            }
            if (blue != 0) {
                tblue -= strenght * blue/2;
                tgreen += strenght * blue/4;
                tred += strenght * blue/4;
            }
            if (green != 0) {
                tgreen -= strenght * green/2;
                tblue += strenght * green/4;
                tred += strenght * green/4;
            }

            double[][] transform = new double[][] {
                { tred, tgreen, tblue, 0 },
                { tred, tgreen, tblue, 0 },
                { tred, tgreen, tblue, 0 }
            };

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add(transform);
            return JAI.create("BandCombine", pb, null);
        }

        public PlanarImage setFrontV3() {
            float red = color.getRed() / 255f;
            float green = color.getGreen() / 255f;
            float blue = color.getBlue() / 255f;

            double tred=0, tgreen=1, tblue=0;
            if (red != 0) {
                tred += strenght * red/2;
                tgreen -= strenght * red/4;
                tblue -= strenght * red/4;
            }
            if (blue != 0) {
                tblue += strenght * blue/2;
                tgreen -= strenght * blue/4;
                tred -= strenght * blue/4;
            }
            if (green != 1) {
                tgreen += strenght * green/2;
                tblue -= strenght * green/4;
                tred -= strenght * green/4;
            }

            double[][] transform = new double[][] {
                { tred, tgreen, tblue, 0 },
                { tred, tgreen, tblue, 0 },
                { tred, tgreen, tblue, 0 }
            };

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(Functions.toColorSpace(back, JAIContext.oldLinearColorSpace, null));
            pb.add(transform);
            return JAI.create("BandCombine", pb, null);
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ChannelMixerTransform(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
