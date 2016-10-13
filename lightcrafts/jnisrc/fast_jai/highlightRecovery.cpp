/*
 *  highlightRecovery.cpp
 *
 *
 *  Created by Fabio Riccardi on 5/31/07.
 *  Copyright (C) 2007-2010 Light Crafts, Inc.. All rights reserved.
 *
 */

#include <jni.h>
#include <algorithm>
#include <math.h>
#include <omp.h>
#include "../include/mathlz.h"
#include "../pixutils/HSB.h"

typedef unsigned short ushort;

#ifdef __INTEL_COMPILER
#include <fvec.h>
#include <dvec.h>
#include <pmmintrin.h>
inline F32vec4 convert_high(const Iu16vec8 &a) {
    return _mm_cvtepi32_ps(unpack_high(a, (__m128i)_mm_setzero_ps()));
}

inline F32vec4 convert_low(const Iu16vec8 &a) {
    return _mm_cvtepi32_ps(unpack_low(a, (__m128i)_mm_setzero_ps()));
}

// convert two F32vec4 to a Iu16vec8: deal with the (signed) saturating nature of _mm_packs_epi32
inline Iu16vec8 F32vec4toIu16vec8(const F32vec4 &hi, const F32vec4 &lo) {
    const Iu32vec4 sign_swap32(0x8000, 0x8000, 0x8000, 0x8000);
    const Iu16vec8 sign_swap16(0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000);

    return Iu16vec8(_mm_packs_epi32(_mm_cvtps_epi32(lo)-sign_swap32, _mm_cvtps_epi32(hi)-sign_swap32) ^ sign_swap16);
}

#define CONST_INT32_PS(N, V3,V2,V1,V0) \
static const _MM_ALIGN16 int _##N[]= \
{V0, V1, V2, V3};/*little endian!*/ \
const F32vec4 N = _mm_load_ps((float*)_##N);

// usage example, mask for elements 3 and 1:
// CONST_INT32_PS(mask31, ~0, 0, ~0, 0);

// Convert three vectors of interleaved data into three vectors of segregated data
inline void XYZtoF32vec4(F32vec4& x, F32vec4& y, F32vec4& z, const F32vec4& a, const F32vec4& b, const F32vec4& c) {
    CONST_INT32_PS(mask1,   0,  0, ~0,  0);
    CONST_INT32_PS(mask2,   0, ~0,  0,  0);
    CONST_INT32_PS(mask30, ~0,  0,  0, ~0);

    x = (a & mask30) | (b & mask2) | (c & mask1);
    y = (a & mask1) | (b & mask30) | (c & mask2);
    z = (a & mask2) | (b & mask1) | (c & mask30);
    x = _mm_shuffle_ps(x, x, _MM_SHUFFLE(1,2,3,0));
    y = _mm_shuffle_ps(y, y, _MM_SHUFFLE(2,3,0,1));
    z = _mm_shuffle_ps(z, z, _MM_SHUFFLE(3,0,1,2));
}

inline void F32vec4toXYZ(F32vec4& a, F32vec4& b, F32vec4& c, const F32vec4& x, const F32vec4& y, const F32vec4& z) {
    CONST_INT32_PS(mask1,   0,  0, ~0,  0);
    CONST_INT32_PS(mask2,   0, ~0,  0,  0);
    CONST_INT32_PS(mask30, ~0,  0,  0, ~0);

    F32vec4 ta = _mm_shuffle_ps(x, x, _MM_SHUFFLE(1,2,3,0));
    F32vec4 tb = _mm_shuffle_ps(y, y, _MM_SHUFFLE(2,3,0,1));
    F32vec4 tc = _mm_shuffle_ps(z, z, _MM_SHUFFLE(3,0,1,2));

    a = (ta & mask30) | (tb & mask1) | (tc & mask2);
    b = (ta & mask2) | (tb & mask30) | (tc & mask1);
    c = (ta & mask1) | (tb & mask2) | (tc & mask30);
}

inline void load_vector(const unsigned short * const srcData, F32vec4 v_rgb[2][3]) {
    const F32vec4 v_inv_norm(1.0f/0xffff);

    Iu16vec8 src8_1((__m128i) _mm_lddqu_si128((__m128i *) srcData));      // G2 R2 B1 G1 R1 B0 G0 R0
    Iu16vec8 src8_2((__m128i) _mm_lddqu_si128((__m128i *) (srcData+8)));    // R5 B4 G4 R4 B3 G3 R3 B2
    Iu16vec8 src8_3((__m128i) _mm_lddqu_si128((__m128i *) (srcData+16)));   // B7 G7 R7 B6 G6 R6 B5 G5

    // get the first three F32vec4
    F32vec4 src4_1 = convert_low(src8_1);   // R1 B0 G0 R0 -> a1
    F32vec4 src4_2 = convert_high(src8_1);  // G2 R2 B1 G1 -> b1
    F32vec4 src4_3 = convert_low(src8_2);   // B3 G3 R3 B2 -> c1

    F32vec4 src4_4 = convert_high(src8_2);  // R5 B4 G4 R4 -> a2
    F32vec4 src4_5 = convert_low(src8_3);   // G6 R6 B5 G5 -> b2
    F32vec4 src4_6 = convert_high(src8_3);  // B7 G7 R7 B6 -> c2

    XYZtoF32vec4(v_rgb[0][0], v_rgb[0][1], v_rgb[0][2], src4_1, src4_2, src4_3);
    XYZtoF32vec4(v_rgb[1][0], v_rgb[1][1], v_rgb[1][2], src4_4, src4_5, src4_6);
}

inline void store_vector(unsigned short * const dstData, const F32vec4 v_rgb[2][3]) {
    const F32vec4 v_ffff((float) 0xffff);
    const F32vec4 v_zero(0.0f);

    F32vec4 a1, b1, c1;

    F32vec4toXYZ(a1, b1, c1, v_rgb[0][0], v_rgb[0][1], v_rgb[0][2]);

    // no need to clamp to [0..0xffff], F32vec4toIu16vec8 does it automagically

    /* a1 = simd_max(v_zero, simd_min(a1, v_ffff));
    b1 = simd_max(v_zero, simd_min(b1, v_ffff));
    c1 = simd_max(v_zero, simd_min(c1, v_ffff)); */

    F32vec4 a2, b2, c2;

    F32vec4toXYZ(a2, b2, c2, v_rgb[1][0], v_rgb[1][1], v_rgb[1][2]);

    /* a2 = simd_max(v_zero, simd_min(a2, v_ffff));
    b2 = simd_max(v_zero, simd_min(b2, v_ffff));
    c2 = simd_max(v_zero, simd_min(c2, v_ffff)); */

    _mm_storeu_si128((__m128i *) dstData, F32vec4toIu16vec8(b1, a1));       // G2 R2 B1 G1 R1 B0 G0 R0
    _mm_storeu_si128((__m128i *) (dstData+8), F32vec4toIu16vec8(a2, c1));   // R5 B4 G4 R4 B3 G3 R3 B2
    _mm_storeu_si128((__m128i *) (dstData+16), F32vec4toIu16vec8(c2, b2));  // B7 G7 R7 B6 G6 R6 B5 G5
}

// Ad Horizontal using SSE3
inline float addh(const F32vec4 &a) {
    return _mm_cvtss_f32(_mm_hadd_ps(_mm_hadd_ps(a, _mm_setzero_ps()), _mm_setzero_ps()));
}

#endif

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_HighlightRecoveryOpImage_floatNativeUshortLoop
(JNIEnv *env, jclass cls,
 jshortArray jsrc, jshortArray jdst,
 jintArray jdstBandOffsets, jintArray jsrcBandOffsets,
 jint dstLineStride, jint srcLineStride,
 jint dstPixelStride, jint srcPixelStride,
 jint width, jint height,
 jfloatArray jpreMul, jfloatArray jcsMatrix)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrc, 0);
    ushort *dstData = (ushort *) env->GetPrimitiveArrayCritical(jdst, 0);
    int *dstBandOffsets = (int *) env->GetPrimitiveArrayCritical(jdstBandOffsets, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    float (*csMatrix)[3] = (float (*)[3]) env->GetPrimitiveArrayCritical(jcsMatrix, 0);
    float *preMul = (float*) env->GetPrimitiveArrayCritical(jpreMul, 0);

    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];

    int dstROffset = dstBandOffsets[0];
    int dstGOffset = dstBandOffsets[1];
    int dstBOffset = dstBandOffsets[2];

    const float threshold = 0.8 * 0xffff;
    const float maximum = 1.0 * 0xffff;

#ifdef __INTEL_COMPILER
    const F32vec4 v_threshold(threshold);
    const F32vec4 v_maximum(maximum);

    F32vec4 v_csMatrix[3][3], v_preMul[3];

    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++)
            v_csMatrix[i][j] = F32vec4(csMatrix[i][j]);
        v_preMul[i] = F32vec4(preMul[i]);
    }
#endif

#if _OPENMP < 201307
#pragma omp parallel for schedule (guided)
#else
#pragma omp parallel for simd schedule (guided)
#endif
    for (int row = 0; row < height; row++) {
        int col = 0;
#ifdef __INTEL_COMPILER
        for (/*int col = 0*/; col < width-8; col+=8) {
            const int srcPixOffset = srcPixelStride * col + row * srcLineStride + srcROffset;

            F32vec4 v_raw[2][3];

            load_vector(&srcData[srcPixOffset], v_raw);

            const F32vec4 v_0(0.0f);
            const F32vec4 v_1(1.0f);
            const F32vec4 v_33(1/3.0f);

            F32vec4 v_rgb[2][3] = {v_0, v_0, v_0, v_0, v_0, v_0};

            for (int k = 0; k < 2; k++) {
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        v_rgb[k][i] += v_csMatrix[i][j] * v_raw[k][j];

                F32vec4 val_max = v_0;
                F32vec4 sum = v_0;
                F32vec4 saturated = v_0;
                for (int i = 0; i < 3; i++) {
                    F32vec4 val = simd_min(v_preMul[i] * v_raw[k][i], v_maximum);
                    saturated += select_gt(val, v_threshold, v_1, v_0);
                    val_max = simd_max(val, val_max);
                    sum += val;
                }

                if (_mm_movemask_ps(cmpgt(saturated, v_0)) != 0) { // see if any pixel is saturated
                    F32vec4 m1 = (v_maximum - val_max) * rcp(v_maximum - v_threshold);
                    F32vec4 m2 = (v_maximum - v_33 * sum) * rcp(v_maximum - v_threshold);
                    F32vec4 s = (v_maximum - v_33 * sum) * rcp(v_maximum);

                    /*
                        The following is equivalent to:

                        for (int i = 1; i < saturated; i++)
                            s *= s;
                     */

                    s = select_lt(F32vec4(1.0f), saturated, s * s, s);
                    s = select_lt(F32vec4(2.0f), saturated, s * s, s);

                    F32vec4 m = simd_min(s * m2 + (v_1 - s) * m1, v_1);

                    // use Haeberli's saturation change: http://www.graficaobscura.com/interp/index.html

                    F32vec4 lum = (v_1 - m) * v_33 * (v_rgb[k][0] + v_rgb[k][1] + v_rgb[k][2]);

                    v_rgb[k][0] = m * v_rgb[k][0] + lum;
                    v_rgb[k][1] = m * v_rgb[k][1] + lum;
                    v_rgb[k][2] = m * v_rgb[k][2] + lum;
                }
            }

            int dstPixOffset = dstPixelStride * col + row * dstLineStride + srcROffset;

            store_vector(&dstData[dstPixOffset], v_rgb);
        }
#endif
        for (/*int col = 0*/; col < width; col++) {
            const int srcPixOffset = srcPixelStride * col + row * srcLineStride;

            float raw[3] = {
                static_cast<float>(srcData[srcPixOffset + srcROffset]),
                static_cast<float>(srcData[srcPixOffset + srcGOffset]),
                static_cast<float>(srcData[srcPixOffset + srcBOffset])
            };

            float rgb[3];
            for (int i = 0; i < 3; i++) {
#ifdef FP_FAST_FMAF
                rgb[i] = fmaf(csMatrix[i][2], raw[2],
                         fmaf(csMatrix[i][1], raw[1],
                              csMatrix[i][0] * raw[0]));
#else
                rgb[i] = csMatrix[i][0] * raw[0] +
                         csMatrix[i][1] * raw[1] +
                         csMatrix[i][2] * raw[2];
#endif
            }

            float val_max = 0;
            float sum = 0;
            float saturated = 0;
            for (int i = 0; i < 3; i++) {
                float val = std::min(preMul[i] * raw[i], maximum);
                if (val > threshold) {
                    saturated++;
                    if (val > val_max)
                        val_max = val;
                }
                sum += val;
            }

            if (saturated > 0) {
                float m1 = (maximum - val_max) / (float) (maximum - threshold);
                float m2 = (maximum - sum/3) / (float) (maximum - threshold);
                float s = (maximum - sum/3) / (float) maximum;

                for (int i = 1; i < saturated; i++)
                    s *= s;

#ifdef FP_FAST_FMAF
                const float m = fmaf(s, m2, fmaf(m1, -s, m1));
#else
                const float m = s * m2 + (1 - s) * m1;
#endif

                if (m < 1.0f) {
                    // use Haeberli's saturation change: http://www.graficaobscura.com/interp/index.html

                    float lum = (rgb[0] + rgb[1] + rgb[2]) / 3.0f;
#ifdef FP_FAST_FMAF
                    rgb[0] = fmaf(rgb[0], m, fmaf(lum, -m, lum));
                    rgb[1] = fmaf(rgb[1], m, fmaf(lum, -m, lum));
                    rgb[2] = fmaf(rgb[2], m, fmaf(lum, -m, lum));
#else
                    rgb[0] = rgb[0] * m + lum * (1.0f - m);
                    rgb[1] = rgb[1] * m + lum * (1.0f - m);
                    rgb[2] = rgb[2] * m + lum * (1.0f - m);
#endif
                }
            }

            int dstPixOffset = dstPixelStride * col + row * dstLineStride;
            for (int i = 0; i < 3; i++)
                dstData[dstPixOffset + dstBandOffsets[i]] = clampUShort(rgb[i]);
        }
    }

    env->ReleasePrimitiveArrayCritical(jpreMul, preMul, 0);
    env->ReleasePrimitiveArrayCritical(jcsMatrix, csMatrix, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jdstBandOffsets, dstBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jsrc, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdst, dstData, 0);
}
