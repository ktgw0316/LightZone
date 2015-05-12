inline void transpose(float* srcBuf, float* dstBuf, const int width, const int height) {
#   pragma omp for simd
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            dstBuf[x*height+y] = srcBuf[y*width+x];
        }
    }
}
