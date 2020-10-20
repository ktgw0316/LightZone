/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <jni.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

#ifndef __APPLE__
// local
#include "LC_JNIUtils.h" /* for LC_hasSSE2() */
#endif

#if defined(__SSE__) && !defined(__APPLE__)
#define JNI_SSE_BUG 1
#define JNI_SSE_ASM()                                                                              \
    (void)__builtin_alloca(16);                                                                    \
    __asm__ __volatile__("andl $-16, %esp")
#endif

#define FILTER_LUMA_WRAP_ARGS                                                                      \
    JNIEnv *env, jclass cls, jshortArray jsrcData, jshortArray jdestData, jint wr, jint ws,        \
        jfloat scale_r, jfloatArray jkernel, jint width, jint height, jint srcROffset,             \
        jint srcGOffset, jint srcBOffset, jint destROffset, jint destGOffset, jint destBOffset,    \
        jint srcLineStride, jint destLineStride

#define CONST 0x10000

// Very fast approximation of exp, faster than lookup table!
// See paper of Nicol N. Schraudolph, adapted here to single precision floats.

static inline float fast_exp(float val) __attribute__((always_inline));
static inline float fast_exp(float val) {
    const float fast_exp_a = (1 << 23) / M_LN2;
#if defined(__i386__)
    const float fast_exp_b_c = 127.0f * (1 << 23) - 405000;
#else
    const float fast_exp_b_c = 127.0f * (1 << 23) - 347000;
#endif
    if (val < -16)
        return 0;

    union {
        float f;
        int i;
    } result;

    result.i = (int)(fast_exp_a * val + fast_exp_b_c);
    return result.f;
}

#if defined(__SSE__) || defined(__VEC__)

#define USE_VECTOR 1

typedef int vInt __attribute__((vector_size(16)));
typedef unsigned int vUInt __attribute__((vector_size(16)));
typedef unsigned short vUInt16 __attribute__((vector_size(16)));
typedef unsigned char vByte __attribute__((vector_size(16)));
typedef float vFloat __attribute__((vector_size(16)));

typedef union {
    vFloat v;
    float a[4];
} vsFloat;

typedef union {
    vInt v;
    int a[4];
} vsInt;

#if defined(__i386__) || defined(__x86_64__)

#include <xmmintrin.h>

static inline vFloat loadUnalignedFloat(float *addr) __attribute__((always_inline));
static inline vFloat loadUnalignedFloat(float *addr) { return (vFloat)_mm_loadu_ps(addr); }

static inline vFloat loadAndSplatFloat(float *addr) __attribute__((always_inline));
static inline vFloat loadAndSplatFloat(float *addr) { return _mm_load1_ps(addr); }

static inline vFloat load_zerof() __attribute__((always_inline));
static inline vFloat load_zerof() { return (vFloat)_mm_setzero_ps(); }

static inline vFloat _mm_sel_ps(vFloat a, vFloat b, vFloat mask) __attribute__((always_inline));
static inline vFloat _mm_sel_ps(vFloat a, vFloat b, vFloat mask) {
    b = _mm_and_ps(b, mask);
    a = _mm_andnot_ps(mask, a);
    return _mm_or_ps(a, b);
}

static inline vFloat vfast_exp(vFloat val) __attribute__((always_inline));
static inline vFloat vfast_exp(vFloat val) {
    const float fast_exp_a = (1 << 23) / M_LN2;
    const float fast_exp_b_c = 127.0f * (1 << 23) - 405000;

    vFloat llmask = _mm_cmplt_ps(val, _mm_set1_ps(-16));
    vFloat expval = _mm_set1_ps(fast_exp_a) * val + _mm_set1_ps(fast_exp_b_c);
    vFloat result = (vFloat)_mm_cvtps_epi32(expval);
    return _mm_sel_ps(result, (vFloat)_mm_setzero_ps(), llmask);
}

static inline vFloat vexp_estimate(vFloat arg) __attribute__((always_inline));
static inline vFloat vexp_estimate(vFloat arg) { return vfast_exp(-arg); }

static inline vFloat vabs(vFloat v) __attribute__((always_inline));
static inline vFloat vabs(vFloat v) {
    return (vFloat)_mm_srli_epi32(_mm_slli_epi32((__m128i)v, 1), 1);
}

static inline vInt vec_unpacklo_epu16(vUInt16 v) __attribute__((always_inline));
static inline vInt vec_unpacklo_epu16(vUInt16 v) {
    __m128i zero = _mm_xor_si128((__m128i)v, (__m128i)v);
    return (vInt)_mm_unpacklo_epi16((__m128i)v, zero);
}

typedef int csr_context;

static inline csr_context denormals_off() {
#if defined(__SSE2__)
    int oldMXCSR = _mm_getcsr();      // read the old MXCSR setting
    int newMXCSR = oldMXCSR | 0x8040; // set DAZ and FZ bits
    _mm_setcsr(newMXCSR);             // write the new MXCSR setting to the MXCSR
    return oldMXCSR;
#else
    return 0;
#endif
}

static inline void reset_denormals(csr_context oldMXCSR) {
#if defined(__SSE2__)
    // restore old MXCSR settings to turn denormals back on if they were on
    _mm_setcsr(oldMXCSR);
#endif
}

#endif // defined(__i386__) || defined(__x86_64__)

#if defined(__ppc__) || defined(__ppc64__)
#include <altivec.h>

static inline vFloat loadUnalignedFloat(float *target) {
    vFloat MSQ, LSQ;
    vByte mask;

    MSQ = vec_ld(0, target);         // most significant quadword
    LSQ = vec_ld(15, target);        // least significant quadword
    mask = vec_lvsl(0, target);      // create the permute mask
    return vec_perm(MSQ, LSQ, mask); // align the data
}

static inline vFloat loadAndSplatFloat(float *word) {
    vFloat vv = vec_lde(0, word);
    vByte moveToStart = vec_lvsl(0, word);
    vv = vec_perm(vv, vv, moveToStart);
    vv = vec_splat(vv, 0);
    return vv;
}

static inline vFloat vfast_exp(vFloat val) __attribute__((always_inline));
static inline vFloat vfast_exp(vFloat val) {
    const float min_limit __attribute__((aligned(16))) = -80;
    const float fast_exp_a __attribute__((aligned(16))) = (1 << 23) / M_LN2;
    const float fast_exp_b_c __attribute__((aligned(16))) = 127.0f * (1 << 23) - 366000.0f;

    val = vec_max(val, vec_splat(vec_lde(0, &min_limit), 0));

    const vFloat a = vec_splat(vec_lde(0, &fast_exp_a), 0);
    const vFloat b_c = vec_splat(vec_lde(0, &fast_exp_b_c), 0);

    return (vFloat)vec_cts(a * val + b_c, 0);
}

static inline vFloat vexp_estimate(vFloat in) __attribute__((always_inline));
static inline vFloat vexp_estimate(vFloat in) {
#if 1 // ALTIVEC
    return vec_expte(-in);
#else
    return vfast_exp(-in);
#endif
}

static inline vFloat load_zerof() { return (vFloat)vec_splat_u32(0); }

typedef vUInt csr_context;

static inline csr_context denormals_off() {
    vUInt javaOffMask = (vUInt){0x00010000};
    vUInt java;
    vUInt oldJavaMode = (vUInt)vec_mfvscr();
    java = vec_or(oldJavaMode, javaOffMask);
    vec_mtvscr(java);
    return oldJavaMode;
}

static void reset_denormals(csr_context oldJavaMode) { vec_mtvscr(oldJavaMode); }

#endif // defined(__ppc__) || defined(__ppc64__)
#endif // defined(__SSE2__) || defined (__VEC__)

#if USE_VECTOR
int main() {
    int i, it = 1000000;
    float min = -16, max = 0;
    float max_diff = 0;
    float max_xdiff;
    float max_vdiff = 0;
    float max_xvdiff;
    float max_fdiff = 0;
    float max_xfdiff;
    for (i = 0; i < it; i++) {
        float x = min + i * (max - min) / it;
        float expfx = expf(x);
        float fast_expx = fast_exp(x);
        vsFloat vfast_expx;
        vfast_expx.v = vfast_exp(loadAndSplatFloat(&x));
        float vdiff = fabs(expfx - vfast_expx.a[0]) / expfx;
        float diff = fabs(expfx - fast_expx) / expfx;
        float fdiff = fabs(vfast_expx.a[0] - fast_expx) / expfx;
        if (diff > max_diff && i > 0) {
            max_diff = diff;
            max_xdiff = x;
        }
        if (vdiff > max_vdiff && i > 0) {
            max_vdiff = vdiff;
            max_xvdiff = x;
        }
        if (fdiff > max_fdiff && i > 0) {
            max_fdiff = fdiff;
            max_xfdiff = x;
        }
        // printf("x: %e, expf: %e, fast_exp: %e, vfast_exp: %e, diff: %f\n", x, expfx, fast_expx,
        // vfast_expx.a[0], diff);
    }

    int ss = -5;

    unsigned uu = (unsigned)ss;

    printf("bozo: %d\n", uu < 10);

    printf("max diff: %f at %f\n", max_diff, max_xdiff);
    printf("max vdiff: %f at %f\n", max_vdiff, max_xvdiff);
    printf("max fdiff: %f at %f\n", max_fdiff, max_xfdiff);

    for (i = -10; i < 10; i++) {
        float x = i;
        vsFloat vfast_expx;
        vfast_expx.v = vfast_exp(loadAndSplatFloat(&x));
        printf("expf(%d): %f, fast_exp(%d): %f, vfast_exp(%d): %f\n", i, expf(i), i, fast_exp(i), i,
               vfast_expx.a[0]);
    }
}
#endif

#define SQR(x)                                                                                     \
    ({                                                                                             \
        typeof(x) _x = (x);                                                                        \
        _x *_x;                                                                                    \
    })

#ifdef JNI_SSE_BUG
static void filterLumaWrapSSE(FILTER_LUMA_WRAP_ARGS) __attribute__((used));
#endif /* JNI_SSE_BUG */

JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_BilateralFilterOpImage_bilateralFilterLuma(FILTER_LUMA_WRAP_ARGS) {
#ifdef JNI_SSE_BUG
    JNI_SSE_ASM();

    filterLumaWrapSSE(env, cls, jsrcData, jdestData, wr, ws, scale_r, jkernel, width, height,
                      srcROffset, srcGOffset, srcBOffset, destROffset, destGOffset, destBOffset,
                      srcLineStride, destLineStride);
}
#endif /* JNI_SSE_BUG */

#ifdef JNI_SSE_BUG
static void filterLumaWrapSSE(FILTER_LUMA_WRAP_ARGS) {
#endif /* JNI_SSE_BUG */
#if USE_VECTOR
    csr_context old_csr = denormals_off();
#endif

    unsigned short *srcData = (unsigned short *)(*env)->GetPrimitiveArrayCritical(env, jsrcData, 0);
    unsigned short *destData =
        (unsigned short *)(*env)->GetPrimitiveArrayCritical(env, jdestData, 0);
    float *kernel =
        (float *)(*env)->GetPrimitiveArrayCritical(env, jkernel, 0) + wr; // Keep 0 in the middle...

    int i, wlast, row, col, y, x;
    size_t window_size = (ws + 1) * sizeof(float *) + (size_t) ws * width * sizeof(float);
    float **window = (float **)calloc(window_size, 1);
    if (window == NULL)
        return;

    for (i = 0; i <= ws; i++)
        window[i] = (float *)(window + ws + 1) + i * width;

    float k0 = kernel[0];

#if USE_VECTOR
    const vFloat vk0 = loadAndSplatFloat(&k0);
    scale_r /= CONST;
    const vFloat vscale_r = loadAndSplatFloat(&scale_r);
#endif
    for (wlast = -1, row = 0; row < height; row++) {
        while (wlast < row + wr) {
            wlast++;
            for (i = 0; i <= ws; i++) /* rotate window rows */
                window[(ws + i) % (ws + 1)] = window[i];
            if (wlast < height) {
                int base = wlast * srcLineStride + srcGOffset;
                float sqrtc = sqrtf(CONST);
                col = 0;
#if USE_VECTOR && defined(__SSE2__)
                const vFloat vsqrtc = (vFloat){sqrtc, sqrtc, sqrtc, sqrtc};

                for (/*col=0*/; col < width - 3; col += 4) {
                    vInt ig = (vInt){srcData[3 * (col + 0) + base], srcData[3 * (col + 1) + base],
                                     srcData[3 * (col + 2) + base], srcData[3 * (col + 3) + base]};
                    _mm_storeu_ps(&window[ws - 1][col],
                                  vsqrtc * _mm_sqrt_ps(_mm_cvtepi32_ps((__m128i)ig)));
                }
#endif
                for (/*col=0*/; col < width; col++) {
                    float g = srcData[3 * col + base];
                    window[ws - 1][col] = sqrtc * sqrtf(g);
                }
            }
        }
        for (col = 0; col < width; col++) {
            float sl = 0, ss = 0;

            // Separable bilateral filter
            {
                float w0 = window[wr][col];

                x = col > wr ? -wr : -col;
#if USE_VECTOR
                if (ws > 3) {
                    vsFloat vsl, vss;
                    vsl.v = vss.v = load_zerof();

                    vFloat vw0 = loadAndSplatFloat(&w0);

                    for (; x <= wr - 3 && (col + x) < width - 3; x += 4) {
                        vFloat wxy = loadUnalignedFloat(&window[wr][col + x]);
                        vFloat vexp = vexp_estimate(SQR(wxy - vw0) * vscale_r + vk0 +
                                                    loadUnalignedFloat(&kernel[x]));

                        vsl.v += vexp * wxy;
                        vss.v += vexp;
                    }

                    sl += vsl.a[0] + vsl.a[1] + vsl.a[2] + vsl.a[3];
                    ss += vss.a[0] + vss.a[1] + vss.a[2] + vss.a[3];
                }
#endif
                for (; x <= wr && (col + x) < width; x++) {
                    float wx0 = window[wr][col + x];
                    float exp = fast_exp(-(SQR(wx0 - w0) * scale_r + k0 + kernel[x]));
                    if (exp > 0) {
                        sl += exp * wx0;
                        ss += exp;
                    }
                }

                w0 = window[wr][col] = sl / ss;

                sl = 0;
                ss = 0;

                y = row > wr ? -wr : -row;
#if USE_VECTOR
                if (ws > 3) {
                    vsFloat vsl, vss;
                    vsl.v = vss.v = load_zerof();

                    vFloat vw0 = loadAndSplatFloat(&w0);

                    for (; y <= wr - 3 && (row + y) < height - 3; y += 4) {
                        vsFloat y0;
                        for (i = 0; i < 4; i++) {
                            if (wr + y + i < ws) {
                                y0.a[i] = window[wr + y + i][col];
                            } else
                                y0.a[i] = 0;
                        }

                        vFloat vexp = vexp_estimate(SQR(y0.v - vw0) * vscale_r + vk0 +
                                                    loadUnalignedFloat(&kernel[y]));

                        vsl.v += vexp * y0.v;
                        vss.v += vexp;
                    }

                    sl += vsl.a[0] + vsl.a[1] + vsl.a[2] + vsl.a[3];
                    ss += vss.a[0] + vss.a[1] + vss.a[2] + vss.a[3];
                }
#endif
                for (; y <= wr && (row + y) < height; y++) {
                    float wy0 = window[wr + y][col];
                    float exp = fast_exp(-(SQR(wy0 - w0) * scale_r + kernel[y] + k0));
                    if (exp > 0) {
                        sl += exp * wy0;
                        ss += exp;
                    }
                }
            }
            if (col >= wr && col < width - wr && row >= wr && row < height - wr) {
                int g1 = (int)(0x10000 * SQR((sl / ss) / (float)0x10000));

                int srcPixelOffset = 3 * col + row * srcLineStride;

                int g = srcData[srcPixelOffset + srcGOffset];
                int r = srcData[srcPixelOffset + srcROffset];
                int b = srcData[srcPixelOffset + srcBOffset];

                int dstPixelOffset = 3 * (col - wr) + (row - wr) * destLineStride;

                g1 = g1 < 0 ? 0 : g1 > 0xffff ? 0xffff : g1;

                int r1 = (g1 - (g - r));
                int b1 = (g1 - (g - b));
                destData[dstPixelOffset + destROffset] =
                    (unsigned short)(r1 < 0 ? 0 : r1 > 0xffff ? 0xffff : r1);
                destData[dstPixelOffset + destBOffset] =
                    (unsigned short)(b1 < 0 ? 0 : b1 > 0xffff ? 0xffff : b1);
                destData[dstPixelOffset + destGOffset] = (unsigned short)g1;
            }
        }
    }
    free(window);

    (*env)->ReleasePrimitiveArrayCritical(env, jsrcData, srcData, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jdestData, destData, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jkernel, kernel, 0);

#if USE_VECTOR
    reset_denormals(old_csr);
#endif
}

/* vim:set et sw=4 ts=4: */
