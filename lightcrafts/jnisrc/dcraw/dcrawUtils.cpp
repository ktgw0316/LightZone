/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2014-     Masahir Kitagawa */

#include <jni.h>
#include <climits>
#include <cstdlib>

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_DCRaw.h"
#endif

#include "LC_JNIUtils.h"
#include "../include/mathlz.h"

#define DCRaw_METHOD(method) \
        name4(Java_,com_lightcrafts_utils_DCRaw,_,method)

#define srcDatum(x, y) \
        srcData[(y) * srcLineStride + (x) + srcOffset]

#define dstDatum(x, y, offset) \
        dstData[3 * ((y) * dstLineStride + (x)) + (offset)]

#define cDatum(xOffset, yOffset) \
        data[3 * ((y + cy0 + (yOffset)) * lineStride + x + cx0 + (xOffset)) + cOffset]

#define gDatum(xOffset, yOffset) \
        data[3 * ((y + cy0 + (yOffset)) * lineStride + x + cx0 + (xOffset)) + gOffset]

JNIEXPORT void JNICALL DCRaw_METHOD(interpolateGreen)
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdstData, jint width, jint height,
  jint srcLineStride, jint dstLineStride,
  jint srcOffset, jint rOffset, jint gOffset, jint bOffset,
  jint gx, jint gy, jint ry )
{
    auto srcData = static_cast<unsigned short*>(env->GetPrimitiveArrayCritical(jsrcData, 0));
    auto dstData = static_cast<unsigned short*>(env->GetPrimitiveArrayCritical(jdstData, 0));

#pragma omp parallel shared (srcData, dstData)
{

    // copy RAW data to RGB layer and remove hot pixels

#pragma omp for schedule (dynamic)
    for (int y = 0; y < height; y++) {
        const int cOffset = (y&1) == (ry&1) ? rOffset : bOffset;
        const int x0 = (y&1) == (gy&1) ? gx+1 : gx;
        for (int x = 0; x < width; x++) {
            const bool colorPixel = (x & 1) == (x0 & 1);
            const int offset = colorPixel ? cOffset : gOffset;

            int value = srcDatum(x, y);
            if (x >= 2 && x < width-2 && y >= 2 && y < height-2) {
                int v[4];
                bool replace = true;
                if (colorPixel) {
                    if (value < 2 * (v[0] = srcDatum(x, y-2))
                            || value < 2 * (v[1] = srcDatum(x, y+2))
                            || value < 2 * (v[2] = srcDatum(x-2, y))
                            || value < 2 * (v[3] = srcDatum(x+2, y))

                            || value < 2 * 2 * srcDatum(x-1, y-1)
                            || value < 2 * 2 * srcDatum(x, y-1)
                            || value < 2 * 2 * srcDatum(x+1, y-1)

                            || value < 2 * 2 * srcDatum(x-1, y)
                            || value < 2 * 2 * srcDatum(x+1, y)

                            || value < 2 * 2 * srcDatum(x-1, y+1)
                            || value < 2 * 2 * srcDatum(x, y+1)
                            || value < 2 * 2 * srcDatum(x+1, y+1)) {
                        replace = false;
                    }
                } else {
                    if (value < 2 * (v[0] = srcDatum(x-1, y-1))
                            || value < 2 * (v[1] = srcDatum(x+1, y-1))
                            || value < 2 * (v[2] = srcDatum(x-1, y+1))
                            || value < 2 * (v[3] = srcDatum(x+1, y+1))

                            || value < 2 * 2 * srcDatum(x, y-1)
                            || value < 2 * 2 * srcDatum(x-1, y)
                            || value < 2 * 2 * srcDatum(x+1, y)
                            || value < 2 * 2 * srcDatum(x, y+1)) {
                        replace = false;
                    }
                }
                if (replace) {
                    value = (v[0] + v[1] + v[2] + v[3]) / 4;
                }
            }
            dstDatum(x, y, offset) = static_cast<unsigned short>(value);
        }
    }

    // green channel interpolation

#pragma omp for schedule (dynamic)
    for (int y = 2; y < height-2; y++) {
        const int cOffset = (y&1) == (ry&1) ? rOffset : bOffset;
        const int x0 = (y&1) == (gy&1) ? gx+1 : gx;
        
        int hl = dstDatum(x0-1, y, gOffset);
        int cxy = dstDatum(x0, y, cOffset);
        int chl = dstDatum(x0-2, y, cOffset);
        
        const int x_min = (x0 & 1) ? 3 : 2; 
        for (int x = x_min; x < width-2; x += 2) {
            const int hr = dstDatum(x+1, y, gOffset);
            const int vu = dstDatum(x, y-1, gOffset);
            const int vd = dstDatum(x, y+1, gOffset);
            const int dh = abs(hl - hr);
            const int dv = abs(vu - vd);

            const int chr = dstDatum(x+2, y, cOffset);
            const int cvu = dstDatum(x, y-2, cOffset);
            const int cvd = dstDatum(x, y+2, cOffset);
            const int cdh = abs(chl + chr - 2 * cxy);
            const int cdv = abs(cvu + cvd - 2 * cxy);

            // we're doing edge directed bilinear interpolation on the green channel,
            // which is a low pass operation (averaging), so we add some signal from the
            // high frequencies of the observed color channel

            int sample;
            if (dv + cdv - (dh + cdh) > 0) {
                sample = (hl + hr) / 2;
                if (sample < 4 * cxy && cxy < 4 * sample)
                    sample += (cxy - (chl + chr)/2) / 4;
            } else if (dh + cdh - (dv + cdv) > 0) {
                sample = (vu + vd) / 2;
                if (sample < 4 * cxy && cxy < 4 * sample)
                    sample += (cxy - (cvu + cvd)/2) / 4;
            } else {
                sample = (vu + hl + vd + hr) / 4;
                if (sample < 4 * cxy && cxy < 4 * sample)
                    sample += (cxy - (chl + chr + cvu + cvd)/4) / 8;
            }
            dstDatum(x, y, gOffset) = clampUShort(sample);

            hl = hr;
            chl = cxy;
            cxy = chr;
        }
    }

    // get the constant component out of the reconstructed green pixels and add to it
    // the "high frequency" part of the corresponding observed color channel
    
#pragma omp for schedule (dynamic)
    for (int y = 2; y < height-2; y++) {
        const int cOffset = (y&1) == (ry&1) ? rOffset : bOffset;
        const int x0 = (y&1) == (gy&1) ? gx+1 : gx;
        
        int xy = dstDatum(x0, y, gOffset);
        int hl = dstDatum(x0-2, y, gOffset);
        int ul = dstDatum(x0-2, y-2, gOffset);
        int bl = dstDatum(x0-2, y+2, gOffset);
        
        int cxy = dstDatum(x0, y, cOffset);
        int chl = dstDatum(x0-2, y, cOffset);
        int cul = dstDatum(x0-2, y-2, cOffset);
        int cbl = dstDatum(x0-2, y+2, cOffset);
        
        for (int x = 2; x < width-2; x+=2) {
            const int hr = dstDatum(x+2, y, gOffset);
            const int ur = dstDatum(x+2, y-2, gOffset);
            const int br = dstDatum(x+2, y+2, gOffset);
            int vu = dstDatum(x, y-2, gOffset);
            int vd = dstDatum(x, y+2, gOffset);
            
            const int chr = dstDatum(x+2, y, cOffset);
            const int cur = dstDatum(x+2, y-2, cOffset);
            const int cbr = dstDatum(x+2, y+2, cOffset);
            int cvu = dstDatum(x, y-2, cOffset);
            int cvd = dstDatum(x, y+2, cOffset);

            // Only work on the pixels that have a strong enough correlation between channels
            
            if (xy < 4 * cxy && cxy < 4 * xy) {
                const int dh = xy - (hl + hr)/2;
                const int dv = xy - (vu + vd)/2;
                const int ne = xy - (ul + br)/2;
                const int nw = xy - (ur + bl)/2;

                const int cdh = cxy - (chl + chr)/2;
                const int cdv = cxy - (cvu + cvd)/2;
                const int cne = cxy - (cul + cbr)/2;
                const int cnw = cxy - (cur + cbl)/2;
                
                const int gradients[4] = {
                    abs(dh) + abs(cdh),
                    abs(dv) + abs(cdv),
                    abs(ne) + abs(cne),
                    abs(nw) + abs(cnw)
                };
                
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
                            sample = (xy + (hl + hr)/2 + cdh) / 2;
                            break;
                        case 1: // vertical
                            sample = (xy + (vu + vd)/2 + cdv) / 2;
                            break;
                        case 2: // north-east
                            sample = (xy + (ul + br)/2 + cne) / 2;
                            break;
                        case 3: // north-west
                            sample = (xy + (ur + bl)/2 + cnw) / 2;
                            break;
                        case 4: // flat
                            // nothing to do
                            break;
                    }
                    dstDatum(x, y, gOffset) = clampUShort(sample);
                }
            }
            
            hl = xy;
            xy = hr;
            ul = vu;
            vu = ur;
            bl = vd;
            vd = br;
            chl = cxy;
            cxy = chr;
            cul = cvu;
            cvu = cur;
            cbl = cvd;
            cvd = cbr;
        }
    }

} // #pragma omp parallel

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
}

JNIEXPORT void JNICALL DCRaw_METHOD(interpolateRedBlue)
( JNIEnv *env, jclass cls,
  jshortArray jdata, jint width, jint height,
  jint lineStride,
  jint rOffset, jint gOffset, jint bOffset,
  jint rx0, jint ry0, jint bx0, jint by0 )
{
    auto data = static_cast<unsigned short*>(env->GetPrimitiveArrayCritical(jdata, 0));

    for (int i = 0; i < 2; i++) {
        int cx0, cy0, cOffset;
        if (i == 0) {
            cx0 = rx0;
            cy0 = ry0;
            cOffset = rOffset;
        } else {
            cx0 = bx0;
            cy0 = by0;
            cOffset = bOffset;
        }

#pragma omp parallel for schedule (guided) shared (data)
        for (int y = 1; y < height-2; y += 2) {
            int x = 1;
            int gne = gDatum(-1, 1);
            int gse = gDatum(-1, -1);
            int cne = gne - cDatum(-1, 1);
            int cse = gse - cDatum(-1, -1);

            for (; x < width-2; x += 2) {
                const int gnw = gDatum(1, 1);
                const int gsw = gDatum(1, -1);
                const int cnw = gnw - cDatum(1, 1);
                const int csw = gsw - cDatum(1, -1);

                // Pixel at the other color location
                const int gc = gDatum(0, 0);
                const int sample_c = gc - (cne + csw + cnw + cse) / 4;
                cDatum(0, 0) = clampUShort(sample_c);

                // Pixel at green location - vertical
                const int gw = gDatum(1, 0);
                const int sample_w = gw - (csw + cnw) / 2;
                cDatum(1, 0) = clampUShort(sample_w);

                // Pixel at green location - horizontal
                const int gs = gDatum(0, -1);
                const int sample_s = gs - (cse + csw) / 2;
                cDatum(0, -1) = clampUShort(sample_s);

                gne = gnw;
                gse = gsw;
                cne = cnw;
                cse = csw;
            }
        }
    }
    env->ReleasePrimitiveArrayCritical(jdata, data, 0);
}
