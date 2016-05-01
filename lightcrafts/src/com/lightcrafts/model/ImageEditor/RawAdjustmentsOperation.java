/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.ColorDropperOperation;
import com.lightcrafts.model.RawAdjustmentOperation;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.BilateralFilterRGBOpImage;
import com.lightcrafts.jai.opimage.HighlightRecoveryOpImage;

import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.utils.ColorScience;
import com.lightcrafts.utils.DCRaw;
import com.lightcrafts.image.types.AuxiliaryImageInfo;
import com.lightcrafts.image.types.RawImageInfo;

import java.text.DecimalFormat;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.*;

import Jama.Matrix;
import java.lang.String;

public class RawAdjustmentsOperation extends BlendedOperation implements ColorDropperOperation, RawAdjustmentOperation {
    private static final String SOURCE = "Temperature";
    private static final String TINT = "Tint";
    private static final String EXPOSURE = "Exposure";
    private static final String COLOR_NOISE = "Color_Noise";
    private static final String GRAIN_NOISE = "Grain_Noise";

    private final float originalTemperature;
    private final float daylightTemperature;

    private float temperature = 5000;
    private float tint = 0;
    private float exposure = 0;
    private float color_noise = 4;
    private float grain_noise = 0;

    private Point2D p = null;
    private boolean autoWB = false;
    private float[][] cameraRGBWB, cameraRGBCA;

    private float[] preMul;

    private float mixer(float t) {
        double p = (t - originalTemperature)/2000;
        return p > -1 && p < 1 ? (float) (Math.cos(Math.PI * p) + 1) / 2 : 0;
    }

    private float[][] cameraRGB(float t) {
        if (cameraRGBWB == null)
            return cameraRGBCA;

        float matrix[][] = new float[3][3];
        float m = mixer(t);

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                matrix[i][j] = cameraRGBCA[i][j] * (1-m) + cameraRGBWB[i][j] * m;

        return matrix;
    }

    private static final ColorScience.CAMethod caMethod = ColorScience.CAMethod.Mixed;

    static final OperationType typeV1 = new OperationTypeImpl("RAW Adjustments");
    static final OperationType typeV2 = new OperationTypeImpl("RAW Adjustments V2");

    private static final Matrix RGBtoZYX = new Matrix(ColorScience.RGBtoZYX()).transpose();
    private static final Matrix XYZtoRGB = RGBtoZYX.inverse();

    public RawAdjustmentsOperation(Rendering rendering, OperationType type) {
        super(rendering, type);

        colorInputOnly = true;

        AuxiliaryImageInfo auxInfo = rendering.getEngine().getAuxInfo();

        if (auxInfo instanceof RawImageInfo) {
            final DCRaw dcRaw = ((RawImageInfo)auxInfo).getDCRaw();

            float[] daylightMultipliers = dcRaw.getDaylightMultipliers();
            preMul = daylightMultipliers.clone();
            float[] cameraMultipliers = dcRaw.getCameraMultipliers();

            if (daylightMultipliers[0] != 0) {
                daylightTemperature = neutralTemperature(daylightMultipliers, 5000);

                System.out.println("daylightMultipliers: "
                        + daylightMultipliers[0] + ", "
                        + daylightMultipliers[1] + ", "
                        + daylightMultipliers[2]);

                System.out.println("Daylight Temperature : " + daylightTemperature);
            } else
                daylightTemperature = 5000;

            if (cameraMultipliers[0] != 0) {
                originalTemperature = temperature = neutralTemperature(cameraMultipliers, 5000);

                System.out.println("cameraMultipliers: "
                        + cameraMultipliers[0] + ", "
                        + cameraMultipliers[1] + ", "
                        + cameraMultipliers[2]);

                System.out.println("Camera Temperature: " + originalTemperature);
            } else
                originalTemperature = temperature = daylightTemperature;

            // we preserve the hilights in dcraw, here we have to make sure that
            // we clip them appropratedly together with white balance
            // double dmin = Math.min(daylightMultipliers[0], Math.min(daylightMultipliers[1], daylightMultipliers[2]));
            double dmax = Math.max(daylightMultipliers[0], Math.max(daylightMultipliers[1], daylightMultipliers[2]));

            // System.out.println("dmax: " + dmax + ", dmin: " + dmin);

            for (int c=0; c < 3; c++)
                daylightMultipliers[c] /= dmax;

            // Apply camera white balance
            if (cameraMultipliers[0] > 0) {
                float[] wb = new float[] {(cameraMultipliers[0] / (cameraMultipliers[1] * daylightMultipliers[0])),
                                          (cameraMultipliers[1] / (cameraMultipliers[1] * daylightMultipliers[1])),
                                          (cameraMultipliers[2] / (cameraMultipliers[1] * daylightMultipliers[2]))};

                System.out.println("Scaling with: " + wb[0] + ", " + wb[1] + ", " + wb[2]);
                System.out.println("Correlated Temperature: " + neutralTemperature(wb, 5000));

                cameraRGBWB = dcRaw.getCameraRGB();

                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        cameraRGBWB[j][i] *= wb[i];

                Matrix B = new Matrix(ColorScience.chromaticAdaptation(daylightTemperature, originalTemperature, caMethod));
                Matrix combo = XYZtoRGB.times(B.times(RGBtoZYX));

                cameraRGBWB = combo.inverse().times(new Matrix(cameraRGBWB)).getArrayFloat();
            }

            cameraRGBCA = dcRaw.getCameraRGB();
            dcRaw.getDaylightMultipliers();

            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    cameraRGBCA[j][i] *= dmax;
        } else {
            originalTemperature = 5000;
            daylightTemperature = 5000;
        }

        addSliderKey(EXPOSURE);
        addSliderKey(COLOR_NOISE);
        if (type == typeV2)
            addSliderKey(GRAIN_NOISE);
        addSliderKey(SOURCE);
        addSliderKey(TINT);

        DecimalFormat format = new DecimalFormat("0.00");

        setSliderConfig(EXPOSURE, new SliderConfig(-4, 4, exposure, .01, false, format));
        setSliderConfig(SOURCE, new SliderConfig(1000, 40000, temperature, 10, true, new DecimalFormat("0")));
        setSliderConfig(TINT, new SliderConfig(-20, 20, tint, 0.1, false, new DecimalFormat("0.0")));
        setSliderConfig(COLOR_NOISE, new SliderConfig(0, 20, color_noise, .01, false, format));
        if (type == typeV2)
            setSliderConfig(GRAIN_NOISE, new SliderConfig(0, 20, grain_noise, .01, false, format));
    }

    static float neutralTemperature(float rgb[], float refT) {
        float sat = Float.MAX_VALUE;
        float minT = 0;
        for (int t = 1000; t < 40000; t+= (0.01 * t)) {
            Matrix B = new Matrix(ColorScience.chromaticAdaptation(t, refT, caMethod));
            Matrix combo = XYZtoRGB.times(B.times(RGBtoZYX));

            Matrix color = new Matrix(new double[][]{{rgb[0]}, {rgb[1]}, {rgb[2]}});

            color = combo.times(color);

            double r = color.get(0, 0);
            double g = color.get(1, 0);
            double b = color.get(2, 0);

            float tSat = (float) ColorScience.saturation(r, g, b);

            if (tSat < sat) {
                sat = tSat;
                minT = t;
            }
        }
        return minT;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(SOURCE) && temperature != value) {
            temperature = (float) value;
        } else if (key.equals(TINT) && tint != value) {
            tint = (float) value;
        } else if (key.equals(EXPOSURE) && exposure != value) {
            exposure = (float) value;
        } else if (key.equals(COLOR_NOISE) && color_noise != value) {
            color_noise = (float) value;
        } else if (key.equals(GRAIN_NOISE) && grain_noise != value) {
            grain_noise = (float) value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    @Override
    public Map<String, Float> setColor(Point2D p) {
        this.p = p;
        settingsChanged();
        this.p = null;

        Map<String, Float> result = new TreeMap<String, Float>();
        result.put(SOURCE, temperature);
        result.put(TINT, tint);
        return result;
    }

    @Override
    public Map<String, Double> getAuto() {
        autoWB = true;
        settingsChanged();
        autoWB = false;

        Map<String, Double> result = new TreeMap<String, Double>();
        result.put(SOURCE, (double) temperature);
        result.put(TINT, (double) tint);
        return result;
    }

    @Override
    public Map<String, Double> getAsShot() {
        Map<String, Double> result = new TreeMap<String, Double>();
        result.put(SOURCE, (double) originalTemperature);
        result.put(TINT, (double) 0);
        return result;
    }

    int[] getPixel(PlanarImage image, int x, int y, int[] pixel) {
        Raster tile = image.getTile(image.XToTileX(x), image.YToTileY(y));
        return tile != null ? tile.getPixel(x, y, pixel) : null;
    }

    private float[] autoWhiteBalance(PlanarImage image) {
        int iheight = image.getHeight();
        int iwidth = image.getWidth();
        double dsum[] = new double[6];
        int sum[] = new int[6];
        int maximum = 0xffff;
        int black = 0;

        int pixel[] = new int[3];
        int caPixel[] = new int[3];

        for (int row = 0; row < iheight - 7; row += 8) {
            skip_block:
            for (int col = 0; col < iwidth - 7; col += 8) {
                Arrays.fill(sum, 0);
                for (int y = row; y < row + 8; y++)
                    for (int x = col; x < col + 8; x++) {
                        pixel = getPixel(image, x, y, pixel);

                        Arrays.fill(caPixel, 0);
                        for (int i = 0; i < 3; i++)
                            for (int j = 0; j < 3; j++)
                                caPixel[j] += (int) (pixel[i] * cameraRGBCA[j][i]);

                        for (int c = 0; c < 3; c++) {
                            int val = caPixel[c];
                            if (val == 0)
                                continue;
                            if (val > maximum - 25)
                                continue skip_block;
                            val -= black;
                            if (val < 0)
                                val = 0;
                            sum[c] += val;
                            sum[c + 3]++;
                        }
                    }
                for (int c = 0; c < 6; c++)
                    dsum[c] += sum[c];
            }
        }

        float pre_mul[] = new float[3];
        for (int c = 0; c < 3; c++)
            if (dsum[c] != 0)
                pre_mul[c] = (float) (dsum[c + 3] / dsum[c]);

        double dmax = Math.max(pre_mul[0], Math.max(pre_mul[1], pre_mul[2]));

        for (int c = 0; c < 3; c++)
            pre_mul[c] /= dmax;

        return pre_mul;
    }

    private class RawAdjustments extends BlendedTransform {
        RawAdjustments(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            PlanarImage front = back;

            /*** WHITE BALANCE and EXPOSURE ***/

            float lightness = 0.18f;

            if (autoWB) {
                float wb[] = autoWhiteBalance(back);
                System.out.println("Auto WB: " + wb[0] + ", " + wb[1] + ", " + wb[2]);

                temperature = neutralTemperature(wb, daylightTemperature);
                tint = 0;

                System.out.println("Correlated Temperature: " + temperature);
            } else if (p != null) {
                int pixel[] = pointToPixel(p);

                if (pixel != null) {
                    float oldTemperature = 0;

                    for (int k = 0; k < 10 && Math.abs(oldTemperature - temperature) > 0.01 * temperature; k++) {
                        oldTemperature = temperature;

                        int newPixel[] = new int[3];
                        for (int i = 0; i < 3; i++)
                            for (int j = 0; j < 3; j++)
                                newPixel[j] += (int) (pixel[i] * cameraRGB(temperature)[j][i]);

                        float n[] = WhiteBalanceV2.neutralize(newPixel, caMethod, temperature, daylightTemperature);

                        lightness = newPixel[1]/255.0f;

                        temperature = n[0];
                        tint = Math.min(Math.max(n[1], -20), 20);
                    }
                }
            }

            // Chromatic adaptation matrix
            Matrix B = new Matrix(ColorScience.chromaticAdaptation(daylightTemperature, temperature, caMethod));
            Matrix CA = XYZtoRGB.times(B.times(RGBtoZYX));

            // Normalize the CA matrix to keep exposure constant
            Matrix m = CA.times(new Matrix(new double[][]{{1},{1},{1}}));
            double max = m.get(1, 0);
            if (max != 1)
                CA = CA.times(new Matrix(new double[][]{{1/max, 0, 0},{0, 1/max, 0},{0, 0, 1/max}}));

            // The matrix taking into account the camera color space and its basic white balance and exposure
            float camMatrix[][] = CA.times(new Matrix(cameraRGB(temperature)).times(Math.pow(2, exposure))).getArrayFloat();

            front = new HighlightRecoveryOpImage(front, preMul, camMatrix, JAIContext.fileCacheHint);
            front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);

            if (tint != 0)
                front = WhiteBalanceV2.tintCast(front, tint, lightness);

            front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);

            /*** NOISE REDUCTION ***/

            if (color_noise != 0 || grain_noise != 0) {
            /* if (true) { */
                BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                front = new BilateralFilterRGBOpImage(front, borderExtender, JAIContext.fileCacheHint, null, grain_noise * scale, 0.02f, color_noise * scale, 0.04f);
                // front = new O1BilateralFilterOpImage(front, JAIContext.fileCacheHint, grain_noise * scale, 0.03f, color_noise * scale, 0.03f);
                front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            /*
            } else {
                ColorScience.LinearTransform transform = new ColorScience.YST();

                double[][] rgb2llab = transform.fromRGB(back.getSampleModel().getDataType());
                double[][] llab2rgb = transform.toRGB(back.getSampleModel().getDataType());

                ParameterBlock pb = new ParameterBlock();
                pb.addSource( front );
                pb.add( rgb2llab );
                PlanarImage ystImage = JAI.create("BandCombine", pb, null);

                RenderingHints mfHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

                pb = new ParameterBlock();
                pb.addSource(ystImage);
                pb.add(color_noise * scale);
                pb.add(0.02f + 0.001f * color_noise);
                ystImage = JAI.create("BilateralFilter", pb, mfHints);

                pb = new ParameterBlock();
                pb.addSource( ystImage );
                pb.add( llab2rgb );
                front = JAI.create("BandCombine", pb, null);
                front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            }
            */
            }

            return front;
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new RawAdjustments(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
