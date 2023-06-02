/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <cstdint>
#include <cstring>
#include <vector>

template <typename T> struct Values {
  static const T maxVal;
};

template <> const uint8_t Values<uint8_t>::maxVal = 0xFF;
template <> const uint16_t Values<uint16_t>::maxVal = 0xFFFF;
template <> const int8_t Values<int8_t>::maxVal = 0x7F;
template <> const int16_t Values<int16_t>::maxVal = 0x7FFF;
template <> const uint32_t Values<uint32_t>::maxVal = 0xFFFFFFFF;
template <> const int Values<int>::maxVal = 0x7FFFFFFF;
template <> const float Values<float>::maxVal = 1.0;
template <> const double Values<double>::maxVal = 1.0;

#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
#include <altivec.h>

static void TurnJavaModeOff(vector uint32_t *oldJavaMode) {
  vector uint32_t javaOffMask = (vector uint32_t){0x00010000};
  vector uint32_t java;
  *oldJavaMode = (vector uint32_t)vec_mfvscr();
  java = vec_or(*oldJavaMode, javaOffMask);
  vec_mtvscr(java);
}

static void RestoreJavaMode(vector uint32_t *oldJavaMode) {
  vec_mtvscr(*oldJavaMode);
}

static inline vector uint8_t loadUnalignedChar(uint8_t *target) {
  vector uint8_t MSQ, LSQ;
  vector uint8_t mask;

  MSQ = vec_ld(0, target);         // most significant quadword
  LSQ = vec_ld(15, target);        // least significant quadword
  mask = vec_lvsl(0, target);      // create the permute mask
  return vec_perm(MSQ, LSQ, mask); // align the data
}

static inline vector uint16_t loadUnalignedShort(uint16_t *target) {
  vector uint16_t MSQ, LSQ;
  vector uint8_t mask;

  MSQ = vec_ld(0, target);         // most significant quadword
  LSQ = vec_ld(15, target);        // least significant quadword
  mask = vec_lvsl(0, target);      // create the permute mask
  return vec_perm(MSQ, LSQ, mask); // align the data
}

static inline vector uint32_t loadUnalignedInt(uint32_t *target) {
  vector uint32_t MSQ, LSQ;
  vector uint8_t mask;

  MSQ = vec_ld(0, target);         // most significant quadword
  LSQ = vec_ld(15, target);        // least significant quadword
  mask = vec_lvsl(0, target);      // create the permute mask
  return vec_perm(MSQ, LSQ, mask); // align the data
}

static inline vector float loadUnalignedFloat(float *target) {
  vector float MSQ, LSQ;
  vector uint8_t mask;

  MSQ = vec_ld(0, target);         // most significant quadword
  LSQ = vec_ld(15, target);        // least significant quadword
  mask = vec_lvsl(0, target);      // create the permute mask
  return vec_perm(MSQ, LSQ, mask); // align the data
}

template <typename T>
static inline vector float loadFloatPixels(T *data)
    __attribute__((always_inline));

template <typename T> static inline vector float loadFloatPixels(T *data) {
  vector uint32_t ll;
  if (sizeof(T) == 1) {
    vector uint8_t x = loadUnalignedChar((uint8_t *)data);
    vector uint16_t xx = vec_unpackh((vector uint8_t)x);
    ll = vec_unpackh(xx);
    ll = vec_and(ll, (vector uint32_t){0xFF, 0xFF, 0xFF, 0xFF});
  } else if (sizeof(T) == 2) {
    vector uint16_t x = loadUnalignedShort((uint16_t *)data);
    ll = vec_unpackh((vector int16_t)x);
    ll = vec_and(ll, (vector uint32_t){0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF});
  } else if (sizeof(T) == 4) {
    ll = loadUnalignedInt((uint32_t *)data);
  }
  return vec_ctf(ll, 0);
}

template <typename T>
void storeFloatPixels(T *dest, int bands, vector float data)
    __attribute__((always_inline));

template <typename T>
void storeFloatPixels(T *dest, int bands, vector float data) {
  vector int res = vec_cts(data, 0);

  if (sizeof(T) == 1) {
    vector uint16_t sres = vec_packsu(res, res);
    vector uint8_t bres = vec_packsu(sres, sres);

    bres = vec_perm(bres, bres, vec_lvsr(0, dest));

    for (int b = 0; b < bands; b++)
      vec_ste(bres, b, (uint8_t *)dest);
  } else if (sizeof(T) == 2) {
    vector uint16_t sres = vec_packsu(res, res);

    sres = vec_perm(sres, sres, vec_lvsr(0, dest));

    for (int b = 0; b < bands; b++)
      vec_ste(sres, 2 * b, (uint16_t *)dest);
  } else if (sizeof(T) == 4) {
    res = vec_perm(res, res, vec_lvsr(0, (int *)dest));

    for (int b = 0; b < bands; b++)
      vec_ste(res, 4 * b, (int *)dest);
  }
}

static inline vector float addAll(vector float t1)
    __attribute__((always_inline));

static inline vector float addAll(vector float t1) {
  t1 = vec_add(t1, vec_sld(t1, t1, 8));
  return vec_add(t1, vec_sld(t1, t1, 4));
}

#define W0 0, 1, 2, 3
#define W1 4, 5, 6, 7
#define W2 8, 9, 10, 11
#define W3 12, 13, 14, 15
#define W4 16, 17, 18, 19
#define W5 20, 21, 22, 23
#define W6 24, 25, 26, 27
#define W7 28, 29, 30, 31

vector float loadKernelElem(int pos, float *kernel) {
  float *ptr = &kernel[pos];
  vector float vv = vec_lde(0, ptr);
  vector uint8_t moveToStart = vec_lvsl(0, ptr);
  vv = vec_perm(vv, vv, moveToStart);
  vv = vec_splat(vv, 0);
  return vv;
}

template <typename T> vector float conv_line0(T *data, float *kernel, int kw) {
  vector float ff = (vector float)vec_splat_u32(0);

  for (int v = 0; v < kw; v++) {
    vector float hvv = loadKernelElem(v, kernel);

    vector float ss = loadFloatPixels(&data[3 * v]);

    ff = vec_madd(hvv, ss, ff);
  }
  return ff;
}

template <typename T> vector float conv_line3(T *data, float *kernel, int kw) {
  vector float ff;

  if (kw / 4 > 0) {
    vector float fr, fg, fb;

    fr = fg = fb = (vector float)vec_splat_u32(0);

    for (int v = 0; v < kw / 4; v++) {
      vector float hv = vec_ld(0, &kernel[4 * v]);

      vector float ss1 = loadFloatPixels(&data[3 * 4 * v]);
      vector float ss2 = loadFloatPixels(&data[3 * 4 * v + 4]);
      vector float ss3 = loadFloatPixels(&data[3 * 4 * v + 8]);

      vector float ssa, ssb, ssc;
      ssa = vec_perm(ss1, ss2, (vector uint8_t){W0, W3, W6, W2});
      ssb = vec_perm(ss1, ss2, (vector uint8_t){W1, W4, W7, W5});
      ss1 = vec_perm(ssa, ss3, (vector uint8_t){W0, W1, W2, W5});
      fr = vec_madd(hv, ss1, fr);
      ssc = vec_perm(ssa, ss3, (vector uint8_t){W3, W6, W4, W7});
      ss2 = vec_perm(ssb, ss3, (vector uint8_t){W0, W1, W2, W6});
      fg = vec_madd(hv, ss2, fg);
      ss3 = vec_perm(ssc, ssb, (vector uint8_t){W0, W7, W2, W3});
      fb = vec_madd(hv, ss3, fb);
    }

    fr = addAll(fr);
    fg = addAll(fg);
    fb = addAll(fb);

    ff = vec_perm(fr, fg, (vector uint8_t){W0, W4, W1, W2});
    ff = vec_perm(ff, fb, (vector uint8_t){W0, W1, W4, W2});
  } else
    ff = (vector float)vec_splat_u32(0);

  // we have to make an extra step of the computation for the last element of
  // the kernel...

  for (int v = 4 * (kw / 4); v < kw; v++) {
    vector float hvv = loadKernelElem(v, kernel);

    vector float ss = loadFloatPixels(&data[3 * v]);

    ff = vec_madd(hvv, ss, ff);
  }

  return ff;
}

template <typename T> float conv_line1(T *data, float *kernel, int kw) {
  float f __attribute__((aligned(16)));

  if (kw / 4 > 0) {
    vector float ff = (vector float)vec_splat_u32(0);

    for (int v = 0; v < kw / 4; v++) {
      vector float hv = vec_ld(0, &kernel[4 * v]);
      vector float ss = loadFloatPixels(&data[4 * v]);
      ff = vec_madd(hv, ss, ff);
    }

    ff = addAll(ff);
    vec_ste(ff, 0, &f);
  } else
    f = 0;

  // we have to make an extra step of the computation for the last element of
  // the kernel...

  for (int v = 4 * (kw / 4); v < kw; v++) {
    f += data[v] * kernel[v];
  }

  return f;
}

#endif

template <typename T, int bands>
static void convolveBandsLoop(T *srcData, T *dstData, int srcScanlineOffset,
                              int dstScanlineOffset, int srcScanlineStride,
                              int dstScanlineStride, int dheight, int dwidth,
                              int kw, int kh, float *hValues, float *vValues) {
#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
  vector uint32_t oldJavaMode;
  TurnJavaModeOff(&oldJavaMode);
#endif

#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
  std::vector<float> tmpBuffer(kh * dwidth *
                               4); // waste some space but go faster...
#else
  std::vector<float> tmpBuffer(kh * dwidth * bands);
#endif
  const int tmpBufferSize = kh * dwidth;

  float hkernel[kw] __attribute__((aligned(16)));
  float vkernel[kh] __attribute__((aligned(16)));

  memcpy(hkernel, hValues, kw * sizeof(float));
  memcpy(vkernel, vValues, kh * sizeof(float));

  int revolver = 0;
  int kvRevolver = 0; // to match kernel vkernel
  for (int j = 0; j < kh - 1; j++) {
    int srcPixelOffset = srcScanlineOffset;
#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
    for (int i = 0; i < dwidth; i++) {
      vector float ff = conv_line3(&srcData[srcPixelOffset], hkernel, kw);
      vec_st(ff, 0, &tmpBuffer[4 * (revolver + i)]);
      srcPixelOffset += bands;
    }
#else
    for (int i = 0; i < dwidth; i++) {
      float f[bands];
      for (int b = 0; b < bands; b++)
        f[b] = 0.0;

      for (int v = 0, imageOffset = srcPixelOffset; v < kw;
           v++, imageOffset += bands) {
        float hv = hkernel[v];
        for (int b = 0; b < bands; b++) {
          f[b] += srcData[imageOffset + b] * hv;
        }
      }

      for (int b = 0; b < bands; b++)
        tmpBuffer[bands * (revolver + i) + b] = f[b];

      srcPixelOffset += bands;
    }
#endif
    revolver += dwidth;
    srcScanlineOffset += srcScanlineStride;
  }

  // srcScanlineStride already bumped by
  // kh-1*scanlineStride
  for (int j = 0; j < dheight; j++) {
    for (int i = 0, srcPixelOffset = srcScanlineOffset,
             dstPixelOffset = dstScanlineOffset;
         i < dwidth; i++, srcPixelOffset += bands, dstPixelOffset += bands) {
#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
      vector float ff = conv_line3(&srcData[srcPixelOffset], hkernel, kw);

      vec_st(ff, 0, (vector float *)(&tmpBuffer[4 * (revolver + i)]));
      ff = (vector float){0.5f, 0.5f, 0.5f};
#else
      float f[bands];
      for (int b = 0; b < bands; b++)
        f[b] = 0.0;

      for (int v = 0, imageOffset = srcPixelOffset; v < kw;
           v++, imageOffset += bands) {
        float hv = hkernel[v];
        for (int b = 0; b < bands; b++) {
          f[b] += srcData[imageOffset + b] * hv;
        }
      }

      for (int b = 0; b < bands; b++) {
        tmpBuffer[bands * (revolver + i) + b] = f[b];
        f[b] = 0.5;
      }
#endif

      // The vertical kernel must revolve as well
      int b = kvRevolver + i;
      for (int a = 0; a < kh; a++) {
#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
        vector float vv = loadKernelElem(a, vkernel);
        // vector float ss = loadUnalignedFloat(&tmpBuffer[bands*b]);
        vector float ss = vec_ld(0, (vector float *)(&tmpBuffer[4 * b]));

        ff = vec_madd(vv, ss, ff);
#else
        float vv = vkernel[a];
        for (int c = 0; c < bands; c++)
          f[c] += tmpBuffer[bands * b + c] * vv;
#endif
        b += dwidth;
        if (b >= tmpBufferSize)
          b -= tmpBufferSize;
      }

#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
      storeFloatPixels(&dstData[dstPixelOffset], bands, ff);
#else
      for (int b = 0; b < bands; b++) {
        float res = f[b];
        if (res < 0) {
          res = 0;
        } else if (res > Values<T>::maxVal) {
          res = Values<T>::maxVal;
        }

        dstData[dstPixelOffset + b] = (T)res;
      }
#endif
    }

    revolver += dwidth;
    if (revolver == tmpBufferSize) {
      revolver = 0;
    }
    kvRevolver += dwidth;
    if (kvRevolver == tmpBufferSize) {
      kvRevolver = 0;
    }
    srcScanlineOffset += srcScanlineStride;
    dstScanlineOffset += dstScanlineStride;
  }
#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
  RestoreJavaMode(&oldJavaMode);
#endif
}

template <typename T>
static void convolveLoop(T *srcData, T *dstData, int srcScanlineOffset,
                         int dstScanlineOffset, int srcScanlineStride,
                         int dstScanlineStride, int srcPixelStride,
                         int dstPixelStride, int dheight, int dwidth, int kw,
                         int kh, float *hValues, float *vValues) {
#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
  vector uint32_t oldJavaMode;
  TurnJavaModeOff(&oldJavaMode);
#endif

  std::vector<float> tmpBuffer(kh * dwidth);
  const int tmpBufferSize = kh * dwidth;

  float hkernel[kw] __attribute__((aligned(16)));
  float vkernel[kh] __attribute__((aligned(16)));

  memcpy(hkernel, hValues, kw * sizeof(float));
  memcpy(vkernel, vValues, kh * sizeof(float));

  int revolver = 0;
  int kvRevolver = 0; // to match kernel vValues
  for (int j = 0; j < kh - 1; j++) {
    int srcPixelOffset = srcScanlineOffset;

    for (int i = 0; i < dwidth; i++) {
      float f;

#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
      if (srcPixelStride == 1)
        f = conv_line1(&srcData[srcPixelOffset], hkernel, kw);
      else
#endif
      {
        f = 0.0f;
        for (int v = 0, imageOffset = srcPixelOffset; v < kw;
             v++, imageOffset += srcPixelStride)
          f += srcData[imageOffset] * hkernel[v];
      }
      tmpBuffer[revolver + i] = f;
      srcPixelOffset += srcPixelStride;
    }
    revolver += dwidth;
    srcScanlineOffset += srcScanlineStride;
  }

  const float fmaxVal = (float)Values<T>::maxVal;

  // srcScanlineStride already bumped by
  // kh-1*scanlineStride
  for (int j = 0; j < dheight; j++) {
    int srcPixelOffset = srcScanlineOffset;
    int dstPixelOffset = dstScanlineOffset;

    for (int i = 0; i < dwidth; i++) {

#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
      union {
        vector float ff;
        float fa[4];
        float f;
      };
#else
      float f;
#endif

#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
      if (srcPixelStride == 1)
        f = conv_line1(&srcData[srcPixelOffset], hkernel, kw);
      else
#endif
      {
        f = 0.0f;
        for (int v = 0, imageOffset = srcPixelOffset; v < kw;
             v++, imageOffset += srcPixelStride)
          f += srcData[imageOffset] * hkernel[v];
      }
      tmpBuffer[revolver + i] = f;

      // A bug in gcc 4.0 causes the following code to "drop pixels", check
      // forthcoming gcc4 updates...

#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
      if (srcPixelStride == 1 && kh / 4 != 0) {
        ff = (vector float){0.5f, 0, 0, 0};
        // The vertical kernel must revolve as well
        int b = kvRevolver + i;
        for (int a = 0; a < kh / 4; a++) {
          union {
            vector float tvb;
            float ftb[4];
          };
          for (int i = 0; i < 4; i++) {
            ftb[i] = tmpBuffer[b];
            b += dwidth;
            if (b >= tmpBufferSize)
              b -= tmpBufferSize;
          }
          vector float vv = vec_ld(
              0, &vkernel[4 * a]); // loadUnalignedFloat(&vkernel[4 * a]);
          ff = vec_madd(vv, tvb, ff);
        }

        ff = addAll(ff);

        for (int a = 4 * (kh / 4); a < kh; a++) {
          f += tmpBuffer[b] * vkernel[a];
          b += dwidth;
          if (b >= tmpBufferSize)
            b -= tmpBufferSize;
        }
      } else
#endif
      {
        f = 0.5f;
        // The vertical kernel must revolve as well
        int b = kvRevolver + i;
        for (int a = 0; a < kh; a++) {
          f += tmpBuffer[b] * vkernel[a];
          b += dwidth;
          if (b >= tmpBufferSize)
            b -= tmpBufferSize;
        }
      }
      if (f < 0.0f) {
        f = 0.0f;
      } else if (f > fmaxVal) {
        f = fmaxVal;
      }

      dstData[dstPixelOffset] = (T)f;
      srcPixelOffset += srcPixelStride;
      dstPixelOffset += dstPixelStride;
    }

    revolver += dwidth;
    if (revolver == tmpBufferSize) {
      revolver = 0;
    }
    kvRevolver += dwidth;
    if (kvRevolver == tmpBufferSize) {
      kvRevolver = 0;
    }
    srcScanlineOffset += srcScanlineStride;
    dstScanlineOffset += dstScanlineStride;
  }
#if defined(__POWERPC__) && defined(LC_USE_ALTIVEC)
  RestoreJavaMode(&oldJavaMode);
#endif
}

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_Convolutions.h"
#endif

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cByteLoop(
    JNIEnv *env, jclass cls, jbyteArray src, jbyteArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint srcPixelStride, jint dstPixelStride,
    jint dheight, jint dwidth, jint kw, jint kh, jfloatArray jhValues,
    jfloatArray jvValues) {
  uint8_t *srcData = (uint8_t *)env->GetPrimitiveArrayCritical(src, 0);
  uint8_t *dstData = (uint8_t *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);
  // convolveBandsLoop<uint8_t, 3>(srcData, dstData,
  convolveLoop(srcData, dstData, srcScanlineOffset, dstScanlineOffset,
               srcScanlineStride, dstScanlineStride, srcPixelStride,
               dstPixelStride, dheight, dwidth, kw, kh, hValues, vValues);
  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cShortLoop(
    JNIEnv *env, jclass cls, jshortArray src, jshortArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint srcPixelStride, jint dstPixelStride,
    jint dheight, jint dwidth, jint kw, jint kh, jfloatArray jhValues,
    jfloatArray jvValues) {
  int16_t *srcData = (int16_t *)env->GetPrimitiveArrayCritical(src, 0);
  int16_t *dstData = (int16_t *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  convolveLoop(srcData, dstData, srcScanlineOffset, dstScanlineOffset,
               srcScanlineStride, dstScanlineStride, srcPixelStride,
               dstPixelStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cUShortLoop(
    JNIEnv *env, jclass cls, jshortArray src, jshortArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint srcPixelStride, jint dstPixelStride,
    jint dheight, jint dwidth, jint kw, jint kh, jfloatArray jhValues,
    jfloatArray jvValues) {
  uint16_t *srcData = (uint16_t *)env->GetPrimitiveArrayCritical(src, 0);
  uint16_t *dstData = (uint16_t *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  // convolveBandsLoop<uint16_t, 3>(srcData, dstData,
  convolveLoop(srcData, dstData, srcScanlineOffset, dstScanlineOffset,
               srcScanlineStride, dstScanlineStride, srcPixelStride,
               dstPixelStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cIntLoop(
    JNIEnv *env, jclass cls, jintArray src, jintArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint srcPixelStride, jint dstPixelStride,
    jint dheight, jint dwidth, jint kw, jint kh, jfloatArray jhValues,
    jfloatArray jvValues) {
  int *srcData = (int *)env->GetPrimitiveArrayCritical(src, 0);
  int *dstData = (int *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  convolveLoop(srcData, dstData, srcScanlineOffset, dstScanlineOffset,
               srcScanlineStride, dstScanlineStride, srcPixelStride,
               dstPixelStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cFloatLoop(
    JNIEnv *env, jclass cls, jfloatArray src, jfloatArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint srcPixelStride, jint dstPixelStride,
    jint dheight, jint dwidth, jint kw, jint kh, jfloatArray jhValues,
    jfloatArray jvValues) {
  float *srcData = (float *)env->GetPrimitiveArrayCritical(src, 0);
  float *dstData = (float *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  convolveLoop(srcData, dstData, srcScanlineOffset, dstScanlineOffset,
               srcScanlineStride, dstScanlineStride, srcPixelStride,
               dstPixelStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cDoubleLoop(
    JNIEnv *env, jclass cls, jdoubleArray src, jdoubleArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint srcPixelStride, jint dstPixelStride,
    jint dheight, jint dwidth, jint kw, jint kh, jfloatArray jhValues,
    jfloatArray jvValues) {
  double *srcData = (double *)env->GetPrimitiveArrayCritical(src, 0);
  double *dstData = (double *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  convolveLoop(srcData, dstData, srcScanlineOffset, dstScanlineOffset,
               srcScanlineStride, dstScanlineStride, srcPixelStride,
               dstPixelStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

// three colors interleaved special version

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cInterleaved3ByteLoop(
    JNIEnv *env, jclass cls, jbyteArray src, jbyteArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint dheight, jint dwidth, jint kw, jint kh,
    jfloatArray jhValues, jfloatArray jvValues) {
  uint8_t *srcData = (uint8_t *)env->GetPrimitiveArrayCritical(src, 0);
  uint8_t *dstData = (uint8_t *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);
  convolveBandsLoop<uint8_t, 3>(
      srcData, dstData, srcScanlineOffset, dstScanlineOffset, srcScanlineStride,
      dstScanlineStride, dheight, dwidth, kw, kh, hValues, vValues);
  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cInterleaved3ShortLoop(
    JNIEnv *env, jclass cls, jshortArray src, jshortArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint dheight, jint dwidth, jint kw, jint kh,
    jfloatArray jhValues, jfloatArray jvValues) {
  int16_t *srcData = (int16_t *)env->GetPrimitiveArrayCritical(src, 0);
  int16_t *dstData = (int16_t *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  convolveBandsLoop<int16_t, 3>(
      srcData, dstData, srcScanlineOffset, dstScanlineOffset, srcScanlineStride,
      dstScanlineStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cInterleaved3UShortLoop(
    JNIEnv *env, jclass cls, jshortArray src, jshortArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint dheight, jint dwidth, jint kw, jint kh,
    jfloatArray jhValues, jfloatArray jvValues) {
  uint16_t *srcData = (uint16_t *)env->GetPrimitiveArrayCritical(src, 0);
  uint16_t *dstData = (uint16_t *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  convolveBandsLoop<uint16_t, 3>(
      srcData, dstData, srcScanlineOffset, dstScanlineOffset, srcScanlineStride,
      dstScanlineStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lightcrafts_jai_opimage_Convolutions_cInterleaved3IntLoop(
    JNIEnv *env, jclass cls, jintArray src, jintArray dst,
    jint srcScanlineOffset, jint dstScanlineOffset, jint srcScanlineStride,
    jint dstScanlineStride, jint dheight, jint dwidth, jint kw, jint kh,
    jfloatArray jhValues, jfloatArray jvValues) {
  int *srcData = (int *)env->GetPrimitiveArrayCritical(src, 0);
  int *dstData = (int *)env->GetPrimitiveArrayCritical(dst, 0);
  float *hValues = (float *)env->GetPrimitiveArrayCritical(jhValues, 0);
  float *vValues = (float *)env->GetPrimitiveArrayCritical(jvValues, 0);

  convolveBandsLoop<int, 3>(
      srcData, dstData, srcScanlineOffset, dstScanlineOffset, srcScanlineStride,
      dstScanlineStride, dheight, dwidth, kw, kh, hValues, vValues);

  env->ReleasePrimitiveArrayCritical(src, srcData, 0);
  env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
  env->ReleasePrimitiveArrayCritical(jhValues, hValues, 0);
  env->ReleasePrimitiveArrayCritical(jvValues, vValues, 0);
}
