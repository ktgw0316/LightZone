#pragma once
#include "omp_util.h"

//
// This function is intended to be used in omp parallel reagions.
//
static inline void transpose(const float * const srcBuf, float* dstBuf,
                             const int width, const int height)
{
    constexpr int blocksize = 16;

#pragma omp for collapse(2)
    for (int i = 0; i < height; i += blocksize) {
        for (int j = 0; j < width; j += blocksize) {
            const int row_max = i + blocksize < height ? i + blocksize : height;
            const int col_max = j + blocksize < width  ? j + blocksize : width;

            for (int row = i; row < row_max; ++row) {
                OMP_SIMD
                for (int col = j; col < col_max; ++col) {
                    dstBuf[col * height + row] = srcBuf[row * width + col];
                }
            }
        }
    }
}

