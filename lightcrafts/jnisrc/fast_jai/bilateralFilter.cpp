#include <math.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>

#ifdef __INTEL_COMPILER
#include <fvec.h>
#include <dvec.h>

// #define _mm_loadu_si128 _mm_lddqu_si128
// #define _mm_loadu_ps(x) (__m128) _mm_lddqu_si128((__m128i *) x)

inline F32vec4 convert_high(const Iu16vec8 &a) {
    return _mm_cvtepi32_ps(unpack_high(a, (__m128i)_mm_setzero_ps()));
}

inline F32vec4 convert_low(const Iu16vec8 &a) {
    return _mm_cvtepi32_ps(unpack_low(a, (__m128i)_mm_setzero_ps()));
}

inline I16vec8 convert(const F32vec4 &hi, const F32vec4 &lo) {
    // return _mm_packs_epi32(_mm_cvtps_epi32(hi), _mm_cvtps_epi32(lo));
    I16vec8 result = _mm_set_epi64(_mm_cvtps_pi16(hi), _mm_cvtps_pi16(lo));
    _mm_empty();
    return result;
}

// Ad Horizontal using SSE3
/* inline float addh(const F32vec4 &a) 
{ 
    return _mm_cvtss_f32(_mm_hadd_ps(_mm_hadd_ps(a, _mm_setzero_ps()), _mm_setzero_ps()));
} */

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

template <typename T> static inline T SQR( T x )
{
    return x * x;
}

static inline float fast_exp(float val)
{
    const float fast_exp_a = (1 << 23)/M_LN2;	
    const float fast_exp_b_c = 127.0f * (1 << 23) - 405000;
    
    if (val < -16)
        return 0;
    
    union {
        float f;
        int i;
    } result;
    
    result.i = (int)(fast_exp_a * val + fast_exp_b_c);
    return result.f;
}

static inline float inv_sqrt(float x) 
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


template <typename T>
unsigned short clampUShort(T x) {
    return x < 0 ? 0 : x > 0xffff ? 0xffff : (unsigned short) x;
}

/*******************************************************************************
 * rlm_separable_bf_mono_tile()
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
void rlm_separable_bf_mono_tile(
    unsigned short *srcData,                // pointer to source data buffer 
    unsigned short *dstData,                // pointer to origin of the dst ROI
    float sr,                               // the usual range sigma
    int wr,                                 // window radius in pixels
    float *kernel,                          // half-kernel containing the exponents of the spatial Gaussian
    int width, int height,                  // dimensions of the source image
    int srcOffset, int dstOffset,           // offsets of the 0th element in src and dst buffers (elements)
    int srcStep, int dstStep)               // length in elements (not bytes) of a single image row
{
    float *fsrc = new float[width * height];
    float *ibuf = new float[width * height];
    
    if ( ibuf == NULL || fsrc == NULL ) {
        fprintf(stderr, "can't allocate buffers\n");
        return;
    }
    
    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr) );
    
    unsigned short *src = &srcData[srcOffset];
    unsigned short *dst = &dstData[dstOffset];

#ifdef __INTEL_COMPILER
    const F32vec4 v_inv_norm(1.0f/0xffff);
#endif
    
    for (int y=0; y < height; y++) {
        int x=0;
#ifdef __INTEL_COMPILER
        for (; x < width-8; x+=8) {
            const int idx = x + y*srcStep;
            
            Iu16vec8 src8(_mm_loadu_si128((__m128i *) &src[idx]));
            F32vec4 src_high = convert_high(src8);
            F32vec4 src_low = convert_low(src8);
            
            _mm_storeu_ps(&fsrc[idx], v_inv_norm * src_low);
            _mm_storeu_ps(&fsrc[idx+4], v_inv_norm * src_high);
        }
#endif
        for (/*int x=0*/; x < width; x++) {
            const int idx = x + y*srcStep;
            fsrc[idx] = src[idx] / (float) 0xffff;
        }

        for (int x=0; x < width; x++) {
            const int idx = x + y*srcStep;
            // Copy border region to buffer
            if ((x < wr || x >= width-wr) || (y < wr || y >= height-wr))
                ibuf[idx] = fsrc[idx];
        }
    }
    
    //--------------------------------------------------------------------------
    // Filter Rows
    //--------------------------------------------------------------------------
    
    for (int y=wr; y < height - wr; y++) {
        int x=wr;
#ifdef __INTEL_COMPILER
        for (/*int x=wr*/; x < width - wr-4; x+=4) {
            const F32vec4 v_zero = _mm_setzero_ps();
            const F32vec4 v_one = F32vec4(1.0f);
            const F32vec4 v_ffff = F32vec4((float) 0xffff);
            
            const int idx = x + y*srcStep;
            
            // compute adaptive kernel and convolve color channels
            F32vec4 v_num = v_zero;
            F32vec4 v_denom = v_zero;
            const F32vec4 v_I_s0 = _mm_loadu_ps(&fsrc[idx]);
            const F32vec4 v_Ar(Ar);
            
            for (int k = 0; k <= 2*wr; k++) {
                const F32vec4 v_I_s = _mm_loadu_ps(&fsrc[k-wr + idx]);
                const F32vec4 v_D_sq = SQR(v_I_s - v_I_s0);
                const F32vec4 v_f = v_fast_exp(v_Ar * v_D_sq - F32vec4(kernel[k]));
                v_num += v_f * v_I_s;
                v_denom += v_f;
            }
            
            // normalize
            v_denom = select_eq(v_denom, v_zero, v_one, v_denom);
                        
            _mm_storeu_ps(&ibuf[idx], v_num / v_denom);
        }
#endif
        for (/*int x=wr*/; x < width - wr; x++) {
            
            const int idx = x + y*srcStep;
            const float I_s0 = fsrc[idx];
            
            // compute adaptive kernel and convolve color channels
            float num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const float I_s = fsrc[k-wr + idx];
                const float D_sq = SQR(I_s - I_s0);
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                num += f * I_s;
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            ibuf[idx] = num / denom;
        }
    }    
    
    delete [] fsrc;	
    
    //--------------------------------------------------------------------------
    // Filter Columns
    //--------------------------------------------------------------------------
    
    // Note: this looks weird because the outer loop is on y, but we're really iterating on columns (see the indexing in ibuf)
    
    for (int y=wr; y < height - wr; y++) {
        int x=wr;
#ifdef __INTEL_COMPILER
        for (/*int x=wr*/; x < width - wr-4; x+=4) {
            const F32vec4 v_zero = _mm_setzero_ps();
            const F32vec4 v_one = F32vec4(1.0f);
            const F32vec4 v_ffff = F32vec4((float) 0xffff);
            
            const int idx = x + y*srcStep;
            
            // compute adaptive kernel and convolve color channels
            F32vec4 v_num = v_zero;
            F32vec4 v_denom = v_zero;
            const F32vec4 v_I_s0 = _mm_loadu_ps(&ibuf[idx]);
            const F32vec4 v_Ar(Ar);
            
            for (int k = 0; k <= 2*wr; k++) {
                const F32vec4 v_I_s = _mm_loadu_ps(&ibuf[(k-wr)*srcStep + idx]);
                const F32vec4 v_D_sq = SQR(v_I_s - v_I_s0);
                const F32vec4 v_f = v_fast_exp(v_Ar * v_D_sq - F32vec4(kernel[k]));
                v_num += v_f * v_I_s;
                v_denom += v_f;
            }
            
            // normalize
            v_denom = select_eq(v_denom, v_zero, v_one, v_denom);
            
            union {
                __m128i v_result;
                int     a_result[4];
            };
            
            v_result = _mm_cvttps_epi32(simd_max(v_zero, simd_min(v_ffff * v_num / v_denom, v_ffff)));
            
            const int dst_idx = (x-wr) + (y-wr)*dstStep;
            
            for (int i = 0; i < 4; i++)
                dst[dst_idx + i] = a_result[i];
        }
#endif
        for (/*int x=wr*/; x < width - wr; x++) {
            
            const int idx = x + y*srcStep;
            
            const float I_s0 = ibuf[idx];
            
            // compute adaptive kernel and convolve color channels
            float num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const float I_s = ibuf[(k-wr)*srcStep + idx];
                const float D_sq = SQR(I_s - I_s0);
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                num += f * I_s;
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            const int dst_idx = (x-wr) + (y-wr)*dstStep;			
            dst[dst_idx] = clampUShort(0xffff * num / denom);
        }
    }
    delete [] ibuf;	
}

#ifdef __INTEL_COMPILER
#define CONST_INT32_PS(N, V3,V2,V1,V0) \
static const _MM_ALIGN16 int _##N[]= \
    {V0, V1, V2, V3};/*little endian!*/ \
const F32vec4 N = _mm_load_ps((float*)_##N);

// usage example, mask for elements 3 and 1:
// CONST_INT32_PS(mask31, ~0, 0, ~0, 0);

// Convert three array of interleaved data into three arrays of segregated data
inline void XYZtoF32vec4(F32vec4& x, F32vec4& y, F32vec4& z, F32vec4& a, F32vec4& b, F32vec4& c)
{
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
    a = R1 B0 G0 R0
    b = G2 R2 B1 G1
    c = B3 G3 R3 B2

    i = R1 R2 R3 R0 = a(0,3) | b(1)    | c(2)
    i = G2 G3 G0 G1 = a(2)   | b(0, 3) | c(1)
    i = B3 B0 B1 B2 = a(1)   | b(2)    | c(0, 3)
*/

// The inverse function is almost identical
inline void F32vec4toXYZ(F32vec4& a, F32vec4& b, F32vec4& c, F32vec4& x, F32vec4& y, F32vec4& z)
{
    CONST_INT32_PS(mask1,   0,  0, ~0,  0);
    CONST_INT32_PS(mask2,   0, ~0,  0,  0);
    CONST_INT32_PS(mask30, ~0,  0,  0, ~0);
    
    a = (x & mask30) | (y & mask1) | (z & mask2);
    b = (x & mask2) | (y & mask30) | (z & mask1);
    c = (x & mask1) | (y & mask2) | (z & mask30);
    a = _mm_shuffle_ps(a, a, _MM_SHUFFLE(1,2,3,0));
    b = _mm_shuffle_ps(b, b, _MM_SHUFFLE(2,3,0,1));
    c = _mm_shuffle_ps(c, c, _MM_SHUFFLE(3,0,1,2));
}
#endif

void planar_YST_to_interleaved_RGB(unsigned short * const dstData, int dstStep,
                                   const float * const buf_y, const float * const buf_s, const float * const buf_t,
                                   int width, int height,
                                   float *yst_to_rgb) {
#ifdef __INTEL_COMPILER
    F32vec4 v_ffff((float) 0xffff);
    F32vec4 v_yst_to_rgb[9];
    
    for (int i = 0; i < 9; i++)
        v_yst_to_rgb[i] = F32vec4(yst_to_rgb[i]);
#endif
    
    for (int y=0; y < height; y++) {
        int x=0;
#ifdef __INTEL_COMPILER
        for (; x < width-8; x+=8) {
            const int dst_idx = 3*x + y*dstStep;
            const int idx = x + y*width;
            
            F32vec4 y = _mm_loadu_ps(&buf_y[idx]);
            F32vec4 s = _mm_loadu_ps(&buf_s[idx]);
            F32vec4 t = _mm_loadu_ps(&buf_t[idx]);
            
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
            
            y = _mm_loadu_ps(&buf_y[idx]);
            s = _mm_loadu_ps(&buf_s[idx]);
            t = _mm_loadu_ps(&buf_t[idx]);
            
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
            
            _mm_storeu_si128((__m128i *) &dstData[dst_idx], convert(b1, a1));       // G2 R2 B1 G1 R1 B0 G0 R0
            _mm_storeu_si128((__m128i *) &dstData[dst_idx + 8], convert(a2, c1));   // R5 B4 G4 R4 B3 G3 R3 B2
            _mm_storeu_si128((__m128i *) &dstData[dst_idx + 16], convert(c2, b2));  // B7 G7 R7 B6 G6 R6 B5 G5
        }
#endif
        for (/*int x=0*/; x < width; x++) {
            const int dst_idx = 3*x + y*dstStep;
            const int idx = x + y*width;
            
            float y = buf_y[idx];
            float s = buf_s[idx];
            float t = buf_t[idx];
            
            for (int c = 0; c < 3; c++)
                dstData[dst_idx + c] = 0xffff * (yst_to_rgb[3*c] * y +
                                                 yst_to_rgb[3*c+1] * s +
                                                 yst_to_rgb[3*c+2] * t);
        }
    }
}

void interleaved_RGB_to_planar_YST(const unsigned short * const srcData, int srcStep,
                                   float *buf_y, float *buf_s, float *buf_t,
                                   int width, int height,
                                   float *rgb_to_yst) {
    const float norm = (float)0x10000;
    const float inv_norm = 1.0f/norm;
    
#ifdef __INTEL_COMPILER
    const F32vec4 v_inv_norm(inv_norm);
    
    F32vec4 v_rgb_to_yst[3][3];
    
    for (int y = 0; y < 3; y++)
        for (int x = 0; x < 3; x++)
            v_rgb_to_yst[y][x] = F32vec4(rgb_to_yst[3 * y + x]);
#endif
        
    for (int y=0; y < height; y++) {
        int x=0;
#ifdef __INTEL_COMPILER
        for (; x < width-8; x+=8) {
            const int src_idx = 3*x + y*srcStep;
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
            
            F32vec4 v_YST[3];
            
            for (int y = 0; y < 3; y++)
                v_YST[y] = v_rgb_to_yst[y][0] * src4_rgb[0] +
                           v_rgb_to_yst[y][1] * src4_rgb[1] +
                           v_rgb_to_yst[y][2] * src4_rgb[2];
            
            _mm_storeu_ps(&buf_y[idx], v_YST[0]);
            _mm_storeu_ps(&buf_s[idx], v_YST[1]);
            _mm_storeu_ps(&buf_t[idx], v_YST[2]);
            
            // second batch
            XYZtoF32vec4(src4_rgb[0], src4_rgb[1], src4_rgb[2], src4_4, src4_5, src4_6);
            
            for (int y = 0; y < 3; y++)
                v_YST[y] = v_rgb_to_yst[y][0] * src4_rgb[0] +
                           v_rgb_to_yst[y][1] * src4_rgb[1] +
                           v_rgb_to_yst[y][2] * src4_rgb[2];
            
            _mm_storeu_ps(&buf_y[idx+4], v_YST[0]);
            _mm_storeu_ps(&buf_s[idx+4], v_YST[1]);
            _mm_storeu_ps(&buf_t[idx+4], v_YST[2]);
        }
#endif
        for (/*int x=0*/; x < width; x++) {
            const int src_idx = 3*x + y*srcStep;
            const int idx = x + y*width;
            
            float r = inv_norm * (float) srcData[src_idx];
            float g = inv_norm * (float) srcData[src_idx+1];
            float b = inv_norm * (float) srcData[src_idx+2];
            
            float YST[3];
            
            for (int y = 0; y < 3; y++)
                YST[y] = rgb_to_yst[3*y] * r +
                         rgb_to_yst[3*y+1] * g +
                         rgb_to_yst[3*y+2] * b;
            
            buf_y[idx] = YST[0];
            buf_s[idx] = YST[1];
            buf_t[idx] = YST[2];
        }
    }    
}

/*******************************************************************************
 * rlm_separable_bf_chroma_tile()
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
void rlm_separable_bf_chroma_tile(
    unsigned short *srcData,                // pointer to the source buffer 
    unsigned short *dstData,                // pointer to destination buffer
    float sr,                               // the usual range sigma
    int wr,                                 // window radius in pixels
    float *kernel,                          // half-kernel containing the exponents of the spatial Gaussian
    int width, int height,                  // dimensions of the source image
    int src_L_offset, int src_a_offset, int src_b_offset, // source band offsets (in elements)
    int dst_L_offset, int dst_a_offset, int dst_b_offset, // destination band offsets (in elements)
    int srcStep, int dstStep)               // length in elements (not bytes) of a single image row
{
    const float norm = (float)0x10000;
    const float inv_norm = 1.0f/norm;
    const float cnorm = (float)0x4000;
    const float inv_cnorm = 1.0f/cnorm;
    
    //--------------------------------------------------------------------------
    // Initialize
    //
    // NOTE that color channels are not normalized to [0,1], but are "stretched"
    // to the range [0, 4]. This is accounted for here so that the parameter
    // 'sr' is comparable to the range sigma in other filter implementations
    // that do use normalized values
    //--------------------------------------------------------------------------
    
    float *ibuf = new float[2 * width * height];
    float *fsrc = new float[2 * width * height];
    
    if ( ibuf == NULL || fsrc == NULL ) {
        fprintf(stderr, "can't allocate buffers\n");
        return;
    }
    
    float const scale_sr = (float)0xffff / cnorm;
    
    // coefficient of the exponent for the range Gaussian
    const float Ar = - 1.0f / (2.0f * SQR(sr * scale_sr) );
    
    // set pointers to bands
    unsigned short *src_L = &srcData[src_L_offset];
    unsigned short *src_a = &srcData[src_a_offset];
    unsigned short *src_b = &srcData[src_b_offset];
    
    // float *fsrc_L = &fsrc[0];
    float *fsrc_a = &fsrc[0];
    float *fsrc_b = &fsrc[width * height];
    
    // float *buf_L = &ibuf[0];
    float *buf_a = &ibuf[0];
    float *buf_b = &ibuf[width * height];
    
    unsigned short *dst_L = &dstData[dst_L_offset];
    unsigned short *dst_a = &dstData[dst_a_offset];
    unsigned short *dst_b = &dstData[dst_b_offset];
    
    /* for (int y=0; y < height; y++) {
        for (int x=0; x < width; x++) {
            const int src_idx = 3*x + y*srcStep;
            const int idx = x + y*width;
            
            float L = fsrc_L[idx] = inv_norm * (float)src_L[src_idx];
            float a = fsrc_a[idx] = inv_cnorm * (float)src_a[src_idx];
            float b = fsrc_b[idx] = inv_cnorm * (float)src_b[src_idx];
            
            // Copy border region to buffer
            if ((x < wr || x >= width-wr) || (y < wr || y >= height-wr)) {
                buf_L[idx] = L;
                buf_a[idx] = a;
                buf_b[idx] = b;
            }
        }
    } */
    
#ifdef __INTEL_COMPILER
    const F32vec4 v_inv_norm(inv_norm);
    const F32vec4 v_inv_cnorm(inv_cnorm);
#endif
    
    for (int y=0; y < height; y++) {
        int x=0;
#ifdef __INTEL_COMPILER
        for (; x < width-8; x+=8) {
            const int src_idx = 3*x + y*srcStep;
            const int idx = x + y*width;
            
            // Use SSE swizzling magic to turn interleaved RGB data to planar
            
            // load 3 arrays of Iu16vec8
            Iu16vec8 src8_1(_mm_loadu_si128((__m128i *) &srcData[src_idx]));        // a2 L2 b1 a1 L1 b0 a0 L0
            Iu16vec8 src8_2(_mm_loadu_si128((__m128i *) &srcData[src_idx + 8]));    // L5 b4 a4 L4 b3 a3 L3 b2
            Iu16vec8 src8_3(_mm_loadu_si128((__m128i *) &srcData[src_idx + 16]));   // b7 a7 L7 b6 a6 L6 b5 a5
            
            // get the first three F32vec4
            F32vec4 src4_1 = convert_high(src8_1);  // a2 L2 b1 a1 -> b1
            F32vec4 src4_2 = convert_low(src8_1);   // L1 b0 a0 L0 -> a1
            F32vec4 src4_3 = convert_high(src8_2);  // L5 b4 a4 L4 -> a2
            
            F32vec4 src4_4 = convert_low(src8_2);   // b3 a3 L3 b2 -> c1
            F32vec4 src4_5 = convert_high(src8_3);  // b7 a7 L7 b6 -> c2
            F32vec4 src4_6 = convert_low(src8_3);   // a6 L6 b5 a5 -> b2
            
            F32vec4 src4_L, src4_a, src4_b;
            
            // swizzle them and store
            XYZtoF32vec4(src4_L, src4_a, src4_b, src4_2, src4_1, src4_4);
            // _mm_storeu_ps(&fsrc_L[idx], v_inv_norm * src4_L);
            _mm_storeu_ps(&fsrc_a[idx], v_inv_cnorm * src4_a);
            _mm_storeu_ps(&fsrc_b[idx], v_inv_cnorm * src4_b);
            
            // second batch
            XYZtoF32vec4(src4_L, src4_a, src4_b, src4_3, src4_6, src4_5);
            // _mm_storeu_ps(&fsrc_L[idx+4], v_inv_norm * src4_L);
            _mm_storeu_ps(&fsrc_a[idx+4], v_inv_cnorm * src4_a);
            _mm_storeu_ps(&fsrc_b[idx+4], v_inv_cnorm * src4_b);

            /* F32vec4 src4_L = _mm_cvtepi32_ps(I32vec4(src_L[src_idx+9], src_L[src_idx+6], src_L[src_idx+3], src_L[src_idx]));
            _mm_storeu_ps(&fsrc_L[idx], v_inv_norm * src4_L);
            
            F32vec4 src4_a = _mm_cvtepi32_ps(I32vec4(src_a[src_idx+9], src_a[src_idx+6], src_a[src_idx+3], src_a[src_idx]));
            _mm_storeu_ps(&fsrc_a[idx], v_inv_cnorm * src4_a);
            
            F32vec4 src4_b = _mm_cvtepi32_ps(I32vec4(src_b[src_idx+9], src_b[src_idx+6], src_b[src_idx+3], src_b[src_idx]));
            _mm_storeu_ps(&fsrc_b[idx], v_inv_cnorm * src4_b); */
        }
#endif
        for (/*int x=0*/; x < width; x++) {
            const int src_idx = 3*x + y*srcStep;
            const int idx = x + y*width;
            
            // fsrc_L[idx] = inv_norm * (float)src_L[src_idx];
            fsrc_a[idx] = inv_cnorm * (float)src_a[src_idx];
            fsrc_b[idx] = inv_cnorm * (float)src_b[src_idx];
        }

        for (int x=0; x < width; x++) {
            const int idx = x + y*width;
            
            // Copy border region to buffer
            if ((x < wr || x >= width-wr) || (y < wr || y >= height-wr)) {
                // buf_L[idx] = fsrc_L[idx];
                buf_a[idx] = fsrc_a[idx];
                buf_b[idx] = fsrc_b[idx];
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // Filter Rows
    //--------------------------------------------------------------------------
    
    for (int y=wr; y < height - wr; y++) {
        int x=wr;
#ifdef __INTEL_COMPILER
        const F32vec4 v_zero = _mm_setzero_ps();
        const F32vec4 v_one = F32vec4(1.0f);
        
        for (/*int x=wr*/; x < width - wr-4; x+=4) {
            // initialize central pixel
            const int idx0 = x + y*width;
            
            // const F32vec4 s0_L = _mm_loadu_ps(&fsrc_L[idx0]);
            const F32vec4 s0_a = _mm_loadu_ps(&fsrc_a[idx0]);
            const F32vec4 s0_b = _mm_loadu_ps(&fsrc_b[idx0]);
            const F32vec4 v_Ar(Ar);
            
            // _mm_storeu_ps(&buf_L[idx0], s0_L); // needed for column filtering
            
            // compute adaptive kernel and convolve color channels
            F32vec4 a_num = v_zero;
            F32vec4 b_num = v_zero;
            F32vec4 denom = v_zero;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + idx0;
                
                // const F32vec4 s_L = _mm_loadu_ps(&fsrc_L[idx]);
                const F32vec4 s_a = _mm_loadu_ps(&fsrc_a[idx]);
                const F32vec4 s_b = _mm_loadu_ps(&fsrc_b[idx]);
                
                const F32vec4 D_sq = /*SQR(s_L - s0_L) +*/ SQR(s_a - s0_a) + SQR(s_b - s0_b);		
                const F32vec4 f = v_fast_exp(v_Ar * D_sq - F32vec4(kernel[k]));
                
                a_num += f * s_a;
                b_num += f * s_b;
                denom += f;
            }
            
            // normalize
            denom = select_eq(denom, v_zero, v_one, denom);
            
            _mm_storeu_ps(&buf_a[idx0], a_num / denom);
            _mm_storeu_ps(&buf_b[idx0], b_num / denom);
        }
#endif
        for (/*int x=wr*/; x < width - wr; x++) {
            // initialize central pixel
            const int idx0 = x + y*width;
            
            // const float s0_L = fsrc_L[idx0];
            const float s0_a = fsrc_a[idx0];
            const float s0_b = fsrc_b[idx0];
            
            // buf_L[idx0] = s0_L; // needed for column filtering
            
            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + idx0;
                
                // const float s_L = fsrc_L[idx];
                const float s_a = fsrc_a[idx];
                const float s_b = fsrc_b[idx];
                
                const float D_sq = /* SQR(s_L - s0_L) + */ SQR(s_a - s0_a) + SQR(s_b - s0_b);
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                
                a_num += f * s_a;
                b_num += f * s_b;
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            buf_a[idx0] = a_num / denom;
            buf_b[idx0] = b_num / denom;
        }
    }
    delete [] fsrc;
    
    //--------------------------------------------------------------------------
    // Filter Columns
    //--------------------------------------------------------------------------
#if 0
    // Note: this looks weird because the outer loop is on y, but we're really iterating on columns (see the indexing in ibuf)
    
    for (int y=wr; y < height - wr; y++) {
        int x=wr;
#ifdef __INTEL_COMPILER
        const F32vec4 v_zero(0.0);
        const F32vec4 v_one(1.0f);
        const F32vec4 v_ffff((float) 0xffff);
        
        for (/*int x=wr*/; x < width - wr-4; x+=4) {
            // initialize central pixel
            const int idx0 = x + y*width;
            const int src_idx0 = 3*x + y*srcStep;
            const int dst_idx0 = 3*(x-wr) + (y-wr)*dstStep;
            
            // const F32vec4 b0_L = _mm_loadu_ps(&buf_L[idx0]);
            const F32vec4 b0_a = _mm_loadu_ps(&buf_a[idx0]);
            const F32vec4 b0_b = _mm_loadu_ps(&buf_b[idx0]);
            const F32vec4 v_Ar(Ar);
            
            for (int i = 0; i < 4; i++)
                dst_L[dst_idx0 + 3*i] = src_L[src_idx0 + 3*i]; // copy luminance channel
            
            // compute adaptive kernel and convolve color channels
            F32vec4 a_num = v_zero;
            F32vec4 b_num = v_zero;
            F32vec4 denom = v_zero;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr)*width + idx0;
                
                // const F32vec4 b_L = _mm_loadu_ps(&buf_L[idx]);
                const F32vec4 b_a = _mm_loadu_ps(&buf_a[idx]);
                const F32vec4 b_b = _mm_loadu_ps(&buf_b[idx]);
                
                // Prefetch data for the next loop while we do the math
                const int idx1 = (k+1-wr)*width + idx0;
                // _mm_prefetch((char *) &buf_L[idx1], _MM_HINT_T0);
                _mm_prefetch((char *) &buf_a[idx1], _MM_HINT_T0);
                _mm_prefetch((char *) &buf_b[idx1], _MM_HINT_T0);
                
                const F32vec4 D_sq = /* SQR(b_L - b0_L) + */ SQR(b_a - b0_a) + SQR(b_b - b0_b);		
                const F32vec4 f = v_fast_exp(v_Ar * D_sq - F32vec4(kernel[k]));
                
                a_num += f * b_a;
                b_num += f * b_b;
                denom += f;
            }
            
            // normalize
            denom = select_eq(denom, v_zero, v_one, denom);
            
            union {
                __m128i v_result;
                int     a_result[4];
            };
            
            const F32vec4 v_cnorm(cnorm);
            
            v_result = _mm_cvttps_epi32(simd_max(v_zero, simd_min(v_cnorm * a_num / denom, v_ffff)));
            for (int i = 0; i < 4; i++)
                dst_a[dst_idx0 + 3*i] = a_result[i];
            
            v_result = _mm_cvttps_epi32(simd_max(v_zero, simd_min(v_cnorm * b_num / denom, v_ffff)));
            for (int i = 0; i < 4; i++)
                dst_b[dst_idx0 + 3*i] = a_result[i];
        }
#endif
        for (/*int x=wr*/; x < width - wr; x++) {
            // initialize central pixel
            const int idx0 = x + y*width;
            const int src_idx0 = 3*x + y*srcStep;
            const int dst_idx0 = 3*(x-wr) + (y-wr)*dstStep;
            
            // const float b0_L = buf_L[idx0];
            const float b0_a = buf_a[idx0];
            const float b0_b = buf_b[idx0];
            
            dst_L[dst_idx0] = src_L[src_idx0]; // copy luminance channel
            
            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr)*width + idx0;
                
                // const float b_L = buf_L[idx];
                const float b_a = buf_a[idx];
                const float b_b = buf_b[idx];
                
                const float D_sq = /* SQR(b_L - b0_L) + */ SQR(b_a - b0_a) + SQR(b_b - b0_b);		
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                
                a_num += f * b_a;
                b_num += f * b_b;
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            dst_a[dst_idx0] = clampUShort(cnorm * a_num / denom);
            dst_b[dst_idx0] = clampUShort(cnorm * b_num / denom);
        }
    }
    
#else
    
    float *cbuf_a = new float[height];
    float *cbuf_b = new float[height];
    
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
            const int src_idx0 = 3*x + y*srcStep;
            const int dst_idx0 = 3*(x-wr) + (y-wr)*dstStep;
            
            // const float b0_L = buf_L[idx0];
            const F32vec4 b0_a = _mm_loadu_ps(&cbuf_a[y]);
            const F32vec4 b0_b = _mm_loadu_ps(&cbuf_b[y]);
            const F32vec4 v_Ar(Ar);
            
            for (int i = 0; i < 4; i++)
                dst_L[dst_idx0 + dstStep*i] = src_L[src_idx0 + srcStep*i]; // copy luminance channel
            
            // compute adaptive kernel and convolve color channels
            F32vec4 a_num = v_zero;
            F32vec4 b_num = v_zero;
            F32vec4 denom = v_zero;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + y;
                
                // const float b_L = buf_L[idx];
                const F32vec4 b_a = _mm_loadu_ps(&cbuf_a[idx]);
                const F32vec4 b_b = _mm_loadu_ps(&cbuf_b[idx]);
                
                const F32vec4 D_sq = /* SQR(b_L - b0_L) + */ SQR(b_a - b0_a) + SQR(b_b - b0_b);		
                const F32vec4 f = v_fast_exp(v_Ar * D_sq - F32vec4(kernel[k]));
                
                a_num += f * b_a;
                b_num += f * b_b;
                denom += f;
            }
            
            // normalize
            denom = select_eq(denom, v_zero, v_one, denom);
            
            union {
                __m128i v_result;
                int     a_result[4];
            };
            
            const F32vec4 v_cnorm(cnorm);
            
            v_result = _mm_cvttps_epi32(simd_max(v_zero, simd_min(v_cnorm * a_num / denom, v_ffff)));
            for (int i = 0; i < 4; i++)
                dst_a[dst_idx0 + dstStep*i] = a_result[i];
            
            v_result = _mm_cvttps_epi32(simd_max(v_zero, simd_min(v_cnorm * b_num / denom, v_ffff)));
            for (int i = 0; i < 4; i++)
                dst_b[dst_idx0 + dstStep*i] = a_result[i];
        }
#endif
        for (/* int y=wr */; y < height - wr; y++) {
            // initialize central pixel
            const int src_idx0 = 3*x + y*srcStep;
            const int dst_idx0 = 3*(x-wr) + (y-wr)*dstStep;
            
            // const float b0_L = buf_L[idx0];
            const float b0_a = cbuf_a[y];
            const float b0_b = cbuf_b[y];
            
            dst_L[dst_idx0] = src_L[src_idx0]; // copy luminance channel
            
            // compute adaptive kernel and convolve color channels
            float a_num = 0;
            float b_num = 0;
            float denom = 0;
            
            for (int k = 0; k <= 2*wr; k++) {
                const int idx = (k-wr) + y;
                
                // const float b_L = buf_L[idx];
                const float b_a = cbuf_a[idx];
                const float b_b = cbuf_b[idx];
                
                const float D_sq = /* SQR(b_L - b0_L) + */ SQR(b_a - b0_a) + SQR(b_b - b0_b);		
                const float f = fast_exp(Ar * D_sq - kernel[k]);
                
                a_num += f * b_a;
                b_num += f * b_b;
                denom += f;
            }
            
            // normalize
            if (denom == 0)
                denom = 1.0;
            
            dst_a[dst_idx0] = clampUShort(cnorm * a_num / denom);
            dst_b[dst_idx0] = clampUShort(cnorm * b_num / denom);
        }
    }    
    
    delete [] cbuf_a;
    delete [] cbuf_b;
#endif
    
    delete [] ibuf;	
}

/*******************************************************************************
 * JNI wrapper for rlm_separable_bf_mono_tile()
 *******************************************************************************/
extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_BilateralFilterOpImage_bilateralFilterMonoRLM
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
    float *kernel = (float *) env->GetPrimitiveArrayCritical(jkernel, 0); // + wr; // Keep 0 in the middle...
    
    float sigma_r = sqrt(1.0/(2*scale_r));
    rlm_separable_bf_mono_tile(srcData, destData, sigma_r, wr, kernel, width, height, 
                               srcLOffset, destLOffset, srcLineStride, destLineStride);
    
    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
    env->ReleasePrimitiveArrayCritical(jkernel, kernel, 0);	
}


/*******************************************************************************
 * JNI wrapper for rlm_separable_bf_chroma_tile()
 *******************************************************************************/
extern "C"
JNIEXPORT void JNICALL Java_com_lightcrafts_jai_opimage_BilateralFilterOpImage_bilateralFilterChromaRLM
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
    float *kernel = (float *) env->GetPrimitiveArrayCritical(jkernel, 0); // + wr; // Keep 0 in the middle...
    
    float sigma_r = sqrt(1.0/(2*scale_r));
    rlm_separable_bf_chroma_tile(srcData, destData, sigma_r, wr, kernel, width, height,
                                 srcLOffset, srcAOffset, srcBOffset, 
                                 destLOffset, destAOffset, destBOffset, 
                                 srcLineStride, destLineStride);
    
    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
    env->ReleasePrimitiveArrayCritical(jkernel, kernel, 0);	
}
