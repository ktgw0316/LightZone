#pragma once
#include "omp_util.h"

static inline void transpose(const float * const srcBuf, float* dstBuf,
                             const int width, const int height)
{
    OMP_FOR_SIMD
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            dstBuf[x*height+y] = srcBuf[y*width+x];
        }
    }
}
