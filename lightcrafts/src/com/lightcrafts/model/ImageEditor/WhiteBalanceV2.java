/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.image.types.AuxiliaryImageInfo;
import com.lightcrafts.image.types.RawImageInfo;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.ColorDropperOperation;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.utils.DCRaw;
import com.lightcrafts.utils.LCMatrix;
import com.lightcrafts.utils.splines;
import lombok.val;
import org.ejml.simple.SimpleMatrix;

import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: May 31, 2005
 * Time: 7:08:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class WhiteBalanceV2 extends BlendedOperation implements ColorDropperOperation {
    private static final String SOURCE = "Temperature";
    private final String TINT = "Tint";
    private float tint = 0;
    private Point2D p = null;

    static final OperationType typeV2 = new OperationTypeImpl("White Point V2");
    static final OperationType typeV3 = new OperationTypeImpl("White Point V3");

    private float source = 5000;
    private float REF_T = 5000;
    private ColorScience.CAMethod caMethod = ColorScience.CAMethod.Bradford;

    public WhiteBalanceV2(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        caMethod = ColorScience.CAMethod.Mixed;

        AuxiliaryImageInfo auxInfo = rendering.getEngine().getAuxInfo();

        if (false && auxInfo instanceof RawImageInfo) {
            final DCRaw dcRaw = ((RawImageInfo)auxInfo).getDCRaw();

            float[] daylightMultipliers = dcRaw.getDaylightMultipliers();
            float[] cameraMultipliers = dcRaw.getCameraMultipliers();

            System.out.println("daylightMultipliers: " + daylightMultipliers[0] + ", " + daylightMultipliers[1] + ", " + daylightMultipliers[2]);
            System.out.println("cameraMultipliers: " + cameraMultipliers[0] + ", " + cameraMultipliers[1] + ", " + cameraMultipliers[2]);

            float[][] RGBToXYZMat = ColorScience.RGBToXYZMat;

            if (false && daylightMultipliers[0] != 0) {
                float max = Math.max(daylightMultipliers[0], Math.max(daylightMultipliers[1], daylightMultipliers[2]));

                float[] xyz_dt = new float[3];
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        xyz_dt[i] += RGBToXYZMat[j][i] * daylightMultipliers[i]/max;

                float x = xyz_dt[0]/(xyz_dt[0]+xyz_dt[1]+xyz_dt[2]);

                REF_T = ColorScience.findTemperature(daylightMultipliers, 5000, caMethod);

                System.out.println("Daylight Temperature a (" + x + ") : " + ColorScience.CCTX(x) + ", " + REF_T);

                float[] rgb = new float[3];

                for (int i = 0; i < 3; i++)
                    rgb[i] = 0.18f * daylightMultipliers[i] / max;

                float[] wb = ColorScience.neutralTemperature(rgb, 5000, caMethod);
                REF_T = wb[0];

                System.out.println("Daylight Temperature b (" + x + ") : " + ColorScience.CCTX(x) + ", " + REF_T + ", tint: " + wb[1]);

                float[] inverseMultipliers = new float[] {1/daylightMultipliers[0], 1/daylightMultipliers[1], 1/daylightMultipliers[2]};
                float invMax = Math.max(inverseMultipliers[0], Math.max(inverseMultipliers[1], inverseMultipliers[2]));
                for (int i = 0; i < 3; i++)
                    rgb[i] = 0.18f * inverseMultipliers[i] / invMax;

                int[] rgbi = new int[] {(int) (0xffff * rgb[0]), (int) (0xffff * rgb[1]), (int) (0xffff * rgb[2])};

                wb = neutralize(rgbi, caMethod, 5000, 5000);
                source = wb[0];
                tint = 0; // wb[1];

                System.out.println("Daylight Temperature c (" + x + ") : " + ColorScience.CCTX(x) + ", " + source + ", tint: " + wb[1]);

            }

            if (cameraMultipliers[0] != 0) {
                float max = Math.max(cameraMultipliers[0], Math.max(cameraMultipliers[1], cameraMultipliers[2]));

                float[] xyz_ct = new float[3];
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        xyz_ct[i] += RGBToXYZMat[j][i] * cameraMultipliers[i]/max;

                float x = xyz_ct[0]/(xyz_ct[0]+xyz_ct[1]+xyz_ct[2]);

                float[] rgb = new float[3];

                for (int i = 0; i < 3; i++)
                    rgb[i] = 0.18f * cameraMultipliers[i] / max;

                float[] wb = ColorScience.neutralTemperature(rgb, REF_T, caMethod);
                source = wb[0];
                tint = 0.18f * 256 * wb[1];

                System.out.println("Camera Temperature a (" + x + ") : " + ColorScience.CCTX(x) + ", " + source + ", tint: " + tint);

                float[] inverseMultipliers = new float[] {1/cameraMultipliers[0], 1/cameraMultipliers[1], 1/cameraMultipliers[2]};
                float invMax = Math.max(inverseMultipliers[0], Math.max(inverseMultipliers[1], inverseMultipliers[2]));
                for (int i = 0; i < 3; i++)
                    rgb[i] = 0.18f * inverseMultipliers[i] / invMax;

                int[] rgbi = new int[] {(int) (0xffff * rgb[0]), (int) (0xffff * rgb[1]), (int) (0xffff * rgb[2])};

                wb = neutralize(rgbi, caMethod, 5000, REF_T);
                source = wb[0];
                tint = 0; // wb[1];

                System.out.println("Camera Temperature b (" + x + ") : " + ColorScience.CCTX(x) + ", " + source + ", tint: " + wb[1]);
            }

            double dmax = Math.max(daylightMultipliers[0], Math.max(daylightMultipliers[1], daylightMultipliers[2]));

            for (int c=0; c < 3; c++)
                daylightMultipliers[c] /= dmax;

            /* float[] wb = new float[] {(cameraMultipliers[0] / (cameraMultipliers[1] * daylightMultipliers[0])),
                                      (cameraMultipliers[1] / (cameraMultipliers[1] * daylightMultipliers[1])),
                                      (cameraMultipliers[2] / (cameraMultipliers[1] * daylightMultipliers[2]))};

            System.out.println("wb: " + wb[0] + ", " + wb[1] + ", " + wb[2]);

            for (int i = 0; i < 3; i++)
                wb[i] = 1/wb[i];

            System.out.println("inverse wb: " + wb[0] + ", " + wb[1] + ", " + wb[2]);

            float n[] = neutralize(new int[] {(int) (128 * 256 * wb[0]),
                                              (int) (128 * 256 * wb[1]),
                                              (int) (128 * 256 * wb[2])}, caMethod, source, REF_T);
            if (n != null) {
                source = n[0];
                // tint = Math.min(Math.max(n[1], -20), 20);
            } */
        }

        addSliderKey(SOURCE);
        addSliderKey(TINT);

        setSliderConfig(SOURCE, new SliderConfig(1000, 40000, source, 10, true, new DecimalFormat("0")));
        setSliderConfig(TINT, new SliderConfig(-20, 20, tint, 0.1, false, new DecimalFormat("0.0")));
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(SOURCE) && source != value) {
            source = (float) value;
        } else if (key.equals(TINT) && tint != value) {
            tint = (float) value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    static private float[] W(float original, float target) {
        float[] originalW = ColorScience.W(original);
        float[] targetW = ColorScience.W(target);
        return new float[]{originalW[0] / targetW[0], originalW[1] / targetW[1], originalW[2] / targetW[2]};
    }

    @Override
    public Map<String, Float> setColor(Point2D p) {
        this.p = p;
        settingsChanged();
        this.p = null;

        Map<String, Float> result = new TreeMap<String, Float>();
        result.put(SOURCE, source);
        result.put(TINT, tint);
        return result;
    }

    private static SimpleMatrix RGBtoZYX = new LCMatrix(ColorScience.RGBtoZYX()).transpose();
    private static SimpleMatrix XYZtoRGB = RGBtoZYX.invert();

    static float[] neutralize(int[] pixel, ColorScience.CAMethod caMethod, float source, float REF_T) {
        double r = pixel[0];
        double g = pixel[1];
        double b = pixel[2];
        double sat = ColorScience.saturation(r, g, b);
        int minT = (int) source;
        double wbr = 0, wbg = 0, wbb = 0;

        for (int t = 1000; t < 40000; t+= 0.001 * t) {
            val B = new LCMatrix(ColorScience.chromaticAdaptation(REF_T, t, caMethod));
            val combo = XYZtoRGB.mult(B.mult(RGBtoZYX));

            SimpleMatrix color = new LCMatrix(new float[][]{{pixel[0]}, {pixel[1]}, {pixel[2]}});
            color = combo.mult(color);

            r = color.get(0, 0);
            g = color.get(1, 0);
            b = color.get(2, 0);

            val tSat = ColorScience.saturation(r, g, b);

            if (tSat < sat) {
                sat = tSat;
                minT = t;
                wbr = r / 256;
                wbg = g / 256;
                wbb = b / 256;
            }
        }

        if (wbr != 0 || wbg != 0 || wbb != 0) {
            System.out.println("wb: " + wbr + ", " + wbg + ", " + wbb + ", sat: " + sat);
            return new float[] {minT, (float) (- (wbg - (wbr + wbb) / 2))};
        } else
            return new float[] {REF_T, 0};
    }

    static public PlanarImage whiteBalance(RenderedImage image, float source,
                                           float REF_T, float tint, float lightness,
                                           ColorScience.CAMethod caMethod) {
        return whiteBalance(image, source, REF_T, tint, lightness, 1, null, caMethod);
    }

    static public float[][] whiteBalanceMatrix(float source, float REF_T, float mult, float[][] cameraRGB, ColorScience.CAMethod caMethod) {
        val B = new LCMatrix(ColorScience.chromaticAdaptation(REF_T, source, caMethod));
        SimpleMatrix combo = XYZtoRGB.mult(B.mult(RGBtoZYX));

        val m = combo.mult(new LCMatrix(new float[][]{{1},{1},{1}}));

        val max = (float) m.get(1, 0); // Math.max(m.get(1, 0), Math.max(m.get(1, 0), m.get(2, 0)));
        if (max != 1)
            combo = combo.mult(new LCMatrix(new float[][]{{1/max, 0, 0},{0, 1/max, 0},{0, 0, 1/max}}));

        if (cameraRGB != null)
            combo = combo.mult(new LCMatrix(cameraRGB));

        if (mult != 1)
            combo = combo.scale(mult);

        return LCMatrix.getArrayFloat(combo);
    }

    static public PlanarImage tintCast(PlanarImage image, float tint, float lightness) {
        if (tint != 0) {
            double tred = - tint / 4;
            double tgreen = tint / 2;
            double tblue = - tint / 4;

            double[][] polygon = {
                    {0, 0},
                    {lightness, 0},
                    {1, 0}
            };

            polygon[1][1] = tred;
            double[][] redCurve = new double[256][2];
            splines.bspline(2, polygon, redCurve);

            polygon[1][1] = tgreen;
            double[][] greenCurve = new double[256][2];
            splines.bspline(2, polygon, greenCurve);

            polygon[1][1] = tblue;
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
            pb.addSource(image);
            pb.add(lookupTable);
            return JAI.create("lookup", pb, null);
        } else
            return image;
    }

    static public PlanarImage whiteBalance(RenderedImage image, float source, float REF_T,
                                           float tint, float lightness, float mult, float[][] cameraRGB,
                                           ColorScience.CAMethod caMethod) {
        float[][] b = whiteBalanceMatrix(source, REF_T, mult, cameraRGB, caMethod);

        double[][] t = new double[3][4]; // for BC, last column si going to be zero
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                t[i][j] = b[i][j];

        RenderedOp cargb = JAI.create("BandCombine", image, t, null);

        if (tint != 0)
            return tintCast(cargb, tint, lightness);
        else
            return cargb;
    }

    private class WhiteBalanceTransform extends BlendedTransform {
        WhiteBalanceTransform(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            float lightness = 0.18f;

            if (p != null) {
                int[] pixel = pointToPixel(p);

                if (pixel != null) {
                    float[] n = neutralize(pixel, caMethod, source, REF_T);
                    if (n != null) {
                        lightness = pixel[1]/255.0f;

                        source = n[0];
                        tint = Math.min(Math.max(n[1], -20), 20);
                    }
                }
            }

            return whiteBalance(back, source, REF_T, tint, lightness, caMethod);
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new WhiteBalanceTransform(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
