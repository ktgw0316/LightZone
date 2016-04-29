/* Copyright (C) 2005-2011 Fabio Riccardi */

typedef unsigned char byte;
typedef unsigned short ushort;

#include <stdlib.h>
#include <math.h>
#include <omp.h>
#include "../include/mathlz.h"

#include <jni.h>

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_RGBColorSelectionMaskOpImage.h"
#endif

JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_RGBColorSelectionMaskOpImage_nativeUshortLoop
(JNIEnv *env, jobject cls, jshortArray jsrcData, jbyteArray jdstData,
 jint width, jint height, jintArray jsrcBandOffsets,
 jint dstOffset, jint srcLineStride, jint dstLineStride,
 jfloatArray jcolorSelection, jboolean inverted)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    byte *dstData = (byte *) env->GetPrimitiveArrayCritical(jdstData, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    float *colorSelection = (float *) env->GetPrimitiveArrayCritical(jcolorSelection, 0);

    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];

    float sL                        = colorSelection[0];
    float sa                        = colorSelection[1];
    float sb                        = colorSelection[2];
    float radius                    = colorSelection[3];
    float luminosityLower           = colorSelection[4];
    float luminosityLowerFeather    = colorSelection[5];
    float luminosityUpper           = colorSelection[6];
    float luminosityUpperFeather    = colorSelection[7];

#if _OPENMP < 201307
#pragma omp parallel for schedule (guided)
#else
#pragma omp parallel for simd schedule (guided)
#endif
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            float L = srcData[3 * col + row * srcLineStride + srcROffset];
            float a = srcData[3 * col + row * srcLineStride + srcGOffset] / (float) 0xffff;
            float b = srcData[3 * col + row * srcLineStride + srcBOffset] / (float) 0xffff;

            float brightnessMask, colorMask;

            if (radius >= 0) {
                const float rmin = 3 * radius / 16;
                const float rmax = 5 * radius / 16;

                float da = sa - a;
                float db = sb - b;
                float m = da * da + db * db;
                m = m * inv_sqrt(m);
                if (m < rmin)
                    colorMask = 1;
                else if (m < rmax)
                    colorMask = (rmax - m) / (rmax - rmin);
                else
                    colorMask = 0;
            } else
                colorMask = 1;

            if (luminosityLower > 0 || luminosityUpper < 1) {
#if defined(__ppc__)
	            float luminosity = log2f(L / 0x100 + 1)/8;
#else
                float luminosity = fast_log2(L / 256.0F + 1.0F)/8;
#endif
                if (luminosity > 1)
                    luminosity = 1;

                if (luminosity >= luminosityLower && luminosity <= luminosityUpper)
                    brightnessMask = 1;
                else if (luminosity >= (luminosityLower - luminosityLowerFeather) && luminosity < luminosityLower)
                    brightnessMask = (luminosity - (luminosityLower - luminosityLowerFeather))/luminosityLowerFeather;
                else if (luminosity > luminosityUpper && luminosity <= (luminosityUpper + luminosityUpperFeather))
                    brightnessMask = (luminosityUpper + luminosityUpperFeather - luminosity)/luminosityUpperFeather;
                else
                    brightnessMask = 0;

                colorMask *= brightnessMask;
            }

            if (inverted)
                colorMask = 1-colorMask;

            dstData[col + row * dstLineStride + dstOffset] = (byte) (0xff * colorMask);
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jcolorSelection, colorSelection, 0);
}
