/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cmath>

#include <algorithm>
#include <fstream>
#include <iostream>
#include <sstream>

#ifndef M_LN2
#define M_LN2		0.69314718055994530942
#define M_PI		3.14159265358979323846
#endif

#define NO_XML
// #define CHRONO
#include "include/geom.h"
#include "include/fast_lbf.h"

using namespace std; 

static inline float fast_log2 (float val)
{
    int * const  exp_ptr = reinterpret_cast <int *> (&val);
    int          x = *exp_ptr;
    const int    log_2 = ((x >> 23) & 255) - 128;
    x &= ~(255 << 23);
    x += 127 << 23;
    *exp_ptr = x;
    
    return (val + log_2);
}

static inline float fast_log (float x) {
    return fast_log2(x) / 1.394409;
}

// Very fast approximation of exp, faster than lookup table!
// See paper of Nicol N. Schraudolph, adapted here to single precision floats.

// static inline float fast_exp(float val) __attribute__ ((always_inline));
static inline float fast_exp(float val)
{
    const float fast_exp_a = (1 << 23)/M_LN2;	
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

// static inline float inv_sqrt(float x) __attribute__ ((always_inline));
static inline float inv_sqrt(float x) 
{ 
    float xhalf = 0.5f * x; 
    union {
        float f;
        unsigned int i;
    } n;
    
    n.f = x;                          // get bits for floating value 
    n.i = 0x5f375a86 - (n.i>>1);      // gives initial guess y0 
    x = n.f;                          // convert bits back to float 
    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
//    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
//    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
//    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
    return x; 
}

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

typedef Array_2D<Geometry::Vec3<float> > rgb_image_type;

extern "C" JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_FastBilateralFilterOpImage_fastBilateralFilterColor
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
    
    rgb_image_type image(width,height);
    
    for(unsigned y = 0; y < height; y++)
        for(unsigned x = 0; x < width; x++) {
            int idx = srcPixelStride * x + y * srcLineStride;
            image(x,y)[0] = sqrtf(srcData[idx + srcROffset] / (float) 0xffff);
            image(x,y)[1] = sqrtf(srcData[idx + srcGOffset] / (float) 0xffff);
            image(x,y)[2] = sqrtf(srcData[idx + srcBOffset] / (float) 0xffff);
        }
    
    rgb_image_type filtered_image(width,height);
    
//    Image_filter::fast_LBF(image, image,
//                           sigma_s, sigma_r,
//                           false,
//                           &filtered_image, &filtered_image);
    
    Image_filter::fast_color_BF(image,
                                sigma_s,sigma_r,
                                &filtered_image);
 
    int padding = 2 * ceil(sigma_s);
    
    for(unsigned y = 0; y < height - 2 * padding; y++)
        for(unsigned x = 0; x < width - 2 * padding; x++) {
            const float R = filtered_image(x+padding, y+padding)[0];
            const float G = filtered_image(x+padding, y+padding)[1];
            const float B = filtered_image(x+padding, y+padding)[2];

            const unsigned short r = static_cast<unsigned short>(Math_tools::clamp(0.0, 0xffff, R * R * 0xffff));
            const unsigned short g = static_cast<unsigned short>(Math_tools::clamp(0.0, 0xffff, G * G * 0xffff));
            const unsigned short b = static_cast<unsigned short>(Math_tools::clamp(0.0, 0xffff, B * B * 0xffff));

            int idx = destPixelStride * x + y * destLineStride;
            
            destData[idx + destROffset] = r;
            destData[idx + destGOffset] = g;
            destData[idx + destBOffset] = b;
        }
            
            env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
}
