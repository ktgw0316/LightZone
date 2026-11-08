/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2014-     Masahiro Kitagawa */

#include "mathlz.h"
#include <algorithm>
#include <array>
#include <climits>
#include <cstdint>
#include <cstdlib>

constexpr int pixelStride = 3;
using array4 = std::array<int, 4>;

#define srcDatum(xOffset, yOffset)                                             \
  srcData[(y + (yOffset)) * srcLineStride + x + (xOffset)]

#define cDatum(xOffset, yOffset)                                               \
  data[((y + (yOffset)) * lineStride + x + (xOffset)) * pixelStride + cOffset]

#define gDatum(xOffset, yOffset)                                               \
  data[((y + (yOffset)) * lineStride + x + (xOffset)) * pixelStride + gOffset]

int colorHotPixelRemoval(const int value, const int x, const int y,
                         const uint16_t *srcData, const int srcLineStride) {
  array4 neighbors;
  const bool isNormalPixel =
      value < 2 * (neighbors[0] = srcDatum(0, -2)) ||
      value < 2 * (neighbors[1] = srcDatum(0, 2)) ||
      value < 2 * (neighbors[2] = srcDatum(-2, 0)) ||
      value < 2 * (neighbors[3] = srcDatum(2, 0)) ||
      value < 2 * 2 * srcDatum(-1, -1) || value < 2 * 2 * srcDatum(0, -1) ||
      value < 2 * 2 * srcDatum(1, -1) || value < 2 * 2 * srcDatum(-1, 0) ||
      value < 2 * 2 * srcDatum(1, 0) || value < 2 * 2 * srcDatum(-1, 1) ||
      value < 2 * 2 * srcDatum(0, 1) || value < 2 * 2 * srcDatum(1, 1);
  return isNormalPixel
             ? value
             : (neighbors[0] + neighbors[1] + neighbors[2] + neighbors[3]) / 4;
}

int greenHotPixelRemoval(const int value, const int x, const int y,
                         const uint16_t *srcData, const int srcLineStride) {
  array4 neighbors;
  const bool isNormalPixel =
      value < 2 * (neighbors[0] = srcDatum(-1, -1)) ||
      value < 2 * (neighbors[1] = srcDatum(1, -1)) ||
      value < 2 * (neighbors[2] = srcDatum(-1, 1)) ||
      value < 2 * (neighbors[3] = srcDatum(1, 1)) ||
      value < 2 * 2 * srcDatum(0, -1) || value < 2 * 2 * srcDatum(-1, 0) ||
      value < 2 * 2 * srcDatum(1, 0) || value < 2 * 2 * srcDatum(0, 1);
  return isNormalPixel
             ? value
             : (neighbors[0] + neighbors[1] + neighbors[2] + neighbors[3]) / 4;
}

/// copy RAW data to RGB layer and remove hot pixels
void copyRawToRgb(const uint16_t *srcData, uint16_t *data, int width,
                  int height, int srcLineStride, int lineStride, int srcOffset,
                  int rOffset, int gOffset, int bOffset, int gx, int gy,
                  int ry) {
#pragma omp for schedule(dynamic)
  for (int y = 0; y < height; ++y) {
    const int cOffset = (y & 1) == (ry & 1) ? rOffset : bOffset;
    const int leftmost_non_green_pixel_x =
        (y & 1) == (gy & 1) ? !gx : gx; // 0 or 1

    for (int x = 0; x < width; ++x) {
      const bool colorPixel = (x & 1) == leftmost_non_green_pixel_x;

      int value = srcDatum(0, 0);

      // Copy the border pixels without any processing
      const bool on_border =
          y < 2 || y >= height - 2 || x < 2 || x >= width - 2;

      if (colorPixel) {
        if (!on_border) {
          value = colorHotPixelRemoval(value, x, y, srcData, srcLineStride);
        }
        cDatum(0, 0) = static_cast<uint16_t>(value);
      } else {
        if (!on_border) {
          value = greenHotPixelRemoval(value, x, y, srcData, srcLineStride);
        }
        gDatum(0, 0) = static_cast<uint16_t>(value);
      }
    }
  }
}

void interpolateGreenChannel(uint16_t *data, int width, int height,
                             int lineStride, int gx, int gy, int ry,
                             int rOffset, int gOffset, int bOffset) {
#pragma omp for schedule(dynamic)
  for (int y = 2; y < height - 2; y++) {
    const int cOffset = (y & 1) == (ry & 1) ? rOffset : bOffset;
    const int leftmost_non_green_pixel_x =
        (y & 1) == (gy & 1) ? !gx : gx; // 0 or 1
    int x = leftmost_non_green_pixel_x + 2;

    int hl = gDatum(-1, 0);
    int cxy = cDatum(0, 0);
    int chl = cDatum(-2, 0);

    for (; x < width - 2; x += 2) {
      const int hr = gDatum(1, 0);
      const int vu = gDatum(0, -1);
      const int vd = gDatum(0, 1);
      const int dh = abs(hl - hr);
      const int dv = abs(vu - vd);

      const int chr = cDatum(2, 0);
      const int cvu = cDatum(0, -2);
      const int cvd = cDatum(0, 2);
      const int cdh = abs(chl + chr - 2 * cxy);
      const int cdv = abs(cvu + cvd - 2 * cxy);

      // we're doing edge directed bilinear interpolation on the green
      // channel, which is a low pass operation (averaging), so we add some
      // signal from the high frequencies of the observed color channel

      int sample;
      if (dv + cdv - (dh + cdh) > 0) {
        sample = (hl + hr) / 2;
        if (sample < 4 * cxy && cxy < 4 * sample)
          sample += (cxy - (chl + chr) / 2) / 4;
      } else if (dh + cdh - (dv + cdv) > 0) {
        sample = (vu + vd) / 2;
        if (sample < 4 * cxy && cxy < 4 * sample)
          sample += (cxy - (cvu + cvd) / 2) / 4;
      } else {
        sample = (vu + hl + vd + hr) / 4;
        if (sample < 4 * cxy && cxy < 4 * sample)
          sample += (cxy - (chl + chr + cvu + cvd) / 4) / 8;
      }
      gDatum(0, 0) = clampUShort(sample);

      hl = hr;
      chl = cxy;
      cxy = chr;
    }
  }
}

// get the constant component out of the reconstructed green pixels and add
// to it the "high frequency" part of the corresponding observed color
// channel
void refineGreenChannel(uint16_t *data, int width, int height, int lineStride,
                        int gx, int gy, int ry, int rOffset, int gOffset,
                        int bOffset) {
#pragma omp for schedule(dynamic)
  for (int y = 2; y < height - 2; y++) {
    const int cOffset = (y & 1) == (ry & 1) ? rOffset : bOffset;
    const int leftmost_green_pixel_x = (y & 1) == (gy & 1) ? gx : !gx; // 0 or 1
    int x = leftmost_green_pixel_x + 2;

    int ul = gDatum(-2, -2);
    int hl = gDatum(-2, 0);
    int bl = gDatum(-2, 2);
    int vu = gDatum(0, -2);
    int xy = gDatum(0, 0);
    int vd = gDatum(0, 2);

    int cul = cDatum(-2, -2);
    int chl = cDatum(-2, 0);
    int cbl = cDatum(-2, 2);
    int cvu = cDatum(0, -2);
    int cxy = cDatum(0, 0);
    int cvd = cDatum(0, 2);

    for (; x < width - 2; x += 2) {
      const int hr = gDatum(2, 0);
      const int ur = gDatum(2, -2);
      const int br = gDatum(2, 2);

      const int chr = cDatum(2, 0);
      const int cur = cDatum(2, -2);
      const int cbr = cDatum(2, 2);

      // Only work on the pixels that have a strong enough correlation between
      // channels
      if (xy < 4 * cxy && cxy < 4 * xy) {
        // Horizontal, vertical, north-east and north-west
        const array4 means = {(hl + hr) / 2, (vu + vd) / 2, (ul + br) / 2,
                              (ur + bl) / 2};
        const array4 c_means = {(chl + chr) / 2, (cvu + cvd) / 2,
                                (cul + cbr) / 2, (cur + cbl) / 2};
        const array4 diffs = {xy - means[0], xy - means[1], xy - means[2],
                              xy - means[3]};
        const array4 c_diffs = {cxy - c_means[0], cxy - c_means[1],
                                cxy - c_means[2], cxy - c_means[3]};

        // Only work on parts of the image that have enough "detail"
        const array4 grads = {
            abs(diffs[0]) + abs(c_diffs[0]), abs(diffs[1]) + abs(c_diffs[1]),
            abs(diffs[2]) + abs(c_diffs[2]), abs(diffs[3]) + abs(c_diffs[3])};
        const auto min_grad = std::min_element(grads.cbegin(), grads.cend());
        const size_t min_dir = std::distance(grads.cbegin(), min_grad);
        if (*min_grad > xy / 4) {
          const int sample = (xy + means[min_dir] + c_diffs[min_dir]) / 2;
          gDatum(0, 0) = clampUShort(sample);
        }
      }

      ul = vu;
      vu = ur;

      hl = xy;
      xy = hr;

      bl = vd;
      vd = br;

      cul = cvu;
      cvu = cur;

      chl = cxy;
      cxy = chr;

      cbl = cvd;
      cvd = cbr;
    }
  }
}

void interpolateRedOrBlue(uint16_t *data, int width, int height, int lineStride,
                          int cx0, int cy0, int gOffset, int cOffset) {
#pragma omp for schedule(guided) nowait
  for (int y = cy0 + 1; y < height - 1; y += 2) {
    int x = cx0 + 1;
    int gne = gDatum(-1, 1);
    int gse = gDatum(-1, -1);
    int cne = gne - cDatum(-1, 1);
    int cse = gse - cDatum(-1, -1);

    for (; x < width - 1; x += 2) {
      const int gnw = gDatum(1, 1);
      const int gsw = gDatum(1, -1);
      const int cnw = gnw - cDatum(1, 1);
      const int csw = gsw - cDatum(1, -1);

      {
        // Pixel at the other color location
        const int gc = gDatum(0, 0);
        const int sample_c = gc - (cne + csw + cnw + cse) / 4;
        cDatum(0, 0) = clampUShort(sample_c);
      }
      {
        // Pixel at green location - vertical
        const int gw = gDatum(1, 0);
        const int sample_w = gw - (csw + cnw) / 2;
        cDatum(1, 0) = clampUShort(sample_w);
      }
      {
        // Pixel at green location - horizontal
        const int gs = gDatum(0, -1);
        const int sample_s = gs - (cse + csw) / 2;
        cDatum(0, -1) = clampUShort(sample_s);
      }
      gne = gnw;
      gse = gsw;
      cne = cnw;
      cse = csw;
    }
  }
}

void interpolateGreen(uint16_t *data, int width, int height, int lineStride,
                      int gx, int gy, int ry, int rOffset, int gOffset,
                      int bOffset) {
#pragma omp parallel shared(data)
  {
    interpolateGreenChannel(data, width, height, lineStride, gx, gy, ry,
                            rOffset, gOffset, bOffset);
    refineGreenChannel(data, width, height, lineStride, gx, gy, ry, rOffset,
                       gOffset, bOffset);
  }
}

static bool validSourceBuffer(const uint16_t *data, int length, int width,
                              int height, int lineStride) {
  if (data == nullptr || length < 0 || width <= 0 || height <= 0 ||
      lineStride < width) {
    return false;
  }
  const uint64_t required =
      static_cast<uint64_t>(height - 1) * lineStride + width;
  return required <= static_cast<uint64_t>(length);
}

static bool validRgbBuffer(const uint16_t *data, int length, int width,
                           int height, int lineStride, int rOffset,
                           int gOffset, int bOffset) {
  if (data == nullptr || length < 0 || width <= 0 || height <= 0 ||
      lineStride < width || rOffset < 0 || rOffset > 2 || gOffset < 0 ||
      gOffset > 2 || bOffset < 0 || bOffset > 2) {
    return false;
  }
  const uint64_t lastPixel =
      static_cast<uint64_t>(height - 1) * lineStride + width - 1;
  const uint64_t required =
      lastPixel * pixelStride + std::max({rOffset, gOffset, bOffset}) + 1;
  return required <= static_cast<uint64_t>(length);
}

extern "C" int dcraw_interpolate_green(
    const uint16_t *srcData, int srcLength, uint16_t *dstData, int dstLength,
    int width, int height, int srcLineStride, int dstLineStride, int srcOffset,
    int rOffset, int gOffset, int bOffset, int gx, int gy, int ry) {
  if (!validSourceBuffer(srcData, srcLength, width, height, srcLineStride) ||
      !validRgbBuffer(dstData, dstLength, width, height, dstLineStride, rOffset,
                      gOffset, bOffset) ||
      gx < 0 || gx > 1 || gy < 0 || gy > 1 || ry < 0 || ry > 1) {
    return -1;
  }
  (void)srcOffset;
#pragma omp parallel shared(srcData, dstData)
  {
    copyRawToRgb(srcData, dstData, width, height, srcLineStride, dstLineStride,
                 srcOffset, rOffset, gOffset, bOffset, gx, gy, ry);
    if (width >= 4 && height >= 5) {
      interpolateGreenChannel(dstData, width, height, dstLineStride, gx, gy,
                              ry, rOffset, gOffset, bOffset);
      refineGreenChannel(dstData, width, height, dstLineStride, gx, gy, ry,
                         rOffset, gOffset, bOffset);
    }
  }
  return 0;
}

extern "C" int dcraw_interpolate_red_blue(
    uint16_t *data, int length, int width, int height, int lineStride,
    int rOffset, int gOffset, int bOffset, int rx0, int ry0, int bx0, int by0) {
  if (!validRgbBuffer(data, length, width, height, lineStride, rOffset,
                      gOffset, bOffset) ||
      rx0 < 0 || rx0 > 1 || ry0 < 0 || ry0 > 1 || bx0 < 0 || bx0 > 1 ||
      by0 < 0 || by0 > 1) {
    return -1;
  }
  if (width < 3 || height < 3) {
    return 0;
  }
#pragma omp parallel shared(data)
  {
    interpolateRedOrBlue(data, width, height, lineStride, rx0, ry0, gOffset,
                         rOffset);
    interpolateRedOrBlue(data, width, height, lineStride, bx0, by0, gOffset,
                         bOffset);
  }
  return 0;
}
