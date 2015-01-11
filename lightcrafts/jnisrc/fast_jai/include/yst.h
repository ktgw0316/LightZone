
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

template <typename T> static inline T SQR( T x )
{
    return x * x;
}

template <typename T>
unsigned short clampUShort(T x) {
    return x < 0 ? 0 : x > 0xffff ? 0xffff : (unsigned short) x;
}

inline void planar_YST_to_interleaved_RGB(unsigned short * const dstData, int dstStep,
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
            
            float y = buf_y[idx];
            float s = buf_s[idx] - 0.5f;
            float t = buf_t[idx] - 0.5f;
                        
            for (int c = 0; c < 3; c++)
                dstData[dst_idx+c] = clampUShort(0xffff * (yst_to_rgb[3*c] * y +
                                                           yst_to_rgb[3*c+1] * s +
                                                           yst_to_rgb[3*c+2] * t));
        }
    }
}

inline void interleaved_RGB_to_planar_YST(const unsigned short * const srcData, int srcStep,
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
            
            y = v_rgb_to_yst[0][0] * v_r + v_rgb_to_yst[0][1] * v_g + v_rgb_to_yst[0][2] * v_b;
            s = v_rgb_to_yst[1][0] * v_r + v_rgb_to_yst[1][1] * v_g + v_rgb_to_yst[1][2] * v_b + v_05;
            t = v_rgb_to_yst[2][0] * v_r + v_rgb_to_yst[2][1] * v_g + v_rgb_to_yst[2][2] * v_b + v_05;
            
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
            
            for (int c = 0; c < 3; c++)
                YST[c] = rgb_to_yst[3*c] * r +
                         rgb_to_yst[3*c+1] * g +
                         rgb_to_yst[3*c+2] * b + (c > 0 ? 0.5f : 0);
            
            buf_y[idx] = YST[0];
            buf_s[idx] = YST[1];
            buf_t[idx] = YST[2];
        }
    }    
}

