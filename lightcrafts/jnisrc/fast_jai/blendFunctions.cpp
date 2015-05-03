/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
  Smartass C++ code for layer blending modes

  Copyright (C) 2005 Fabio Riccardi - Light Crafts, Inc.

  To get an idea of what is going on here look at:

  http://www.pegtop.net/delphi/blendmodes/
*/

#include <math.h>
#include <algorithm>
#include <complex>

typedef unsigned char byte;
typedef unsigned short ushort;

static double hilightsThreshold = 0.59;
static double midtonesThreshold = 0.172;
static double shadowsThreshold = 0.0425;

class BlendMode {
  static BlendMode *blendMode[];

public:
  static const ushort maxVal = 0xFFFF;
  
  virtual ushort blendPixels(ushort front, ushort back) const = 0;

  static BlendMode *getBlender(int mode) {
    return blendMode[mode];
  }
};

class NormalBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const { return front; }
};

class AverageBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const {
    return (front + back) / 2;
  }
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
  virtual ushort blendPixels(ushort front, ushort back) const {
    if (front < back)
      return front;
    else
      return back;
  }
};

class LightenBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const {
    if (front > back)
      return front;
    else
      return back;
  }
};

class DifferenceBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const {
      return std::abs(front - back);
  }
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
  public :SoftLightBlendMode2(double exp) : exp(exp) { }

  virtual ushort blendPixels(ushort front, ushort back) const {
    ushort m = (front * back) / ((unsigned) maxVal + 1);
    ushort s = maxVal - ((maxVal - front) * (maxVal - back)) / ((unsigned) maxVal + 1);

    double p = pow((back / (double) maxVal), exp);

    return (ushort) (m * (1 - p) + s * p);
  }
};

class ColorDodgeBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const {
    if (front == maxVal)
      return front;
    unsigned int c = (unsigned int) back * (maxVal + 1) / (maxVal - front);
    return std::min(c, (unsigned int) maxVal);
  }
};

class ColorBurnBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const {
    if (front == 0)
      return 0;
    int c = maxVal - (unsigned int) (maxVal - back) * (maxVal + 1) / front;
    return std::max(c, 0);
  }
};

class SoftDodgeBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const {
    if (front + back < maxVal+1) {
      if (front == maxVal)
        return maxVal;
      unsigned int c = (unsigned int) back * (maxVal / 2) / (maxVal - front);
      return std::min(c, (unsigned int) maxVal);
    } else {
      int c = maxVal - (unsigned int) (maxVal - front) * (maxVal / 2) / back;
        return std::max(c, 0);
    }
  }
};

class SoftBurnBlendMode : public BlendMode {
  virtual ushort blendPixels(ushort front, ushort back) const {
    if (front + back < maxVal+1) {
      if (back == maxVal)
        return maxVal;
      unsigned int c = (unsigned int) front * (maxVal / 2) / (maxVal - back);
      return std::min(c, (unsigned int) maxVal);
    } else {
      int c = maxVal - (unsigned int) (maxVal - back) * (maxVal / 2) / front;
        return std::max(c, 0);
    }
  }
};

class LowPassBlendMode : public BlendMode {
  const ushort threshold;
  const ushort transition;

  public :LowPassBlendMode(ushort threshold, ushort transition) :
    threshold(threshold), transition(transition) { }

  virtual ushort blendPixels(ushort front, ushort back) const {
    if (back < threshold - transition)
      return front;
    else if (back > threshold + transition)
      return back;
    else {
    /*
      unsigned long long k = back - (threshold - transition);
      k = (k * k) / (maxVal + 1);
      unsigned long long t4 = 4 * transition * transition;
      return (ushort) ((k * back + (t4 * maxVal - k) * front) / (t4 * (maxVal + 1)));
    */
      double k = (back - (threshold - transition)) / (2.0 * transition);
      k *= k;
      return (ushort) (k * back + (1 - k) * front);
    }
  }
};

class HighPassBlendMode : public BlendMode {
  const ushort threshold;
  const ushort transition;

  public :HighPassBlendMode(ushort threshold, ushort transition) :
    threshold(threshold), transition(transition) { }

  virtual ushort blendPixels(ushort front, ushort back) const {
    if (back > threshold + transition)
      return front;
    else if (back < threshold - transition)
      return back;
    else {
      double k = sqrt((back - (threshold - transition)) / (2.0 * transition));

      return (ushort) (k * front + (1 - k) * back);
    }
  }
};

class BandBlendMode : public BlendMode {
  const LowPassBlendMode shadows;
  const HighPassBlendMode hilights;

  public :BandBlendMode(ushort thresholdLow, ushort transitionLow, ushort thresholdHigh, ushort transitionHigh) :
    shadows(thresholdHigh, transitionHigh),
    hilights(thresholdLow, transitionLow) { }

  virtual ushort blendPixels(ushort front, ushort back) const {
    return shadows.blendPixels(hilights.blendPixels(front, back), back);
  }
};

BlendMode *BlendMode::blendMode[] = {
  new NormalBlendMode(),         // 0
  new AverageBlendMode(),        // 1
  new MultiplyBlendMode(),       // 2
  new ScreenBlendMode(),         // 3
  new DarkenBlendMode(),         // 4
  new LightenBlendMode(),        // 5
  new DifferenceBlendMode(),     // 6
  new NegationBlendMode(),       // 7
  new ExclusionBlendMode(),      // 8
  new OverlayBlendMode(),        // 9
  new HardLightBlendMode(),      // 10
  new SoftLightBlendMode(),      // 11
  new ColorDodgeBlendMode(),     // 12
  new ColorBurnBlendMode(),      // 13
  new SoftDodgeBlendMode(),      // 14
  new SoftBurnBlendMode(),       // 15
  new SoftLightBlendMode2(0.4),  // 16
  new SoftLightBlendMode2(1.31), // 17
  new SoftLightBlendMode2(0.291),// 18
  new LowPassBlendMode((ushort) (midtonesThreshold * maxVal),
                          (ushort) (midtonesThreshold * maxVal / 2)), // 19
  new HighPassBlendMode((ushort) (shadowsThreshold * maxVal),
                           (ushort) (shadowsThreshold * maxVal / 2)), // 20
  new BandBlendMode((ushort) (shadowsThreshold * maxVal),
                           (ushort) (shadowsThreshold * maxVal / 2),
                           (ushort) (hilightsThreshold * maxVal),
                           (ushort) (hilightsThreshold * maxVal / 2)),// 21
};

#include <stdio.h>
#include "../pixutils/HSB.h"

void blendLoop(ushort s1[], ushort s2[], ushort d[], byte m[],
               int bands, int s1bd, int s2bd,
               int s1LineOffset, int s2LineOffset, int dLineOffset, int mLineOffset,
               int s1LineStride, int s2LineStride, int dLineStride, int mLineStride,
               int s1PixelStride, int s2PixelStride, int dPixelStride, int mPixelStride,
               int dheight, int dwidth, int intOpacity, int mode, float colorSelection[])
{
    bool inverted = false;
    
    if (intOpacity < 0) {
        inverted = true;
        intOpacity = -intOpacity;
    }
    
    BlendMode *blender = BlendMode::getBlender(mode);
    const ushort maxVal = BlendMode::maxVal;
    
    float hueLower                  = colorSelection[0];
    float hueLowerFeather           = colorSelection[1];
    float hueUpper                  = colorSelection[2];
    float hueUpperFeather           = colorSelection[3];
    float brightnessLower           = colorSelection[4];
    float brightnessLowerFeather    = colorSelection[5];
    float brightnessUpper           = colorSelection[6];
    float brightnessUpperFeather    = colorSelection[7];
    
    int hueOffset = 0;
    
    if (hueLower < 0 || hueLower - hueLowerFeather < 0 || hueUpper < 0) {
        hueLower += 1;
        hueUpper += 1;
        hueOffset = 1;        
    } else if (hueLower > 1 || hueUpper + hueUpperFeather > 1 || hueUpper > 1) {
        hueOffset = -1;
    }
    
    const bool hasColorSelection = hueLower != 0 || hueUpper != 1 || brightnessLower != 0 || brightnessUpper != 1;
    
    for (int h = 0; h < dheight; h++) {
        int s1PixelOffset = s1LineOffset;
        int s2PixelOffset = s2LineOffset;
        int mPixelOffset = mLineOffset;
        int dPixelOffset = dLineOffset;
        
        s1LineOffset += s1LineStride;
        s2LineOffset += s2LineStride;
        mLineOffset += mLineStride;
        dLineOffset += dLineStride;
        
        for (int w = 0; w < dwidth; w++) {
            byte mValue = 0;
            if (m != NULL)
                mValue = inverted ? 0xFF - m[mPixelOffset] : m[mPixelOffset];
            else if (inverted)
                mValue = 1;
            
            ushort pixel[3] = {
                s2[s2PixelOffset],
                s2[s2PixelOffset+s2bd],
                s2[s2PixelOffset+2*s2bd],
            };
            
            float brightnessMask, colorMask = 1;
            
            if (hasColorSelection) {
                float rgb[3] = {
                    pixel[0]/(float)0xffff, pixel[1]/(float)0xffff, pixel[2]/(float)0xffff
                };
                float hsb[3];
                HSB::fromRGB(rgb, hsb);
                
                float hue = hsb[0];
                
                if (hueOffset == 1 && hue < hueLower - hueLowerFeather)
                    hue += 1;
                else if (hueOffset == -1 && hue < 0.5)
                    hue += 1;
                
                if (hue >= hueLower && hue <= hueUpper)
                    colorMask = 1;
                else if (hue >= (hueLower - hueLowerFeather) && hue < hueLower)
                    colorMask = (hue - (hueLower - hueLowerFeather))/hueLowerFeather;
                else if (hue > hueUpper && hue <= (hueUpper + hueUpperFeather))
                    colorMask = (hueUpper + hueUpperFeather - hue)/hueUpperFeather;
                else
                    colorMask = 0;
                
                float brightness = hsb[2];
                
                if (brightness >= brightnessLower && brightness <= brightnessUpper)
                    brightnessMask = 1;
                else if (brightness >= (brightnessLower - brightnessLowerFeather) && brightness < brightnessLower)
                    brightnessMask = (brightness - (brightnessLower - brightnessLowerFeather))/brightnessLowerFeather;
                else if (brightness > brightnessUpper && brightness <= (brightnessUpper + brightnessUpperFeather))
                    brightnessMask = (brightnessUpper + brightnessUpperFeather - brightness)/brightnessUpperFeather;
                else
                    brightnessMask = 0;
                
                colorMask *= brightnessMask;
            }
            
            for (int i = 0, s1b = 0, s2b = 0; i < bands; i++, s1b += s1bd, s2b += s2bd) {
                ushort s2Value = pixel[i];
                ushort value;
                if (m == NULL) {
                    ushort blended = blender->blendPixels(s1[s1PixelOffset + s1b], s2Value);
                    if (intOpacity == maxVal)
                        value = blended;
                    else
                        value = (intOpacity * blended + (maxVal - intOpacity) * s2Value) / (maxVal+1);
                } else {
                    if (mValue != 0) {
                        ushort blended = blender->blendPixels(s1[s1PixelOffset + s1b], s2Value);
                        int maskedOpacity = (intOpacity * mValue) / 0xFF;
                        value = (maskedOpacity * blended + (maxVal - maskedOpacity) * s2Value) / (maxVal+1);
                    } else
                        value = s2Value;
                }
                if (hasColorSelection)
                    d[dPixelOffset + i] = (int) (colorMask*value + (1-colorMask)*s2Value);
                else
                    d[dPixelOffset + i] = value;
            }
            
            s1PixelOffset += s1PixelStride;
            s2PixelOffset += s2PixelStride;
            mPixelOffset += mPixelStride;
            dPixelOffset += dPixelStride;
        }
    }
}

void blendLoop(ushort s1[], ushort s2[], ushort d[], byte m[], byte cs[],
               int bands, int s1bd, int s2bd,
               int s1LineOffset, int s2LineOffset, int dLineOffset, int mLineOffset, int csLineOffset,
               int s1LineStride, int s2LineStride, int dLineStride, int mLineStride, int csLineStride,
               int s1PixelStride, int s2PixelStride, int dPixelStride, int mPixelStride, int csPixelStride,
               int dheight, int dwidth, int intOpacity, int mode)
{
    bool inverted = false;
    
    if (intOpacity < 0) {
        inverted = true;
        intOpacity = -intOpacity;
    }
    
    BlendMode *blender = BlendMode::getBlender(mode);
    const ushort maxVal = BlendMode::maxVal;
    
    for (int h = 0; h < dheight; h++) {
        int s1PixelOffset = s1LineOffset;
        int s2PixelOffset = s2LineOffset;
        int mPixelOffset = mLineOffset;
        int csPixelOffset = csLineOffset;
        int dPixelOffset = dLineOffset;
        
        s1LineOffset += s1LineStride;
        s2LineOffset += s2LineStride;
        mLineOffset += mLineStride;
        csLineOffset += csLineStride;
        dLineOffset += dLineStride;
        
        for (int w = 0; w < dwidth; w++) {
            int mValue = 0xFF;
            if (m != NULL)
                mValue = inverted ? 0xFF - m[mPixelOffset] : m[mPixelOffset];
            if (cs != NULL)
                mValue = mValue * cs[csPixelOffset] / 0xFF;
            
            ushort pixel[3] = {
                s2[s2PixelOffset],
                s2[s2PixelOffset+s2bd],
                s2[s2PixelOffset+2*s2bd],
            };
                        
            for (int i = 0, s1b = 0, s2b = 0; i < bands; i++, s1b += s1bd, s2b += s2bd) {
                ushort s2Value = pixel[i];
                ushort value;
                if (m == NULL && cs == NULL) {
                    ushort blended = blender->blendPixels(s1[s1PixelOffset + s1b], s2Value);
                    if (intOpacity == maxVal)
                        value = blended;
                    else
                        value = (intOpacity * blended + (maxVal - intOpacity) * s2Value) / (maxVal+1);
                } else {
                    if (mValue != 0) {
                        ushort blended = blender->blendPixels(s1[s1PixelOffset + s1b], s2Value);
                        int maskedOpacity = (intOpacity * mValue) / 0xFF;
                        value = (maskedOpacity * blended + (maxVal - maskedOpacity) * s2Value) / (maxVal+1);
                    } else
                        value = s2Value;
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

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_PixelBlender_cUShortLoop
(JNIEnv *env, jclass cls,
 jshortArray s1, jshortArray s2, jshortArray d, jbyteArray m,
 jint bands, jint s1bd, jint s2bd,
 jint s1LineOffset, jint s2LineOffset, jint dLineOffset, jint mLineOffset,
 jint s1LineStride, jint s2LineStride, jint dLineStride, jint mLineStride,
 jint s1PixelStride, jint s2PixelStride, jint dPixelStride, jint mPixelStride,
 jint dheight, jint dwidth, jint jintOpacity, jint mode, jfloatArray jcolorSelection)
{
  ushort *cs1 = (ushort *) env->GetPrimitiveArrayCritical(s1, 0);
  ushort *cs2 = (ushort *) env->GetPrimitiveArrayCritical(s2, 0);
  ushort *cd = (ushort *) env->GetPrimitiveArrayCritical(d, 0);
  float *colorSelection = (float *) env->GetPrimitiveArrayCritical(jcolorSelection, 0);
  byte *cm = (m != NULL ? (byte *) env->GetPrimitiveArrayCritical(m, 0) : (byte *) NULL);

  blendLoop(cs1, cs2, cd, cm, bands, s1bd, s2bd,
            s1LineOffset, s2LineOffset, dLineOffset, mLineOffset,
            s1LineStride, s2LineStride, dLineStride, mLineStride,
            s1PixelStride, s2PixelStride, dPixelStride, mPixelStride,
            dheight, dwidth, jintOpacity, mode, colorSelection);

  env->ReleasePrimitiveArrayCritical(s1, cs1, 0);
  env->ReleasePrimitiveArrayCritical(s2, cs2, 0);
  env->ReleasePrimitiveArrayCritical(d, cd, 0);
  env->ReleasePrimitiveArrayCritical(jcolorSelection, colorSelection, 0);
  if (cm != NULL) env->ReleasePrimitiveArrayCritical(m, cm, 0);
}

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_PixelBlender_cUShortLoopCS
(JNIEnv *env, jclass cls,
 jshortArray s1, jshortArray s2, jshortArray d, jbyteArray m, jbyteArray cs,
 jint bands, jint s1bd, jint s2bd,
 jint s1LineOffset, jint s2LineOffset, jint dLineOffset, jint mLineOffset, jint csLineOffset,
 jint s1LineStride, jint s2LineStride, jint dLineStride, jint mLineStride, jint csLineStride,
 jint s1PixelStride, jint s2PixelStride, jint dPixelStride, jint mPixelStride, jint csPixelStride,
 jint dheight, jint dwidth, jint jintOpacity, jint mode)
{
    ushort *cs1 = (ushort *) env->GetPrimitiveArrayCritical(s1, 0);
    ushort *cs2 = (ushort *) env->GetPrimitiveArrayCritical(s2, 0);
    ushort *cd = (ushort *) env->GetPrimitiveArrayCritical(d, 0);
    byte *cm = (m != NULL ? (byte *) env->GetPrimitiveArrayCritical(m, 0) : (byte *) NULL);
    byte *ccs = (cs != NULL ? (byte *) env->GetPrimitiveArrayCritical(cs, 0) : (byte *) NULL);
    
    blendLoop(cs1, cs2, cd, cm, ccs, bands, s1bd, s2bd,
              s1LineOffset, s2LineOffset, dLineOffset, mLineOffset, csLineOffset,
              s1LineStride, s2LineStride, dLineStride, mLineStride, csLineStride,
              s1PixelStride, s2PixelStride, dPixelStride, mPixelStride, csPixelStride,
              dheight, dwidth, jintOpacity, mode);
    
    env->ReleasePrimitiveArrayCritical(s1, cs1, 0);
    env->ReleasePrimitiveArrayCritical(s2, cs2, 0);
    env->ReleasePrimitiveArrayCritical(d, cd, 0);
    if (cm != NULL) env->ReleasePrimitiveArrayCritical(m, cm, 0);
    if (ccs != NULL) env->ReleasePrimitiveArrayCritical(cs, ccs, 0);
}

