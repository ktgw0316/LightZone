/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
  Smartass C++ code for layer blending modes

  Copyright (C) 2005 Fabio Riccardi - Light Crafts, Inc.

  To get an idea of what is going on here look at:

  http://www.pegtop.net/delphi/articles/blendmodes/
*/

#include <algorithm>
#include <cmath>
#include <complex>
#include <omp.h>
#include "mathlz.h"

typedef unsigned char byte;
typedef unsigned short ushort;

static constexpr double hilightsThreshold = 0.59;
static constexpr double midtonesThreshold = 0.172;
static constexpr double shadowsThreshold = 0.0425;

class BlendMode {
    static BlendMode *blendMode[];

  public:
    static constexpr ushort maxVal = 0xFFFF;

    virtual ushort blendPixels(ushort front, ushort back) const = 0;

    static BlendMode *getBlender(int mode) { return blendMode[mode]; }
};

class NormalBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const { return front; }
};

class AverageBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const { return (front + back) / 2; }
};

class MultiplyBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        return front * back / (maxVal + 1);
    }
};

class ScreenBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        return maxVal - ((maxVal - front) * (maxVal - back) / (maxVal + 1));
    }
};

class DarkenBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const { return std::min(front, back); }
};

class LightenBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const { return std::max(front, back); }
};

class DifferenceBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const { return std::abs(front - back); }
};

class NegationBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        return maxVal - std::abs(maxVal - front - back);
    }
};

class ExclusionBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        return front + back - (front * back) / (maxVal / 2);
    }
};

class OverlayBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        if (back < maxVal / 2)
            return (front * back) / (maxVal / 2);
        else
            return maxVal - ((maxVal - front) * (maxVal - back) / (maxVal / 2));
    }
};

class HardLightBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        if (front < maxVal / 2)
            return (front * back) / (maxVal / 2);
        else
            return maxVal - ((maxVal - front) * (maxVal - back) / (maxVal / 2));
    }
};

// original soft light
class SoftLightBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        ushort m = front * back / (maxVal + 1);
        ushort s = maxVal - (maxVal - front) * (maxVal - back) / (maxVal + 1);

        return ((maxVal - back) * m + (back * s)) / (maxVal + 1);
    }
};

class SoftLightBlendMode2 : public BlendMode {
  protected:
    const double exp;

  public:
  public:
    SoftLightBlendMode2(double exp) : exp(exp) {}

    virtual ushort blendPixels(ushort front, ushort back) const {
        ushort m = (front * back) / ((unsigned)maxVal + 1);
        ushort s = maxVal - ((maxVal - front) * (maxVal - back)) / ((unsigned)maxVal + 1);

        double p = pow((back / (double)maxVal), exp);

        return (ushort)(m * (1 - p) + s * p);
    }
};

class ColorDodgeBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        if (front == maxVal)
            return front;
        unsigned int c = (unsigned int)back * (maxVal + 1) / (maxVal - front);
        return std::min(c, (unsigned int)maxVal);
    }
};

class ColorBurnBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        if (front == 0)
            return 0;
        int c = maxVal - (unsigned int)(maxVal - back) * (maxVal + 1) / front;
        return std::max(c, 0);
    }
};

class SoftDodgeBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        const auto p = std::minmax(back, ushort(maxVal - front));
        if (p.second == 0)
            return maxVal;
        unsigned int c = (unsigned int)p.first * (maxVal / 2) / p.second;
        return clampUShort(c);
    }
};

class SoftBurnBlendMode : public BlendMode {
    virtual ushort blendPixels(ushort front, ushort back) const {
        const auto p = std::minmax(front, ushort(maxVal - back));
        if (p.second == 0)
            return maxVal;
        unsigned int c = (unsigned int)p.first * (maxVal / 2) / p.second;
        return clampUShort(c);
    }
};

class LowPassBlendMode : public BlendMode {
    const ushort threshold;
    const ushort transition;

  public:
    LowPassBlendMode(ushort threshold, ushort transition)
        : threshold(threshold), transition(transition) {}

    virtual ushort blendPixels(ushort front, ushort back) const {
        if (back < threshold - transition)
            return front;
        if (back > threshold + transition)
            return back;
        /*
          unsigned long long k = back - (threshold - transition);
          k = (k * k) / (maxVal + 1);
          unsigned long long t4 = 4 * transition * transition;
          return (ushort) ((k * back + (t4 * maxVal - k) * front) / (t4 * (maxVal + 1)));
        */
        double k = (back - (threshold - transition)) / (2.0 * transition);
        k *= k;
        return ushort(k * back + (1 - k) * front);
    }
};

class HighPassBlendMode : public BlendMode {
    const ushort threshold;
    const ushort transition;

  public:
    HighPassBlendMode(ushort threshold, ushort transition)
        : threshold(threshold), transition(transition) {}

    virtual ushort blendPixels(ushort front, ushort back) const {
        if (back > threshold + transition)
            return front;
        if (back < threshold - transition)
            return back;

        double k = sqrt((back - (threshold - transition)) / (2.0 * transition));
        return ushort(k * front + (1 - k) * back);
    }
};

class BandBlendMode : public BlendMode {
    const LowPassBlendMode shadows;
    const HighPassBlendMode hilights;

  public:
    BandBlendMode(ushort thresholdLow, ushort transitionLow, ushort thresholdHigh,
                  ushort transitionHigh)
        : shadows(thresholdHigh, transitionHigh), hilights(thresholdLow, transitionLow) {}

    virtual ushort blendPixels(ushort front, ushort back) const {
        return shadows.blendPixels(hilights.blendPixels(front, back), back);
    }
};

BlendMode *BlendMode::blendMode[] = {
    new NormalBlendMode(),          // 0
    new AverageBlendMode(),         // 1
    new MultiplyBlendMode(),        // 2
    new ScreenBlendMode(),          // 3
    new DarkenBlendMode(),          // 4
    new LightenBlendMode(),         // 5
    new DifferenceBlendMode(),      // 6
    new NegationBlendMode(),        // 7
    new ExclusionBlendMode(),       // 8
    new OverlayBlendMode(),         // 9
    new HardLightBlendMode(),       // 10
    new SoftLightBlendMode(),       // 11
    new ColorDodgeBlendMode(),      // 12
    new ColorBurnBlendMode(),       // 13
    new SoftDodgeBlendMode(),       // 14
    new SoftBurnBlendMode(),        // 15
    new SoftLightBlendMode2(0.4),   // 16
    new SoftLightBlendMode2(1.31),  // 17
    new SoftLightBlendMode2(0.291), // 18
    new LowPassBlendMode(ushort(midtonesThreshold * maxVal),
                         ushort(midtonesThreshold * maxVal / 2)), // 19
    new HighPassBlendMode(ushort(shadowsThreshold * maxVal),
                          ushort(shadowsThreshold * maxVal / 2)), // 20
    new BandBlendMode(ushort(shadowsThreshold * maxVal),
                      ushort(shadowsThreshold * maxVal / 2),
                      ushort(hilightsThreshold * maxVal),
                      ushort(hilightsThreshold * maxVal / 2)), // 21
};

#include "../pixutils/HSB.h"
#include <cassert>
#include <cstdio>
#include <functional>

void blendLoop(const ushort s1[], const ushort s2[], ushort d[], const byte m[], const byte cs[],
               int bands, int s1bd, int s2bd, int s1LineOffset, int s2LineOffset, int dLineOffset,
               int mLineOffset, int csLineOffset, int s1LineStride, int s2LineStride,
               int dLineStride, int mLineStride, int csLineStride, int s1PixelStride,
               int s2PixelStride, int dPixelStride, int mPixelStride, int csPixelStride,
               int dheight, int dwidth, int intOpacity, int mode) {
    const bool inverted = intOpacity < 0;
    intOpacity = abs(intOpacity);

    const BlendMode *const blender = BlendMode::getBlender(mode);
    const auto blendPixels = [&](ushort f, ushort b) { return blender->blendPixels(f, b); };
    const ushort maxVal = BlendMode::maxVal;

#pragma omp parallel for schedule(guided)
    for (int h = 0; h < dheight; h++) {
        int s1PixelOffset = s1LineOffset + h * s1LineStride;
        int s2PixelOffset = s2LineOffset + h * s2LineStride;
        int mPixelOffset = mLineOffset + h * mLineStride;
        int csPixelOffset = csLineOffset + h * csLineStride;
        int dPixelOffset = dLineOffset + h * dLineStride;

        for (int w = 0; w < dwidth; w++) {
            int mValue = 0xFF;
            if (m != NULL)
                mValue = inverted ? 0xFF - m[mPixelOffset] : m[mPixelOffset];
            if (cs != NULL)
                mValue = mValue * cs[csPixelOffset] / 0xFF;

            const ushort pixel[3] = {
                s2[s2PixelOffset],
                s2[s2PixelOffset + s2bd],
                s2[s2PixelOffset + 2 * s2bd],
            };

            for (int i = 0, s1b = 0; i < bands; i++, s1b += s1bd) {
                const ushort s2Value = pixel[i];
                ushort value;
                if (mValue == 0) {
                    value = s2Value;
                } else {
                    const ushort blended = blendPixels(s1[s1PixelOffset + s1b], s2Value);
                    if (m == NULL && cs == NULL) {
                        if (intOpacity == maxVal) {
                            value = blended;
                        } else {
                            value = (intOpacity * blended + (maxVal - intOpacity) * s2Value) /
                                    maxVal;
                        }
                    } else {
                        const int maskedOpacity = (intOpacity * mValue) / 0xFF;
                        value = (maskedOpacity * blended + (maxVal - maskedOpacity) * s2Value) /
                                maxVal;
                    }
                }
                d[dPixelOffset + i] = value;
            }

            s1PixelOffset += s1PixelStride;
            s2PixelOffset += s2PixelStride;
            mPixelOffset += mPixelStride;
            csPixelOffset += csPixelStride;
            dPixelOffset += dPixelStride;
        }
    }
}

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_PixelBlender.h"
#endif

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_PixelBlender_cUShortLoopCS(
    JNIEnv *env, jclass cls, jshortArray s1, jshortArray s2, jshortArray d, jbyteArray m,
    jbyteArray cs, jint bands, jint s1bd, jint s2bd, jint s1LineOffset, jint s2LineOffset,
    jint dLineOffset, jint mLineOffset, jint csLineOffset, jint s1LineStride, jint s2LineStride,
    jint dLineStride, jint mLineStride, jint csLineStride, jint s1PixelStride, jint s2PixelStride,
    jint dPixelStride, jint mPixelStride, jint csPixelStride, jint dheight, jint dwidth,
    jint jintOpacity, jint mode) {
    ushort *cs1 = (ushort *)env->GetPrimitiveArrayCritical(s1, 0);
    ushort *cs2 = (ushort *)env->GetPrimitiveArrayCritical(s2, 0);
    ushort *cd = (ushort *)env->GetPrimitiveArrayCritical(d, 0);
    byte *cm = (m != NULL ? (byte *)env->GetPrimitiveArrayCritical(m, 0) : (byte *)NULL);
    byte *ccs = (cs != NULL ? (byte *)env->GetPrimitiveArrayCritical(cs, 0) : (byte *)NULL);

    blendLoop(cs1, cs2, cd, cm, ccs, bands, s1bd, s2bd, s1LineOffset, s2LineOffset, dLineOffset,
              mLineOffset, csLineOffset, s1LineStride, s2LineStride, dLineStride, mLineStride,
              csLineStride, s1PixelStride, s2PixelStride, dPixelStride, mPixelStride, csPixelStride,
              dheight, dwidth, jintOpacity, mode);

    env->ReleasePrimitiveArrayCritical(s1, cs1, 0);
    env->ReleasePrimitiveArrayCritical(s2, cs2, 0);
    env->ReleasePrimitiveArrayCritical(d, cd, 0);
    if (cm != NULL)
        env->ReleasePrimitiveArrayCritical(m, cm, 0);
    if (ccs != NULL)
        env->ReleasePrimitiveArrayCritical(cs, ccs, 0);
}
