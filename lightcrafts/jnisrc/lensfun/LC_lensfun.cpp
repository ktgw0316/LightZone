/* Copyright (C) 2015 Masahiro Kitagawa */

#include <cmath>
#include <iostream>
#include <jni.h>
#include <omp.h>
#include <lensfun.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_DistortionOpImage.h"
#endif

#include "LC_JNIUtils.h"

#include "interpolation.h"

inline float Coeff(const float k1, const float k2, const float radiusSq)
{
    // 5th order polynomial distortion model, scaled
    return (1 + k1 * radiusSq + k2 * radiusSq * radiusSq) / (1 + k1 + k2);
}

void correct_distortion_mono
( const unsigned short *srcData, unsigned short *dstData,
  const int fullWidth, const int fullHeight,
  const int rectX, const int rectY, const int rectWidth, const int rectHeight,
  const int srcPixelStride, const int dstPixelStride,
  const int srcOffset, const int dstOffset,
  const int srcLineStride, const int dstLineStride,
  const float k1, const float k2, const float magnitude )
{
    const float centerX = 0.5 * fullWidth;
    const float centerY = 0.5 * fullHeight;

    const float maxRadiusSq = centerX * centerX + centerY * centerY;

#pragma omp parallel for schedule (guided)
    for (int y = rectY; y < rectY + rectHeight; ++y) {
        const float offY = y - centerY;

        for (int x = rectX; x < rectX + rectWidth; ++x) {
            // Calc distortion
            const float offX = x - centerX;
            const float radiusSq = (offX * offX + offY * offY) / maxRadiusSq;
            const float coeff = Coeff(k1, k2, radiusSq);

            const float srcX = magnitude * coeff * offX + centerX;
            const float srcY = magnitude * coeff * offY + centerY;

            // Skip the pixels outside of the source image
            if (srcX < 1 || srcX >= fullWidth  - 1 ||
                srcY < 1 || srcY >= fullHeight - 1)
                continue;

            const int dstIdx =
                dstPixelStride * (x - rectX) + (y - rectY) * dstLineStride;
            const unsigned short value =
                // BilinearInterp(srcData, srcPixelStride, srcOffset, srcLineStride,
                               // srcX, srcY);
                MitchellInterp(srcData, srcPixelStride, srcOffset, srcLineStride,
                               srcX, srcY);
            dstData[dstIdx + dstOffset] = value < 0xffff ? value : 0xffff;
        }
    }
}

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_distortionMono
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdstData,
  jint fullWidth, jint fullHeight,
  jint rectX, jint rectY, jint rectWidth, jint rectHeight,
  jint srcPixelStride, jint dstPixelStride,
  jint srcOffset, jint dstOffset,
  jint srcLineStride, jint dstLineStride,
  jfloat k1, jfloat k2 )
{
    unsigned short *srcData = (unsigned short *)env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *dstData = (unsigned short *)env->GetPrimitiveArrayCritical(jdstData, 0);

    correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
            rectX, rectY, rectWidth, rectHeight, srcPixelStride,dstPixelStride,
            srcOffset, dstOffset, srcLineStride, dstLineStride,
            k1, k2, 1.f);

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
}

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_distortionColor
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdstData,
  jint fullWidth, jint fullHeight,
  jint rectX, jint rectY, jint rectWidth, jint rectHeight,
  jint srcPixelStride, jint dstPixelStride,
  jint srcROffset, jint srcGOffset, jint srcBOffset,
  jint dstROffset, jint dstGOffset, jint dstBOffset,
  jint srcLineStride, jint dstLineStride,
  jfloat k1, jfloat k2, jfloat kr, jfloat kb )
{
    unsigned short *srcData = (unsigned short *)env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *dstData = (unsigned short *)env->GetPrimitiveArrayCritical(jdstData, 0);

#pragma omp parallel
#pragma omp for single nowait
    {
        // Red
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
                rectX, rectY, rectWidth, rectHeight, srcPixelStride,dstPixelStride,
                srcROffset, dstROffset, srcLineStride, dstLineStride,
                k1, k2, kr);

        // Green
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
                rectX, rectY, rectWidth, rectHeight, srcPixelStride,dstPixelStride,
                srcGOffset, dstGOffset, srcLineStride, dstLineStride,
                k1, k2, 1);

        // Blue
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
                rectX, rectY, rectWidth, rectHeight, srcPixelStride,dstPixelStride,
                srcBOffset, dstBOffset, srcLineStride, dstLineStride,
                k1, k2, kb);
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
}

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_lensfun
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdstData,
  jint fullWidth, jint fullHeight,
  jint rectX, jint rectY, jint rectWidth, jint rectHeight,
  jint srcPixelStride, jint dstPixelStride,
  jint srcROffset, jint srcGOffset, jint srcBOffset,
  jint dstROffset, jint dstGOffset, jint dstBOffset,
  jint srcLineStride, jint dstLineStride,
  jstring cameraMakerStr, jstring cameraModelStr,
  jstring lensNameStr, jfloat focal, jfloat aperture )
{
    unsigned short *srcData = (unsigned short *)env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *dstData = (unsigned short *)env->GetPrimitiveArrayCritical(jdstData, 0);

    const float  distance = 0.0; // TODO;
    const float     scale = 0.0; // automatic scaling
    const lfLensType geom = LF_RECTILINEAR; // TODO;
    const int       flags = LF_MODIFY_SCALE | LF_MODIFY_GEOMETRY | LF_MODIFY_DISTORTION;
    const bool    inverse = false;
    float crop;

    const char *cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const char *cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);
    const char *lensName    = env->GetStringUTFChars(lensNameStr, NULL);

    // Load lensfun database
    lfDatabase *ldb = lf_db_new();
    ldb->Load();

    // Find camera in the database
    const lfCamera *cam = NULL;
    const lfCamera **cameras = ldb->FindCamerasExt(cameraMaker, cameraModel);

    if (!cameras) {
        std::cerr << "Cannot find the camera " << cameraModel << " in database" << std::endl;
    }
    else {
        cam = cameras[0];
        std::cerr << "camera: " << cam->Model << std::endl; // DEBUG
    }
    lf_free(cameras);
    env->ReleaseStringUTFChars(cameraMakerStr, 0);
    env->ReleaseStringUTFChars(cameraModelStr, 0);

    // Get lens informations from the database
    const lfLens **lenses = ldb->FindLenses(cam, NULL, lensName);

    if (!lenses) {
        std::cerr << "Cannot find the lens " << lensName << " in database" << std::endl;
        env->ReleaseStringUTFChars(lensNameStr, 0);
        // TODO: Copy src to dst
        env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
        env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
        return;
    }

    for (int i = 0, imax = sizeof(lenses) / sizeof(lenses[0]); i < imax; ++i) {
        std::cerr << "lense: " << lenses[i]->Model << std::endl; // DEBUG
    }
    const lfLens *lens = lenses[0];
    lf_free(lenses);
    env->ReleaseStringUTFChars(lensNameStr, 0);

    crop = lens->CropFactor;
    if (focal < 0.1f)
        focal = lens->MaxFocal;
    if (aperture < 0.1f)
        aperture = lens->MinAperture;

#ifdef DEBUG
    lfLensCalibDistortion** dists = lens->CalibDistortion;
    lfLensCalibDistortion* dist = dists[0];
    const lfDistortionModel model = dist->Model;
    const float* terms = dist->Terms;

    switch (model) {
        case LF_DIST_MODEL_POLY3:
            std::cerr << "DistortionModel: poly3" << std::endl;
            std::cerr << "Terms: " << terms[0] << std::endl;
            break;
        case LF_DIST_MODEL_POLY5:
            std::cerr << "DistortionModel: poly5" << std::endl;
            std::cerr << "Terms: " << terms[0] << ", " << terms[1] << std::endl;
            break;
        case LF_DIST_MODEL_PTLENS:
            std::cerr << "DistortionModel: PTLens" << std::endl;
            std::cerr << "Terms: " << terms[0] << ", " << terms[1] << ", "
                      << terms[2] << std::endl;
            break;
        default:
            std::cerr << "DistortionModel: Unknown" << std::endl;
    }

    std::cerr << "full size: " << fullWidth << ", " << fullHeight << std::endl;
    std::cerr << "rect pos.: " << rectX << ", " << rectY << std::endl;
    std::cerr << "rect size: " << rectWidth << ", " << rectHeight << std::endl;
#endif

	// Define modifier
    lfModifier *mod = lfModifier::Create(lens, crop, fullWidth, fullHeight);
    int modflags = mod->Initialize(lens, LF_PF_U8, focal, aperture,
                                   distance, scale, geom, flags, inverse);

    // Apply modifier
#pragma omp parallel
    {
        float *pos = new float[rectWidth * 2 * 3];

        // TCA and geometry correction
#pragma omp for schedule (guided)
        for (int y = 0; y < rectHeight; ++y) {
            mod->ApplySubpixelGeometryDistortion(rectX, y + rectY, rectWidth, 1, pos);

            float *src = pos;

            for (int x = 0, i = 0; x < rectWidth; ++x, i += 6) {
                const int dstIdx = dstPixelStride * x + y * dstLineStride;

                const float rX = src[i+0], rY = src[i+1];
                const float gX = src[i+2], gY = src[i+3];
                const float bX = src[i+4], bY = src[i+5];

                if (rX < 1 || rX >= fullWidth - 1 || rY < 1 || rY >= fullHeight - 1 ||
                    gX < 1 || gX >= fullWidth - 1 || gY < 1 || gY >= fullHeight - 1 ||
                    bX < 1 || bX >= fullWidth - 1 || bY < 1 || bY >= fullHeight - 1)
                    continue;

                dstData[dstROffset + dstIdx] = BilinearInterp(srcData, srcPixelStride, srcROffset, srcLineStride, rX, rY);
                dstData[dstGOffset + dstIdx] = BilinearInterp(srcData, srcPixelStride, srcGOffset, srcLineStride, gX, gY);
                dstData[dstBOffset + dstIdx] = BilinearInterp(srcData, srcPixelStride, srcBOffset, srcLineStride, bX, bY);
            }
        }

        delete[] pos;
    } // omp parallel

    lf_free(mod);
    ldb->Destroy();
    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
}

