#pragma once
#include "../../include/mathlz.h"

inline void planar_YST_to_interleaved_RGB(unsigned short * const dstData, int dstStep,
                                   int r_offset, int g_offset, int b_offset, int wr,
                                   const float * const buf_y, const float * const buf_s, const float * const buf_t,
                                   int width, int height,
                                   float *yst_to_rgb) {
#if _OPENMP < 201307
#   pragma omp for
#else
#   pragma omp for simd
#endif
    for (int y=wr; y < height-wr; y++) {
        for (int x=wr; x < width-wr; x++) {
            const int dst_idx = 3*(x-wr) + (y-wr)*dstStep + r_offset;
            const int idx = x + y*width;

            const float y = buf_y[idx];
            const float s = buf_s[idx] - 0.5f;
            const float t = buf_t[idx] - 0.5f;

            for (int c = 0; c < 3; c++)
#ifdef FP_FAST_FMAF
                dstData[dst_idx+c] = clampUShort(0xffff * fmaf(yst_to_rgb[3*c+2], t,
                                                          fmaf(yst_to_rgb[3*c+1], s,
                                                               yst_to_rgb[3*c] * y)));
#else
                dstData[dst_idx+c] = clampUShort(0xffff * (yst_to_rgb[3*c]   * y +
                                                           yst_to_rgb[3*c+1] * s +
                                                           yst_to_rgb[3*c+2] * t));
#endif
        }
    }
}

inline void interleaved_RGB_to_planar_YST(const unsigned short * const srcData, int srcStep,
                                   int r_offset, int g_offset, int b_offset,
                                   float *buf_y, float *buf_s, float *buf_t,
                                   int width, int height,
                                   float *rgb_to_yst) {
    const float norm = (float)0x10000;
    const float inv_norm = 1.0f/norm;

#if _OPENMP < 201307
#   pragma omp for
#else
#   pragma omp for simd
#endif
    for (int y=0; y < height; y++) {
        for (int x=0; x < width; x++) {
            const int src_idx = 3*x + y*srcStep;
            const int idx = x + y*width;

            const float r = inv_norm * (float) srcData[src_idx+r_offset];
            const float g = inv_norm * (float) srcData[src_idx+g_offset];
            const float b = inv_norm * (float) srcData[src_idx+b_offset];

            float YST[3];

            for (int c = 0; c < 3; c++)
#ifdef FP_FAST_FMAF
                YST[c] = fmaf(rgb_to_yst[3*c],   r,
                         fmaf(rgb_to_yst[3*c+1], g,
                         fmaf(rgb_to_yst[3*c+2], b, (c > 0 ? 0.5f : 0))));
#else
                YST[c] = rgb_to_yst[3*c]   * r +
                         rgb_to_yst[3*c+1] * g +
                         rgb_to_yst[3*c+2] * b + (c > 0 ? 0.5f : 0);
#endif

            buf_y[idx] = YST[0];
            buf_s[idx] = YST[1];
            buf_t[idx] = YST[2];
        }
    }
}
