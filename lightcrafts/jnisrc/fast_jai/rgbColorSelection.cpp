/* Copyright (C) 2005-2011 Fabio Riccardi */

typedef unsigned char byte;
typedef unsigned short ushort;

#include <stdlib.h>
#include <math.h>

static inline float orig_fast_log2 (float val)
{
    int * const  exp_ptr = reinterpret_cast <int *> (&val);
    int          x = *exp_ptr;
    const int    log_2 = ((x >> 23) & 255) - 128;
    x &= ~(255 << 23);
    x += 127 << 23;
    *exp_ptr = x;
    
    return (val + log_2);
}

typedef union { float f; int i; } floatint_union;

static inline float fast_log2 (float val)
{
    floatint_union * vval =  (floatint_union*)&val ;
    const int    log_2 = ((vval->i >> 23) & 255) - 128;
    vval->i &= ~(255 << 23);
    vval->i += 127 << 23;
    
    return (vval->f + log_2);
}


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

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_RGBColorSelectionMaskOpImage.h"
#endif

JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_RGBColorSelectionMaskOpImage_nativeUshortLoop
(JNIEnv *env, jobject cls, jshortArray jsrcData, jbyteArray jdstData,
 jint width, jint height, jintArray jsrcBandOffsets,
 jint dstOffset, jint srcLineStride, jint dstLineStride,
 jfloatArray jcolorSelection, jboolean inverted)
{
    ushort *srcData = (ushort *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    byte *dstData = (byte *) env->GetPrimitiveArrayCritical(jdstData, 0);
    int *srcBandOffsets = (int *) env->GetPrimitiveArrayCritical(jsrcBandOffsets, 0);
    float *colorSelection = (float *) env->GetPrimitiveArrayCritical(jcolorSelection, 0);
    
    int srcROffset = srcBandOffsets[0];
    int srcGOffset = srcBandOffsets[1];
    int srcBOffset = srcBandOffsets[2];
    
    float sL                        = colorSelection[0];
    float sa                        = colorSelection[1];
    float sb                        = colorSelection[2];
    float radius                    = colorSelection[3];
    float luminosityLower           = colorSelection[4];
    float luminosityLowerFeather    = colorSelection[5];
    float luminosityUpper           = colorSelection[6];
    float luminosityUpperFeather    = colorSelection[7];
    
	const float rmin = (3 * radius) / 16;
	const float rmax = (5 * radius) / 16;

    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            float L = srcData[3 * col + row * srcLineStride + srcROffset];
            float a = srcData[3 * col + row * srcLineStride + srcGOffset] / (float) 0xffff;
            float b = srcData[3 * col + row * srcLineStride + srcBOffset] / (float) 0xffff;
            
            float brightnessMask, colorMask;
            
            if (radius >= 0) {
                
                float da = sa - a;
                float db = sb - b;
                float m = da * da + db * db;
                m = m * inv_sqrt(m);
                if (m < rmin)
                    colorMask = 1;
                else if (m < rmax)
                    colorMask = (rmax - m) / (rmax - rmin);
                else
                    colorMask = 0;
            } else
                colorMask = 1;
            
            if (luminosityLower > 0 || luminosityUpper < 1) {
#if defined(__ppc__)
	            float luminosity = log2f(L / 0x100 + 1)/8;
#else
                float luminosity = fast_log2(L / 256.0F + 1.0F)/8;
#endif
                if (luminosity > 1)
                    luminosity = 1;
                
                if (luminosity >= luminosityLower && luminosity <= luminosityUpper)
                    brightnessMask = 1;
                else if (luminosity >= (luminosityLower - luminosityLowerFeather) && luminosity < luminosityLower)
                    brightnessMask = (luminosity - (luminosityLower - luminosityLowerFeather))/luminosityLowerFeather;
                else if (luminosity > luminosityUpper && luminosity <= (luminosityUpper + luminosityUpperFeather))
                    brightnessMask = (luminosityUpper + luminosityUpperFeather - luminosity)/luminosityUpperFeather;
                else
                    brightnessMask = 0;
                
                colorMask *= brightnessMask;
            }
            
            if (inverted)
                colorMask = 1-colorMask;
            
            dstData[col + row * dstLineStride + dstOffset] = (byte) (0xff * colorMask);
        }
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
    env->ReleasePrimitiveArrayCritical(jsrcBandOffsets, srcBandOffsets, 0);
    env->ReleasePrimitiveArrayCritical(jcolorSelection, colorSelection, 0);
}
