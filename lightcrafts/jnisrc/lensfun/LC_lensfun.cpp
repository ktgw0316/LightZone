/* Copyright (C) 2015 Masahiro Kitagawa */

#include <cmath>
#include <functional>
#include <iostream>
#include <jni.h>
#include <omp.h>
#include <lensfun.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_DistortionOpImage.h"
#endif

#include "LC_JNIUtils.h"

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
    default: // LightZone's scaled 5th order polynomial distortion model
        return [k](float radiusSq){
            return (1 + k[0] * radiusSq + k[1] * radiusSq * radiusSq) / (1 + k[0] + k[1]);
        };
    }
}

void correct_distortion_mono
( const unsigned short *srcData, unsigned short *dstData,
  const int fullWidth, const int fullHeight,
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
    const float centerX = 0.5 * fullWidth;
    const float centerY = 0.5 * fullHeight;

    const float maxRadiusSq = centerX * centerX + centerY * centerY;

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

    correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
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
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride,dstPixelStride,
                srcROffset, dstROffset, srcLineStride, dstLineStride,
                coeff, kr);

        // Green
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride,dstPixelStride,
                srcGOffset, dstGOffset, srcLineStride, dstLineStride,
                coeff, 1);

        // Blue
#pragma omp task mergable
        correct_distortion_mono(srcData, dstData, fullWidth, fullHeight,
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

extern "C"
JNIEXPORT jboolean JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_lensfunTerms
( JNIEnv *env, jclass cls,
  jintArray jDistModel, jfloatArray jDistTerms,
  jfloatArray jTcaTerms,
  jstring cameraMakerStr, jstring cameraModelStr,
  jstring lensNameStr, jfloat focal, jfloat aperture )
{
    const char *cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const char *cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);
    const char *lensName    = env->GetStringUTFChars(lensNameStr, NULL);

    // Load lensfun database
    lfDatabase *ldb = lf_db_new();
    ldb->Load();

    // Find camera in the database
    const lfCamera *camera = NULL;
    const lfCamera **cameras = ldb->FindCamerasExt(cameraMaker, cameraModel);

    if (!cameras) {
        std::cerr << "Cannot find the camera " << cameraModel << " in database" << std::endl;
    }
    else {
        camera = cameras[0];
        std::cerr << "camera: " << camera->Model << std::endl; // DEBUG
    }
    lf_free(cameras);
    env->ReleaseStringUTFChars(cameraMakerStr, 0);
    env->ReleaseStringUTFChars(cameraModelStr, 0);

    // Get lens informations from the database
    const lfLens **lenses = ldb->FindLenses(camera, NULL, lensName);

    if (!lenses) {
        std::cerr << "Cannot find the lens " << lensName << " in database" << std::endl;
        env->ReleaseStringUTFChars(lensNameStr, 0);
        return false;
    }

    for (int i = 0, imax = sizeof(lenses) / sizeof(lenses[0]); i < imax; ++i) {
        std::cerr << "lense" << i << ": " << lenses[i]->Model << std::endl; // DEBUG
    }
    const lfLens *lens = lenses[0];
    lf_free(lenses);
    env->ReleaseStringUTFChars(lensNameStr, 0);

    float crop = lens->CropFactor;
    if (focal < 0.1f) {
        focal = lens->MaxFocal;
    }
    if (aperture < 0.1f) {
        aperture = lens->MinAperture;
    }

    // Get distortion model and terms
    lfLensCalibDistortion dist;
    if (lens->InterpolateDistortion((float)focal, dist)) {
        const lfDistortionModel dist_model = dist.Model;
        float* dist_terms = dist.Terms;

        jint* distModel = env->GetIntArrayElements(jDistModel, 0);
        jfloat* distTerms = env->GetFloatArrayElements(jDistTerms, 0);

        switch (dist_model) {
        case LF_DIST_MODEL_POLY3:
            distModel[0] = 1;
            distTerms[0] = dist_terms[0];

            std::cerr << "DistortionModel: poly3" << std::endl;
            std::cerr << "Terms: " << dist_terms[0] << std::endl;
            break;
        case LF_DIST_MODEL_POLY5:
            distModel[0] = 2;
            distTerms[0] = dist_terms[0];
            distTerms[1] = dist_terms[1];

            std::cerr << "DistortionModel: poly5" << std::endl;
            std::cerr << "Terms: " << dist_terms[0] << ", "
                                   << dist_terms[1] << std::endl;
            break;
        case LF_DIST_MODEL_PTLENS:
            distModel[0] = 3;
            distTerms[0] = dist_terms[0];
            distTerms[1] = dist_terms[1];
            distTerms[2] = dist_terms[2];

            std::cerr << "DistortionModel: PTLens" << std::endl;
            std::cerr << "Terms: " << dist_terms[0] << ", "
                                   << dist_terms[1] << ", "
                                   << dist_terms[2] << std::endl;
            break;
        default:
            std::cerr << "DistortionModel: Unknown" << std::endl;
        }

        env->ReleaseIntArrayElements(jDistModel, distModel, 0);
        env->ReleaseFloatArrayElements(jDistTerms, distTerms, 0);
    }

    // Get TCA model and terms
    lfLensCalibTCA tca;
    if (lens->InterpolateTCA((float)focal, tca)) {
        const lfTCAModel tca_model = tca.Model;
        float* tca_terms = tca.Terms;

        // jint* tcaModel = env->GetIntArrayElements(jTcaModel, 0);
        jfloat* tcaTerms = env->GetFloatArrayElements(jTcaTerms, 0);

        switch (tca_model) {
        case LF_TCA_MODEL_LINEAR:
            // tcaModel[0] = 1;
            tcaTerms[0] = tca_terms[0];
            tcaTerms[1] = tca_terms[1];

            std::cerr << "TCAModel: Linear" << std::endl;
            std::cerr << "Terms: " << "kr = "<< tca_terms[0] << ", "
                                   << "kb = "<< tca_terms[1] << std::endl;
            break;
        case LF_TCA_MODEL_POLY3:
            // tcaModel[0] = 2;
            // for (int i = 0; i < 6; ++i) {
            //     tcaTerms[0] = tca_terms[0];
            // }

            // NOTE: LightZone currently supports linear TCA model only.
            tcaTerms[0] = 1;
            tcaTerms[1] = 1;

            std::cerr << "TCAModel: Poly3" << std::endl;
            std::cerr << "Terms: " << "br = " << tca_terms[0] << ", "
                                   << "cr = " << tca_terms[1] << ", "
                                   << "vr = " << tca_terms[2] << "; "
                                   << "bb = " << tca_terms[3] << ", "
                                   << "cb = " << tca_terms[4] << ", "
                                   << "vb = " << tca_terms[5] << std::endl;
            break;
        default:
            std::cerr << "TCAModel: Unknown" << std::endl;
        }

        env->ReleaseFloatArrayElements(jTcaTerms, tcaTerms, 0);
    }

    // TODO: Get vignetting model and terms

    ldb->Destroy();
    return true;
}
