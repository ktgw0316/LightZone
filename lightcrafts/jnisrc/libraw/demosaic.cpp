/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2014-     Masahir Kitagawa */

#include <climits>
#include <cstdlib>

#include "demosaic.h"
#include "mathlz.h"

#define cDatum(xOffset, yOffset)                                               \
  data[((y + (yOffset)) * lineStride + x + (xOffset)) * pixelStride + cOffset]

#define gDatum(xOffset, yOffset)                                               \
  data[((y + (yOffset)) * lineStride + x + (xOffset)) * pixelStride + gOffset]

void interpolateGreenChannel(uint16_t *data, int width, int height, int pixelStride, int lineStride,
                      int gx, int gy, int ry, int rOffset, int gOffset, int bOffset) {
#pragma omp for schedule(dynamic)
  for (int y = 2; y < height - 2; y++) {
    const int cOffset = (y & 1) == (ry & 1) ? rOffset : bOffset;
    const int leftmost_non_green_pixel_x = (y & 1) == (gy & 1) ? !gx : gx; // 0 or 1
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
void refineGreenChannel(uint16_t *data, int width, int height, int pixelStride, int lineStride,
                        int gx, int gy, int ry,
                        int rOffset, int gOffset, int bOffset) {
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
        const int dh = xy - (hl + hr) / 2;
        const int dv = xy - (vu + vd) / 2;
        const int ne = xy - (ul + br) / 2;
        const int nw = xy - (ur + bl) / 2;

        const int cdh = cxy - (chl + chr) / 2;
        const int cdv = cxy - (cvu + cvd) / 2;
        const int cne = cxy - (cul + cbr) / 2;
        const int cnw = cxy - (cur + cbl) / 2;

        const int gradients[4] = {abs(dh) + abs(cdh), abs(dv) + abs(cdv),
                                  abs(ne) + abs(cne), abs(nw) + abs(cnw)};

        int mind = 4;
        int ming = INT_MAX;
        for (int i = 0; i < 4; i++) {
          if (gradients[i] < ming) {
            ming = gradients[i];
            mind = i;
          }
        }

        // Only work on parts of the image that have enough "detail"

        if (mind != 4 && ming > xy / 4) {
          int sample;
          switch (mind) {
          case 0: // horizontal
            sample = (xy + (hl + hr) / 2 + cdh) / 2;
            break;
          case 1: // vertical
            sample = (xy + (vu + vd) / 2 + cdv) / 2;
            break;
          case 2: // north-east
            sample = (xy + (ul + br) / 2 + cne) / 2;
            break;
          case 3: // north-west
            sample = (xy + (ur + bl) / 2 + cnw) / 2;
            break;
          case 4: // flat
            // nothing to do
            break;
          }
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

void interpolateRedOrBlue(uint16_t *data, int width, int height, int pixelStride, int lineStride,
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

void interpolateGreen(
    uint16_t *data, int width, int height, int pixelStride, int lineStride,
    int gx, int gy, int ry,
    int rOffset, int gOffset, int bOffset) {
#pragma omp parallel shared(data)
  {
    interpolateGreenChannel(data, width, height, pixelStride, lineStride, gx, gy, ry,
                            rOffset, gOffset, bOffset);
    refineGreenChannel(data, width, height, pixelStride, lineStride, gx, gy, ry,
                       rOffset, gOffset, bOffset);
  }
}

void interpolateRedBlue(
    uint16_t *data, int width, int height, int pixelStride, int lineStride,
    int rx0, int ry0, int bx0, int by0,
    int rOffset, int gOffset, int bOffset) {
#pragma omp parallel shared(data)
  {
    interpolateRedOrBlue(data, width, height, pixelStride, lineStride, rx0, ry0,
                         gOffset, rOffset);
    interpolateRedOrBlue(data, width, height, pixelStride, lineStride, bx0, by0,
                         gOffset, bOffset);
  }
}