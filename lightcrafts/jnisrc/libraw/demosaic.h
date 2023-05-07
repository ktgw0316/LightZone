#pragma once

void interpolateGreen(
    uint16_t *data, int width, int height, int pixelStride, int lineStride,
    int gx, int gy, int ry,
    int rOffset=0, int gOffset=1, int bOffset=2);

void interpolateRedBlue(
    uint16_t *data, int width, int height, int pixelStride, int lineStride,
    int rx0, int ry0, int bx0, int by0,
    int rOffset=0, int gOffset=1, int bOffset=2);
