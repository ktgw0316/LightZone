#include <math.h>
#include <omp.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>

#include "../include/mathlz.h"

#ifdef __INTEL_COMPILER
#include <fvec.h>
#include <dvec.h>

// TODO: check out _mm_lddqu_si128

inline F32vec4 convert_high(const Iu16vec8 &a) {
    return _mm_cvtepi32_ps(unpack_high(a, (__m128i)_mm_setzero_ps()));
}

inline F32vec4 convert_low(const Iu16vec8 &a) {
    return _mm_cvtepi32_ps(unpack_low(a, (__m128i)_mm_setzero_ps()));
}

// convert two F32vec4 to a Iu16vec8: deal with the (signed) saturating nature of _mm_packs_epi32
inline Iu16vec8 F32vec4toIu16vec8(const F32vec4 &hi, const F32vec4 &lo) {
    const Iu32vec4 sign_swap32(0x8000, 0x8000, 0x8000, 0x8000);
    const Iu16vec8 sign_swap16(0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000);
    
    return Iu16vec8(_mm_packs_epi32(_mm_cvtps_epi32(lo)-sign_swap32, _mm_cvtps_epi32(hi)-sign_swap32) ^ sign_swap16);
}

inline Is16vec8 F32vec4toIs16vec8(const F32vec4 &hi, const F32vec4 &lo) {
    return _mm_packs_epi32(_mm_cvttps_epi32(lo), _mm_cvttps_epi32(hi));
}

// Ad Horizontal using SSE3
/* inline float addh(const F32vec4 &a) 
{ 
    return _mm_cvtss_f32(_mm_hadd_ps(_mm_hadd_ps(a, _mm_setzero_ps()), _mm_setzero_ps()));
}
*/

static inline F32vec4 v_fast_exp(F32vec4 val)
{
    const F32vec4 fast_exp_a((1 << 23)/M_LN2);
    const F32vec4 fast_exp_b_c(127.0f * (1 << 23) - 405000);
    const F32vec4 v_zero = _mm_setzero_ps();
    const F32vec4 v_m16 = F32vec4(-16.0f);
    
    F32vec4 result = (__m128) _mm_cvtps_epi32(fast_exp_a * val + fast_exp_b_c);
    
    return select_lt(val, v_m16, v_zero, result);
}

#endif

/*******************************************************************************
 * separable_bf_mono_tile()
 *
 * Apply a separable bilateral filter to a rectangular region of a single-band 
 * raster
 *
 * Dimensions of source and destination rectangles are related by
 *
 *     dst_width = src_width - 2*wr
 *     dst_height = src_height - 2*wr
 *
 * 'kernel' points to the mid-point of a (2*wr + 1)-length array containing 
 * either 
 *     1) spatial gaussian filter coefficients for distances 0, 1, ..., wr
 *     2) negated exponents of the spatial gaussian function (this is what Fabio 
 *        passes from his BilateralFilterOpImage class)
 *
 * The macro GS_x_GR(x) controls the interpretation.
 *******************************************************************************/
void separable_bf_mono_tile(
    float *ibuf,                            // pointer to source data buffer 
    float sr,                               // the usual range sigma
    int wr,                                 // window radius in pixels
    float *kernel,                          // half-kernel containing the exponents of the spatial Gaussian
    int width, int height)                  // dimensions of the source image
{
    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr) );
    
#pragma omp parallel
{
    //--------------------------------------------------------------------------
    // Filter Rows
    //--------------------------------------------------------------------------
    
    float *rbuf = new float[width];
    
#pragma omp for
    for (int y=wr; y < height - wr; y++) {
        
        memcpy(rbuf, &ibuf[y * width], width * sizeof(float));
        
        int x=wr;
#ifdef __INTEL_COMPILER
        for (/*int x=wr*/; x < width - wr-4; x+=4) {
            const F32vec4 v_zero = _mm_setzero_ps();
            const F32vec4 v_one = F32vec4(1.0f);
            const F32vec4 v_ffff = F32vec4((float) 0xffff);
            
            // compute adaptive kernel and convolve color channels
            F32vec4 v_num = v_zero;
            F32vec4 v_denom = v_zero;
            const F32vec4 v_I_s0 = _mm_loadu_ps(&rbuf[x]);
            const F32vec4 v_Ar(Ar);
            
            for (int k = 0; k <= 2*wr; k++) {
                const F32vec4 v_I_s = _mm_loadu_ps(&rbuf[k-wr + x]);
                const F32vec4 v_D_sq = SQR(v_I_s - v_I_s0);
                const F32vec4 v_f = v_fast_exp(v_Ar * v_D_sq - F32vec4(kernel[k]));
                v_num += v_f * v_I_s;
                v_denom += v_f;
            }
            
            // normalize
            v_denom = select_eq(v_denom, v_zero, v_one, v_denom);
                        
            const int idx = x + y*width;
            _mm_storeu_ps(&ibuf[idx], v_num / v_denom);
        }
#endif
        for (/*int x=wr*/; x < width - wr; x++) {
            
            const float I_s0 = rbuf[x];
            
            // compute adaptive kernel and convolve color channels
            float num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const float I_s = rbuf[k-wr + x];
                const float D_sq = SQR(I_s - I_s0);

#ifdef FP_FAST_FMAF
                const float f = fast_exp(fmaf(Ar, D_sq, -kernel[k]));
                num = fmaf(f, I_s, num);
#else
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                num += f * I_s;
#endif
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            const int idx = x + y*width;
            ibuf[idx] = num / denom;
        }
    }
    
    delete [] rbuf;
    
    //--------------------------------------------------------------------------
    // Filter Columns
    //--------------------------------------------------------------------------
    
    // Buffer for processing column data
    float *cbuf = new float[height];
    
#pragma omp for
    for (int x=wr; x < width - wr; x++) {
        for (int y=0; y < height; y++)
            cbuf[y] = ibuf[x + y*width];
        
        int y=wr;
#ifdef __INTEL_COMPILER
        const F32vec4 v_zero(0.0f);
        const F32vec4 v_one(1.0f);
        const F32vec4 v_ffff((float) 0xffff);
        
        for (; y < height - wr - 4; y+=4) {
            // initialize central pixel
            const F32vec4 b0 = _mm_loadu_ps(&cbuf[y]);
            const F32vec4 v_Ar(Ar);
            
            // compute adaptive kernel and convolve color channels
            F32vec4 num = v_zero;
            F32vec4 denom = v_zero;
            
            for (int k = 0; k <= 2*wr; k++) {                
                const F32vec4 b = _mm_loadu_ps(&cbuf[(k-wr) + y]);
                const F32vec4 D_sq = SQR(b - b0);
                const F32vec4 f = v_fast_exp(v_Ar * D_sq - F32vec4(kernel[k]));
                
                num += f * b;
                denom += f;
            }
            
            // normalize
            denom = select_eq(denom, v_zero, v_one, denom);
            
            union {
                __m128 v_result;
                float  a_result[4];
            };
            
            v_result = num / denom;
            for (int i = 0; i < 4; i++)
                ibuf[x + (y+i)*width] = a_result[i];
        }
#endif
        for (/* int y=wr */; y < height - wr; y++) {
            // initialize central pixel
            const float b0 = cbuf[y];
            
            // compute adaptive kernel and convolve color channels
            float num = 0;
            float denom = 0;            
            for (int k = 0; k <= 2*wr; k++) {
                const float b = cbuf[(k-wr) + y];
                const float D_sq = SQR(b - b0);

#ifdef FP_FAST_FMAF
                const float f = fast_exp(fmaf(Ar, D_sq, -kernel[k]));
                num = fmaf(f, b, num);
#else
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                num += f * b;
#endif
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            const int idx = y * width + x;
            ibuf[idx] = num / denom;
        }
    }    
    
    delete [] cbuf;
} // omp parallel
}

#ifdef __INTEL_COMPILER
#define CONST_INT32_PS(N, V3,V2,V1,V0) \
static const _MM_ALIGN16 int _##N[]= \
    {V0, V1, V2, V3};/*little endian!*/ \
const F32vec4 N = _mm_load_ps((float*)_##N);

// usage example, mask for elements 3 and 1:
// CONST_INT32_PS(mask31, ~0, 0, ~0, 0);

// Convert three array of interleaved data into three arrays of segregated data
inline void XYZtoF32vec4(F32vec4& x, F32vec4& y, F32vec4& z, const F32vec4& a, const F32vec4& b, const F32vec4& c) {
    CONST_INT32_PS(mask1,   0,  0, ~0,  0);
    CONST_INT32_PS(mask2,   0, ~0,  0,  0);
    CONST_INT32_PS(mask30, ~0,  0,  0, ~0);
    
    x = (a & mask30) | (b & mask2) | (c & mask1);
    y = (a & mask1) | (b & mask30) | (c & mask2);
    z = (a & mask2) | (b & mask1) | (c & mask30);
    x = _mm_shuffle_ps(x, x, _MM_SHUFFLE(1,2,3,0));
    y = _mm_shuffle_ps(y, y, _MM_SHUFFLE(2,3,0,1));
    z = _mm_shuffle_ps(z, z, _MM_SHUFFLE(3,0,1,2));
}

/*
 
 x = a3, a2, a1, a0
 y = b3, b2, b1, b0
 z = c3, c2, c1, c0
 
 shuffle
 
 a = a1, a2, a3, a0
 b = b2, b3, b0, b1
 c = c3, c0, c1, c2
 
 or
 
 a = a1, c0, b0, a0 = a(3), c(2), b(1), a(0)
 b = b2, a2, c1, b1 = b(3), a(2), c(1), b(0)
 c = c3, b3, a3, c2 = c(3), b(2), a(1), c(0)
 
 */

inline void F32vec4toXYZ(F32vec4& a, F32vec4& b, F32vec4& c, const F32vec4& x, const F32vec4& y, const F32vec4& z)
{
    CONST_INT32_PS(mask1,   0,  0, ~0,  0);
    CONST_INT32_PS(mask2,   0, ~0,  0,  0);
    CONST_INT32_PS(mask30, ~0,  0,  0, ~0);
    
    F32vec4 ta = _mm_shuffle_ps(x, x, _MM_SHUFFLE(1,2,3,0));
    F32vec4 tb = _mm_shuffle_ps(y, y, _MM_SHUFFLE(2,3,0,1));
    F32vec4 tc = _mm_shuffle_ps(z, z, _MM_SHUFFLE(3,0,1,2));
    
    a = (ta & mask30) | (tb & mask1) | (tc & mask2);
    b = (ta & mask2) | (tb & mask30) | (tc & mask1);
    c = (ta & mask1) | (tb & mask2) | (tc & mask30);
}
#endif

void planar_YST_to_interleaved_RGB(unsigned short * const dstData, int dstStep,
                                   int r_offset, int g_offset, int b_offset, int wr,
                                   const float * const buf_y, const float * const buf_s, const float * const buf_t,
                                   int width, int height,
                                   float *yst_to_rgb) {
#ifdef __INTEL_COMPILER
    const F32vec4 v_ffff((float) 0xffff);
    const F32vec4 v_zero(0.0f);
    const F32vec4 v_05(0.5f);
    
    F32vec4 v_yst_to_rgb[9];
    
    for (int i = 0; i < 9; i++)
        v_yst_to_rgb[i] = F32vec4(yst_to_rgb[i]);
#endif    
    
#pragma omp parallel for
    for (int y=wr; y < height-wr; y++) {
        int x=wr;
#ifdef __INTEL_COMPILER
        for (/*int x=wr*/; x < width-wr-8; x+=8) {
            const int dst_idx = 3*(x-wr) + (y-wr)*dstStep + r_offset;
            const int idx = x + y*width;
            
            F32vec4 y = _mm_loadu_ps(&buf_y[idx]);
            F32vec4 s = _mm_loadu_ps(&buf_s[idx]) - v_05;
            F32vec4 t = _mm_loadu_ps(&buf_t[idx]) - v_05;
            
            F32vec4 v_rgb[3];
            
            for (int c = 0; c < 3; c++)
                v_rgb[c] = v_ffff * (v_yst_to_rgb[3*c] * y +
                                     v_yst_to_rgb[3*c+1] * s +
                                     v_yst_to_rgb[3*c+2] * t);
            F32vec4 a1, b1, c1;
            
            /*
             
             a1 = R1 B0 G0 R0
             b1 = G2 R2 B1 G1
             c1 = B3 G3 R3 B2
             
             */
            
            F32vec4toXYZ(a1, b1, c1, v_rgb[0], v_rgb[1], v_rgb[2]);
            
            // no need to clamp to [0..0xffff], F32vec4toIu16vec8 does it automagically
            
            /* a1 = simd_max(v_zero, simd_min(a1, v_ffff));
            b1 = simd_max(v_zero, simd_min(b1, v_ffff));
            c1 = simd_max(v_zero, simd_min(c1, v_ffff)); */
            
            y = _mm_loadu_ps(&buf_y[idx+4]);
            s = _mm_loadu_ps(&buf_s[idx+4]) - v_05;
            t = _mm_loadu_ps(&buf_t[idx+4]) - v_05;
            
            v_rgb[3];
            
            for (int c = 0; c < 3; c++)
                v_rgb[c] = v_ffff * (v_yst_to_rgb[3*c] * y +
                                     v_yst_to_rgb[3*c+1] * s +
                                     v_yst_to_rgb[3*c+2] * t);
            F32vec4 a2, b2, c2;
            
            /*
             
             a2 = R5 B4 G4 R4
             b2 = G6 R6 B5 G5
             c2 = B7 G7 R7 B6
             
             */
            
            F32vec4toXYZ(a2, b2, c2, v_rgb[0], v_rgb[1], v_rgb[2]);
            
            /* a2 = simd_max(v_zero, simd_min(a2, v_ffff));
            b2 = simd_max(v_zero, simd_min(b2, v_ffff));
            c2 = simd_max(v_zero, simd_min(c2, v_ffff)); */
            
            _mm_storeu_si128((__m128i *) &dstData[dst_idx], F32vec4toIu16vec8(b1, a1));       // G2 R2 B1 G1 R1 B0 G0 R0
            _mm_storeu_si128((__m128i *) &dstData[dst_idx + 8], F32vec4toIu16vec8(a2, c1));   // R5 B4 G4 R4 B3 G3 R3 B2
            _mm_storeu_si128((__m128i *) &dstData[dst_idx + 16], F32vec4toIu16vec8(c2, b2));  // B7 G7 R7 B6 G6 R6 B5 G5
        }
#endif
        for (/*int x=wr*/; x < width-wr; x++) {
            const int dst_idx = 3*(x-wr) + (y-wr)*dstStep + r_offset;
            const int idx = x + y*width;
            
            const float y = buf_y[idx];
            const float s = buf_s[idx] - 0.5f;
            const float t = buf_t[idx] - 0.5f;
                        
            for (int c = 0; c < 3; c++) {
#ifdef FP_FAST_FMAF
                const float rgb = fmaf(yst_to_rgb[3*c+2], t,
                                  fmaf(yst_to_rgb[3*c+1], s,
                                       yst_to_rgb[3*c]  * y));
#else
                const float rgb = yst_to_rgb[3*c]   * y +
                                  yst_to_rgb[3*c+1] * s +
                                  yst_to_rgb[3*c+2] * t;
#endif
                dstData[dst_idx+c] = clampUShort(0xffff * rgb);
            }
        }
    }
}

void inverseMatrix(const float A[3][3], float result[3][3]) {
    float determinant = +A[0][0]*(A[1][1]*A[2][2]-A[2][1]*A[1][2])
                        -A[0][1]*(A[1][0]*A[2][2]-A[1][2]*A[2][0])
                        +A[0][2]*(A[1][0]*A[2][1]-A[1][1]*A[2][0]);
    
    float invdet = 1/determinant;
    
    result[0][0] =  (A[1][1]*A[2][2]-A[2][1]*A[1][2])*invdet;
    result[1][0] = -(A[0][1]*A[2][2]-A[0][2]*A[2][1])*invdet;
    result[2][0] =  (A[0][1]*A[1][2]-A[0][2]*A[1][1])*invdet;
    result[0][1] = -(A[1][0]*A[2][2]-A[1][2]*A[2][0])*invdet;
    result[1][1] =  (A[0][0]*A[2][2]-A[0][2]*A[2][0])*invdet;
    result[2][1] = -(A[0][0]*A[1][2]-A[1][0]*A[0][2])*invdet;
    result[0][2] =  (A[1][0]*A[2][1]-A[2][0]*A[1][1])*invdet;
    result[1][2] = -(A[0][0]*A[2][1]-A[2][0]*A[0][1])*invdet;
    result[2][2] =  (A[0][0]*A[1][1]-A[1][0]*A[0][1])*invdet;
}

void interleaved_RGB_to_planar_YST(const unsigned short * const srcData, int srcStep,
                                   int r_offset, int g_offset, int b_offset,
                                   float *buf_y, float *buf_s, float *buf_t,
                                   int width, int height,
                                   float *rgb_to_yst) {
    const float norm = (float)0x10000;
    const float inv_norm = 1.0f/norm;
    
#ifdef __INTEL_COMPILER
    const F32vec4 v_inv_norm(inv_norm);
    const F32vec4 v_05(0.5f);
    
    F32vec4 v_rgb_to_yst[3][3];
    
    for (int y = 0; y < 3; y++)
        for (int x = 0; x < 3; x++)
            v_rgb_to_yst[y][x] = F32vec4(rgb_to_yst[3 * y + x]);
#endif
        
#pragma omp parallel for
    for (int y=0; y < height; y++) {
        int x=0;
#ifdef __INTEL_COMPILER
        for (; x < width-8; x+=8) {
            const int src_idx = 3*x + y*srcStep + r_offset;
            const int idx = x + y*width;
            
            // Use SSE swizzling magic to turn interleaved RGB data to planar
            
            // load 3 arrays of Iu16vec8
            Iu16vec8 src8_1(_mm_loadu_si128((__m128i *) &srcData[src_idx]));        // G2 R2 B1 G1 R1 B0 G0 R0
            Iu16vec8 src8_2(_mm_loadu_si128((__m128i *) &srcData[src_idx + 8]));    // R5 B4 G4 R4 B3 G3 R3 B2
            Iu16vec8 src8_3(_mm_loadu_si128((__m128i *) &srcData[src_idx + 16]));   // B7 G7 R7 B6 G6 R6 B5 G5
            
            // get the first three F32vec4
            F32vec4 src4_1 = convert_low(src8_1);   // R1 B0 G0 R0 -> a1
            F32vec4 src4_2 = convert_high(src8_1);  // G2 R2 B1 G1 -> b1
            F32vec4 src4_3 = convert_low(src8_2);   // B3 G3 R3 B2 -> c1
            
            F32vec4 src4_4 = convert_high(src8_2);  // R5 B4 G4 R4 -> a2
            F32vec4 src4_5 = convert_low(src8_3);   // G6 R6 B5 G5 -> b2            
            F32vec4 src4_6 = convert_high(src8_3);  // B7 G7 R7 B6 -> c2
            
            F32vec4 src4_rgb[3];
            
            // swizzle them and store
            XYZtoF32vec4(src4_rgb[0], src4_rgb[1], src4_rgb[2], src4_1, src4_2, src4_3);
            
            F32vec4 v_r = v_inv_norm * src4_rgb[0];
            F32vec4 v_g = v_inv_norm * src4_rgb[1];
            F32vec4 v_b = v_inv_norm * src4_rgb[2];
            
            F32vec4 y = v_rgb_to_yst[0][0] * v_r + v_rgb_to_yst[0][1] * v_g + v_rgb_to_yst[0][2] * v_b;
            F32vec4 s = v_rgb_to_yst[1][0] * v_r + v_rgb_to_yst[1][1] * v_g + v_rgb_to_yst[1][2] * v_b + v_05;
            F32vec4 t = v_rgb_to_yst[2][0] * v_r + v_rgb_to_yst[2][1] * v_g + v_rgb_to_yst[2][2] * v_b + v_05;
            
            _mm_storeu_ps(&buf_y[idx], y);
            _mm_storeu_ps(&buf_s[idx], s);
            _mm_storeu_ps(&buf_t[idx], t);
            
            // second batch
            XYZtoF32vec4(src4_rgb[0], src4_rgb[1], src4_rgb[2], src4_4, src4_5, src4_6);
            
            v_r = v_inv_norm * src4_rgb[0];
            v_g = v_inv_norm * src4_rgb[1];
            v_b = v_inv_norm * src4_rgb[2];
            
#ifdef FP_FAST_FMAF
            y = fmaf(v_rgb_to_yst[0][2], v_b, fmaf(v_rgb_to_yst[0][1], v_g,      v_rgb_to_yst[0][0] * v_r));
            s = fmaf(v_rgb_to_yst[1][2], v_b, fmaf(v_rgb_to_yst[1][1], v_g, fmaf(v_rgb_to_yst[1][0], v_r, v_05)));
            t = fmaf(v_rgb_to_yst[2][2], v_b, fmaf(v_rgb_to_yst[2][1], v_g, fmaf(v_rgb_to_yst[2][0], v_r, v_05)));
#else
            y = v_rgb_to_yst[0][0] * v_r + v_rgb_to_yst[0][1] * v_g + v_rgb_to_yst[0][2] * v_b;
            s = v_rgb_to_yst[1][0] * v_r + v_rgb_to_yst[1][1] * v_g + v_rgb_to_yst[1][2] * v_b + v_05;
            t = v_rgb_to_yst[2][0] * v_r + v_rgb_to_yst[2][1] * v_g + v_rgb_to_yst[2][2] * v_b + v_05;
#endif
            
            _mm_storeu_ps(&buf_y[idx+4], y);
            _mm_storeu_ps(&buf_s[idx+4], s);
            _mm_storeu_ps(&buf_t[idx+4], t);
        }
#endif
        for (/*int x=0*/; x < width; x++) {
            const int src_idx = 3*x + y*srcStep;
            const int idx = x + y*width;
            
            float r = inv_norm * (float) srcData[src_idx+r_offset];
            float g = inv_norm * (float) srcData[src_idx+g_offset];
            float b = inv_norm * (float) srcData[src_idx+b_offset];
            
            float YST[3];
            
            for (int c = 0; c < 3; c++) {
#ifdef FP_FAST_FMAF
                YST[c] = fmaf(rgb_to_yst[3*c+2], b,
                         fmaf(rgb_to_yst[3*c+1], g,
                         fmaf(rgb_to_yst[3*c],   r,
                         (c > 0 ? 0.5f : 0))));
#else
                YST[c] = rgb_to_yst[3*c]   * r +
                         rgb_to_yst[3*c+1] * g +
                         rgb_to_yst[3*c+2] * b +
                         (c > 0 ? 0.5f : 0);
#endif
            }
            
            buf_y[idx] = YST[0];
            buf_s[idx] = YST[1];
            buf_t[idx] = YST[2];
        }
    }    
}

/*******************************************************************************
 * separable_bf_chroma_tile()
 *
 * Apply a separable bilateral filter to a rectangular region of a color raster 
 *
 * Dimensions of source and destination rectangles are related by
 *
 *     dst_width = src_width - 2*wr
 *     dst_height = src_height - 2*wr
 *
 * 'kernel' points to the mid-point of a (2*wr + 1)-length array containing 
 * either 
 *     1) spatial gaussian filter coefficients for distances 0, 1, ..., wr
 *     2) negated exponents of the spatial gaussian function (this is what Fabio 
 *        passes from his BilateralFilterOpImage class)
 *
 * The macro GS_x_GR(x) controls the interpretation.
 *******************************************************************************/
void separable_bf_chroma_tile(
    float *buf_a,                           // pointer to the s source/destination buffer 
    float *buf_b,                           // pointer to the t source/destination buffer
    float sr,                               // the usual range sigma
    int wr,                                 // window radius in pixels
    float *kernel,                          // half-kernel containing the exponents of the spatial Gaussian
    int width, int height)                  // dimensions of the source image
{    
    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr) );
    
    //--------------------------------------------------------------------------
    // Filter Rows
    //--------------------------------------------------------------------------
    
#pragma omp parallel
{
    float *rbuf_a = new float[width];
    float *rbuf_b = new float[width];
    
#pragma omp for
    for (int y=wr; y < height - wr; y++) {
        int x=wr;
        
        memcpy(rbuf_a, &buf_a[y * width], width * sizeof(float));
        memcpy(rbuf_b, &buf_b[y * width], width * sizeof(float));
        
#ifdef __INTEL_COMPILER
        const F32vec4 v_zero = _mm_setzero_ps();
        const F32vec4 v_one = F32vec4(1.0f);
        
        for (/*int x=wr*/; x < width - wr-4; x+=4) {
            // initialize central pixel
            const F32vec4 s0_a = _mm_loadu_ps(&rbuf_a[x]);
            const F32vec4 s0_b = _mm_loadu_ps(&rbuf_b[x]);
            const F32vec4 v_Ar(Ar);
            
            // compute adaptive kernel and convolve color channels
            F32vec4 a_num = v_zero;
            F32vec4 b_num = v_zero;
            F32vec4 denom = v_zero;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + x;
                
                const F32vec4 s_a = _mm_loadu_ps(&rbuf_a[idx]);
                const F32vec4 s_b = _mm_loadu_ps(&rbuf_b[idx]);
                
                const F32vec4 D_sq = SQR(s_a - s0_a) + SQR(s_b - s0_b);		
                const F32vec4 f = v_fast_exp(v_Ar * D_sq - F32vec4(kernel[k]));
                
                a_num += f * s_a;
                b_num += f * s_b;
                denom += f;
            }
            
            // normalize
            denom = select_eq(denom, v_zero, v_one, denom);
            
            const int idx0 = x + y*width;
            _mm_storeu_ps(&buf_a[idx0], a_num / denom);
            _mm_storeu_ps(&buf_b[idx0], b_num / denom);
        }
#endif
        for (/*int x=wr*/; x < width - wr; x++) {
            // initialize central pixel
            const float s0_a = rbuf_a[x];
            const float s0_b = rbuf_b[x];
            
            // buf_L[idx0] = s0_L; // needed for column filtering
            
            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + x;
                
                const float s_a = rbuf_a[idx];
                const float s_b = rbuf_b[idx];
                
                const float D_sq = /* SQR(s_L - s0_L) + */ SQR(s_a - s0_a) + SQR(s_b - s0_b);

#ifdef FP_FAST_FMAF
                const float f = fast_exp(fmaf(Ar, D_sq, -kernel[k]));
                a_num = fmaf(f, s_a, a_num);
                b_num = fmaf(f, s_b, b_num);
#else
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                a_num += f * s_a;
                b_num += f * s_b;
#endif
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            const int idx0 = x + y*width;
            buf_a[idx0] = a_num / denom;
            buf_b[idx0] = b_num / denom;
        }
    }
    delete [] rbuf_a;
    delete [] rbuf_b;
    
    //--------------------------------------------------------------------------
    // Filter Columns
    //--------------------------------------------------------------------------
    
    // Buffers for processing column data
    float *cbuf_a = new float[height];
    float *cbuf_b = new float[height];
    
#pragma omp for
    for (int x=wr; x < width - wr; x++) {
        for (int y=0; y < height; y++) {
            const int idx = x + y*width;
            float a = buf_a[idx];
            float b = buf_b[idx];
            cbuf_a[y] = a;
            cbuf_b[y] = b;
        }
        int y=wr;
#ifdef __INTEL_COMPILER
        const F32vec4 v_zero(0.0f);
        const F32vec4 v_one(1.0f);
        const F32vec4 v_ffff((float) 0xffff);
        
        for (; y < height - wr - 4; y+=4) {
            // initialize central pixel
            const F32vec4 b0_a = _mm_loadu_ps(&cbuf_a[y]);
            const F32vec4 b0_b = _mm_loadu_ps(&cbuf_b[y]);
            const F32vec4 v_Ar(Ar);
            
            // compute adaptive kernel and convolve color channels
            F32vec4 a_num = v_zero;
            F32vec4 b_num = v_zero;
            F32vec4 denom = v_zero;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + y;
                
                const F32vec4 b_a = _mm_loadu_ps(&cbuf_a[idx]);
                const F32vec4 b_b = _mm_loadu_ps(&cbuf_b[idx]);
                
                const F32vec4 D_sq = SQR(b_a - b0_a) + SQR(b_b - b0_b);		
                const F32vec4 f = v_fast_exp(v_Ar * D_sq - F32vec4(kernel[k]));
                
                a_num += f * b_a;
                b_num += f * b_b;
                denom += f;
            }
            
            // normalize
            denom = select_eq(denom, v_zero, v_one, denom);
            
            union {
                __m128 v_result;
                float  a_result[4];
            };
            
            v_result = a_num / denom;
            for (int i = 0; i < 4; i++)
                buf_a[x + (y+i)*width] = a_result[i];
            
            v_result = b_num / denom;
            for (int i = 0; i < 4; i++)
                buf_b[x + (y+i)*width] = a_result[i];
        }
#endif
        for (/* int y=wr */; y < height - wr; y++) {
            // initialize central pixel
            const float b0_a = cbuf_a[y];
            const float b0_b = cbuf_b[y];
                        
            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + y;
                
                const float b_a = cbuf_a[idx];
                const float b_b = cbuf_b[idx];
                
                const float D_sq = SQR(b_a - b0_a) + SQR(b_b - b0_b);		

#ifdef FP_FAST_FMAF
                const float f = fast_exp(fmaf(Ar, D_sq, -kernel[k]));
                a_num = fmaf(f, b_a, a_num);
                b_num = fmaf(f, b_b, b_num);
#else
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                a_num += f * b_a;
                b_num += f * b_b;
#endif
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            const int idx = y * width + x;
            buf_a[idx] = a_num / denom;
            buf_b[idx] = b_num / denom;
        }
    }
    
    delete [] cbuf_a;
    delete [] cbuf_b;
} // omp parallel for
}

/*******************************************************************************
 * JNI wrapper for separable_bf_mono_tile() and separable_bf_chroma_tile()
 *******************************************************************************/
extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_BilateralFilterRGBOpImage_bilateralFilterRGB
(JNIEnv *env, jclass cls,
 jshortArray jsrcData, jshortArray jdestData,
 jint y_wr, jint c_wr, jint y_ws, jint c_ws, jfloat y_scale_r, jfloat c_scale_r,
 jfloatArray jy_kernel, jfloatArray jc_kernel, jfloatArray jrgb_to_yst, jfloatArray jyst_to_rgb,
 jint width, jint height,
 jint srcROffset, jint srcGOffset, jint srcBOffset,
 jint destROffset, jint destGOffset, jint destBOffset,
 jint srcLineStride, jint destLineStride)
{
    unsigned short *srcData = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);
    float *y_kernel = (float *) (jy_kernel != NULL ? env->GetPrimitiveArrayCritical(jy_kernel, 0) : NULL);
    float *c_kernel = (float *) (jc_kernel != NULL ? env->GetPrimitiveArrayCritical(jc_kernel, 0) : NULL);
    float *rgb_to_yst = (float *) env->GetPrimitiveArrayCritical(jrgb_to_yst, 0);
    float *yst_to_rgb = (float *) env->GetPrimitiveArrayCritical(jyst_to_rgb, 0);

    float *buf_y = new float[width*height];
    float *buf_s = new float[width*height];
    float *buf_t = new float[width*height];
    
    interleaved_RGB_to_planar_YST(srcData, srcLineStride, srcROffset, srcGOffset, srcBOffset,
                                  buf_y, buf_s, buf_t, width, height, rgb_to_yst);
    
    if (y_scale_r != 0 && y_wr != 0 && y_kernel != NULL) {
        float y_sigma_r = sqrt(1.0/(2*y_scale_r));
        separable_bf_mono_tile(buf_y, y_sigma_r, y_wr, y_kernel, width, height);
    }
    if (c_scale_r != 0 && c_wr != 0 && c_kernel != NULL) {
        float c_sigma_r = sqrt(1.0/(2*c_scale_r));
        separable_bf_chroma_tile(buf_s, buf_t, c_sigma_r, c_wr, c_kernel, width, height);
    }
    
    int wr = y_wr > c_wr ? y_wr : c_wr;
    
    planar_YST_to_interleaved_RGB(destData, destLineStride, destROffset, destGOffset, destBOffset, wr,
                                  buf_y, buf_s, buf_t, width, height, yst_to_rgb);
    
    delete [] buf_y;
    delete [] buf_s;
    delete [] buf_t;
    
    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
    
    if (y_kernel != NULL)
        env->ReleasePrimitiveArrayCritical(jy_kernel, y_kernel, 0);
    if (c_kernel != NULL)
        env->ReleasePrimitiveArrayCritical(jc_kernel, c_kernel, 0);
    
    env->ReleasePrimitiveArrayCritical(jrgb_to_yst, rgb_to_yst, 0);	
    env->ReleasePrimitiveArrayCritical(jyst_to_rgb, yst_to_rgb, 0);	
}
