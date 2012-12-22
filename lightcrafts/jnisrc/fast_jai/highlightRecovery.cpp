/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 *  highlightRecovery.cpp
 *  
 *
 *  Created by Fabio Riccardi on 5/31/07.
 *  Copyright © 2007 Light Crafts, Inc.. All rights reserved.
 *
 */

#include <jni.h>
#include "vecUtils.h"
#include "../pixutils/HSB.h"

namespace hr {
    template <typename T>
    T min(T a, T b) {
        return a < b ? a : b;
    }
    
    template <typename T>
    ushort clampUShort(T x) {
        return x < 0 ? 0 : x > 0xffff ? 0xffff : (ushort) x;
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_HighlightRecoveryOpImage_intNativeUshortLoop
(JNIEnv *env, jclass cls,
 jshortArray jsrc, jshortArray jdst,
 jintArray jdstBandOffsets, jintArray jsrcBandOffsets,
 jint dstLineStride, jint srcLineStride,
 jint dstPixelStride, jint srcPixelStride,
 jint width, jint height,
 jfloatArray jpreMul, jfloatArray jcsMatrix, jfloatArray jwbMatrix)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrc, 0);
    ushort *dstData = (ushort *) env->GetPrimitiveArrayCritical(jdst, 0);
    int *dstBandOffsets = (int *) env->GetPrimitiveArrayCritical(jdstBandOffsets, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    float (*csMatrix)[3] = (float (*)[3]) env->GetPrimitiveArrayCritical(jcsMatrix, 0);
    float (*wbMatrix)[3] = (float (*)[3]) env->GetPrimitiveArrayCritical(jwbMatrix, 0);
    float *preMul = (float*) env->GetPrimitiveArrayCritical(jpreMul, 0);
    
    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];
    
    int dstROffset = dstBandOffsets[0];
    int dstGOffset = dstBandOffsets[1];
    int dstBOffset = dstBandOffsets[2];
    
    const int scale = 0x1000;
    
    int t00 = (int) (scale * csMatrix[0][0]), t01 = (int) (scale * csMatrix[0][1]), t02 = (int) (scale * csMatrix[0][2]),
        t10 = (int) (scale * csMatrix[1][0]), t11 = (int) (scale * csMatrix[1][1]), t12 = (int) (scale * csMatrix[1][2]),
        t20 = (int) (scale * csMatrix[2][0]), t21 = (int) (scale * csMatrix[2][1]), t22 = (int) (scale * csMatrix[2][2]);
    
    int w00 = (int) (scale * wbMatrix[0][0]), w01 = (int) (scale * wbMatrix[0][1]), w02 = (int) (scale * wbMatrix[0][2]),
        w10 = (int) (scale * wbMatrix[1][0]), w11 = (int) (scale * wbMatrix[1][1]), w12 = (int) (scale * wbMatrix[1][2]),
        w20 = (int) (scale * wbMatrix[2][0]), w21 = (int) (scale * wbMatrix[2][1]), w22 = (int) (scale * wbMatrix[2][2]);
    
    int pm[] = {(int) (scale * preMul[0]), (int) (scale * preMul[1]), (int) (scale * preMul[2])};
    
    const int threshold = (int) (0.8 * 0xffff);
    const int maximum = (int) (1 * 0xffff);
    
    int raw[3];
    float hsb[3];
    float rgb3[3];
    
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            int srcPixOffset = srcPixelStride * col + row * srcLineStride;
            int rr = raw[0] = srcData[srcPixOffset + srcROffset];
            int rg = raw[1] = srcData[srcPixOffset + srcGOffset];
            int rb = raw[2] = srcData[srcPixOffset + srcBOffset];
            
            int r = (t00 * rr + t01 * rg + t02 * rb)/scale;
            int g = (t10 * rr + t11 * rg + t12 * rb)/scale;
            int b = (t20 * rr + t21 * rg + t22 * rb)/scale;
            
            int max = 0;
            int sum = 0;
            int saturated = 0;
            for (int i = 0; i < 3; i++) {
                int val = hr::min((pm[i] * raw[i]) / scale, maximum);
                if (val > threshold) {
                    saturated++;
                    if (val > max)
                        max = val;
                }
                sum += val;
            }
            
            if (saturated > 0) {
                float m1 = (maximum - max) / (float) (maximum - threshold);
                float m2 = (maximum - sum/3) / (float) (maximum - threshold);
                float s = (maximum - sum/3) / (float) maximum;
                
                for (int i = 1; i < saturated; i++)
                    s *= s;
                
                float m = s * m2 + (1 - s) * m1;
                
                if (m < 1) {
                    rgb3[0] = r;
                    rgb3[1] = g;
                    rgb3[2] = b;
                    HSB::fromRGB(rgb3, hsb);
                    hsb[1] *= m; // Math.sqrt(m);
                    HSB::toRGB(hsb, rgb3);
                    r = (int) rgb3[0];
                    g = (int) rgb3[1];
                    b = (int) rgb3[2];
                }
            }
            
            int wbr = (w00 * r + w01 * g + w02 * b)/scale;
            int wbg = (w10 * r + w11 * g + w12 * b)/scale;
            int wbb = (w20 * r + w21 * g + w22 * b)/scale;
            r = wbr; g = wbg; b = wbb;
            
            int dstPixOffset = dstPixelStride * col + row * dstLineStride;
            dstData[dstPixOffset + dstROffset] = hr::clampUShort(r);
            dstData[dstPixOffset + dstGOffset] = hr::clampUShort(g);
            dstData[dstPixOffset + dstBOffset] = hr::clampUShort(b);
        }
    }    
    
    env->ReleasePrimitiveArrayCritical(jpreMul, preMul, 0);
    env->ReleasePrimitiveArrayCritical(jwbMatrix, wbMatrix, 0);
    env->ReleasePrimitiveArrayCritical(jcsMatrix, csMatrix, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jdstBandOffsets, dstBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jsrc, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdst, dstData, 0);
}

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_HighlightRecoveryOpImage_floatNativeUshortLoop
(JNIEnv *env, jclass cls,
 jshortArray jsrc, jshortArray jdst,
 jintArray jdstBandOffsets, jintArray jsrcBandOffsets,
 jint dstLineStride, jint srcLineStride,
 jint dstPixelStride, jint srcPixelStride,
 jint width, jint height,
 jfloatArray jpreMul, jfloatArray jcsMatrix, jfloatArray jwbMatrix)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrc, 0);
    ushort *dstData = (ushort *) env->GetPrimitiveArrayCritical(jdst, 0);
    int *dstBandOffsets = (int *) env->GetPrimitiveArrayCritical(jdstBandOffsets, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    float (*csMatrix)[3] = (float (*)[3]) env->GetPrimitiveArrayCritical(jcsMatrix, 0);
    float (*wbMatrix)[3] = (float (*)[3]) env->GetPrimitiveArrayCritical(jwbMatrix, 0);
    float *preMul = (float*) env->GetPrimitiveArrayCritical(jpreMul, 0);
    
    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];
    
    int dstROffset = dstBandOffsets[0];
    int dstGOffset = dstBandOffsets[1];
    int dstBOffset = dstBandOffsets[2];
        
    const float threshold = 0.8 * 0xffff;
    const float maximum = 1.0 * 0xffff;
    
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            const int srcPixOffset = srcPixelStride * col + row * srcLineStride;

            float raw[3] = {
                srcData[srcPixOffset + srcROffset],
                srcData[srcPixOffset + srcGOffset],
                srcData[srcPixOffset + srcBOffset]
            };
            
            float rgb[3] = {0, 0, 0};
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    rgb[i] += csMatrix[i][j] * raw[j];
            
            float max = 0;
            float sum = 0;
            float saturated = 0;
            for (int i = 0; i < 3; i++) {
                float val = hr::min(preMul[i] * raw[i], maximum);
                if (val > threshold) {
                    saturated++;
                    if (val > max)
                        max = val;
                }
                sum += val;
            }
            
            if (saturated > 0) {
                float m1 = (maximum - max) / (float) (maximum - threshold);
                float m2 = (maximum - sum/3) / (float) (maximum - threshold);
                float s = (maximum - sum/3) / (float) maximum;
                
                for (int i = 1; i < saturated; i++)
                    s *= s;
                
                float m = s * m2 + (1 - s) * m1;
                
                if (m < 1) {
                    float hsb[3];                    
                    HSB::fromRGB(rgb, hsb);
                    hsb[1] *= m;
                    HSB::toRGB(hsb, rgb);
                }
            }

            float wrgb[3] = {0, 0, 0};
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    wrgb[i] += wbMatrix[i][j] * rgb[j];
            
            int dstPixOffset = dstPixelStride * col + row * dstLineStride;
            for (int i = 0; i < 3; i++)
                dstData[dstPixOffset + dstBandOffsets[i]] = hr::clampUShort(wrgb[i]);
        }
    }    
    
    env->ReleasePrimitiveArrayCritical(jpreMul, preMul, 0);
    env->ReleasePrimitiveArrayCritical(jwbMatrix, wbMatrix, 0);
    env->ReleasePrimitiveArrayCritical(jcsMatrix, csMatrix, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jdstBandOffsets, dstBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jsrc, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdst, dstData, 0);
}

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_HighlightRecoveryOpImage_vecNativeUshortLoop
(JNIEnv *env, jclass cls,
 jshortArray jsrc, jshortArray jdst,
 jintArray jdstBandOffsets, jintArray jsrcBandOffsets,
 jint dstLineStride, jint srcLineStride,
 jint dstPixelStride, jint srcPixelStride,
 jint width, jint height,
 jfloatArray jpreMul, jfloatArray jcsMatrix, jfloatArray jwbMatrix)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrc, 0);
    ushort *dstData = (ushort *) env->GetPrimitiveArrayCritical(jdst, 0);
    int *dstBandOffsets = (int *) env->GetPrimitiveArrayCritical(jdstBandOffsets, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    float (*csMatrix)[3] = (float (*)[3]) env->GetPrimitiveArrayCritical(jcsMatrix, 0);
    float (*wbMatrix)[3] = (float (*)[3]) env->GetPrimitiveArrayCritical(jwbMatrix, 0);
    float *preMul = (float*) env->GetPrimitiveArrayCritical(jpreMul, 0);
    
    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];
    
    int dstROffset = dstBandOffsets[0];
    int dstGOffset = dstBandOffsets[1];
    int dstBOffset = dstBandOffsets[2];
    
    const float threshold = 0.8 * 0xffff;
    const float maximum = 1.0 * 0xffff;

    vfloat t0 = vfloat::fill<0>(); for (int i = 0; i < 3; i++) t0[i] = csMatrix[i][0];
    vfloat t1 = vfloat::fill<0>(); for (int i = 0; i < 3; i++) t1[i] = csMatrix[i][1];
    vfloat t2 = vfloat::fill<0>(); for (int i = 0; i < 3; i++) t2[i] = csMatrix[i][2];
    
    vfloat wt0 = vfloat::fill<0>(); for (int i = 0; i < 3; i++) wt0[i] = wbMatrix[i][0];
    vfloat wt1 = vfloat::fill<0>(); for (int i = 0; i < 3; i++) wt1[i] = wbMatrix[i][1];
    vfloat wt2 = vfloat::fill<0>(); for (int i = 0; i < 3; i++) wt2[i] = wbMatrix[i][2];
    
    const float mv[4] = {0xffff, 0xffff, 0xffff, 0xffff};
    const vfloat maxVal(loadu((float *) mv));

    const float th[4] = {threshold, threshold, threshold, threshold};
    const vfloat vthreshold(loadu((float *) th));
    
    const vfloat vpremul(loadu(preMul));
    
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            const int srcPixOffset = srcPixelStride * col + row * srcLineStride;
            
            vushort pix(loadu(&srcData[srcPixOffset + srcROffset]));
#if defined( __SSE__ )
            const vfloat vraw = mmx::cvt<vfloat, vint>(data_cast<vint>(mmx::unpacklo(pix, vushort::fill<0>())));
#else
            const vfloat vraw = altivec::ctf<0>(data_cast<vint>(altivec::mergeh(vushort::fill<0>(), pix)));
#endif
            vfloat vrgb = t0 * SPLAT(vraw, 0);
            vrgb = vrgb + t1 * SPLAT(vraw, 1);
            vrgb = vrgb + t2 * SPLAT(vraw, 2);

            const vfloat vval = vmin(vpremul * vraw, maxVal);
            VECALIGN(float val[4]);
            vstore(val, vval);
                        
            const vint vcmp(data_cast<vint>(cmpgt(vval, vthreshold)));            
            VECALIGN(int cmp[4]);
            vstore(cmp, vcmp);
            
            float max = 0;
            float sum = 0;
            float saturated = 0;
            for (int i = 0; i < 3; i++) {
                if (cmp[i] != 0) {
                    saturated++;
                    if (val[i] > max)
                        max = val[i];
                }
                sum += val[i];
            }
            
            if (saturated > 0) {
                float m1 = (maximum - max) / (float) (maximum - threshold);
                float m2 = (maximum - sum/3) / (float) (maximum - threshold);
                float s = (maximum - sum/3) / (float) maximum;
                
                for (int i = 1; i < saturated; i++)
                    s *= s;
                
                float m = s * m2 + (1 - s) * m1;
                
                if (m < 1) {
                    float hsb[3];
                    VECALIGN(float rgb[4]);
                    vstore(rgb, vrgb);
                    HSB::fromRGB(rgb, hsb);
                    hsb[1] *= m;
                    HSB::toRGB(hsb, rgb);
                    vrgb = load((float *) rgb);
                }
            }

            vfloat wbrgb = wt0 * SPLAT(vrgb, 0);
            wbrgb = wbrgb + wt1 * SPLAT(vrgb, 1);
            wbrgb = wbrgb + wt2 * SPLAT(vrgb, 2);
                        
            wbrgb = vmin(vmax(wbrgb, vfloat::fill<0>()), maxVal);
            
#if defined( __SSE__ )
            const vint irgb(mmx::cvtt<vint, vfloat>(wbrgb));
            VECALIGN(int wrgb[4]);
            vstore(wrgb, irgb);
#else
            const vuint irgb(altivec::ctu<0>(wbrgb));
            const vushort usrgb(altivec::pack(irgb, irgb));
            VECALIGN(ushort wrgb[8]);
            vstore(wrgb, usrgb);
#endif

            int dstPixOffset = dstPixelStride * col + row * dstLineStride;
            dstData[dstPixOffset + dstROffset] = wrgb[0];
            dstData[dstPixOffset + dstGOffset] = wrgb[1];
            dstData[dstPixOffset + dstBOffset] = wrgb[2];
        }
    }    
    
    env->ReleasePrimitiveArrayCritical(jpreMul, preMul, 0);
    env->ReleasePrimitiveArrayCritical(jwbMatrix, wbMatrix, 0);
    env->ReleasePrimitiveArrayCritical(jcsMatrix, csMatrix, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jdstBandOffsets, dstBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jsrc, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdst, dstData, 0);
}
