/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cmath>

#include <algorithm>
#include <fstream>
#include <iostream>
#include <sstream>

#define NO_XML
// #define CHRONO
#include "include/fast_lbf.h"
#include "include/geom.h"
#include "mathlz.h"

using namespace std;

#include <jni.h>

typedef Array_2D<float> image_type;

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_FastBilateralFilterOpImage_fastBilateralFilterMono
(   JNIEnv *env, jclass cls,
    jshortArray jsrcData, jshortArray jdestData,
    jfloat sigma_s, jfloat sigma_r,
    jint width, jint height,
    jint srcPixelStride, jint destPixelStride,
    jint srcOffset, jint destOffset,
    jint srcLineStride, jint destLineStride,
    jfloatArray jtransform)
{
    unsigned short *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);
    float *transform = (float *) env->GetPrimitiveArrayCritical(jtransform, 0);

    const int padding = 2 * ceil(sigma_s);

    image_type image(width,height);
    image_type profile(width,height);

    for(unsigned y = 0; y < height; y++)
        for(unsigned x = 0; x < width; x++) {
            int g = srcData[srcPixelStride * x + y * srcLineStride + srcOffset];
            image(x,y) = g / (float) 0xffff;
            profile(x,y) = transform[g];
        }

    image_type filtered_image(width-2*padding,height-2*padding);
    image_type weight(width-2*padding,height-2*padding);

    Image_filter::fast_LBF(image, profile,
                           sigma_s, sigma_r,
                           false,
                           &weight, &filtered_image);

    for(unsigned y = 0; y < height - 2 * padding; y++)
        for(unsigned x = 0; x < width - 2 * padding; x++) {
            const float BF = filtered_image(x, y);
            const float W = weight(x, y)/(sigma_s*sigma_s);
            const unsigned short bf = static_cast<unsigned short>(Math_tools::clamp(0.0, 0xffff, BF * 0xffff));
            destData[destPixelStride * x + y * destLineStride + destOffset] = bf;
            const unsigned short w = static_cast<unsigned short>(Math_tools::clamp(0.0, 0xffff, W * 0xffff));
            destData[destPixelStride * x + y * destLineStride + destOffset + 1] = w;
        }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
    env->ReleasePrimitiveArrayCritical(jtransform, transform, 0);
}

inline float fsqrt(float x) {
    return x == 0 ? 0 : x * inv_sqrt(x);
}

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_FastBilateralFilterOpImage_fastBilateralFilterChroma
(   JNIEnv *env, jclass cls,
    jshortArray jsrcData, jshortArray jdestData,
    jfloat sigma_s, jfloat sigma_r,
    jint width, jint height,
    jint srcPixelStride, jint destPixelStride,
    jint srcROffset, jint srcGOffset, jint srcBOffset,
    jint destROffset, jint destGOffset, jint destBOffset,
    jint srcLineStride, jint destLineStride )
{
    unsigned short *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);

    // image_type l_image(width,height);
    image_type a_image(width,height);
    image_type b_image(width,height);

    for(unsigned y = 0; y < height; y++)
        for(unsigned x = 0; x < width; x++) {
            float l = srcData[srcPixelStride * x + y * srcLineStride + srcROffset] / (float) 0xffff;
            float a = srcData[srcPixelStride * x + y * srcLineStride + srcGOffset] / (float) 0xffff;
            float b = srcData[srcPixelStride * x + y * srcLineStride + srcBOffset] / (float) 0xffff;

            // l_image(x,y) = fsqrt(l);
            a_image(x,y) = a;
            b_image(x,y) = b;
        }

    image_type filtered_a_image(width,height);
    image_type filtered_b_image(width,height);

    Image_filter::fast_LBF(a_image, a_image,
                           sigma_s, sigma_r,
                           false,
                           &filtered_a_image, &filtered_a_image);

    Image_filter::fast_LBF(b_image, b_image,
                           sigma_s, sigma_r,
                           false,
                           &filtered_b_image, &filtered_b_image);

    int padding = 2 * ceil(sigma_s);

    for(unsigned y = 0; y < height - 2 * padding; y++)
        for(unsigned x = 0; x < width - 2 * padding; x++) {
            const float A = filtered_a_image(x+padding, y+padding);
            const float B = filtered_b_image(x+padding, y+padding);

            int l = srcData[srcPixelStride * (x+padding) + (y+padding) * srcLineStride + srcROffset];

            destData[destPixelStride * x + y * destLineStride + destROffset] = l;

            const unsigned short a = static_cast<unsigned short>(Math_tools::clamp(0.0, 0xffff, A * 0xffff));
            destData[destPixelStride * x + y * destLineStride + destGOffset] = a;

            const unsigned short b = static_cast<unsigned short>(Math_tools::clamp(0.0, 0xffff, B * 0xffff));
            destData[destPixelStride * x + y * destLineStride + destBOffset] = b;
        }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
}
