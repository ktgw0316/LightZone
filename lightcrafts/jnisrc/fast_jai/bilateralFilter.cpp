/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <jni.h>

#include "vecUtils.h"

template <typename T> static INLINE T SQR( T x )
{
    return x * x;
}

static INLINE float fast_exp(float val)
{
    const float fast_exp_a = (1 << 23)/M_LN2;	
#if defined(__i386__)
    const float fast_exp_b_c = 127.0f * (1 << 23) - 405000;
#else
    const float fast_exp_b_c = 127.0f * (1 << 23) - 347000;
#endif
    if (val < -16)
        return 0;
    
    union {
        float f;
        int i;
    } result;
    
    result.i = (int)(fast_exp_a * val + fast_exp_b_c);
    return result.f;
}

static INLINE float inv_sqrt(float x) 
{ 
    float xhalf = 0.5f * x; 
    union {
        float f;
        unsigned int i;
    } n;
    
    n.f = x;                          // get bits for floating value 
    n.i = 0x5f375a86 - (n.i>>1);      // gives initial guess y0 
    x = n.f;                          // convert bits back to float 
    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
//    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
//    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
//    x = x*(1.5f-xhalf*x*x);           // Newton step, repeating increases accuracy 
    return x; 
}

#define SEPARABLE   1

#define idx(i)      (3 * (col+i) + wlast * srcLineStride)

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_BilateralFilterOpImage_bilateralFilterChroma
(JNIEnv *env, jclass cls,
 jshortArray jsrcData, jshortArray jdestData,
 jint wr, jint ws, jfloat scale_r, jfloatArray jkernel,
 jint width, jint height,
 jint srcLOffset, jint srcAOffset, jint srcBOffset,
 jint destLOffset, jint destAOffset, jint destBOffset,
 jint srcLineStride, jint destLineStride)
{
    unsigned short *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);
    float *kernel = (float *) env->GetPrimitiveArrayCritical(jkernel, 0) + wr; // Keep 0 in the middle...
    
    int i, wlast, row, col, y, x;
    int window_size = (ws+1) * sizeof(float *) + ws * width * sizeof(float);
    char *buffer = (char *) calloc (3 * window_size, 1);
    if (buffer == NULL)
        return;
    
    float **windowa = (float **) (buffer);
    float **windowb = (float **) (buffer + window_size);
    float **windowl = (float **) (buffer + 2 * window_size);
    
    for (i=0; i <= ws; i++) {
        windowa[i] = (float *) (windowa+ws+1) + i * width;
        windowb[i] = (float *) (windowb+ws+1) + i * width;
        windowl[i] = (float *) (windowl+ws+1) + i * width;
    }
    
    float k0 = kernel[0];
    
    float cnorm = 0x4000;
    
    const vfloat vk0 = vfloat::fill(k0);
    const vfloat vscale_r = vfloat::fill(scale_r);

    for (wlast=-1, row=0; row < height; row++) {
        while (wlast < row+wr) {
            wlast++;
            for (i=0; i <= ws; i++) {	/* rotate window rows */
                windowa[(ws+i) % (ws+1)] = windowa[i];
                windowb[(ws+i) % (ws+1)] = windowb[i];
                windowl[(ws+i) % (ws+1)] = windowl[i];
            }
            if (wlast < height) {
                col=0;
#if defined(__i386__)
                const float norm = 1 / (float) 0x10000;
                const vfloat vnorm = vfloat::fill(norm);
                const float normc = 1 / cnorm;
                const vfloat vnormc = vfloat::fill(normc);
                
                for (; col < width-3; col+=4) {
                    int i;
                    vint vl, va, vb;
                    
                    for (i = 0; i < 4; i++) {
                        int idx = 3 * (col+i) + wlast * srcLineStride;
                        vl[i] = srcData[idx + srcLOffset];
                        va[i] = srcData[idx + srcAOffset];
                        vb[i] = srcData[idx + srcBOffset];
                    }                    
                    
                    mmx::storeu(&windowl[ws-1][col], mmx::sqrt(mmx::cvt<vfloat, vint>(vl) * vnorm));
                    mmx::storeu(&windowa[ws-1][col], mmx::cvt<vfloat, vint>(va) * vnormc);
                    mmx::storeu(&windowb[ws-1][col], mmx::cvt<vfloat, vint>(vb) * vnormc);
                }
#endif
                for (; col < width; col++) {
                    float l = srcData[3 * col + wlast * srcLineStride + srcLOffset] / (float) 0x10000;
                    float a = srcData[3 * col + wlast * srcLineStride + srcAOffset] / cnorm;
                    float b = srcData[3 * col + wlast * srcLineStride + srcBOffset] / cnorm;
                    
                    windowl[ws-1][col] = l * inv_sqrt(l);
                    windowa[ws-1][col] = a;
                    windowb[ws-1][col] = b;
                }
            }
        }
        
        for (col=0; col < width; col++) {
            float sa = 0, sb = 0, ss = 0;
            if (!SEPARABLE) {
                float wa = windowa[wr][col];
                float wb = windowb[wr][col];
                float wl = windowl[wr][col];

                vfloat va = vfloat::fill(wa);
                vfloat vb = vfloat::fill(wb);
                vfloat vl = vfloat::fill(wl);

                for (y = row > wr ? -wr : -row; y <= wr && (row+y) < height; y++) {
                    float ky = kernel[y];
                    
                    x = col > wr ? -wr : -col;

                    if (ws > 3) {
                        vfloat vsa, vsb, vss;
                        vsa = vsb = vss = vfloat::fill<0>();
                        
                        vfloat vky = vfloat::fill(ky);
                        for (; x <= wr-3 && (col+x) < width-3; x+=4) {
                            vfloat vxy0 = loadu(&windowa[wr+y][col+x]);
                            vfloat vxy1 = loadu(&windowb[wr+y][col+x]);
                            vfloat vxyl = loadu(&windowl[wr+y][col+x]);
                            vfloat vexp = vexp_estimate((SQR(vxy0 - va) +
                                                         SQR(vxy1 - vb) +
                                                         SQR(vxyl - vl)) * vscale_r + vky + loadu(&kernel[x]));
                            vsa = vsa + vexp * vxy0;
                            vsb = vsb + vexp * vxy1;
                            vss = vss + vexp;
                        }
                        
                        sa += vsa.sum();
                        sb += vsb.sum();
                        ss += vss.sum();
                    }

                    for (; x <= wr && (col+x) < width; x++) {
                        float wxy0 = windowa[wr+y][col+x];
                        float wxy1 = windowb[wr+y][col+x];
                        float wxyl = windowl[wr+y][col+x];
                        float exp = fast_exp(- ((SQR(wxy0 - wa) + SQR(wxy1 - wb) + SQR(wxyl - wl)) * scale_r + ky + kernel[x]));
                        // if (exp > 0) {
                            sa += exp * wxy0;
                            sb += exp * wxy1;
                            ss += exp;
                        // }
                    }
                }
            } else {
                float wa = windowa[wr][col];
                float wb = windowb[wr][col];
                float wl = windowl[wr][col];
                
                x = col > wr ? -wr : -col;

                if (ws > 3) {
                    vfloat vsa, vsb, vss;
                    vsa = vsb = vss = vfloat::fill<0>();
                    
                    vfloat va = vfloat::fill(wa);
                    vfloat vb = vfloat::fill(wb);
                    vfloat vl = vfloat::fill(wl);
                    
                    for (; x <= wr-3 && (col+x) < width-3; x+=4) {
                        vfloat vx0 = loadu(&windowa[wr][col+x]);
                        vfloat vx1 = loadu(&windowb[wr][col+x]);
                        vfloat vxl = loadu(&windowl[wr][col+x]);
                        vfloat vexp = vexp_estimate((SQR(vx0 - va) + SQR(vx1 - vb) + SQR(vxl - vl)) * vscale_r + vk0 + loadu(&kernel[x]));
                        
                        vsa = vsa + vexp * vx0;
                        vsb = vsb + vexp * vx1;
                        vss = vss + vexp;
                    }
                    
                    sa += vsa.sum();
                    sb += vsb.sum();
                    ss += vss.sum();
                }

                for (; x <= wr && (col+x) < width; x++) {
                    float wx0 = windowa[wr][col+x];
                    float wx1 = windowb[wr][col+x];
                    float wxl = windowl[wr][col+x];
                    float exp = fast_exp(- ((SQR(wx0 - wa) + SQR(wx1 - wb) + SQR(wxl - wl)) * scale_r + k0 + kernel[x]));
                    // if (exp > 0) {
                        sa += exp * wx0;
                        sb += exp * wx1;
                        ss += exp;
                    // }
                }
                
                wa = windowa[wr][col] = sa / ss;
                wb = windowb[wr][col] = sb / ss;
                
                sa = 0; sb = 0; ss = 0;
                
                y = row > wr ? -wr : -row;

                if (ws > 3) {
                    vfloat vsa, vsb, vss;
                    vsa = vsb = vss = vfloat::fill<0>();
                    
                    vfloat va = vfloat::fill(wa);
                    vfloat vb = vfloat::fill(wb);
                    vfloat vl = vfloat::fill(wl);
                    
                    for (; y <= wr-3 && (row+y) < height-3; y+=4) {
                        vfloat y0, y1, yl;
                        for (i = 0; i < 4; i++) {
                            if (wr+y+i < ws) {
                                y0[i] = windowa[wr+y+i][col];
                                y1[i] = windowb[wr+y+i][col];
                                yl[i] = windowl[wr+y+i][col];
                            } else {
                                y0[i] = 0;
                                y1[i] = 0;
                                yl[i] = 0;
                            }
                        }
                        
                        vfloat vexp = vexp_estimate((SQR(y0 - va) + SQR(y1 - vb) + SQR(yl - vl)) * vscale_r + vk0 + loadu(&kernel[y]));
                        
                        vsa = vsa + vexp * y0;
                        vsb = vsb + vexp * y1;
                        vss = vss + vexp;
                    }
                    
                    sa += vsa.sum();
                    sb += vsb.sum();
                    ss += vss.sum();
                }

                for (; y <= wr && (row+y) < height; y++) {
                    float wy0 = windowa[wr+y][col];
                    float wy1 = windowb[wr+y][col];
                    float wyl = windowl[wr+y][col];
                    float exp = fast_exp(- ((SQR(wy0 - wa) + SQR(wy1 - wb) + SQR(wyl - wl)) * scale_r  + kernel[y] + k0));
                    // if (exp > 0) {
                        sa += exp * wy0;
                        sb += exp * wy1;
                        ss += exp;
                    // }
                }
            }
            
            if (col >= wr && col < width - wr && row >= wr && row < height - wr) {
                int srcPixelOffset = 3 * col + row * srcLineStride;
                
                int l = srcData[srcPixelOffset + srcLOffset];
                int a = (int) (cnorm * (sa / ss));
                int b = (int) (cnorm * (sb / ss));
                
                int dstPixelOffset = 3 * (col-wr) + (row-wr) * destLineStride;
                
                destData[dstPixelOffset + destLOffset] = (unsigned short) l; // (r < 0 ? 0 : r > 0xffff ? 0xffff : r);
                destData[dstPixelOffset + destAOffset] = (unsigned short) (a < 0 ? 0 : a > 0xffff ? 0xffff : a);
                destData[dstPixelOffset + destBOffset] = (unsigned short) (b < 0 ? 0 : b > 0xffff ? 0xffff : b);
            }
        }
    }

    free (buffer);

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
    env->ReleasePrimitiveArrayCritical(jkernel, kernel, 0);
}

extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_BilateralFilterOpImage_bilateralFilterMono
(JNIEnv *env, jclass cls,
 jshortArray jsrcData, jshortArray jdestData,
 jint wr, jint ws, jfloat scale_r, jfloatArray jkernel,
 jint width, jint height,
 jint srcPixelStride, jint destPixelStride,
 jint srcLOffset, jint destLOffset,
 jint srcLineStride, jint destLineStride)
{
    unsigned short *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);
    float *kernel = (float *) env->GetPrimitiveArrayCritical(jkernel, 0) + wr; // Keep 0 in the middle...
    
    int i, wlast, row, col, y, x;
    int window_size = (ws+1) * sizeof(float *) + ws * width * sizeof(float);
    float **windowl = (float **) calloc (window_size, 1);
    if (windowl == NULL)
        return;
    
    for (i=0; i <= ws; i++)
        windowl[i] = (float *) (windowl+ws+1) + i * width;
    
    float k0 = kernel[0];
    
    const vfloat vk0 = vfloat::fill(k0);
    const vfloat vscale_r = vfloat::fill(scale_r);

    for (wlast=-1, row=0; row < height; row++) {
        while (wlast < row+wr) {
            wlast++;
            for (i=0; i <= ws; i++)     /* rotate window rows */
                windowl[(ws+i) % (ws+1)] = windowl[i];

            if (wlast < height) {
                col=0;

#if defined(__i386__)
                const float norm = 1 / (float) 0x10000;
                const vfloat vnorm = vfloat::fill(norm);
                
                for (; col < width-7; col+=8) {
                    vushort bunch(loadu(&srcData[srcPixelStride * col + wlast * srcLineStride + srcLOffset]));

                    const vfloat low = mmx::cvt<vfloat, vint>(data_cast<vint>(mmx::unpacklo(bunch, vushort::fill<0>())));
                    mmx::storeu(&windowl[ws-1][col], mmx::sqrt(low * vnorm));

                    const vfloat high = mmx::cvt<vfloat, vint>(data_cast<vint>(mmx::unpackhi(bunch, vushort::fill<0>())));
                    mmx::storeu(&windowl[ws-1][col+4], mmx::sqrt(high * vnorm));

                    // const vfloat vraw = altivec::ctf<0>(data_cast<vint>(altivec::mergeh(vushort::fill<0>(), bunch)));
                }
#endif
                for (; col < width; col++) {
                    float l = srcData[srcPixelStride * col + wlast * srcLineStride + srcLOffset] / (float) 0x10000;
                    windowl[ws-1][col] = l * inv_sqrt(l);
                }
            }
        }
        
        for (col=0; col < width; col++) {
            float sl = 0, ss = 0;
            float wl = windowl[wr][col];
            vfloat vl = vfloat::fill(wl);
            
            if (1 || !SEPARABLE) {
                for (y = row > wr ? -wr : -row; y <= wr && (row+y) < height; y++) {
                    float ky = kernel[y];
                    vfloat vky = vfloat::fill(ky);
                    float *windowy = windowl[wr+y];
                                        
                    x = col > wr ? -wr : -col;

                    if (ws > 3) {
                        vfloat vsl, vss;
                        vsl = vss = vfloat::fill<0>();

                        for (; x <= wr-3 && (col+x) < width-3; x+=4) {
                            vfloat vxyl = loadu(&windowy[col+x]);
                            vfloat vexp = vexp_estimate(SQR(vxyl - vl) * vscale_r + vky + loadu(&kernel[x]));
                            
                            vsl = vsl + vexp * vxyl;
                            vss = vss + vexp;
                        }

                        sl += vsl.sum();
                        ss += vss.sum();
                    }

                    for (; x <= wr && (col+x) < width; x++) {
                        float wxyl = windowy[col+x];
                        float exp = fast_exp(- (SQR(wxyl - wl) * scale_r + ky + kernel[x]));
                        // if (exp > 0) {
                            sl += exp * wxyl;
                            ss += exp;
                        // }
                    }
                }
            } else {
                x = col > wr ? -wr : -col;

                if (ws > 3) {
                    vfloat vsl, vss;
                    vsl = vss = vfloat::fill<0>();
                                        
                    for (; x <= wr-3 && (col+x) < width-3; x+=4) {
                        vfloat vxl = loadu(&windowl[wr][col+x]);
                        vfloat vexp = vexp_estimate(SQR(vxl - vl) * vscale_r + vk0 + loadu(&kernel[x]));
                        vsl = vsl + vexp * vxl;
                        vss = vss + vexp;
                    }
                    
                    sl += vsl.sum();
                    ss += vss.sum();
                }

                for (; x <= wr && (col+x) < width; x++) {
                    float wxl = windowl[wr][col+x];
                    float exp = fast_exp(- (SQR(wxl - wl) * scale_r + k0 + kernel[x]));
                    // if (exp > 0) {
                        sl += exp * wxl;
                        ss += exp;
                    // }
                }
                
                wl = windowl[wr][col] = sl / ss;
                
                sl = 0; ss = 0;
                
                y = row > wr ? -wr : -row;

                if (ws > 3) {
                    vfloat vsl, vss;
                    vsl = vss = vfloat::fill<0>();
                    
                    vfloat vl = vfloat::fill(wl);
                    
                    for (; y <= wr-3 && (row+y) < height-3; y+=4) {
                        vfloat yl;
                        for (i = 0; i < 4; i++) {
                            if (wr+y+i < ws) {
                                yl[i] = windowl[wr+y+i][col];
                            } else {
                                yl[i] = 0;
                            }
                        }
                        
                        vfloat vexp = vexp_estimate(SQR(yl - vl) * vscale_r + vk0 + loadu(&kernel[y]));
                        
                        vsl = vsl + vexp * yl;
                        vss = vss + vexp;
                    }
                    
                    sl += vsl.sum();
                    ss += vss.sum();
                }

                for (; y <= wr && (row+y) < height; y++) {
                    float wyl = windowl[wr+y][col];
                    float exp = fast_exp(- (SQR(wyl - wl) * scale_r  + kernel[y] + k0));
                    // if (exp > 0) {
                        sl += exp * wyl;
                        ss += exp;
                    // }
                }
            }
            
            if (col >= wr && col < width - wr && row >= wr && row < height - wr) {
                int l = (int) (0x10000 * SQR(sl / ss));
                
                int dstPixelOffset = destPixelStride * (col-wr) + (row-wr) * destLineStride;
                
                destData[dstPixelOffset + destLOffset] = (unsigned short) (l < 0 ? 0 : l > 0xffff ? 0xffff : l);
            }
        }
    }

    free (windowl);

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
    env->ReleasePrimitiveArrayCritical(jkernel, kernel, 0);
}
