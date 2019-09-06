/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.color.ColorMatrix2;
import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.utils.splines;

import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Map;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_COLOR_BALANCE;

public class ColorBalanceOperationV2 extends BlendedOperation implements com.lightcrafts.model.ColorPickerDropperOperation {
    static final OperationType typeV2 = new OperationTypeImpl("Color Balance V2");
    static final OperationType typeV3 = new OperationTypeImpl("Color Balance V3");

    private final String MIDPOINT = "Midpoint";

    private double midpoint = 0.18;
    private Color color = Color.gray;
    private Point2D p = null;

    public ColorBalanceOperationV2(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        setHelpTopic(HELP_TOOL_COLOR_BALANCE);

        DecimalFormat format = new DecimalFormat("0.00");
        addSliderKey(MIDPOINT);
        setSliderConfig(MIDPOINT, new SliderConfig(0.01, 1, midpoint, 0.01, true, format));
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(MIDPOINT) && midpoint != value) {
            midpoint = value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    @Override
    public Map<String, Double> setColor(Point2D p) {
        this.p = p;
        settingsChanged();
        return Collections.singletonMap(MIDPOINT, midpoint);
    }

    @Override
    public Map<String, Double> setColor(Color color) {
        this.color = color;
        this.p = null;
        // System.out.println("setColor: " + color);
        settingsChanged();
        return Collections.emptyMap();
    }

    @Override
    public Color getColor() {
        // System.out.println("getColor: " + color);
        return color;
    }

    private static int clipColor(int color) {
        return color < 0 ? 0 : color > 255 ? 255 : color;
    }

    private class ColorBalanceV2 extends BlendedTransform {
        ColorBalanceV2(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            if (p != null || color != null) {
                int[] pixel;
                if (p != null) {
                    pixel = pointToPixel(p);
                    if (pixel != null) {
                        if (type == typeV2) {
                            int r = pixel[0] / 256;
                            int g = pixel[1] / 256;
                            int b = pixel[2] / 256;

                            float[][] matrix = {
                                    {1, 0, 0, 0},
                                    {0, 1, 0, 0},
                                    {0, 0, 1, 0},
                                    {0, 0, 0, 1},
                            };
                            ColorMatrix2.huerotatemat(matrix, 180);

                            int red = clipColor((int) (r * matrix[0][0] +
                                                       g * matrix[1][0] +
                                                       b * matrix[2][0]));
                            int green = clipColor((int) (r * matrix[0][1] +
                                                         g * matrix[1][1] +
                                                         b * matrix[2][1]));
                            int blue = clipColor((int) (r * matrix[0][2] +
                                                        g * matrix[1][2] +
                                                        b * matrix[2][2]));

                            color = new Color(red, green, blue);
                        } else {
                            // Get the complementary color
                            float[] hsb = new float[3];
                            hsb = Color.RGBtoHSB(pixel[0] / 256, pixel[1] / 256, pixel[2] / 256, hsb);
                            hsb[0] += 0.5;
                            if (hsb[0] >= 1)
                                hsb[0] -= 1;
                            color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
                        }
                        midpoint = (ColorScience.Wr * pixel[0] + ColorScience.Wg * pixel[1] + ColorScience.Wb * pixel[2]) / (double) 0xffff;
                        p = null;
                    } else {
                        System.out.println("Something funny here...");
                        return back;
                    }
                } else {
                    pixel = new int[]{color.getRed() * 256, color.getGreen() * 256, color.getBlue() * 256};
                }

                double tred = pixel[0] / 2 - pixel[2] / 4 - pixel[1] / 4;
                double tgreen = pixel[1] / 2 - pixel[0] / 4 - pixel[2] / 4;
                double tblue = pixel[2] / 2 - pixel[0] / 4 - pixel[1] / 4;

                double[][] polygon = {
                        {0, 0},
                        {midpoint, 0},
                        {1, 0}
                };

                polygon[1][1] = tred / 256.;
                double[][] redCurve = new double[256][2];
                splines.bspline(2, polygon, redCurve);

                polygon[1][1] = tgreen / 256.;
                double[][] greenCurve = new double[256][2];
                splines.bspline(2, polygon, greenCurve);

                polygon[1][1] = tblue / 256.;
                double[][] blueCurve = new double[256][2];
                splines.bspline(2, polygon, blueCurve);

                short[][] table = new short[3][0x10000];

                splines.Interpolator interpolator = new splines.Interpolator();

                for (int i = 0; i < 0x10000; i++)
                    table[0][i] = (short) (0xffff & (int) Math.min(Math.max(i + 0xff * interpolator.interpolate(i / (double) 0xffff, redCurve), 0), 0xffff));

                interpolator.reset();
                for (int i = 0; i < 0x10000; i++)
                    table[1][i] = (short) (0xffff & (int) Math.min(Math.max(i + 0xff * interpolator.interpolate(i / (double) 0xffff, greenCurve), 0), 0xffff));

                interpolator.reset();
                for (int i = 0; i < 0x10000; i++)
                    table[2][i] = (short) (0xffff & (int) Math.min(Math.max(i + 0xff * interpolator.interpolate(i / (double) 0xffff, blueCurve), 0), 0xffff));

                LookupTableJAI lookupTable = new LookupTableJAI(table, true);

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(back);
                pb.add(lookupTable);
                return JAI.create("lookup", pb, JAIContext.noCacheHint);
            } else {
                return back;
            }
        }
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ColorBalanceV2(source);
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
