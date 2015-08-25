/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <jni.h>
#include <stdlib.h>
#include <math.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_DCRaw.h"
#endif

#include "LC_JNIUtils.h"

#define DCRaw_METHOD(method) \
        name4(Java_,com_lightcrafts_utils_DCRaw,_,method)

#include<limits.h>
#include<omp.h>

JNIEXPORT void JNICALL DCRaw_METHOD(interpolateGreen)
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdestData, jint width, jint height,
  jint srcLineStride, jint destLineStride,
  jint srcOffset, jint rOffset, jint gOffset, jint bOffset,
  jint gx, jint gy, jint ry )
{
    unsigned short *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);

#pragma omp parallel shared (srcData, destData)
{

    // copy RAW data to RGB layer and remove hot pixels

#pragma omp for schedule (dynamic)
    for (int y = 0; y < height; y++) {
        int cOffset = (y&1) == (ry&1) ? rOffset : bOffset;
        int x0 = (y&1) == (gy&1) ? gx+1 : gx;
        for (int x = 0; x < width; x++) {
            bool colorPixel = (x & 1) == (x0 & 1);
            int offset = colorPixel ? cOffset : gOffset;
            
            int value = srcData[y * srcLineStride + x + srcOffset];
            if (x >= 2 && x < width-2 && y >= 2 && y < height-2) {
                int v[12];
                int n;
                if (!colorPixel) {
                    n = 8;
                    v[0] = srcData[(y-1) * srcLineStride + x-1 + srcOffset];
                    v[1] = srcData[(y-1) * srcLineStride + x+1 + srcOffset];
                    v[2] = srcData[(y+1) * srcLineStride + x-1 + srcOffset];
                    v[3] = srcData[(y+1) * srcLineStride + x+1 + srcOffset];

                    v[4] = 2 * srcData[(y-1) * srcLineStride + x + srcOffset];
                    v[5] = 2 * srcData[(y+1) * srcLineStride + x + srcOffset];
                    v[6] = 2 * srcData[y * srcLineStride + x-1 + srcOffset];
                    v[7] = 2 * srcData[y * srcLineStride + x+1 + srcOffset];
                } else {
                    n = 12;
                    v[0] = srcData[(y-2) * srcLineStride + x + srcOffset];
                    v[1] = srcData[(y+2) * srcLineStride + x + srcOffset];
                    v[2] = srcData[y * srcLineStride + x-2 + srcOffset];
                    v[3] = srcData[y * srcLineStride + x+2 + srcOffset];
                    
                    v[4] = 2 * srcData[(y-1) * srcLineStride + x-1 + srcOffset];
                    v[5] = 2 * srcData[(y-1) * srcLineStride + x+1 + srcOffset];
                    v[6] = 2 * srcData[(y+1) * srcLineStride + x-1 + srcOffset];
                    v[7] = 2 * srcData[(y+1) * srcLineStride + x+1 + srcOffset];

                    v[8] = 2 * srcData[(y-1) * srcLineStride + x + srcOffset];
                    v[9] = 2 * srcData[(y+1) * srcLineStride + x + srcOffset];
                    v[10] = 2 * srcData[y * srcLineStride + x-1 + srcOffset];
                    v[11] = 2 * srcData[y * srcLineStride + x+1 + srcOffset];
                };
                bool replace = true;
                for (int i = 0; i < n; i++)
                    if (value < 2 * v[i]) {
                        replace = false;
                        break;
                    }
                if (replace)
                    value = (v[0] + v[1] + v[2] + v[3]) / 4;
            }
            destData[3 * (y * destLineStride + x) + offset] = value;
        }
    }

    // green channel interpolation

#pragma omp for schedule (dynamic)
    for (int y = 2; y < height-2; y++) {
        int cOffset = (y&1) == (ry&1) ? rOffset : bOffset;
        int x0 = (y&1) == (gy&1) ? gx+1 : gx;
        
        int hl = destData[3 * (y * destLineStride + (x0-1)) + gOffset];
        int cxy = destData[3 * (y * destLineStride + x0) + cOffset];
        int chl = destData[3 * (y * destLineStride + (x0-2)) + cOffset];
        
        const int x_min = (x0 & 1) ? 3 : 2; 
        for (int x = x_min; x < width-2; x += 2) {
            int hr = destData[3 * (y * destLineStride + (x+1)) + gOffset];
            int vu = destData[3 * ((y-1) * destLineStride + x) + gOffset];
            int vd = destData[3 * ((y+1) * destLineStride + x) + gOffset];
            int dh = abs(hl - hr);
            int dv = abs(vu - vd);

            int chr = destData[3 * (y * destLineStride + (x+2)) + cOffset];
            int cvu = destData[3 * ((y-2) * destLineStride + x) + cOffset];
            int cvd = destData[3 * ((y+2) * destLineStride + x) + cOffset];
            int cdh = abs(chl + chr - 2 * cxy);
            int cdv = abs(cvu + cvd - 2 * cxy);

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

            int value = sample < 0 ? 0 : sample > 0xffff ? 0xffff : sample;

            destData[3 * (y * destLineStride + x) + gOffset] = (unsigned short) value;

            hl = hr;
            chl = cxy;
            cxy = chr;
        }
    }

    // get the constant component out of the reconstructed green pixels and add to it
    // the "high frequency" part of the corresponding observed color channel
    
#pragma omp for schedule (dynamic)
    for (int y = 2; y < height-2; y++) {
        int cOffset = (y&1) == (ry&1) ? rOffset : bOffset;
        int x0 = (y&1) == (gy&1) ? gx+1 : gx;
        
        int xy = destData[3 * (y * destLineStride + x0) + gOffset];
        int hl = destData[3 * (y * destLineStride + x0-2) + gOffset];
        int ul = destData[3 * ((y-2) * destLineStride + x0-2) + gOffset];
        int bl = destData[3 * ((y+2) * destLineStride + x0-2) + gOffset];
        
        int cxy = destData[3 * (y * destLineStride + x0) + cOffset];
        int chl = destData[3 * (y * destLineStride + x0-2) + cOffset];
        int cul = destData[3 * ((y-2) * destLineStride + x0-2) + cOffset];
        int cbl = destData[3 * ((y+2) * destLineStride + x0-2) + cOffset];
        
        for (int x = 2; x < width-2; x+=2) {
            int hr = destData[3 * (y * destLineStride + x+2) + gOffset];
            int ur = destData[3 * ((y-2) * destLineStride + x+2) + gOffset];
            int br = destData[3 * ((y+2) * destLineStride + x+2) + gOffset];
            int vu = destData[3 * ((y-2) * destLineStride + x) + gOffset];
            int vd = destData[3 * ((y+2) * destLineStride + x) + gOffset];
            
            int chr = destData[3 * (y * destLineStride + x+2) + cOffset];
            int cur = destData[3 * ((y-2) * destLineStride + x+2) + cOffset];
            int cbr = destData[3 * ((y+2) * destLineStride + x+2) + cOffset];
            int cvu = destData[3 * ((y-2) * destLineStride + x) + cOffset];
            int cvd = destData[3 * ((y+2) * destLineStride + x) + cOffset];

            // Only work on the pixels that have a strong enough correlation between channels
            
            if (xy < 4 * cxy && cxy < 4 * xy) {
                int dh = xy - (hl + hr)/2;
                int dv = xy - (vu + vd)/2;
                int ne = xy - (ul + br)/2;
                int nw = xy - (ur + bl)/2;
                
                int cdh = cxy - (chl + chr)/2;
                int cdv = cxy - (cvu + cvd)/2;
                int cne = cxy - (cul + cbr)/2;
                int cnw = cxy - (cur + cbl)/2;
                
                int gradients[4] = {abs(dh)+abs(cdh), abs(dv)+abs(cdv), abs(ne)+abs(cne), abs(nw)+abs(cnw)};
                
                int mind = 4, maxd = 4;
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

                    int value = sample < 0 ? 0 : sample > 0xffff ? 0xffff : sample;
                    destData[3 * (y * destLineStride + x) + gOffset] = (unsigned short) value;
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
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
}

JNIEXPORT void JNICALL DCRaw_METHOD(interpolateRedBlue)
( JNIEnv *env, jclass cls,
  jshortArray jdata, jint width, jint height,
  jint lineStride,
  jint rOffset, jint gOffset, jint bOffset,
  jint rx0, jint ry0, jint bx0, jint by0 )
{
    unsigned short *data = (unsigned short *) env->GetPrimitiveArrayCritical(jdata, 0);

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

#pragma omp parallel for shared (data) schedule (dynamic)
        for (int y = 1; y < height-1; y++) {
            for (int x = 1; x < width-1; x++) {
                if (((x+cx0)&1) != (cx0&1) || ((y+cy0)&1) != (cy0&1)) {
                    int sample;
                    int cg = data[3 * (x + cx0 + (y + cy0) * lineStride) + gOffset];

                    if (((x+cx0)&1) != (cx0&1) && ((y+cy0)&1) != (cy0&1)) {
                        // Pixel at the other color location
                        int gne = data[3 * (x + cx0 - 1 + (y + cy0 + 1) * lineStride) + gOffset];
                        int gnw = data[3 * (x + cx0 + 1 + (y + cy0 + 1) * lineStride) + gOffset];
                        int gsw = data[3 * (x + cx0 + 1 + (y + cy0 - 1) * lineStride) + gOffset];
                        int gse = data[3 * (x + cx0 - 1 + (y + cy0 - 1) * lineStride) + gOffset];

                        int cne = gne - data[3 * (x + cx0 - 1 + (y + cy0 + 1) * lineStride) + cOffset];
                        int cnw = gnw - data[3 * (x + cx0 + 1 + (y + cy0 + 1) * lineStride) + cOffset];
                        int csw = gsw - data[3 * (x + cx0 + 1 + (y + cy0 - 1) * lineStride) + cOffset];
                        int cse = gse - data[3 * (x + cx0 - 1 + (y + cy0 - 1) * lineStride) + cOffset];

                        sample = cg - (cne + csw + cnw + cse) / 4;
                    } else if (((x+cx0)&1) == (cx0&1) && ((y+cy0)&1) != (cy0&1)) {
                        // Pixel at green location - vertical
                        int gu = data[3 * (x + cx0 + (y + cy0 - 1) * lineStride) + gOffset];
                        int gd = data[3 * (x + cx0 + (y + cy0 + 1) * lineStride) + gOffset];

                        int cu = gu - data[3 * (x + cx0 + (y + cy0 - 1) * lineStride) + cOffset];
                        int cd = gd - data[3 * (x + cx0 + (y + cy0 + 1) * lineStride) + cOffset];

                        sample = cg - (cu + cd) / 2;
                    } else {
                        // Pixel at green location - horizontal
                        int gl = data[3 * (x + cx0 - 1 + (y + cy0) * lineStride) + gOffset];
                        int gr = data[3 * (x + cx0 + 1 + (y + cy0) * lineStride) + gOffset];

                        int cl = gl - data[3 * (x + cx0 - 1 + (y + cy0) * lineStride) + cOffset];
                        int cr = gr - data[3 * (x + cx0 + 1 + (y + cy0) * lineStride) + cOffset];

                        sample = cg - (cl + cr) / 2;
                    }

                    data[3 * (x + cx0 + (y + cy0) * lineStride) + cOffset] = (unsigned short) (sample < 0
                            ? 0
                            : sample > 0xffff
                            ? 0xffff
                            : sample);
                }
            }
        }
    }
    env->ReleasePrimitiveArrayCritical(jdata, data, 0);
}
