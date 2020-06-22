#include <float.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <omp.h>
#include "include/yst.h"
#include "include/transpose.h"
#include "include/omp_util.h"

inline void separable_bf_mono_row(
    float *ibuf,         // pointer to source data buffer
    const float sr,      // the usual range sigma
    const int wr,        // window radius in pixels
    const float *kernel, // half-kernel containing the exponents of the spatial Gaussian
    const int width, const int height, // dimensions of the source image
    const float Ar)                    // coefficient of the exponent for the range Gaussian
{
    float *rbuf = new float[width];

    OMP_FOR_SIMD
    for (int y=wr; y < height - wr; y++) {

        memcpy(rbuf, &ibuf[y * width], width * sizeof(float));

        for (int x=wr; x < width - wr; x++) {

            const float I_s0 = rbuf[x];

            // compute adaptive kernel and convolve color channels
            float num = 0;
            float denom = 0;

            for (int k = 0; k <= 2*wr; k++) {
                const float I_s = rbuf[k-wr + x];
                const float D_sq = SQR(I_s - I_s0);

                const float f = fast_exp(Ar * D_sq - kernel[k]);
                num += f * I_s;
                denom += f;
            }

            // normalize
            if (denom == 0)
                denom = 1.0;

            const int idx = x + y*width;
            ibuf[idx] = num / denom;
        }
    }

    delete [] rbuf;
}

inline void separable_bf_chroma_row(
    float *buf_a,        // pointer to the s source/destination buffer
    float *buf_b,        // pointer to the t source/destination buffer
    const float sr,      // the usual range sigma
    const int wr,        // window radius in pixels
    const float *kernel, // half-kernel containing the exponents of the spatial Gaussian
    const int width, const int height, // dimensions of the source image
    const float Ar)                    // coefficient of the exponent for the range Gaussian
{
    float *rbuf_a = new float[width];
    float *rbuf_b = new float[width];

    OMP_FOR_SIMD
    for (int y=wr; y < height - wr; y++) {
        memcpy(rbuf_a, &buf_a[y * width], width * sizeof(float));
        memcpy(rbuf_b, &buf_b[y * width], width * sizeof(float));

        for (int x=wr; x < width - wr; x++) {
            // initialize central pixel
            const float s0_a = rbuf_a[x];
            const float s0_b = rbuf_b[x];

            // buf_L[idx0] = s0_L; // needed for column filtering

            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;

            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + x;

                const float s_a = rbuf_a[idx];
                const float s_b = rbuf_b[idx];

                const float D_sq = /* SQR(s_L - s0_L) + */ SQR(s_a - s0_a) + SQR(s_b - s0_b);

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
}

/*******************************************************************************
 * separable_bf_mono_tile()
 *
 * Apply a separable bilateral filter to a rectangular region of a single-band
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
inline void separable_bf_mono_tile(
    float *ibuf,         // pointer to source data buffer
    const float sr,      // the usual range sigma
    const int wr,        // window radius in pixels
    const float *kernel, // half-kernel containing the exponents of the spatial Gaussian
    const int width, const int height) // dimensions of the source image
{
    if (fabs(sr) < FLT_EPSILON)
        return;

    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr) );

    float *tbuf = new float[width*height];

#   pragma omp parallel
    {
        // Filter Rows
        separable_bf_mono_row(ibuf, sr, wr, kernel, width, height, Ar);

        // Filter Columns
        transpose(ibuf, tbuf, width, height);
        separable_bf_mono_row(tbuf, sr, wr, kernel, height, width, Ar);
        transpose(tbuf, ibuf, height, width);
    }

    delete [] tbuf;
}

/*******************************************************************************
 * separable_bf_chroma_tile()
 *
 * Apply a separable bilateral filter to a rectangular region of a color raster
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
inline void separable_bf_chroma_tile(
    float *buf_a,          // pointer to the s source/destination buffer
    float *buf_b,          // pointer to the t source/destination buffer
    const float sr,        // the usual range sigma
    const int wr,          // window radius in pixels
    const float *kernel,   // half-kernel containing the exponents of the spatial Gaussian
    int width, int height) // dimensions of the source image
{
    if (fabs(sr) < FLT_EPSILON)
        return;

    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr) );

    float *tbuf_a = new float[width*height];
    float *tbuf_b = new float[width*height];

#   pragma omp parallel
    {
        // Filter Rows
        separable_bf_chroma_row(buf_a, buf_b, sr, wr, kernel, width, height, Ar);

        // Filter Columns
        transpose(buf_a, tbuf_a, width, height);
        transpose(buf_b, tbuf_b, width, height);
        separable_bf_chroma_row(tbuf_a, tbuf_b, sr, wr, kernel, height, width, Ar);
        transpose(tbuf_a, buf_a, height, width);
        transpose(tbuf_b, buf_b, height, width);
    }

    delete [] tbuf_a;
    delete [] tbuf_b;
}

/*******************************************************************************
 * JNI wrapper for separable_bf_mono_tile() and separable_bf_chroma_tile()
 *******************************************************************************/
extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_BilateralFilterRGBOpImage_bilateralFilterRGB
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

    float y_sigma_r = (y_scale_r != 0 && y_wr != 0 && y_kernel != NULL) ? sqrt(1.0/(2*y_scale_r)) : 0.0;
    float c_sigma_r = (c_scale_r != 0 && c_wr != 0 && c_kernel != NULL) ? sqrt(1.0/(2*c_scale_r)) : 0.0;

    const int wr = y_wr > c_wr ? y_wr : c_wr;

    float *buf_y = new float[width*height];
    float *buf_s = new float[width*height];
    float *buf_t = new float[width*height];

    interleaved_RGB_to_planar_YST(srcData, srcLineStride, srcROffset, srcGOffset, srcBOffset,
                                  buf_y, buf_s, buf_t, width, height, rgb_to_yst);

    separable_bf_mono_tile(         buf_y, y_sigma_r, y_wr, y_kernel, width, height);
    separable_bf_chroma_tile(buf_s, buf_t, c_sigma_r, c_wr, c_kernel, width, height);

    planar_YST_to_interleaved_RGB(destData, destLineStride, destROffset, destGOffset, destBOffset, wr,
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
