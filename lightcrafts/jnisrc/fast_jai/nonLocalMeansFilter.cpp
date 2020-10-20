/* Copyright (C) 2015- Masahiro Kitagawa */
#include <algorithm>
#include <float.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <omp.h>
#include "include/yst.h"
#include "include/transpose.h"
#include "include/omp_util.h"

void box_sum_horizontal_and_transpose(
    const float * const src, float *dst, const int swidth, const int sheight,
    const int r)
{
    const int dwidth = sheight;
    const int dheight = swidth;

    float *srct = new float[sheight * swidth]();

#pragma omp parallel
    {
        // Calculate the head (x = r) elements in original coordinate
#pragma omp for nowait
        for (int y = 0; y < sheight; ++y) {
            const int pos0 = y * swidth;

            const int pos = r * dwidth + y; // transeposed
            dst[pos] = 0.0;

            OMP_SIMD
            for (int x = 0; x < 2 * r + 1; ++x) {
                dst[pos] += src[pos0 + x];
            }
        }

        // Transpose src. Note that dst is already transposed.
        transpose(src, srct, swidth, sheight);

        // Calculate the other elements in transposed coordinate
        for (int y = r + 1, pos0 = (r + 1) * dwidth;
                y < dheight - r;
                ++y, pos0 += dwidth) {
            OMP_FOR_SIMD
            for (int x = 0; x < dwidth; ++x) {
                const int pos = pos0 + x;
                dst[pos] = dst[pos - dwidth]
                        - srct[pos - (r + 1) * dwidth]
                        + srct[pos + r * dwidth];
            }
        }
    }

    delete[] srct;
}

void box_sum(
    const float * const src, float *dst,
    const int width, const int height, // dimensions of the source image
    const int box_radius_x, const int box_radius_y)
{
    float *buf = new float[height * width]();
    box_sum_horizontal_and_transpose(src, buf, width, height, box_radius_x);
    box_sum_horizontal_and_transpose(buf, dst, height, width, box_radius_y);
    delete[] buf;
}

/*******************************************************************************
 * nlm_mono_tile()
 *
 * Apply a Non-Local Means filter to a rectangular region of a single-band
 * raster
 *
 * Dimensions of source and destination rectangles are related by
 *
 *     dst_width  = src_width - 2*wr
 *     dst_height = src_height - 2*wr
 *
 * 'kernel' points to the mid-point of a (2*wr + 1)-length array containing
 * either
 *     1) spatial gaussian filter coefficients for distances 0, 1, ..., wr
 *     2) negated exponents of the spatial gaussian function (this is what Fabio
 *        passes from his BilateralFilterOpImage class)
 *
 * This implementation is based on
 * Laurent Condat, "A Simple Trick to Speed Up and Improve the Non-Local Means"
 *******************************************************************************/
inline void nlm_mono_tile(
    float *ibuf,                       // pointer to source data buffer
    const float h,                     // intensity
    const int sr,                      // search window radius in pixels
    const int pr,                      // patch radius in pixels
    const float *kernel,               // half-kernel containing the exponents of the spatial Gaussian
    const int width, const int height) // dimensions of the source image
{
    if (fabs(h) < FLT_EPSILON)
        return;
    if (sr < 1)
        return;

    const int pd = 2 * pr + 1; // search window full width in pixels
    const float C_inv = -1.0f / (SQR(h) * SQR(pd));

    const size_t elems = size_t(width) * height;
    float *obuf  = new float[elems](); // output data buffer
    float *w_sum = new float[elems]();
    float *w_max = new float[elems]();

    float *v     = new float[elems](); // weight map
    float *ui    = new float[elems](); // integral image of square distances

    for (int ny = -sr; ny <= 0; ++ny) {
        for (int nx = -(ny+sr), n_offset = ny * width + nx; nx <= ny+sr && n_offset < 0; ++nx, ++n_offset) {
            const int margin_left  = nx < 0 ? -nx : 0;
            const int margin_right = nx > 0 ?  nx : 0;
            const float f = kernel[-ny] * kernel[abs(nx)];

            // square distances
            OMP_PARALLEL_FOR_SIMD
            for (int y = -ny; y < height; ++y) {
                const int x0 = y * width;

                for (int x = margin_left; x < width - margin_right; ++x) {
                    const int pos0 = x0 + x;
                    const int pos1 = pos0 + n_offset;
                    ui[pos0] = SQR(ibuf[pos0] - ibuf[pos1]);
                }
            }

            // sum in each patch
            box_sum(ui, v, width, height, pr, pr);

            // Update the output pixel values using the weight map
            OMP_PARALLEL_FOR_SIMD // TODO: Is this safe?
            for (int y = -ny; y < height; ++y) {
                const int x0 = y * width;
                for (int x = margin_left; x < width - margin_right; ++x) {
                    const int pos0 = x0 + x;
                    const int pos1 = pos0 + n_offset;
                    const float w = f * fast_exp(v[pos0] * C_inv);

                    obuf[pos0] += w * ibuf[pos1];
                    obuf[pos1] += w * ibuf[pos0];

                    w_sum[pos0] += w;
                    w_sum[pos1] += w;
                    if (w > w_max[pos0])
                        w_max[pos0] = w;
                    if (w > w_max[pos1])
                        w_max[pos1] = w;
                }
            }
        }
    }
    delete [] v;
    delete [] ui;

    // The weight attached to current pixels (n_offset == 0) is maximum of
    // all the weights. Add the contribution of the current pixels to their
    // denoised versions, then normalize.
    OMP_PARALLEL_FOR_SIMD
    for (int pos = 0; pos < elems; ++pos) {
        const float w_cur = w_max[pos] > FLT_EPSILON ? w_max[pos] : 1.0f;
        w_sum[pos] += w_cur;

        obuf[pos] += w_cur * ibuf[pos];
        obuf[pos] /= w_sum[pos];
    }
    delete [] w_max;
    delete [] w_sum;

    std::copy(&obuf[0], &obuf[elems], ibuf);
    delete [] obuf;
}

/*******************************************************************************
 * nlm_chroma_tile()
 *
 * Apply a Non-Local Means filter to a rectangular region of a color raster
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
inline void nlm_chroma_tile(
    float *ibuf_a,                     // pointer to the s source/destination buffer
    float *ibuf_b,                     // pointer to the t source/destination buffer
    const float h,                     // intensity
    const int sr,                      // search window radius in pixels
    const int pr,                      // patch radius in pixels
    const float *kernel,               // half-kernel containing the exponents of the spatial Gaussian
    const int width, const int height) // dimensions of the source image
{
    if (fabs(h) < FLT_EPSILON)
        return;
    if (sr < 1)
        return;

    const int pd = 2 * pr + 1; // search window full width in pixels
    const float C_inv = -1.0f / (SQR(h) * SQR(pd));

    const size_t elems = size_t(width) * height;
    float *obuf_a = new float[elems](); // output data buffer for a
    float *obuf_b = new float[elems](); // output data buffer for b
    float *w_sum  = new float[elems]();
    float *w_max  = new float[elems]();

    float *v     = new float[elems](); // weight map
    float *ui    = new float[elems](); // integral image of square distances

    for (int ny = -sr; ny <= 0; ++ny) {
        for (int nx = -(ny+sr), n_offset = ny * width + nx; nx <= ny+sr && n_offset < 0; ++nx, ++n_offset) {
            const int margin_left  = nx < 0 ? -nx : 0;
            const int margin_right = nx > 0 ?  nx : 0;
            const float f = kernel[-ny] * kernel[abs(nx)];

            // square distances
            OMP_PARALLEL_FOR_SIMD
            for (int y = -ny; y < height; ++y) {
                const int x0 = y * width;

                for (int x = margin_left; x < width - margin_right; ++x) {
                    const int pos0 = x0 + x;
                    const int pos1 = pos0 + n_offset;
                    ui[pos0] = SQR(ibuf_a[pos0] - ibuf_a[pos1])
                             + SQR(ibuf_b[pos0] - ibuf_b[pos1]);
                }
            }

            // sum in each patch
            box_sum(ui, v, width, height, pr, pr);

            // Update the output pixel values using the weight map
            OMP_PARALLEL_FOR_SIMD // TODO: Is this safe?
            for (int y = -ny; y < height; ++y) {
                const int x0 = y * width;
                for (int x = margin_left; x < width - margin_right; ++x) {
                    const int pos0 = x0 + x;
                    const int pos1 = pos0 + n_offset;
                    const float w = f * fast_exp(v[pos0] * C_inv);

                    obuf_a[pos0] += w * ibuf_a[pos1];
                    obuf_a[pos1] += w * ibuf_a[pos0];
                    obuf_b[pos0] += w * ibuf_b[pos1];
                    obuf_b[pos1] += w * ibuf_b[pos0];

                    w_sum[pos0] += w;
                    w_sum[pos1] += w;
                    if (w > w_max[pos0])
                        w_max[pos0] = w;
                    if (w > w_max[pos1])
                        w_max[pos1] = w;
                }
            }
        }
    }
    delete [] v;
    delete [] ui;

    // The weight attached to current pixels (n_offset == 0) is maximum of
    // all the weights. Add the contribution of the current pixels to their
    // denoised versions, then normalize.
    OMP_PARALLEL_FOR_SIMD
    for (int pos = 0; pos < elems; ++pos) {
        const float w_cur = w_max[pos] > FLT_EPSILON ? w_max[pos] : 1.0f;
        w_sum[pos] += w_cur;

        obuf_a[pos] += w_cur * ibuf_a[pos];
        obuf_a[pos] /= w_sum[pos];
        obuf_b[pos] += w_cur * ibuf_b[pos];
        obuf_b[pos] /= w_sum[pos];
    }
    delete [] w_max;
    delete [] w_sum;

    std::copy(&obuf_a[0], &obuf_a[elems], ibuf_a);
    delete [] obuf_a;
    std::copy(&obuf_b[0], &obuf_b[elems], ibuf_b);
    delete [] obuf_b;
}

/*******************************************************************************
 * JNI wrapper for nlm_mono_tile() and nlm_chroma_tile()
 *******************************************************************************/
extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_NonLocalMeansFilterOpImage_nonLocalMeansFilter
(JNIEnv *env, jclass cls,
 jshortArray jsrcData, jshortArray jdestData,
 jint y_search_radius, jint y_patch_radius,
 jint c_search_radius, jint c_patch_radius,
 jfloat y_h, jfloat c_h,
 jfloatArray jy_kernel, jfloatArray jc_kernel,
 jfloatArray jrgb_to_yst, jfloatArray jyst_to_rgb,
 jint width, jint height,
 jint srcROffset, jint srcGOffset, jint srcBOffset,
 jint destROffset, jint destGOffset, jint destBOffset,
 jint srcLineStride, jint destLineStride)
{
    unsigned short *srcData  = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);
    float *y_kernel = (float *) (jy_kernel != NULL ? env->GetPrimitiveArrayCritical(jy_kernel, 0) : NULL);
    float *c_kernel = (float *) (jc_kernel != NULL ? env->GetPrimitiveArrayCritical(jc_kernel, 0) : NULL);
    float *rgb_to_yst = (float *) env->GetPrimitiveArrayCritical(jrgb_to_yst, 0);
    float *yst_to_rgb = (float *) env->GetPrimitiveArrayCritical(jyst_to_rgb, 0);

    const int wr0 = y_search_radius > c_search_radius ? 2*y_search_radius : 2*c_search_radius;

    float *buf_y = new float[width*height];
    float *buf_s = new float[width*height];
    float *buf_t = new float[width*height];

    interleaved_RGB_to_planar_YST(srcData, srcLineStride, srcROffset, srcGOffset, srcBOffset,
                                  buf_y, buf_s, buf_t, width, height, rgb_to_yst);

    float *y_kernel_center;
    y_kernel_center = y_kernel + y_search_radius;
    float *c_kernel_center;
    c_kernel_center = c_kernel + c_search_radius;
    nlm_mono_tile(         buf_y, y_h, y_search_radius, y_patch_radius, y_kernel_center, width, height);
    nlm_chroma_tile(buf_s, buf_t, c_h, c_search_radius, c_patch_radius, c_kernel_center, width, height);

    planar_YST_to_interleaved_RGB(destData, destLineStride, destROffset, destGOffset, destBOffset, wr0,
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

/* vim:set sw=4 ts=4: */
