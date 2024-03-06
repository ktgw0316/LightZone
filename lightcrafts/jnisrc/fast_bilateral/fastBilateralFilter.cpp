/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <cmath>
#include <cstdint>

#define NO_XML
#include "include/fast_lbf.h"
#include "mathlz.h"

#include <jni.h>
#include <omp.h>

using std::ceil;
using std::clamp;

typedef Array_2D<float> image_type;

JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_FastBilateralFilterOpImage_fastBilateralFilterMono
(   JNIEnv *env, jclass cls,
    jshortArray jsrcData, jshortArray jdestData,
    jfloat sigma_s, jfloat sigma_r,
    jint width, jint height,
    jint srcPixelStride, jint destPixelStride,
    jint srcOffset, jint destOffset,
    jint srcLineStride, jint destLineStride,
    jfloatArray jtransform)
{
    const auto srcData = static_cast<uint16_t*>(env->GetPrimitiveArrayCritical(jsrcData, 0));
    const auto destData = static_cast<uint16_t*>(env->GetPrimitiveArrayCritical(jdestData, 0));
    const auto transform = static_cast<float*>(env->GetPrimitiveArrayCritical(jtransform, 0));

    const float inv_max = 1.0f / 0xffff;

    image_type image(width, height);
    image_type profile(width, height);

#pragma omp parallel for
    for(int y = 0; y < height; y++) {
#pragma omp simd
        for(int x = 0; x < width; x++) {
            int g = srcData[srcPixelStride * x + y * srcLineStride + srcOffset];
            image(x, y) = g * inv_max;
            profile(x, y) = transform[g];
        }
    }

    const int padding = 2 * ceil(sigma_s);

    image_type filtered_image(width-2*padding, height-2*padding);
    image_type weight(width-2*padding, height-2*padding);

    Image_filter::fast_LBF(image, profile, sigma_s, sigma_r, &weight, &filtered_image);

    const float inv_sq_sigma_s = 1.0f / (sigma_s * sigma_s);

#pragma omp parallel for
    for(int y = 0; y < height - 2 * padding; y++) {
#pragma omp simd
        for(int x = 0; x < width - 2 * padding; x++) {
            const float BF = filtered_image(x, y);
            const float W = weight(x, y) * inv_sq_sigma_s;
            const auto bf = static_cast<uint16_t>(clamp(BF * 0xffff, 0.0f, static_cast<float>(0xffff)));
            const auto w =  static_cast<uint16_t>(clamp(W * 0xffff, 0.0f, static_cast<float>(0xffff)));
            destData[destPixelStride * x + y * destLineStride + destOffset] = bf;
            destData[destPixelStride * x + y * destLineStride + destOffset + 1] = w;
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
    env->ReleasePrimitiveArrayCritical(jtransform, transform, 0);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_FastBilateralFilterOpImage_fastBilateralFilterChroma
(   JNIEnv *env, jclass cls,
    jshortArray jsrcData, jshortArray jdestData,
    jfloat sigma_s, jfloat sigma_r,
    jint width, jint height,
    jint srcPixelStride, jint destPixelStride,
    jint srcROffset, jint srcGOffset, jint srcBOffset,
    jint destROffset, jint destGOffset, jint destBOffset,
    jint srcLineStride, jint destLineStride )
{
    const auto srcData = static_cast<uint16_t*>(env->GetPrimitiveArrayCritical(jsrcData, 0));
    const auto destData = static_cast<uint16_t*>(env->GetPrimitiveArrayCritical(jdestData, 0));

    const float inv_max = 1.0f / 0xffff;

    image_type a_image(width, height);
    image_type b_image(width, height);

#pragma omp parallel for
    for(int y = 0; y < height; y++) {
#pragma omp simd
        for(int x = 0; x < width; x++) {
            float a = srcData[srcPixelStride * x + y * srcLineStride + srcGOffset] * inv_max;
            float b = srcData[srcPixelStride * x + y * srcLineStride + srcBOffset] * inv_max;
            a_image(x, y) = a;
            b_image(x, y) = b;
        }
    }

    image_type filtered_a_image(width, height);
    image_type filtered_b_image(width, height);

    Image_filter::fast_LBF(a_image, a_image, sigma_s, sigma_r, &filtered_a_image, &filtered_a_image);
    Image_filter::fast_LBF(b_image, b_image, sigma_s, sigma_r, &filtered_b_image, &filtered_b_image);

    const int padding = 2 * ceil(sigma_s);

#pragma omp parallel for
    for(int y = 0; y < height - 2 * padding; y++) {
#pragma omp simd
        for(int x = 0; x < width - 2 * padding; x++) {
            const float A = filtered_a_image(x+padding, y+padding);
            const float B = filtered_b_image(x+padding, y+padding);
            const auto l = srcData[srcPixelStride * (x+padding) + (y+padding) * srcLineStride + srcROffset];
            const auto a = static_cast<uint16_t>(clamp(A * 0xffff, 0.0f, static_cast<float>(0xffff)));
            const auto b = static_cast<uint16_t>(clamp(B * 0xffff, 0.0f, static_cast<float>(0xffff)));
            destData[destPixelStride * x + y * destLineStride + destROffset] = l;
            destData[destPixelStride * x + y * destLineStride + destGOffset] = a;
            destData[destPixelStride * x + y * destLineStride + destBOffset] = b;
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
}
