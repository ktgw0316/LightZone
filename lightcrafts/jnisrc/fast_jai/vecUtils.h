/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <math.h>
#include "../macstl/vec.h"

typedef unsigned short ushort;
typedef unsigned char byte;

using namespace macstl;

typedef vec<float, 4> vfloat;
typedef vec<int, 4> vint;
typedef vec<unsigned int, 4> vuint;
typedef vec<unsigned short, 8> vushort;
typedef vec<short, 8> vshort;
typedef vec<unsigned char, 16> vbyte;

#if defined( __SSE__ )
using namespace macstl::mmx;

typedef __m128i   vInt;
typedef __m128    vFloat;

#ifndef M_LN2
#define M_LN2		0.69314718055994530942
#endif

template <typename T>
static INLINE void vstore(T * target, vec<T, 16/sizeof(T)> const &x) {
    // x.store(target, 0);
    _mm_store_ps((float *) target, (vFloat) x.data());
}

static INLINE vfloat mmx_sel( vfloat a, vfloat b, vfloat mask )
{
    return mmx::vor( mmx::andnot( mask, a ), mmx::vand( b, mask ) );
}

static INLINE vfloat vfast_exp(vfloat val)
{
    const vfloat vfast_exp_a = vfloat::fill((float) (1 << 23)/M_LN2);
    const vfloat vfast_exp_b_c = vfloat::fill(127.0f * (1 << 23) - 405000);
    const vfloat vminval = vfloat::fill(-16.);
    
    const vfloat llmask = data_cast<vfloat>(mmx::cmplt(val, vminval));
    
    const vfloat expval = vfast_exp_a * val + vfast_exp_b_c;
    const vfloat result = data_cast<vfloat>(mmx::cvtt<vint, vfloat>(expval));
    return mmx_sel(result, vfloat::fill<0>(), llmask);
}

static INLINE vfloat vexp_estimate(vfloat arg)
{
    return vfast_exp( vfloat::fill<0>() - arg );
}

#define SPLAT(v, i) (mmx::shuffles <i, i, i, i> (v, v))

#define vmin mmx::min
#define vmax mmx::max

#ifdef __GNUC__
#define VECALIGN(x)   x __attribute__ ((aligned (16)))
#else
#define VECALIGN(x)   __declspec(align(16)) x
#endif

#else // PPC

using namespace macstl::altivec;

typedef int            vInt    __attribute__ ((vector_size (16)));
typedef unsigned int   vUInt   __attribute__ ((vector_size (16)));
typedef unsigned short vUInt16 __attribute__ ((vector_size (16)));
typedef unsigned char  vByte   __attribute__ ((vector_size (16)));
typedef float          vFloat  __attribute__ ((vector_size (16)));

template <typename T>
static void vstore(T * target, vec<T, 16/sizeof(T)> const &x) {
    // vec_st(x.data(), 0, target);
    altivec::st(x, 0, target);
}

template <typename T>
static INLINE vec<T, 16/sizeof(T)> loadu( T *target )
{
    typedef vec<T, 16/sizeof(T)> vtype;
    
    vtype MSQ(ld(0, target));           // most significant quadword
    vtype LSQ(ld(15, target));          // least significant quadword
    vbyte mask(lvsl(0, target));        // create the permute mask
    return perm(MSQ, LSQ, mask);        // align the data
}

template <typename T>
static INLINE vec<T, 16/sizeof(T)> load( T *target ) {
    return altivec::ld(0, target);
}

// #define load(target) (altivec::ld(0, target))
// #define store(target, v) (altivec::st(v, 0, target))

static INLINE vfloat vexp_estimate(vfloat arg)
{
    return altivec::expte( vfloat::fill<0>() - arg );
}

#define SPLAT(v, i) (altivec::splat<i>(v))

#define vmin altivec::min
#define vmax altivec::max

#define VECALIGN(x)   x __attribute__ ((aligned (16)))

#endif
