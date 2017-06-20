/* Copyright (C) 2015- Masahiro Kitagawa */

#include <cmath>
#include <functional>
#include <jni.h>
#include <omp.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_DistortionOpImage.h"
#endif

#include "LC_JNIUtils.h"

#include "LC_lensfun.h"
#include "interpolation.h"

std::function<float(float)> makeCoeff(int distModelType, const float* k)
{
    switch (distModelType) {
    case 0:
        return [](float radiusSq){
            return 1;
        };
    case 1: // 3rd order polynomial distortion model
        return [k](float radiusSq){
            return (1 - k[0] + k[0] * radiusSq);
        };
    case 2: // 5th order polynomial distortion model
        return [k](float radiusSq){
            return (1 + k[0] * radiusSq + k[1] * radiusSq * radiusSq);
        };
    case 3: // PTLens distortion model
        return [k](float radiusSq){
            const float radius = sqrt(radiusSq);
            return (k[0] * radiusSq * radius + k[1] * radiusSq + k[2] * radius
                    + 1 - k[0] - k[1] - k[2]);
        };
    case 4: // Adobe distortion model
        return [](float radiusSq){
            // TODO:
            return 1;
        };
    default: // LightZone's scaled 5th order polynomial distortion model
        return [k](float radiusSq){
            return (1 + k[0] * radiusSq + k[1] * radiusSq * radiusSq) / (1 + k[0] + k[1]);
        };
    }
}

void correct_distortion_mono
( const unsigned short *srcData, unsigned short *dstData,
  const int fullWidth, const int fullHeight,
  const int centerX, const int centerY,
  const int srcRectX, const int srcRectY,
  const int srcRectWidth, const int srcRectHeight,
  const int dstRectX, const int dstRectY,
  const int dstRectWidth, const int dstRectHeight,
  const int srcPixelStride, const int dstPixelStride,
  const int srcOffset, const int dstOffset,
  const int srcLineStride, const int dstLineStride,
  std::function<float(float)> coeff,
  const float magnitude )
{
    const float maxRadiusSq = (fullWidth * fullWidth + fullHeight * fullHeight) / 4.0;

#pragma omp parallel for schedule (guided)
    for (int y = dstRectY; y < dstRectY + dstRectHeight; ++y) {
        const float offY = y - centerY;

        for (int x = dstRectX; x < dstRectX + dstRectWidth; ++x) {
            // Calc distortion
            const float offX = x - centerX;
            const float radiusSq = (offX * offX + offY * offY) / maxRadiusSq;
            const float c = coeff(radiusSq);

            const float srcX = magnitude * c * offX + centerX - srcRectX;
            const float srcY = magnitude * c * offY + centerY - srcRectY;

            const int dstIdx =
                dstPixelStride * (x - dstRectX) + (y - dstRectY) * dstLineStride;

            if (srcX < 0 || srcX >= srcRectWidth || srcY < 0 || srcY >= srcRectHeight) {
                dstData[dstIdx + dstOffset] = 0;
            }
            else {
                const unsigned short value =
                    BilinearInterp(srcData, srcPixelStride, srcOffset, srcLineStride,
                                   srcX, srcY);
                dstData[dstIdx + dstOffset] = value < 0xffff ? value : 0xffff;
            }
        }
    }
}

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_distortionMono
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdstData,
  jint fullWidth, jint fullHeight,
  jint centerX, jint centerY,
  jint srcRectX, jint srcRectY, jint srcRectWidth, jint srcRectHeight,
  jint dstRectX, jint dstRectY, jint dstRectWidth, jint dstRectHeight,
  jint srcPixelStride, jint dstPixelStride,
  jint srcOffset, jint dstOffset,
  jint srcLineStride, jint dstLineStride,
  jint distModelType, jfloatArray jDistTerms )
{
    unsigned short *srcData = (unsigned short *)env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *dstData = (unsigned short *)env->GetPrimitiveArrayCritical(jdstData, 0);
    jfloat* distTerms = env->GetFloatArrayElements(jDistTerms, 0);

    const float k[] = {distTerms[0], distTerms[1], distTerms[2]};
    auto coeff = makeCoeff(distModelType, k);

    correct_distortion_mono(srcData, dstData, fullWidth, fullHeight, centerX, centerY,
            srcRectX, srcRectY, srcRectWidth, srcRectHeight,
            dstRectX, dstRectY, dstRectWidth, dstRectHeight,
            srcPixelStride,dstPixelStride,
            srcOffset, dstOffset, srcLineStride, dstLineStride,
            coeff, 1.f);

    env->ReleaseFloatArrayElements(jDistTerms, distTerms, 0);
    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
}

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_distortionColor
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdstData,
  jint fullWidth, jint fullHeight,
  jint centerX, jint centerY,
  jint srcRectX, jint srcRectY, jint srcRectWidth, jint srcRectHeight,
  jint dstRectX, jint dstRectY, jint dstRectWidth, jint dstRectHeight,
  jint srcPixelStride, jint dstPixelStride,
  jint srcROffset, jint srcGOffset, jint srcBOffset,
  jint dstROffset, jint dstGOffset, jint dstBOffset,
  jint srcLineStride, jint dstLineStride,
  jint distModelType, jfloatArray jDistTerms,
  jfloatArray jTcaTerms )
{
    unsigned short *srcData = (unsigned short *)env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *dstData = (unsigned short *)env->GetPrimitiveArrayCritical(jdstData, 0);

    jfloat*  tcaTerms = env->GetFloatArrayElements(jTcaTerms, 0);
    const float kr = tcaTerms[0];
    const float kb = tcaTerms[1];

    jfloat* distTerms = env->GetFloatArrayElements(jDistTerms, 0);
    const float k[] = {distTerms[0], distTerms[1], distTerms[2]};
    auto coeff = makeCoeff(distModelType, k);

#pragma omp parallel shared (distModel)
#pragma omp for single nowait
    {
        // Red
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight, centerX, centerY,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride,dstPixelStride,
                srcROffset, dstROffset, srcLineStride, dstLineStride,
                coeff, kr);

        // Green
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight, centerX, centerY,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride,dstPixelStride,
                srcGOffset, dstGOffset, srcLineStride, dstLineStride,
                coeff, 1);

        // Blue
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight, centerX, centerY,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride,dstPixelStride,
                srcBOffset, dstBOffset, srcLineStride, dstLineStride,
                coeff, kb);
    }

    env->ReleaseFloatArrayElements(jTcaTerms, tcaTerms, 0);
    env->ReleaseFloatArrayElements(jDistTerms, distTerms, 0);
    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
}

