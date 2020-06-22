#pragma once
#include <math.h>
#include <omp.h>

#ifndef M_LN2
#define M_LN2		0.69314718055994530942
#endif

#ifndef M_PI
#define M_PI		3.14159265358979323846
#endif

// Very fast approximation of exp, faster than lookup table!
// See paper of Nicol N. Schraudolph, adapted here to single precision floats.
#if _OPENMP >= 201307
#   pragma omp declare simd
#endif
static inline
float fast_exp(float val)
{
    if (val < -16)
        return 0;

    constexpr float fast_exp_a = (1 << 23)/M_LN2;
#if defined(__i386__)
    constexpr float fast_exp_b_c = 127.0f * (1 << 23) - 405000;
#else
    constexpr float fast_exp_b_c = 127.0f * (1 << 23) - 347000;
#endif

    union {
        float f;
        int i;
    } result;

    result.i = int(fast_exp_a * val + fast_exp_b_c);

    return result.f;
}

#if _OPENMP >= 201307
#   pragma omp declare simd
#endif
template <typename T> static inline
constexpr unsigned short clampUShort(T x) {
    return x < 0 ? 0 : x > 0xffff ? 0xffff : (unsigned short) x;
}

#if _OPENMP >= 201307
#   pragma omp declare simd
#endif
template <typename T> static inline
constexpr T SQR( T x )
{
    return x * x;
}

#if _OPENMP >= 201307
#   pragma omp declare simd
#endif
static inline float fast_log2 (float val)
{
    union {
        float f;
        int i;
    } n;

    n.f = val;
    int * const exp_ptr = &n.i;
    int x = *exp_ptr;
    const int log_2 = ((x >> 23) & 255) - 128;
    x &= ~(255 << 23);
    x += 127 << 23;
    *exp_ptr = x;

    // increases accuracy
    val = ((-1.0f/3) * n.f + 2) * n.f - 2.0f/3;

    return (val + log_2);
}

#if _OPENMP >= 201307
#   pragma omp declare simd
#endif
static inline float fast_log (float x) {
    return fast_log2(x) * 0.69314718f;
}

#if _OPENMP >= 201307
#   pragma omp declare simd
#endif
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
    // x = x*(1.5f-xhalf*x*x); // Newton step, repeating increases accuracy
    // x = x*(1.5f-xhalf*x*x); // Newton step, repeating increases accuracy
    // x = x*(1.5f-xhalf*x*x); // Newton step, repeating increases accuracy
    return x;
}
