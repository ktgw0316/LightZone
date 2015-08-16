/* Copyright (C) 2015 Masahiro Kitagawa */

#include <cmath>
#include <iostream>
#include <jni.h>
#include <lensfun/lensfun.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_DistortionOpImage.h"
#endif

#include "LC_JNIUtils.h"

const float  distance = 0.0; // TODO;
const float     scale = 0.0; // automatic scaling
const lfLensType geom = LF_RECTILINEAR; // TODO;
const int       flags = LF_MODIFY_SCALE | LF_MODIFY_GEOMETRY | LF_MODIFY_DISTORTION;
const bool    inverse = false;
float  crop;
float  focal;
float  aperture;

/*
 * Round float to int
 */
inline static int ftoi(float f) {
    return f > 0 ? int(f + 0.5) : int(f - 0.5);
}

inline unsigned short BilinearInterp
( const unsigned short *data, const int lineStride, const int offset,
  const float x, const float y)
{
    const float x_floor = floor(x);
    const float y_floor = floor(y); 

    const int pos_tl = 3*x_floor + y_floor * lineStride; // top-left
    const int pos_tr = pos_tl + 3;                       // top-right
    const int pos_bl = pos_tl + lineStride;              // bottom-left
    const int pos_br = pos_bl + 3;                       // bottom-right

    const unsigned short data_tl = data[pos_tl + offset]; 
    const unsigned short data_tr = data[pos_tr + offset]; 
    const unsigned short data_bl = data[pos_bl + offset]; 
    const unsigned short data_br = data[pos_br + offset];

    // Using int is faster than using float
    const int wait_b = 256.f * (y - y_floor);
    const int wait_t = 256 - wait_b;
    const int wait_r = 256.f * (x - x_floor);
    const int wait_l = 256 - wait_r;
    
    return (wait_t * (wait_l * data_tl + wait_r * data_tr) +
            wait_b * (wait_l * data_bl + wait_r * data_br)) >> 16;
}

inline float Coeff(const float k1, const float k2, const float radiusSq)
{
    // 5th order polynomial distortion model, scaled
    return (1 + k1 * radiusSq + k2 * radiusSq * radiusSq) / (1 + k1 + k2);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_distortion
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdestData,
  jint fullWidth, jint fullHeight,
  jint rectX, jint rectY, jint rectWidth, jint rectHeight,
  jint srcROffset, jint srcGOffset, jint srcBOffset,
  jint destROffset, jint destGOffset, jint destBOffset,
  jint srcLineStride, jint destLineStride,
  jfloat k1, jfloat k2, jfloat kr, jfloat kb)
{
    unsigned short  *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);

    const float centerX = 0.5 * fullWidth;
    const float centerY = 0.5 * fullHeight;

    const float maxRadiusSq = centerX * centerX + centerY * centerY;

#pragma omp parallel for schedule (guided)
    for (int y = rectY; y < rectY + rectHeight; ++y) {
        const float offY = y - centerY;

        for (int x = rectX; x < rectX + rectWidth; ++x) {
            const float offX = x - centerX;
            const float radiusSq = (offX * offX + offY * offY) / maxRadiusSq;
            const float coeff = Coeff(k1, k2, radiusSq);

            const float gX = coeff * offX + centerX;
            const float gY = coeff * offY + centerY;

            // Skip the pixels outside the source image
            if (gX < 1 || gX >= fullWidth - 1 || gY < 1 || gY >= fullHeight - 1)
                continue;

            // Linear lateral chromatic aberrations model
            const float rX = kr * coeff * offX + centerX;
            const float rY = kr * coeff * offY + centerY;
            const float bX = kb * coeff * offX + centerX;
            const float bY = kb * coeff * offY + centerY;

            const int dstIdx = 3*(x - rectX) + (y - rectY) * destLineStride;
            destData[dstIdx + destROffset] = BilinearInterp(srcData, srcLineStride, srcROffset, rX, rY);
            destData[dstIdx + destGOffset] = BilinearInterp(srcData, srcLineStride, srcGOffset, gX, gY);
            destData[dstIdx + destBOffset] = BilinearInterp(srcData, srcLineStride, srcBOffset, bX, bY);
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_DistortionOpImage_lensfun
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdestData,
  jint fullWidth, jint fullHeight,
  jint rectX, jint rectY, jint rectWidth, jint rectHeight,
  jint srcROffset, jint srcGOffset, jint srcBOffset,
  jint destROffset, jint destGOffset, jint destBOffset,
  jint srcLineStride, jint destLineStride,
  jstring lensNameStr )
{
    unsigned short  *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);

    // Load lensfun database
    const char *lensName = env->GetStringUTFChars(lensNameStr, NULL);
    lfDatabase *ldb = lf_db_new();
    ldb->Load();

    // Get lens informations from the database
    const lfLens **lenses = ldb->FindLenses(NULL, NULL, lensName);

    if (!lenses)
    {
        std::cerr << "Cannot find the lens " << lensName << " in database" << std::endl;
        // TODO: Copy src to dst
        env->ReleaseStringUTFChars(lensNameStr, 0);
        env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
        env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
        return;
    }

    const lfLens *lens = lenses[0];
    env->ReleaseStringUTFChars(lensNameStr, 0);

    crop = lens->CropFactor;
    focal = lens->MinFocal;
    aperture = lens->MinAperture;

    lfLensCalibDistortion** dists = lens->CalibDistortion;
    lfLensCalibDistortion* dist = dists[0];
    const lfDistortionModel model = dist->Model;
    const float focal = dist->Focal;
    const float* terms = dist->Terms;

#ifdef DEBUG
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
            std::cerr << "Terms: " << terms[0] << ", " << terms[1] << ", " << terms[2] << std::endl;
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
                const int dstIdx = 3*x + y * destLineStride;
#if 0
                //
                // No interporation
                //

                const int rX = ftoi(src[i+0]), rY = ftoi(src[i+1]);
                const int gX = ftoi(src[i+2]), gY = ftoi(src[i+3]);
                const int bX = ftoi(src[i+4]), bY = ftoi(src[i+5]);

                if (rX < 0 || rX >= fullWidth || rY < 0 || rY >= fullHeight ||
                    gX < 0 || gX >= fullWidth || gY < 0 || gY >= fullHeight ||
                    bX < 0 || bX >= fullWidth || bY < 0 || bY >= fullHeight)
                    continue;

                const int rPos = 3*rX + rY * srcLineStride;
                const int gPos = 3*gX + gY * srcLineStride;
                const int bPos = 3*bX + bY * srcLineStride;

                destData[dstIdx + destROffset] = srcData[rPos + srcROffset];
                destData[dstIdx + destGOffset] = srcData[gPos + srcGOffset];
                destData[dstIdx + destBOffset] = srcData[bPos + srcBOffset];
#else
                //
                // Bilinear interporations
                //

                const float rX = src[i+0], rY = src[i+1];
                const float gX = src[i+2], gY = src[i+3];
                const float bX = src[i+4], bY = src[i+5];

                if (rX < 1 || rX >= fullWidth - 1 || rY < 1 || rY >= fullHeight - 1 ||
                    gX < 1 || gX >= fullWidth - 1 || gY < 1 || gY >= fullHeight - 1 ||
                    bX < 1 || bX >= fullWidth - 1 || bY < 1 || bY >= fullHeight - 1)
                    continue;

                destData[dstIdx + destROffset] = BilinearInterp(srcData, srcLineStride, srcROffset, rX, rY);
                destData[dstIdx + destGOffset] = BilinearInterp(srcData, srcLineStride, srcGOffset, gX, gY);
                destData[dstIdx + destBOffset] = BilinearInterp(srcData, srcLineStride, srcBOffset, bX, bY);
#endif
            }
        }

        delete[] pos;
    } // omp parallel

    lf_free(mod);
    lf_free(lenses);
    ldb->Destroy();
    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
}

