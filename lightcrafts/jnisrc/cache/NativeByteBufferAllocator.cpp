/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * NativeByteBufferAllocator
 * 
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <jni.h>

#ifdef DEBUG
#include <iostream>
#endif

#include "LC_JNIUtils.h"                /* defines __WINDOWS__ */

#ifdef __WINDOWS__
#include <windows.h>
#include <winbase.h>
#endif

// local
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_cache_NativeByteBufferAllocator.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define NativeByteBufferAllocator_METHOD(method) \
        name4(Java_,com_lightcrafts_utils_cache_NativeByteBufferAllocator,_,method)

#define NativeByteBufferAllocator_CONSTANT(constant) \
        name3(com_lightcrafts_utils_cache_NativeByteBufferAllocator_,constant)

/**
 * Gets the native address of the given ByteBuffer.
 */
JNIEXPORT jlong JNICALL NativeByteBufferAllocator_METHOD(getNativeAddressOf)
    ( JNIEnv *env, jclass, jobject jByteBuffer )
{
    void *const addr = env->GetDirectBufferAddress( jByteBuffer );
    return reinterpret_cast<jlong>( addr );
}

/**
 * Creates a ByteBuffer on the Java side using native memory that's inside the
 * natively allocated region.
 */
JNIEXPORT jobject JNICALL NativeByteBufferAllocator_METHOD(getNativeByteBuffer)
    ( JNIEnv *env, jclass, jlong jAddr, jint jSize )
{
    void *const addr = reinterpret_cast<char*>( jAddr );
    jobject const buf = env->NewDirectByteBuffer( addr, jSize );
    if ( !buf )
        LC_throwOutOfMemoryError( env, "NewDirectByteBuffer()" );
    return buf;
}

/* vim:set et sw=4 ts=4: */
