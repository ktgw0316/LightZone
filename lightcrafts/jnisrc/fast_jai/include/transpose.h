#pragma once

static inline void transpose(float* srcBuf, float* dstBuf, const int width, const int height) {
#if _OPENMP < 201307
#   pragma omp for
#else
#   pragma omp for simd
#endif
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            dstBuf[x*height+y] = srcBuf[y*width+x];
        }
    }
}
