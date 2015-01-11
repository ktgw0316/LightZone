/* Copyright (C) 2015 Masahiro Kitagawa */
#include <math.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include "include/yst.h"

/*******************************************************************************
 * separable_nlm_mono_tile()
 *
 * Apply a separable Non-Local Means filter to a rectangular region of a single-band
 * raster
 *
 * Dimensions of source and destination rectangles are related by
 *
 *     dst_width = src_width - 2*wr
 *     dst_height = src_height - 2*wr
 *
 * 'kernel' points to the mid-point of a (2*wr + 1)-length array containing
 * either
 *     1) spatial gaussian filter coefficients for distances 0, 1, ..., wr
 *     2) negated exponents of the spatial gaussian function (this is what Fabio
 *        passes from his BilateralFilterOpImage class)
 *
 * The macro GS_x_GR(x) controls the interpretation.
 *******************************************************************************/
void separable_nlm_mono_tile(
    float *ibuf,                            // pointer to source data buffer
    float sr,                               // the usual range sigma
    int wr,                                 // window radius in pixels
    float *kernel,                          // half-kernel containing the exponents of the spatial Gaussian
    int width, int height)                  // dimensions of the source image
{
    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr) );

    //--------------------------------------------------------------------------
    // Filter Rows
    //--------------------------------------------------------------------------

    float *rbuf = new float[width];

    for (int y=2*wr; y < height - 2*wr; y++) {
        memcpy(rbuf, &ibuf[y * width], width * sizeof(float));
        for (int x=2*wr; x < width - 2*wr; x++) {
            // compute adaptive kernel and convolve color channels
            float num = 0;
            float denom = 0;

            for (int k = 0; k <= 2*wr; k++) {
                const int idx = k-wr + x;
                const float I_s = rbuf[idx];

                float D_sq = 0;
                for (int i = -wr; i <= wr; i++) {
                    const float I_s0 = rbuf[x+i];
                    const float I_s1 = rbuf[idx+i];

                    D_sq += SQR(I_s1 - I_s0);
                }
                D_sq /= 2*wr + 1;

                const float f = fast_exp(Ar * D_sq - kernel[k]);
                num += f * I_s;
                denom += f;
            }

            // normalize
            if (denom == 0)
                denom = 1.0;

            const int idx = x + y * width;
            ibuf[idx] = num / denom;
        }
    }

    delete [] rbuf;

    //--------------------------------------------------------------------------
    // Filter Columns
    //--------------------------------------------------------------------------

    // Buffer for processing column data
    float *cbuf = new float[height];

    for (int x=2*wr; x < width - 2*wr; x++) {
        for (int y=0; y < height; y++)
            cbuf[y] = ibuf[x + y*width];

        for (int y=2*wr; y < height - 2*wr; y++) {
            // compute adaptive kernel and convolve color channels
            float num = 0;
            float denom = 0;
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = k-wr + y;
                const float b = cbuf[idx];

                float D_sq = 0;
                for (int i = -wr; i <= wr; i++) {
                    const float b0 = cbuf[y+i];
                    const float b1 = cbuf[idx+i];

                    D_sq += SQR(b1 - b0);
                }
                D_sq /= 2*wr + 1;

                const float f = fast_exp(Ar * D_sq - kernel[k]);

                num += f * b;
                denom += f;
            }

            // normalize
            if (denom == 0)
                denom = 1.0;

            const int idx = y * width + x;
            ibuf[idx] = num / denom;
        }
    }

    delete [] cbuf;
}

/*******************************************************************************
 * separable_nlm_chroma_tile()
 *
 * Apply a separable Non-Local Means filter to a rectangular region of a color raster
 *
 * Dimensions of source and destination rectangles are related by
 *
 *     dst_width = src_width - 2*wr
 *     dst_height = src_height - 2*wr
 *
 * 'kernel' points to the mid-point of a (2*wr + 1)-length array containing
 * either
 *     1) spatial gaussian filter coefficients for distances 0, 1, ..., wr
 *     2) negated exponents of the spatial gaussian function (this is what Fabio
 *        passes from his BilateralFilterOpImage class)
 *
 * The macro GS_x_GR(x) controls the interpretation.
 *******************************************************************************/
void separable_nlm_chroma_tile(
    float *buf_a,                           // pointer to the s source/destination buffer
    float *buf_b,                           // pointer to the t source/destination buffer
    float sr,                               // the usual range sigma
    int wr,                                 // window radius in pixels
    float *kernel,                          // half-kernel containing the exponents of the spatial Gaussian
    int width, int height)                  // dimensions of the source image
{
    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr) );

    //--------------------------------------------------------------------------
    // Filter Rows
    //--------------------------------------------------------------------------

    float *rbuf_a = new float[width];
    float *rbuf_b = new float[width];

    for (int y=2*wr; y < height - 2*wr; y++) {
        memcpy(rbuf_a, &buf_a[y * width], width * sizeof(float));
        memcpy(rbuf_b, &buf_b[y * width], width * sizeof(float));

        for (int x=2*wr; x < width - 2*wr; x++) {
            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;

            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + x;
                const float s_a = rbuf_a[idx];
                const float s_b = rbuf_b[idx];

                float D_sq = 0;
                for (int i = -wr; i <= wr; i++) {
                    const float s0_a = rbuf_a[x+i];
                    const float s0_b = rbuf_b[x+i];
                    const float s1_a = rbuf_a[idx+i];
                    const float s1_b = rbuf_b[idx+i];

                    D_sq += SQR(s1_a - s0_a) + SQR(s1_b - s0_b);
                }
                D_sq /= 2*wr + 1;

                const float f = fast_exp(Ar * D_sq - kernel[k]);

                a_num += f * s_a;
                b_num += f * s_b;
                denom += f;
            }

            // normalize
            if (denom == 0)
                denom = 1.0;

            const int idx0 = x + y*width;
            buf_a[idx0] = a_num / denom;
            buf_b[idx0] = b_num / denom;
        }
    }
    delete [] rbuf_a;
    delete [] rbuf_b;

    //--------------------------------------------------------------------------
    // Filter Columns
    //--------------------------------------------------------------------------

    // Buffers for processing column data
    float *cbuf_a = new float[height];
    float *cbuf_b = new float[height];

    for (int x=2*wr; x < width - 2*wr; x++) {
        for (int y=0; y < height; y++) {
            const int idx = x + y*width;
            float a = buf_a[idx];
            float b = buf_b[idx];
            cbuf_a[y] = a;
            cbuf_b[y] = b;
        }

        for (int y=2*wr; y < height - wr; y++) {
            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;

            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + y;
                const float b_a  = cbuf_a[idx];
                const float b_b  = cbuf_b[idx];

                float D_sq = 0;
                for (int i = -wr; i <= wr; i++) {
                    const float b0_a = cbuf_a[y+i];
                    const float b0_b = cbuf_b[y+i];
                    const float b1_a = cbuf_a[idx+i];
                    const float b1_b = cbuf_b[idx+i];

                    D_sq += SQR(b1_a - b0_a) + SQR(b1_b - b0_b);
                }
                D_sq /= 2*wr + 1;

                const float f = fast_exp(Ar * D_sq - kernel[k]);

                a_num += f * b_a;
                b_num += f * b_b;
                denom += f;
            }

            // normalize
            if (denom == 0)
                denom = 1.0;

            const int idx = y * width + x;
            buf_a[idx] = a_num / denom;
            buf_b[idx] = b_num / denom;
        }
    }

    delete [] cbuf_a;
    delete [] cbuf_b;
}

/*******************************************************************************
 * JNI wrapper for separable_nlm_mono_tile() and separable_nlm_chroma_tile()
 *******************************************************************************/
extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_NonLocalMeansFilterOpImage_nonLocalMeansFilter
(JNIEnv *env, jclass cls,
 jshortArray jsrcData, jshortArray jdestData,
 jint y_wr, jint c_wr, jint y_ws, jint c_ws, jfloat y_scale_r, jfloat c_scale_r,
 jfloatArray jy_kernel, jfloatArray jc_kernel, jfloatArray jrgb_to_yst, jfloatArray jyst_to_rgb,
 jint width, jint height,
 jint srcROffset, jint srcGOffset, jint srcBOffset,
 jint destROffset, jint destGOffset, jint destBOffset,
 jint srcLineStride, jint destLineStride)
{
    unsigned short *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);
    float *y_kernel = (float *) (jy_kernel != NULL ? env->GetPrimitiveArrayCritical(jy_kernel, 0) : NULL);
    float *c_kernel = (float *) (jc_kernel != NULL ? env->GetPrimitiveArrayCritical(jc_kernel, 0) : NULL);
    float *rgb_to_yst = (float *) env->GetPrimitiveArrayCritical(jrgb_to_yst, 0);
    float *yst_to_rgb = (float *) env->GetPrimitiveArrayCritical(jyst_to_rgb, 0);

    float *buf_y = new float[width*height];
    float *buf_s = new float[width*height];
    float *buf_t = new float[width*height];

    interleaved_RGB_to_planar_YST(srcData, srcLineStride, srcROffset, srcGOffset, srcBOffset,
                                  buf_y, buf_s, buf_t, width, height, rgb_to_yst);

    if (y_scale_r != 0 && y_wr != 0 && y_kernel != NULL) {
        float y_sigma_r = sqrt(1.0/(2*y_scale_r));
        separable_nlm_mono_tile(buf_y, y_sigma_r, y_wr, y_kernel, width, height);
    }
    if (c_scale_r != 0 && c_wr != 0 && c_kernel != NULL) {
        float c_sigma_r = sqrt(1.0/(2*c_scale_r));
        separable_nlm_chroma_tile(buf_s, buf_t, c_sigma_r, c_wr, c_kernel, width, height);
    }

    int wr = y_wr > c_wr ? y_wr : c_wr;

    planar_YST_to_interleaved_RGB(destData, destLineStride, destROffset, destGOffset, destBOffset, 2*wr,
                                  buf_y, buf_s, buf_t, width, height, yst_to_rgb);

    delete [] buf_y;
    delete [] buf_s;
    delete [] buf_t;

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);

    if (y_kernel != NULL)
        env->ReleasePrimitiveArrayCritical(jy_kernel, y_kernel, 0);
    if (c_kernel != NULL)
        env->ReleasePrimitiveArrayCritical(jc_kernel, c_kernel, 0);

    env->ReleasePrimitiveArrayCritical(jrgb_to_yst, rgb_to_yst, 0);
    env->ReleasePrimitiveArrayCritical(jyst_to_rgb, yst_to_rgb, 0);
}
