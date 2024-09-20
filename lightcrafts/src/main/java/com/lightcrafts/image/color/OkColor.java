/* Copyright (C) 2024-     Masahiro Kitagawa */

// cf. <a href="https://bottosson.github.io/posts/colorpicker/">Björn Ottosson's blog</a>
//
// Below is the original copyright notice from the original author of
// * <a href="https://bottosson.github.io/misc/ok_color.h">C++ ok_color.h code</a>.
// The code is licensed under the MIT License.
//
// Copyright(c) 2021 Björn Ottosson
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this softwareand associated documentation files(the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and /or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do
// so, subject to the following conditions :
// The above copyright noticeand this permission notice shall be included in all
// copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.lightcrafts.image.color;

import java.awt.Color;
import java.lang.Math;

public class OkColor {

    private static int clampU8(float x) {
        return (int) Math.min(Math.max(x, 0), 255);
    }

    private record RGB(float r, float g, float b) {

        static RGB fromColor(Color c) {
            final float r = c.getRed() / 255.f;
            final float g = c.getGreen() / 255.f;
            final float b = c.getBlue() / 255.f;

            return new RGB(r, g, b);
        }

        public Color toColor() {
            return new Color(
                    clampU8(r * 255.f),
                    clampU8(g * 255.f),
                    clampU8(b * 255.f)
            );
        }
    }

    private record LC(float L, float C) {

        /**
         * finds L_cusp and C_cusp for a given hue.
         * a and b must be normalized so a^2 + b^2 == 1
         */
        static LC find_cusp(float a, float b) {
            // First, find the maximum saturation (saturation S = C/L)
            float S_cusp = compute_max_saturation(a, b);

            // Convert to linear sRGB to find the first point where at least one of r,g or b >= 1:
            final RGB rgb_at_max = new Oklab(1, S_cusp * a, S_cusp * b).toSRGB();
            final var max = Math.max(Math.max(rgb_at_max.r, rgb_at_max.g), rgb_at_max.b);
            final var L_cusp = (float) Math.cbrt(1 / max);
            final var C_cusp = L_cusp * S_cusp;

            return new LC(L_cusp , C_cusp);
        }

        ST toST() {
            return new ST(C / L, C / (1 - L));
        }
    }

    /**
     * Alternative representation of (L_cusp, C_cusp)
     * Encoded so S = C_cusp/L_cusp and T = C_cusp/(1-L_cusp)
     * The maximum value for C in the triangle is then found as fmin(S*L, T*(1-L)), for a given L
     */
    private record ST(float S, float T) {

        /**
         * Returns a smooth approximation of the location of the cusp
         * This polynomial was created by an optimization process
         * It has been designed so that S_mid < S_max and T_mid < T_max
         */
        static ST get_ST_mid(float a_, float b_) {
            float S = 0.11516993f + 1.f / (
                    7.44778970f + 4.15901240f * b_
                            + a_ * (-2.19557347f + 1.75198401f * b_
                            + a_ * (-2.13704948f - 10.02301043f * b_
                            + a_ * (-4.24894561f + 5.38770819f * b_ + 4.69891013f * a_
                    )))
            );

            float T = 0.11239642f + 1.f / (
                    1.61320320f - 0.68124379f * b_
                            + a_ * (+0.40370612f + 0.90148123f * b_
                            + a_ * (-0.27087943f + 0.61223990f * b_
                            + a_ * (+0.00299215f - 0.45399568f * b_ - 0.14661872f * a_
                    )))
            );

            return new ST(S, T);
        }
    }

    static private float srgb_transfer_function(float a) {
        return .0031308f >= a ? 12.92f * a : 1.055f * (float) Math.pow(a, .4166666666666667f) - .055f;
    }

    static private float srgb_transfer_function_inv(float a) {
        return .04045f < a ? (float) Math.pow((a + .055f) / 1.055f, 2.4f) : a / 12.92f;
    }

    public record Oklab (float l, float a, float b) {

        static Oklab fromSRGB(RGB rgb) {
            final float r = rgb.r;
            final float g = rgb.g;
            final float b = rgb.b;

            final var l = (float) Math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b);
            final var m = (float) Math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b);
            final var s = (float) Math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b);

            return new Oklab(
                    0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
                    1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
                    0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
            );
        }

        public static Oklab from(Color c) {
            return fromSRGB(RGB.fromColor(c));
        }

        RGB toSRGB() {
            final var l_ = l + 0.3963377774f * a + 0.2158037573f * b;
            final var m_ = l - 0.1055613458f * a - 0.0638541728f * b;
            final var s_ = l - 0.0894841775f * a - 1.2914855480f * b;

            final var l = l_ * l_ * l_;
            final var m = m_ * m_ * m_;
            final var s = s_ * s_ * s_;

            return new RGB(
                    +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
                    -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
                    -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
            );
        }
    }

    /**
     * Finds the maximum saturation possible for a given hue that fits in sRGB
     * Saturation here is defined as S = C/L
     * a and b must be normalized so a^2 + b^2 == 1
     */
    static private float compute_max_saturation(float a, float b) {
        // Max saturation will be when one of r, g or b goes below zero.

        // Select different coefficients depending on which component goes below zero first
        final float k0, k1, k2, k3, k4, wl, wm, ws;

        if (-1.88170328f * a - 0.80936493f * b > 1) {
            // Red component
            k0 = 1.19086277f; k1 = 1.76576728f; k2 = 0.59662641f; k3 = 0.75515197f; k4 = 0.56771245f;
            wl = 4.0767416621f; wm = -3.3077115913f; ws = 0.2309699292f;
        } else if (1.81444104f * a - 1.19445276f * b > 1) {
            // Green component
            k0 = 0.73956515f; k1 = -0.45954404f; k2 = 0.08285427f; k3 = 0.12541070f; k4 = 0.14503204f;
            wl = -1.2684380046f; wm = 2.6097574011f; ws = -0.3413193965f;
        } else {
            // Blue component
            k0 = 1.35733652f; k1 = -0.00915799f; k2 = -1.15130210f; k3 = -0.50559606f; k4 = 0.00692167f;
            wl = -0.0041960863f; wm = -0.7034186147f; ws = 1.7076147010f;
        }

        // Approximate max saturation using a polynomial:
        float S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b;

        // Do one step Halley's method to get closer
        // this gives an error less than 10e6, except for some blue hues where the dS/dh is close to infinite
        // this should be sufficient for most applications, otherwise do two/three steps

        float k_l = +0.3963377774f * a + 0.2158037573f * b;
        float k_m = -0.1055613458f * a - 0.0638541728f * b;
        float k_s = -0.0894841775f * a - 1.2914855480f * b;

        float l_ = 1.f + S * k_l;
        float m_ = 1.f + S * k_m;
        float s_ = 1.f + S * k_s;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;

        float l_dS = 3.f * k_l * l_ * l_;
        float m_dS = 3.f * k_m * m_ * m_;
        float s_dS = 3.f * k_s * s_ * s_;

        float l_dS2 = 6.f * k_l * k_l * l_;
        float m_dS2 = 6.f * k_m * k_m * m_;
        float s_dS2 = 6.f * k_s * k_s * s_;

        float f = wl * l + wm * m + ws * s;
        float f1 = wl * l_dS + wm * m_dS + ws * s_dS;
        float f2 = wl * l_dS2 + wm * m_dS2 + ws * s_dS2;

        return S - f * f1 / (f1 * f1 - 0.5f * f * f2);
    }

    /**
     * A class to convert RGB to okhsl and back.
     */
    public record Okhsl (float h, float s, float l) {

        private RGB toSRGB() {
            if (l == 1.0f) {
                return new RGB(1.f, 1.f, 1.f);
            } else if (l == 0.f) {
                return new RGB(0.f, 0.f, 0.f);
            }

            final var a_ = (float) Math.cos(2.f * Math.PI * h);
            final var b_ = (float) Math.sin(2.f * Math.PI * h);
            final var L = toe_inv(l);

            final var cs = Cs.get_Cs(L, a_, b_);
            final var C_0 = cs.C_0;
            final var C_mid = cs.C_mid;
            final var C_max = cs.C_max;

            final var mid = 0.8f;
            final var mid_inv = 1.25f;

            final float C;
            if (s < mid) {
                final var t = mid_inv * s;

                final var k_1 = mid * C_0;
                final var k_2 = (1.f - k_1 / C_mid);

                C = t * k_1 / (1.f - k_2 * t);
            } else {
                final var t = (s - mid)/ (1 - mid);

                final var k_0 = C_mid;
                final var k_1 = (1.f - mid) * C_mid * C_mid * mid_inv * mid_inv / C_0;
                final var k_2 = (1.f - (k_1) / (C_max - C_mid));

                C = k_0 + t * k_1 / (1.f - k_2 * t);
            }

            final RGB rgb = new Oklab(L, C * a_, C * b_).toSRGB();
            return new RGB(
                    srgb_transfer_function(rgb.r),
                    srgb_transfer_function(rgb.g),
                    srgb_transfer_function(rgb.b)
            );
        }

        public Color toColor() {
            return toSRGB().toColor();
        }

        static Okhsl fromSRGB(RGB rgb) {
            final var lab = Oklab.fromSRGB(new RGB(
                    srgb_transfer_function_inv(rgb.r),
                    srgb_transfer_function_inv(rgb.g),
                    srgb_transfer_function_inv(rgb.b)
            ));

            final var C = (float) Math.hypot(lab.a, lab.b);
            final var a_ = lab.a / C;
            final var b_ = lab.b / C;

            final var L = lab.l;
            final var h = 0.5f + 0.5f * (float) (Math.atan2(-lab.b, -lab.a) / Math.PI);

            final var cs = Cs.get_Cs(L, a_, b_);
            final var C_0 = cs.C_0;
            final var C_mid = cs.C_mid;
            final var C_max = cs.C_max;

            // Inverse of the interpolation in Okhsl#toSRGB:

            final var mid = 0.8f;
            final var mid_inv = 1.25f;

            final float s;
            if (C < C_mid) {
                final var k_1 = mid * C_0;
                final var k_2 = (1.f - k_1 / C_mid);

                final var t = C / (k_1 + k_2 * C);
                s = t * mid;
            } else {
                final var k_0 = C_mid;
                final var k_1 = (1.f - mid) * C_mid * C_mid * mid_inv * mid_inv / C_0;
                final var k_2 = (1.f - (k_1) / (C_max - C_mid));

                final var t = (C - k_0) / (k_1 + k_2 * (C - k_0));
                s = mid + (1.f - mid) * t;
            }

            final float l = toe(L);
            return new Okhsl(h, s, l);
        }

        public static Okhsl from(Color c) {
            return Okhsl.fromSRGB(RGB.fromColor(c));
        }
    }

    /**
     * Finds intersection of the line defined by
     * L = L0 * (1 - t) + t * L1;
     * C = t * C1;
     * a and b must be normalized so a^2 + b^2 == 1
     */
    static private float find_gamut_intersection(float a, float b, float L1, float C1, float L0, LC cusp) {
        // Find the intersection for upper and lower half seprately
        if (((L1 - L0) * cusp.C - (cusp.L - L0) * C1) <= 0.f) {
            // Lower half
            return cusp.C * L0 / (C1 * cusp.L + cusp.C * (L0 - L1));
        }

        // Upper half

        // First intersect with triangle
        float t = cusp.C * (L0 - 1.f) / (C1 * (cusp.L - 1.f) + cusp.C * (L0 - L1));

        // Then one step Halley's method
        float dL = L1 - L0;
        float dC = C1;

        float k_l = +0.3963377774f * a + 0.2158037573f * b;
        float k_m = -0.1055613458f * a - 0.0638541728f * b;
        float k_s = -0.0894841775f * a - 1.2914855480f * b;

        float l_dt = dL + dC * k_l;
        float m_dt = dL + dC * k_m;
        float s_dt = dL + dC * k_s;

        // If higher accuracy is required, 2 or 3 iterations of the following block can be used:

        float L = L0 * (1.f - t) + t * L1;
        float C = t * C1;

        float l_ = L + C * k_l;
        float m_ = L + C * k_m;
        float s_ = L + C * k_s;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;

        float ldt = 3 * l_dt * l_ * l_;
        float mdt = 3 * m_dt * m_ * m_;
        float sdt = 3 * s_dt * s_ * s_;

        float ldt2 = 6 * l_dt * l_dt * l_;
        float mdt2 = 6 * m_dt * m_dt * m_;
        float sdt2 = 6 * s_dt * s_dt * s_;

        float r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s - 1;
        float r1 = 4.0767416621f * ldt - 3.3077115913f * mdt + 0.2309699292f * sdt;
        float r2 = 4.0767416621f * ldt2 - 3.3077115913f * mdt2 + 0.2309699292f * sdt2;

        float u_r = r1 / (r1 * r1 - 0.5f * r * r2);
        float t_r = -r * u_r;

        float g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s - 1;
        float g1 = -1.2684380046f * ldt + 2.6097574011f * mdt - 0.3413193965f * sdt;
        float g2 = -1.2684380046f * ldt2 + 2.6097574011f * mdt2 - 0.3413193965f * sdt2;

        float u_g = g1 / (g1 * g1 - 0.5f * g * g2);
        float t_g = -g * u_g;

        float b0 = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s - 1;
        float b1 = -0.0041960863f * ldt - 0.7034186147f * mdt + 1.7076147010f * sdt;
        float b2 = -0.0041960863f * ldt2 - 0.7034186147f * mdt2 + 1.7076147010f * sdt2;

        float u_b = b1 / (b1 * b1 - 0.5f * b0 * b2);
        float t_b = -b0 * u_b;

        t_r = u_r >= 0.f ? t_r : Float.MAX_VALUE;
        t_g = u_g >= 0.f ? t_g : Float.MAX_VALUE;
        t_b = u_b >= 0.f ? t_b : Float.MAX_VALUE;

        t += Math.min(t_r, Math.min(t_g, t_b));

        return t;
    }

    static private float toe(float x) {
        final float k_1 = 0.206f;
        final float k_2 = 0.03f;
        final float k_3 = (1.f + k_1) / (1.f + k_2);
        return 0.5f * (float) (k_3 * x - k_1 + Math.sqrt((k_3 * x - k_1) * (k_3 * x - k_1) + 4 * k_2 * k_3 * x));
    }

    static private float toe_inv(float x) {
        final float k_1 = 0.206f;
        final float k_2 = 0.03f;
        final float k_3 = (1.f + k_1) / (1.f + k_2);
        return (x * x + k_1 * x) / (k_3 * (x + k_2));
    }

    private record Cs (float C_0, float C_mid, float C_max) {

        static Cs get_Cs(float L, float a_, float b_) {
            LC cusp = LC.find_cusp(a_, b_);

            float C_max = find_gamut_intersection(a_, b_, L, 1, L, cusp);
            ST ST_max = cusp.toST();

            // Scale factor to compensate for the curved part of gamut shape:
            float k = C_max / Math.min((L * ST_max.S), (1 - L) * ST_max.T);

            ST ST_mid = ST.get_ST_mid(a_, b_);

            // Use a soft minimum function, instead of a sharp triangle shape to get a smooth value for chroma.
            float C_a = L * ST_mid.S;
            float C_b = (1.f - L) * ST_mid.T;
            final var C_mid = 0.9f * k * (float) Math.sqrt(Math.sqrt(1 / (1 / (C_a * C_a * C_a * C_a) + 1 / (C_b * C_b * C_b * C_b))));

            // for C_0, the shape is independent of hue, so ST are constant. Values picked to roughly be the average values of ST.
            float C0_a = L * 0.4f;
            float C0_b = (1.f - L) * 0.8f;

            // Use a soft minimum function, instead of a sharp triangle shape to get a smooth value for chroma.
            final var C_0 = (float) Math.sqrt(1.f / (1.f / (C0_a * C0_a) + 1.f / (C0_b * C0_b)));

            return new Cs(C_0, C_mid, C_max);
        }
    }
}
