/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * LCArrays
 * 
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cstring>                      /* for memcpy(3) */

// local
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_LCArrays.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define LCArrays_METHOD(method) \
        name4(Java_,com_lightcrafts_utils_LCArrays,_,method)

/**
 * Copy the raw bytes from an int[] to a byte[].
 */
JNIEXPORT void JNICALL LCArrays_METHOD(copy___3II_3BII)
    ( JNIEnv *env, jclass, jintArray jSrc, jint srcPos,
      jbyteArray jDest, jint destPos, jint length )
{
    jarray_to_c<jint> const cSrc( env, jSrc );
    jarray_to_c<jbyte> cDest( env, jDest );
    ::memcpy( cDest + destPos, cSrc + srcPos, length );
}

/**
 * Copy the raw bytes from a short[] to a byte[].
 */
JNIEXPORT void JNICALL LCArrays_METHOD(copy___3SI_3BII)
    ( JNIEnv *env, jclass, jshortArray jSrc, jint srcPos,
      jbyteArray jDest, jint destPos, jint length )
{
    jarray_to_c<jshort> const cSrc( env, jSrc );
    jarray_to_c<jbyte> cDest( env, jDest );
    ::memcpy( cDest + destPos, cSrc + srcPos, length );
}

/**
 * Copy the raw bytes from a byte[] to an int[].
 */
JNIEXPORT void JNICALL LCArrays_METHOD(copy___3BI_3III)
    ( JNIEnv *env, jclass, jbyteArray jSrc, jint srcPos,
      jintArray jDest, jint destPos, jint length )
{
    jarray_to_c<jbyte> const cSrc( env, jSrc );
    jarray_to_c<jint> cDest( env, jDest );
    ::memcpy( cDest + destPos, cSrc + srcPos, length );
}

/**
 * Copy the raw bytes from a byte[] to a short[].
 */
JNIEXPORT void JNICALL LCArrays_METHOD(copy___3BI_3SII)
    ( JNIEnv *env, jclass, jbyteArray jSrc, jint srcPos,
      jshortArray jDest, jint destPos, jint length )
{
    jarray_to_c<jbyte> const cSrc( env, jSrc );
    jarray_to_c<jshort> cDest( env, jDest );
    ::memcpy( cDest + destPos, cSrc + srcPos, length );
}
/* vim:set et sw=4 ts=4: */
