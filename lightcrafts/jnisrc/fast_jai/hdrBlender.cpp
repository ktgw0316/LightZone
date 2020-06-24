/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <jni.h>

#ifndef AUTO_DEP
extern "C" {
#include "javah/com_lightcrafts_jai_opimage_HDROpImage2.h"
}
#endif

typedef unsigned char byte;
typedef unsigned short ushort;

#include "mathlz.h"
#include <algorithm>
#include <math.h>
#include <omp.h>

static double softLightBlendPixels(double front, double back) {
    double m = front * back;
    return (1 - back) * m * m;
}

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_HDROpImage2_cBlendLoop
  (JNIEnv *env, jclass cls, jshortArray jsrcData, jshortArray jmaskData, jshortArray jdstData,
  jintArray jsrcBandOffsets, jintArray jmaskBandOffsets, jintArray jdstBandOffsets,
  jint dstwidth, jint dstheight, jint srcLineStride, jint srcPixelStride,
  jint maskLineStride, jint maskPixelStride, jint dstLineStride, jint dstPixelStride,
  jfloat shadows, jfloat detail, jfloat highlights, jfloat wr, jfloat wg, jfloat wb)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    ushort *maskData = (ushort *) env->GetPrimitiveArrayCritical(jmaskData, 0);
    ushort *dstData = (ushort *) env->GetPrimitiveArrayCritical(jdstData, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    int *maskBandOffsets = (int *) env->GetPrimitiveArrayCritical(jmaskBandOffsets, 0);
    int *dstBandOffsets = (int *) env->GetPrimitiveArrayCritical(jdstBandOffsets, 0);

    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];

    int maskOffset1, maskOffset2, maskOffset3;

    // if (maskBandOffsets.length == 3) {
        maskOffset1 = maskBandOffsets[0];
        maskOffset2 = maskBandOffsets[1];
        maskOffset3 = maskBandOffsets[2];
//    } else
//        maskOffset1 = maskOffset2 = maskOffset3 = maskBandOffsets[0];

    int dstROffset = dstBandOffsets[0];
    int dstGOffset = dstBandOffsets[1];
    int dstBOffset = dstBandOffsets[2];

#if _OPENMP < 201307
#pragma omp parallel for
#else
#pragma omp parallel for simd
#endif
    for (int row = 0; row < dstheight; row++) {
        for (int col = 0; col < dstwidth; col++) {
            int r = srcData[srcPixelStride * col + row * srcLineStride + srcROffset];
            int g = srcData[srcPixelStride * col + row * srcLineStride + srcGOffset];
            int b = srcData[srcPixelStride * col + row * srcLineStride + srcBOffset];

            float m = maskData[maskPixelStride * col + row * maskLineStride + maskOffset1] / (float) 0xffff;
            // if (maskBandOffsets.length == 3) {
                float um = maskData[maskPixelStride * col + row * maskLineStride + maskOffset2] / (float) 0xffff;
                um = std::min(um*um, 1.0f);

                float bm = maskData[maskPixelStride * col + row * maskLineStride + maskOffset3] / (float) 0xffff;
                m = um * m + (1-um) * bm;
            // }

            float y = (wr * r + wg * g + wb * b) / 0xffff;

            float mm = powf(m, 1/shadows) * powf(y/m, detail);

            float compressedHilights = softLightBlendPixels(1 - m, y);

            float ratio = (compressedHilights * highlights + (1-highlights)) * mm / y;

            r *= ratio;
            g *= ratio;
            b *= ratio;

            dstData[dstPixelStride * col + row * dstLineStride + dstROffset] = clampUShort(r);
            dstData[dstPixelStride * col + row * dstLineStride + dstGOffset] = clampUShort(g);
            dstData[dstPixelStride * col + row * dstLineStride + dstBOffset] = clampUShort(b);
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jmaskData, maskData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jmaskBandOffsets, maskBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jdstBandOffsets, dstBandOffsets, 0);
}
