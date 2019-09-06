package com.lightcrafts.image.color;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.LCMatrix;

import lombok.val;
import org.ejml.simple.SimpleMatrix;

import java.awt.color.ICC_ProfileRGB;
import java.awt.image.DataBuffer;

public class ColorScience {
    private static final float[][] rgbXYZ;
    private static final float[] wtptXYZ;
    private static final float whitePointTemperature;

    public static final float Wr;
    public static final float Wg;
    public static final float Wb;
    public static final float[] W;

    static final float[][] Cxy;

    private static final float[][] XYZToRGBMat;
    public static final float[][] RGBToXYZMat;

    public static float[] XYZ2xy(float[] XYZ) {
        return new float[] {
            XYZ[0] / (XYZ[0] + XYZ[1] + XYZ[2]),
            XYZ[1] / (XYZ[0] + XYZ[1] + XYZ[2])
        };
    }

    public static float[] xy2XYZ(float[] xy) {
        return new float[] {
            xy[0]/xy[1],
            1,
            (1 - xy[0] - xy[1]) / xy[1]
        };
    }

    public static float[] XYZ2xyY(float[] XYZ) {
        return new float[] {
            XYZ[0] / (XYZ[0] + XYZ[1] + XYZ[2]),
            XYZ[1] / (XYZ[0] + XYZ[1] + XYZ[2]),
            XYZ[1]
        };
    }

    public static float[] xyY2XYZ(float[] xyY) {
        return new float[] {
            xyY[2] * xyY[0]/xyY[1],
            xyY[2],
            xyY[2] * (1 - xyY[0] - xyY[1]) / xyY[1]
        };
    }

    static {
        ICC_ProfileParameters pp = new ICC_ProfileParameters((ICC_ProfileRGB) JAIContext.linearProfile);

        rgbXYZ = pp.rgbXYZ;
        wtptXYZ = pp.wtptXYZ;
        Cxy = pp.Cxy;
        whitePointTemperature = pp.whitePointTemperature;
        W = pp.W;

        Wr = pp.W[0];
        Wg = pp.W[1];
        Wb = pp.W[2];

        RGBToXYZMat = pp.RGBToXYZMat;
        XYZToRGBMat = pp.XYZToRGBMat;
    }

    // TODO: we have to finish cleaning up this.

    public static class ICC_ProfileParameters {
        public float[][] rgbXYZ = new float[3][3];
        public float[] wtptXYZ;
        public float[][] Cxy;
        public float whitePointTemperature;
        public float[] W;
        public float[][] RGBToXYZMat;
        public float[][] XYZToRGBMat;

        public ICC_ProfileParameters(ICC_ProfileRGB profile) {
            // Extract the rgbXYZ from the current linear color profile
            float[][] rgbXYZt = (profile).getMatrix();
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    rgbXYZ[i][j] = rgbXYZt[j][i];

            // Same for the white point
            wtptXYZ = profile.getMediaWhitePoint();

            // compute the xy coordinates of the workspace primaries form XYZ
            float Cr = rgbXYZ[0][0] + rgbXYZ[0][1] + rgbXYZ[0][2];
            float Cg = rgbXYZ[1][0] + rgbXYZ[1][1] + rgbXYZ[1][2];
            float Cb = rgbXYZ[2][0] + rgbXYZ[2][1] + rgbXYZ[2][2];

            Cxy = new float[][]{
                {rgbXYZ[0][0] / Cr, rgbXYZ[0][1] / Cr},
                {rgbXYZ[1][0] / Cg, rgbXYZ[1][1] / Cg},
                {rgbXYZ[2][0] / Cb, rgbXYZ[2][1] / Cb}
            };

            float[] Zr = new float[]{Cxy[0][0], Cxy[0][1], 1 - Cxy[0][0] - Cxy[0][1]};
            float[] Zg = new float[]{Cxy[1][0], Cxy[1][1], 1 - Cxy[1][0] - Cxy[1][1]};
            float[] Zb = new float[]{Cxy[2][0], Cxy[2][1], 1 - Cxy[2][0] - Cxy[2][1]};

            // wtptXYZ -> wtptxy
            final float[] wtptxy = XYZ2xy(wtptXYZ);
            whitePointTemperature = CCTX(wtptxy[0]);
            W = W(whitePointTemperature, Cxy);

            RGBToXYZMat = LCMatrix.getArrayFloat(
                    new LCMatrix (new float[][] {
                            mul(W[0] / Zr[1], Zr),
                            mul(W[1] / Zg[1], Zg),
                            mul(W[2] / Zb[1], Zb)
                    }).transpose()
            );

            XYZToRGBMat = LCMatrix.getArrayFloat(
                    new LCMatrix(RGBToXYZMat).invert()
            );
        }
    }

    // Return x,y coordinates of the CIE D illuminant specified by t, good between 4000K and 25000K
    /* static float[] D(float t) {
        double x;
        if (t <= 7000)
            x = -4.6070E9 * Math.pow(t, -3.) + 2.9678E6 * Math.pow(t, -2.) + 0.09911E3 * Math.pow(t, -1.) + 0.244063;
        else
            x = -2.0064E9 * Math.pow(t, -3.) + 1.9018E6 * Math.pow(t, -2.) + 0.24748E3 * Math.pow(t, -1.) + 0.237040;
        double y = -3.000 * Math.pow(x, 2.) + 2.870 * x - 0.275;

        return new float[]{(float) x, (float) y};
    } */

    // Return x,y coordinates of the CIE D illuminant specified by t, good between 1000K and 40000K
    static float[] D(float t) {
        return BlackBody.xy(t);
    }

    public static float CCTX(float x) {
        return BlackBody.t(x);
    }

    public static LCMatrix RGBtoZYX() {
        val XYZRGB = new LCMatrix(rgbXYZ);
        val S = LCMatrix.getArrayFloat(new LCMatrix(1, 3, wtptXYZ).mult(XYZRGB.invert()));

        return new LCMatrix(new float[][] {
                {S[0][0] * rgbXYZ[0][0], S[0][0] * rgbXYZ[0][1], S[0][0] * rgbXYZ[0][2]},
                {S[0][1] * rgbXYZ[1][0], S[0][1] * rgbXYZ[1][1], S[0][1] * rgbXYZ[1][2]},
                {S[0][2] * rgbXYZ[2][0], S[0][2] * rgbXYZ[2][1], S[0][2] * rgbXYZ[2][2]}
        });
    }

    static float[] mul(float c, float[] v) {
        float[] r = new float[v.length];
        for (int i = 0; i < v.length; i++)
            r[i] = c * v[i];
        return r;
    }

    public static float[] W(float T) {
        return W(T, Cxy);
    }

    public static float[] W(float T, float[][] Cxy) {
        float[] DT = D(T);

        // Compute z-coordinates
        float[] R = {Cxy[0][0], Cxy[0][1], 1 - Cxy[0][0] - Cxy[0][1]};
        float[] G = {Cxy[1][0], Cxy[1][1], 1 - Cxy[1][0] - Cxy[1][1]};
        float[] B = {Cxy[2][0], Cxy[2][1], 1 - Cxy[2][0] - Cxy[2][1]};
        float[] W = {DT[0], DT[1], 1 - DT[0] - DT[1]};

        // Compute luminance weights for the primaries using the white point
        val RGB = vec(R, G, B);
        val WGB = vec(W, G, B);
        val RWB = vec(R, W, B);
        val RGW = vec(R, G, W);

        val rgbDet = (float) RGB.determinant();

        return new float[] {(R[1] * (float) WGB.determinant()) / (W[1] * rgbDet),
                            (G[1] * (float) RWB.determinant()) / (W[1] * rgbDet),
                            (B[1] * (float) RGW.determinant()) / (W[1] * rgbDet)};
    }

    /*
        8.2 - HSI, HSL, HSV, and related color spaces

        The representation of the colors in the RGB and CMY(K) color spaces are designed
        for specific devices. But for a human observer, they are not useful definitions.
        For user interfaces a more intuitive color space, designed for the way we actually
        think about color is to be preferred. Such a color space is HSI; Hue, Saturation and
        Intensity, which can be thought of as a RGB cube tipped up onto one corner. The line
        from RGB=min to RGB=max becomes verticle and is the intensity axis. The position of a
        point on the circumference of a circle around this axis is the hue and the saturation
        is the radius from the central intensity axis to the color.

                 Green
                  /\
                /    \    ^
              /V=1 x   \   \ Hue (angle, so that Hue(Red)=0,
       Blue -------------- Red       Hue(Green)=120, and Hue(blue)=240 deg)
            \      |     /
             \     |-> Saturation (distance from the central axis)
              \    |   /
               \   |  /
                \  | /
                 \ |/
               V=0 x (Intensity=0 at the top of the apex and =1 at the base of the cone)

        The big disadvantage of this model is the conversion which is mainly because the hue is
        expressed as an angle. The transforms are given below:

            Hue = (Alpha-arctan((Red-intensity)*(3^0.5)/(Green-Blue)))/(2*PI)
            with { Alpha=PI/2 if Green>Blue
                 { Aplha=3*PI/2 if Green<Blue
                 { Hue=1 if Green=Blue
            Saturation = (Red^2+Green^2+Blue^2-Red*Green-Red*Blue-Blue*Green)^0.5
            Intensity = (Red+Green+Blue)/3

        Note that you have to compute Intensity *before* Hue. If not, you must assume that:

            Hue = (Alpha-arctan((2*Red-Green-Blue)/((Green-Blue)*(3^0.5))))/(2*PI).

        I assume that H, S, L, R, G, and B are within the range of [0;1]. Another point of view of
        this cone is to project the coordinates onto the base. The 2D projection is:

            Red:   (1;0)
            Green: (cos(120 deg);sin(120 deg)) = (-0.5; 0.866)
            Blue:  (cos(240 deg);sin(240 deg)) = (-0.5;-0.866)

        Now you need intermediate coordinates:

            x = Red-0.5*(Green+Blue)
            y = 0.866*(Green-Blue)

        Finally, you have:

            Hue = arctan2(x,y)/(2*PI) ; Just one formula, always in the correct quadrant
            Saturation = (x^2+y^2)^0.5
            Intensity = (Red+Green+Blue)/3

        The intermediate coordinates YST { I, x, y } are a cool substitute for HSI in most calculations

            RGBtoYST = new float[][]{{Wr, Wg, Wb, 0},
                                      {1, -.5, -.5, 0},
                                      {0, .5 * Math.sqrt(3), -.5 * Math.sqrt(3), 0},
                                      {0, 0, 0, 1}};

            YSTtoRGB = new float[][] {{1, (Wg+Wb), (Wb - Wg) / Math.sqrt(3), 0},
                                       {1, (-Wr), (2*Wb + Wr) / Math.sqrt(3), 0},
                                       {1, (-Wr), -(2*Wg + Wr) / Math.sqrt(3), 0}};

        The code below uses a normalized version of the intermediate coordinates YST to make sure it fits in the
        dynamic range of a positive integer representation
    */

    public static abstract class LinearTransform {
        static double[][] scaleTransform(double[][] t, int dataType) {
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    t[1][3] = 0xFF / 2.;
                    t[2][3] = 0xFF / 2.;
                    break;
                case DataBuffer.TYPE_SHORT:
                    t[1][3] = Short.MAX_VALUE / 2.;
                    t[2][3] = Short.MAX_VALUE / 2.;
                    break;
                case DataBuffer.TYPE_USHORT:
                    t[1][3] = 0xFFFF / 2.;
                    t[2][3] = 0xFFFF / 2.;
                    break;
                case DataBuffer.TYPE_INT:
                    t[1][3] = Integer.MAX_VALUE / 2.;
                    t[2][3] = Integer.MAX_VALUE / 2.;
                    break;
                case DataBuffer.TYPE_FLOAT:
                case DataBuffer.TYPE_DOUBLE:
                    t[1][3] = 0.5;
                    t[2][3] = 0.5;
                    break;
            }
            return t;
        }

        abstract double [][] getTransform();

        public double[][] fromRGB(int dataType) {
            double[][] t = scaleTransform(getTransform(), dataType);
            return strip(t);
        }

        public double[][] toRGB(int dataType) {
            double[][] t = scaleTransform(getTransform(), dataType);
            return strip(LCMatrix.getArrayDouble(new SimpleMatrix(t).invert()));
        }
    }

    public static class XYZ extends LinearTransform {
        double [][] getTransform() {
            return new double[][]{
                {Wr,   Wg,    Wb,   0},
                {0.5, -0.25, -0.25, 0.5},
                {0,    0.5,  -0.5,  0.5},
                {0,    0,     0,    1}
            };
        }
    }

    public static class YST extends LinearTransform {
        double [][] getTransform() {
            return new double[][]{
                {Wr,   Wg,    Wb,   0},
                {0.5, -0.25, -0.25, 0.5},
                {0,    0.5,  -0.5,  0.5},
                {0,    0,     0,    1}
            };
        }
    }

    public static class LLab extends LinearTransform {
        double [][] getTransform() {
            return new double[][]{
                {Wr,   Wg,    Wb,   0},
                {0.5, -0.5,   0,    0.5},
                {0,    0.5,  -0.5,  0.5},
                {0,    0,     0,    1}
            };
        }
    }

    public static class YCC extends LinearTransform {
        double [][] getTransform() {
            return new double[][]{
                {Wr, Wg, Wb, 0},
                {-Wr / (2 - 2 * Wb) + .5, -Wg / (2 - 2 * Wb) + .5, (1 - Wb) / (2 - 2 * Wb) + .5, 0},
                {(1 - Wr) / (2 - 2 * Wr) + .5, -Wg / (2 - 2 * Wr) + .5, -Wb / (2 - 2 * Wr) + .5, 0},
                {0, 0, 0, 1}
            };
        }
    }

    private static LCMatrix vec(float[] A, float[] B, float[] C) {
        val ABC = new LCMatrix(3, 3);
        for (int i = 0; i < 3; i++)
            ABC.set(i, 0, A[i]);
        for (int i = 0; i < 3; i++)
            ABC.set(i, 1, B[i]);
        for (int i = 0; i < 3; i++)
            ABC.set(i, 2, C[i]);
        return ABC;
    }

    public static double[][] strip(double[][] x) {
        double[][] r = new double[3][];
        r[0] = x[0];
        r[1] = x[1];
        r[2] = x[2];
        return r;
    }

    // Bradford cone response matrix, seems to deliver more consistent results
    static float[][] Bradford = {
        { 0.8951f,   -0.7502f,    0.0389f},
        { 0.2664f,    1.7135f,   -0.0685f},
        {-0.1614f,    0.0367f,    1.0296f}
    };

    static float[][] VonKries = {
        {0.40024f,   -0.22630f,   0.00000f},
        {0.70760f,    1.16532f,   0.00000f},
        {-0.08081f,   0.04570f,   0.91822f},
    };

    // CAT02 matrix, sometimes gives more "neutral" results (no cyan cast)
    static float[][] CAT02 = {
        { 0.7328f,   -0.7036f,    0.0030f},
        { 0.4296f,    1.6975f,    0.0136f},
        {-0.1624f,    0.0061f,    0.9834f}
    };

    static float[][] Sharp = {
        { 1.2694f,   -0.8364f,    0.0297f},
        {-0.0988f,    1.8006f,   -0.0315f},
        {-0.1706f,    0.0357f,    1.0018f}
    };

    static float[][] CMCCAT2000 = {
        { 0.7982f,   -0.5918f,    0.0008f},
        { 0.3389f,    1.5512f,    0.0239f},
        {-0.1371f,    0.0406f,    0.9753f}
    };

    static float[][] XYZScaling = {
        {1, 0, 0},
        {0, 1, 0},
        {0, 0, 1}
    };

    public enum CAMethod {
        Bradford, VonKries, CAT02, Sharp, CMCCAT2000, XYZScaling, Mixed
    }

    static float mixer(float t) {
        return (float) (Math.atan((t - 5000)/100) / Math.PI + 0.5);
    }

    static float[][] matrix(float t) {
        float[][] matrix = new float[3][3];
        float m = mixer(t);

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                matrix[i][j] = XYZScaling[i][j] * (1-m) + Bradford[i][j] * m;

        return matrix;
    }

    public static float[][] chromaticAdaptation(float source, float target) {
        return chromaticAdaptation(source, target, CAMethod.Bradford);
    }

    public static float[][] chromaticAdaptation(float source, float target, CAMethod caMethod) {
        final float[][] method;

        switch (caMethod) {
            case Bradford:
                method = Bradford;
                break;
            case VonKries:
                method = VonKries;
                break;
            case CAT02:
                method = CAT02;
                break;
            case Sharp:
                method = Sharp;
                break;
            case CMCCAT2000:
                method = CMCCAT2000;
                break;
            case Mixed:
                method = matrix(target);
                break;
            case XYZScaling:
            default:
                method = XYZScaling;
                break;
        }

        val B = new LCMatrix(method);

        // source illuminant tristimulus in cone response domain
        float[] sXYZ = xy2XYZ(D(source));
        sXYZ = LCMatrix.getArrayFloat(
                new LCMatrix(1, 3, sXYZ).mult(B)
        )[0];

        // target illuminant tristimulus in cone response domain
        float[] tXYZ = xy2XYZ(D(target));
        tXYZ = LCMatrix.getArrayFloat(
                new LCMatrix(1, 3, tXYZ).mult(B)
        )[0];

        // scaling matrix for the colors
        float[][] diag = new float[][]{
            {sXYZ[0] / tXYZ[0], 0, 0},
            {0, sXYZ[1] / tXYZ[1], 0},
            {0, 0, sXYZ[2] / tXYZ[2]}
        };

        // total transform
        return LCMatrix.getArrayFloat(
                B.mult(new LCMatrix(diag)).mult(B.invert())
        );
    }

    public static double saturation(double r, double g, double b) {
        double min = Math.min(r, Math.min(g, b));
        double max = Math.max(r, Math.max(g, b));
        return max != 0 ? 1 - min / max : 0;
    }

    private static SimpleMatrix RGBtoZYX = new LCMatrix(RGBtoZYX()).transpose();
    private static SimpleMatrix XYZtoRGB = RGBtoZYX.invert();

    public static float[] neutralTemperature(float[] rgb, float refT, CAMethod caMethod) {
        float sat = Float.MAX_VALUE;
        float minT = 0;
        double wbr = 0, wbg = 0, wbb = 0;

        val color = new LCMatrix(3, 1, rgb);

        for (int t = 1000; t < 40000; t+= (0.001 * t)) {
            val B = new LCMatrix(chromaticAdaptation(t, refT, caMethod));
            val combo = XYZtoRGB.mult(B.mult(RGBtoZYX));

            val adapdedColor = combo.mult(color);

            val r = clip(adapdedColor.get(0, 0));
            val g = clip(adapdedColor.get(1, 0));
            val b = clip(adapdedColor.get(2, 0));

            val tSat = (float) saturation(r, g, b);

            if (tSat < sat) {
                sat = tSat;
                minT = t;
                wbr = r;
                wbg = g;
                wbb = b;
            }
        }

        float tint = 0;
        if (wbr != 0 || wbg != 0 || wbb != 0) {
            tint = (float) (- (wbg - (wbr + wbb) / 2));
        }

        return new float[] {minT, tint};
    }

    static double clip(double x) {
        return Math.min(Math.max(0, x), 1);
    }

    public static float findTemperature(float[] rgb, float refT, CAMethod caMethod) {
        float minDiff = Float.MAX_VALUE;
        float minT = 0;

        val xyzRef = JAIContext.linearColorSpace.toCIEXYZ(rgb);
        val labRef = JAIContext.labColorSpace.fromCIEXYZ(xyzRef);

        SimpleMatrix gray = new LCMatrix(new float[][]{{0.18f}, {0.18f}, {0.18f}});

        for (int t = 1000; t < 40000; t+= (0.001 * t)) {
            val B = new LCMatrix(chromaticAdaptation(t, refT, caMethod));
            val combo = XYZtoRGB.mult(B.mult(RGBtoZYX));

            gray = combo.mult(gray);

            val r = clip(gray.get(0, 0));
            val g = clip(gray.get(1, 0));
            val b = clip(gray.get(2, 0));

            val xyzGray = JAIContext.linearColorSpace.toCIEXYZ(new float[] {(float) r, (float) g, (float) b});
            val labGray = JAIContext.labColorSpace.fromCIEXYZ(xyzGray);

            float diff = 0;
            for (int i = 1; i < 3; i++) {
                val di = labGray[i] - labRef[i];
                diff += di * di;
            }
            diff = (float) Math.sqrt(diff);

            if (diff < minDiff) {
                minDiff = diff;
                minT = t;
                /* wbr = r / 256;
                wbg = g / 256;
                wbb = b / 256; */
            }
        }

        /* if (wbr != 0 || wbg != 0 || wbb != 0) {
            tint = (float) (- (wbg - (wbr + wbb) / 2));
        } */

        return minT;
    }

    public static void main( final String[] args ) {
        for (int i = 2000; i < 25000; i += 500)
            System.out.println("m(" + i + ") : " + mixer(i));

        float[] D65 = D(whitePointTemperature);

        System.out.println("D65: " + D65[0] + ", " + D65[1] + ", " + (1 - D65[0] - D65[1]));

        System.out.println("xr: " + Cxy[0][0] + ", yr: " + Cxy[0][1]);
        System.out.println("xg: " + Cxy[1][0] + ", yg: " + Cxy[1][1]);
        System.out.println("xb: " + Cxy[2][0] + ", yb: " + Cxy[2][1]);

        System.out.println("W: " + Wr + ", " + Wg + ", " + Wb);

        /* Matrix ww = new LCMatrix(RGBtoZYX()).transpose();
        ww.print(8, 6);

        Matrix ca = new LCMatrix(chromaticAdaptation(7500, 5000));
        float[] result = new LCMatrix(new float[][] {{0.2, 0.2, 0.2}}).times(ca).getArray()[0];

        for (int j = 0; j < 3; j++)
            System.out.print(" " + result[j]);
        System.out.println(); */

        System.out.println("RGBToXYZ");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                System.out.print(" " + RGBToXYZMat[i][j]);
            System.out.println();
        }

        val rgb2xyz = RGBtoZYX();

        System.out.println("rgb2xyz");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                System.out.print(" " + rgb2xyz.get(i, j));
            System.out.println();
        }

        System.out.println("XYZToRGB");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                System.out.print(" " + XYZToRGBMat[i][j]);
            System.out.println();
        }
    }
}
