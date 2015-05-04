/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <jni.h>

#ifndef AUTO_DEP
extern "C" {
#include "javah/com_lightcrafts_jai_opimage_HDROpImage2.h"
}
#endif

typedef unsigned char byte;
typedef unsigned short ushort;

#include <math.h>
#include <omp.h>

namespace hdr {
    template <typename T>
    T min(T a, T b) {
        return a < b ? a : b;
    }
    
    template <typename T>
    ushort clampUShort(T x) {
        return x < 0 ? 0 : x > 0xffff ? 0xffff : (ushort) x;
    }

#if 0 // !defined(__ppc__)
    float shift23=(1<<23);
    float OOshift23=1.0/(1<<23);

    float log2(float i) {
        float LogBodge=0.346607f;
        float x;
        float y;
        x=*(int *)&i;
        x*= OOshift23; //1/pow(2,23);
        x=x-127;
        
        y=x-floorf(x);
        y=(y-y*y)*LogBodge;
        return x+y;
	}
    
    float pow2(float i) {
        float PowBodge=0.33971f;
        float x;
        float y=i-floorf(i);
        y=(y-y*y)*PowBodge;
        
        x=i+127-y;
        x*= shift23; //pow(2,23);
        *(int*)&x=(int)x;
        return x;
	}
    float pow(float a, float b)
	{
        return pow2(b*log2(a));
	}
#else
    float pow(float a, float b)
	{
        return ::powf(a, b);
	}
#endif
}

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

#pragma omp parallel for
    for (int row = 0; row < dstheight; row++) {
        for (int col = 0; col < dstwidth; col++) {
            int r = srcData[srcPixelStride * col + row * srcLineStride + srcROffset];
            int g = srcData[srcPixelStride * col + row * srcLineStride + srcGOffset];
            int b = srcData[srcPixelStride * col + row * srcLineStride + srcBOffset];

            float m = maskData[maskPixelStride * col + row * maskLineStride + maskOffset1] / (float) 0xffff;
            // if (maskBandOffsets.length == 3) {
                float um = maskData[maskPixelStride * col + row * maskLineStride + maskOffset2] / (float) 0xffff;
                um = hdr::min(um*um, 1.0f);

                float bm = maskData[maskPixelStride * col + row * maskLineStride + maskOffset3] / (float) 0xffff;
                m = um * m + (1-um) * bm;
            // }

            float y = (wr * r + wg * g + wb * b) / 0xffff;

            float mm = hdr::pow(m, 1/shadows) * hdr::pow(y/m, detail);

            float compressedHilights = softLightBlendPixels(1 - m, y);

            float ratio = (compressedHilights * highlights + (1-highlights)) * mm / y;

            r *= ratio;
            g *= ratio;
            b *= ratio;

            dstData[dstPixelStride * col + row * dstLineStride + dstROffset] = hdr::clampUShort(r);
            dstData[dstPixelStride * col + row * dstLineStride + dstGOffset] = hdr::clampUShort(g);
            dstData[dstPixelStride * col + row * dstLineStride + dstBOffset] = hdr::clampUShort(b);
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jmaskData, maskData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jmaskBandOffsets, maskBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jdstBandOffsets, dstBandOffsets, 0);
}
