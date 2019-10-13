/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.utils.splines;

import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_COLOR_BALANCE;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Feb 27, 2006
 * Time: 10:22:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ColorBalanceOperation extends BlendedOperation {
    static final OperationType type = new OperationTypeImpl("Color Balance");

    private final String RED = "Cyan-Red";
    private final String GREEN = "Magenta-Green";
    private final String BLUE = "Yellow-Blue";
    private final String MIDPOINT = "Midpoint";
    private final String HILIGHTS = "Hilights";
    private final String MIDTONES = "Midtones";
    private final String SHADOWS = "Shadows";

    private double red = 0;
    private double green = 0;
    private double blue = 0;

    private double midpoint = 0.18;

    public ColorBalanceOperation(Rendering rendering) {
        super(rendering, type);
        colorInputOnly = true;

        setHelpTopic(HELP_TOOL_COLOR_BALANCE);

        DecimalFormat format = new DecimalFormat("0.0");

        addSliderKey(RED);
        addSliderKey(GREEN);
        addSliderKey(BLUE);

        setSliderConfig(RED, new SliderConfig(-10, 10, red, 0.1, false, format));
        setSliderConfig(GREEN, new SliderConfig(-10, 10, green, 0.1, false, format));
        setSliderConfig(BLUE, new SliderConfig(-10, 10, blue, 0.1, false, format));

        format = new DecimalFormat("0.00");

        addSliderKey(MIDPOINT);

        setSliderConfig(MIDPOINT, new SliderConfig(0, 1, midpoint, 0.01, false, format));
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(RED) && red != value) {
            red = value;
        } else if (key.equals(GREEN) && green != value) {
            green = value;
        } else if (key.equals(BLUE) && blue != value) {
            blue = value;
        } else if (key.equals(MIDPOINT) && midpoint != value) {
            midpoint = value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    // TODO: get the right values for hilights and shadows
    @Override
    public void setChoiceValue(String key, String value) {
        if (key.equals(MIDPOINT)) {
            if (value.equals(HILIGHTS)) {
                midpoint = 0.62;
            } else if (value.equals(MIDTONES)) {
                midpoint = 0.18;
            } else if (value.equals(SHADOWS)) {
                midpoint = 0.04;
            }
        }
        super.setChoiceValue(key, value);
    }

    private class ColorBalance extends BlendedTransform {
        ColorBalance(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            double tred = red / 2 - blue / 4 - green / 4;
            double tgreen = green / 2 - red / 4 - blue / 4;
            double tblue = blue / 2 - red / 4 - green / 4;

            double[][] polygon = {
                    {0, 0},
                    {midpoint, 0},
                    {1.0, 0},
            };

            polygon[1][1] = tred;
            double[][] redCurve = new double[256][2];
            splines.bspline(3, polygon, redCurve);

            polygon[1][1] = tgreen;
            double[][] greenCurve = new double[256][2];
            splines.bspline(3, polygon, greenCurve);

            polygon[1][1] = tblue;
            double[][] blueCurve = new double[256][2];
            splines.bspline(3, polygon, blueCurve);

            short[][] table = new short[3][0x10000];

            splines.Interpolator interpolator = new splines.Interpolator();

            for (int i = 0; i < 0x10000; i++)
                table[0][i] = (short) (0xffff & (int) Math.min(Math.max(i + 10 * 0xff * interpolator.interpolate(i / (double) 0xffff, redCurve), 0), 0xffff));

            interpolator.reset();
            for (int i = 0; i < 0x10000; i++)
                table[1][i] = (short) (0xffff & (int) Math.min(Math.max(i + 10 * 0xff * interpolator.interpolate(i / (double) 0xffff, greenCurve), 0), 0xffff));

            interpolator.reset();
            for (int i = 0; i < 0x10000; i++)
                table[2][i] = (short) (0xffff & (int) Math.min(Math.max(i + 10 * 0xff * interpolator.interpolate(i / (double) 0xffff, blueCurve), 0), 0xffff));

            LookupTableJAI lookupTable = new LookupTableJAI(table, true);

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add(lookupTable);
            return JAI.create("lookup", pb, null);
        }
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ColorBalance(source);
    }

    @Override
    public boolean neutralDefault() {
        return true;
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
