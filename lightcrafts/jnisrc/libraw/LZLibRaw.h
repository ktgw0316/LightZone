#pragma once

#include <libraw.h>
#include "mathlz.h"

#define MIN(a, b) ((a) < (b) ? (a) : (b))
#define MAX(a, b) ((a) > (b) ? (a) : (b))
#define LIM(x, min, max) MAX(min, MIN(x, max))
#define ULIM(x, y, z) ((y) < (z) ? LIM(x, y, z) : LIM(x, z, y))

#define RUN_CALLBACK(stage, iter, expect)                                      \
  if (callbacks.progress_cb) {                                                 \
    int rr = (*callbacks.progress_cb)(callbacks.progresscb_data, stage, iter,  \
                                      expect);                                 \
    if (rr != 0)                                                               \
      throw LIBRAW_EXCEPTION_CANCELLED_BY_CALLBACK;                            \
  }

class LZLibRaw : public LibRaw {
public:
  LZLibRaw() = default;
  ~LZLibRaw() = default;

  void set_interpolate_bayer_handler(process_step_callback cb) {
    callbacks.interpolate_bayer_cb = cb;
  }

  void lz_interpolate() {
    auto image = (uint16_t(*)[4])imgdata.image;
    const int width = imgdata.sizes.width;
    const int height = imgdata.sizes.height;
    constexpr int g = 1;

    // right, down, down-right, down-left
    const int dir[4] = {1, width, width + 1, width - 1};

    border_interpolate(2);

#pragma omp single
    RUN_CALLBACK(LIBRAW_PROGRESS_INTERPOLATE, 0, 3);

#pragma omp parallel
    {
      /*  Interpolate green layer */
#pragma omp for schedule(dynamic)
      for (int row = 2; row < height - 2; row++) {
        int col = 2 + (FC(row, 2) & 1);
        auto pix = image + row * width + col;
        const int c = FC(row, col);

        for (; col < width - 2; col += 2, pix += 2) {
          int diff[2], mean[2], detail[2];

          int cxy = pix[0][c];

          for (int i = 0; i < 2; i++) {
            const int d = dir[i];

            const int hl = pix[-d][g];
            const int hr = pix[d][g];
            const int chl = pix[-d * 2][c];
            const int chr = pix[d * 2][c];

            diff[i] = abs(hl - hr) + abs(chl + chr - 2 * cxy);
            mean[i] = (hl + hr) / 2;
            detail[i] = (cxy - (chl + chr) / 2) / 4;
          }

          // we're doing edge directed bilinear interpolation on the green
          // channel, which is a low pass operation (averaging), so we add some
          // signal from the high frequencies of the observed color channel
          {
            int sample;
            if (diff[0] == diff[1]) {
              sample = (mean[0] + mean[1]) / 2;
              if (sample < 4 * cxy && cxy < 4 * sample) {
                sample += (detail[0] + detail[1]) / 4;
              }
            } else {
              const int i = diff[0] > diff[1];
              sample = mean[i];
              if (sample < 4 * cxy && cxy < 4 * sample) {
                sample += detail[i];
              }
            }
            pix[0][g] = clampUShort(sample);
          }
        }
      }

      // get the constant component out of the reconstructed green pixels and
      // add to it the "high frequency" part of the corresponding observed color
      // channel

#pragma omp for schedule(dynamic)
      for (int row = 2; row < height - 2; row++) {
        int col = 2 + (FC(row, 2) & 1);
        auto pix = image + row * width + col;
        const int c = FC(row, col);

        for (; col < width - 2; col += 2, pix += 2) {
          const int xy = pix[0][g];
          const int cxy = pix[0][c];

          // Only work on the pixels that have a strong enough correlation
          // between channels
          if (4 * cxy <= xy || 4 * xy <= cxy) {
            continue;
          }

          int min_grad = INT_MAX;
          int detail = 0;

          for (int i = 0; i < 4; i++) {
            const int d = dir[i];

            const int gd = xy - (pix[-d][g] + pix[d][g]) / 2;
            const int cd = cxy - (pix[-d * 2][c] + pix[d * 2][c]) / 2;
            const int grad = abs(gd) + abs(cd);

            if (grad < min_grad) {
              min_grad = grad;
              detail = (cd - gd) / 2;
            }
          }

          // Only work on parts of the image that have enough "detail"
          if (min_grad > xy / 4) {
            pix[0][g] = clampUShort(xy + detail);
          }
        }
      }

#pragma omp single
      RUN_CALLBACK(LIBRAW_PROGRESS_INTERPOLATE, 1, 3);

      /*  Calculate red and blue for each green pixel:		*/
#pragma omp for schedule(dynamic)
      for (int row = 1; row < height - 1; row++) {
        int col = 1 + (FC(row, 2) & 1);
        auto pix = image + row * width + col;
        int c = FC(row, col + 1);

        for (; col < width - 1; col += 2, pix += 2) {
          const int gc = pix[0][g];

          for (int i = 0; i < 2; c = 2 - c, i++) {
            const int d = dir[i];
            const int gnw = pix[d][g];
            const int gse = pix[-d][g];
            const int cnw = gnw - pix[d][c];
            const int cse = gse - pix[-d][c];
            const int sample_c = gc - (cnw + cse) / 2;
            pix[0][c] = clampUShort(sample_c);
          }
        }
      }

#pragma omp single
      RUN_CALLBACK(LIBRAW_PROGRESS_INTERPOLATE, 2, 3);

      /*  Calculate blue for red pixels and vice versa:		*/
#pragma omp for schedule(dynamic)
      for (int row = 1; row < height - 1; row++) {
        int col = 1 + (FC(row, 1) & 1);
        auto pix = image + row * width + col;
        const int c = 2 - FC(row, col);

        int gne = pix[dir[3]][g];
        int gse = pix[-dir[2]][g];
        int cne = gne - pix[dir[3]][c];
        int cse = gse - pix[-dir[2]][c];

        for (; col < width - 1; col += 2, pix += 2) {
          const int gnw = pix[dir[2]][g];
          const int gsw = pix[-dir[3]][g];
          const int cnw = gnw - pix[dir[2]][c];
          const int csw = gsw - pix[-dir[3]][c];

          const int gc = pix[0][g];
          const int sample_c = gc - (cne + csw + cnw + cse) / 4;
          pix[0][c] = clampUShort(sample_c);

          gne = gnw;
          gse = gsw;
          cne = cnw;
          cse = csw;
        }
      }
    }
  }
};
