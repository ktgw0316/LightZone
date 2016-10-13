/* Copyright (C) 2005-2011 Fabio Riccardi */

typedef unsigned char byte;
typedef unsigned short ushort;

#include <math.h>
#include <stdlib.h>
#include <omp.h>
#include "../include/mathlz.h"

static constexpr int sMath_scale = 0x8000;
static constexpr int sMath_PI = (int) (sMath_scale * M_PI);
static const int sqrt3d2 = (int) (sMath_scale * sqrt(3.0) / 2); // 0.866...

static int arctan2(int y, int x) {
    constexpr int coeff_1 = sMath_PI / 4;
    constexpr int coeff_2 = 3 * coeff_1;
    const int abs_y = abs(y) + 1;      // kludge to prevent 0/0 condition
    int angle;

    if (x >= 0) {
        int r = (sMath_scale * (x - abs_y)) / (x + abs_y);
        angle = coeff_1 - coeff_1 * r / sMath_scale;
    } else {
        int r = (sMath_scale * (x + abs_y)) / (abs_y - x);
        angle = coeff_2 - coeff_1 * r / sMath_scale;
    }

    return y < 0 ? -angle : angle;
}

static int hue(int r, int g, int b) {
    int x = r - (g+b) / 2;
    int y = (sqrt3d2 * (g-b)) / sMath_scale;
    int hue = arctan2(y, x);
    if (hue < 0)
        hue += 2 * sMath_PI;
    return hue;
}

static float arctan2(float y, float x) {
    constexpr float coeff_1 = (float) M_PI / 4;
    constexpr float coeff_2 = 3 * coeff_1;
    const float abs_y = fabs(y) + 1e-10f;      // kludge to prevent 0/0 condition
    float angle;

    if (x >= 0) {
        float r = (x - abs_y) / (x + abs_y);
        angle = coeff_1 - coeff_1 * r;
    } else {
        float r = (x + abs_y) / (abs_y - x);
        angle = coeff_2 - coeff_1 * r;
    }

    return y < 0 ? -angle : angle;
}

static float hue(float r, float g, float b) {
    float x = r - (g+b) / 2;
    float y = ((g-b) * sqrt(3.0) / 2);
    float hue = arctan2(y, x);
    if (hue < 0)
        hue += 2 * M_PI;
    return hue;
}

#include <jni.h>

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_ColorSelectionMaskOpImage.h"
#endif

JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_ColorSelectionMaskOpImage_nativeUshortLoop
(JNIEnv *env, jobject cls, jshortArray jsrcData, jbyteArray jdstData,
 jint width, jint height, jintArray jsrcBandOffsets,
 jint dstOffset, jint srcLineStride, jint dstLineStride,
 jfloatArray jcolorSelection, jfloat wr, jfloat wg, jfloat wb)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    byte *dstData = (byte *) env->GetPrimitiveArrayCritical(jdstData, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    float *colorSelection = (float *) env->GetPrimitiveArrayCritical(jcolorSelection, 0);

    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];

    float hueLower                  = colorSelection[0];
    float hueLowerFeather           = colorSelection[1];
    float hueUpper                  = colorSelection[2];
    float hueUpperFeather           = colorSelection[3];
    float luminosityLower           = colorSelection[4];
    float luminosityLowerFeather    = colorSelection[5];
    float luminosityUpper           = colorSelection[6];
    float luminosityUpperFeather    = colorSelection[7];

    int hueOffset = 0;

    if (hueLower < 0 || hueLower - hueLowerFeather < 0 || hueUpper < 0) {
        hueLower += 1;
        hueUpper += 1;
        hueOffset = 1;
    } else if (hueLower > 1 || hueUpper + hueUpperFeather > 1 || hueUpper > 1) {
        hueOffset = -1;
    }

#if _OPENMP < 201307
#pragma omp parallel for schedule (guided)
#else
#pragma omp parallel for simd schedule (guided)
#endif
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            float r = srcData[3 * col + row * srcLineStride + srcROffset];
            float g = srcData[3 * col + row * srcLineStride + srcGOffset];
            float b = srcData[3 * col + row * srcLineStride + srcBOffset];

            // float hue = hue(r / (float) 0xffff, g / (float) 0xffff, b / (float) 0xffff) / (float) (2 * Math.PI);

            float cmax = (r > g) ? r : g;
            if (b > cmax) cmax = b;
            float cmin = (r < g) ? r : g;
            if (b < cmin) cmin = b;

            float saturation;
            if (cmax != 0)
                saturation = (cmax - cmin) / cmax;
            else
                saturation = 0;

#if defined(__ppc__)
            float luminosity = (float) (log1pf((wr * r + wg * g + wb * b)/0x100) / (8 * logf(2)));
#else
            float luminosity = (float) (fast_log2((wr * r + wg * g + wb * b)/0x100) / 8);
#endif
            float luminosityMask, colorMask;

            const float stmin = 0.01f;
            const float stmax = 0.02f;

            const float ltmin = .01;
            const float ltmax = .02;

            if (saturation > stmin && luminosity > ltmin) {
                float h = hue(r, g, b) / (float) (2 * M_PI);

                if (hueOffset == 1 && h < hueLower - hueLowerFeather)
                    h += 1;
                else if (hueOffset == -1 && h < 0.5)
                    h += 1;

                if (h >= hueLower && h <= hueUpper)
                    colorMask = 1;
                else if (h >= (hueLower - hueLowerFeather) && h < hueLower)
                    colorMask = (h - (hueLower - hueLowerFeather))/hueLowerFeather;
                else if (h > hueUpper && h <= (hueUpper + hueUpperFeather))
                    colorMask = (hueUpper + hueUpperFeather - h)/hueUpperFeather;
                else
                    colorMask = 0;

                if (saturation < stmax)
                    colorMask *= (saturation - stmin) / (stmax - stmin);

                if (luminosity < ltmax)
                    colorMask *= (luminosity - ltmin) / (ltmax - ltmin);
            } else
                colorMask = 0;

            if (luminosity >= luminosityLower && luminosity <= luminosityUpper)
                luminosityMask = 1;
            else if (luminosity >= (luminosityLower - luminosityLowerFeather) && luminosity < luminosityLower)
                luminosityMask = (luminosity - (luminosityLower - luminosityLowerFeather))/luminosityLowerFeather;
            else if (luminosity > luminosityUpper && luminosity <= (luminosityUpper + luminosityUpperFeather))
                luminosityMask = (luminosityUpper + luminosityUpperFeather - luminosity)/luminosityUpperFeather;
            else
                luminosityMask = 0;

            colorMask *= luminosityMask;

            dstData[col + row * dstLineStride + dstOffset] = (byte) (0xff * colorMask);
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jcolorSelection, colorSelection, 0);
}
