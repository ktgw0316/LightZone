/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * NativeChunk
 * 
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <jni.h>

#ifdef DEBUG
#include <iostream>
#endif

#include "LC_JNIUtils.h"                /* defines __WINDOWS__ */

#if defined( __APPLE__ )
#   include <mach/mach.h>
#elif defined( __WINDOWS__ )
#   include <windows.h>
#   include <winbase.h>
#endif

// local
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_cache_NativeChunk.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define NativeChunk_METHOD(method) \
        name4(Java_,com_lightcrafts_utils_cache_NativeChunk,_,method)

#define NativeChunk_CONSTANT(constant) \
        name3(com_lightcrafts_utils_cache_NativeChunk_,constant)

/**
 * Allocate a chunk of native memory.
 */
JNIEXPORT jlong JNICALL NativeChunk_METHOD(alloc)
    ( JNIEnv *env, jclass, jint jSize )
{
#if defined( __APPLE__ )
    vm_address_t addr;
    kern_return_t const result =
        ::vm_allocate( mach_task_self(), &addr, jSize, VM_FLAGS_ANYWHERE );
    return result == KERN_SUCCESS ? static_cast<jlong>( addr ) : 0;

#elif defined( __WINDOWS__ )
    return reinterpret_cast<jlong>(
        ::VirtualAlloc( NULL, jSize, MEM_RESERVE | MEM_COMMIT, PAGE_READWRITE )
    );

#else
    return reinterpret_cast<jlong>( new char[ jSize ] );
#endif
}

/**
 * Free a chunk of native memory.
 */
JNIEXPORT void JNICALL NativeChunk_METHOD(free)
    ( JNIEnv *env, jclass, jlong jAddr, jint jSize )
{
    if ( char *const addr = reinterpret_cast<char*>( jAddr ) ) {
#if defined( __APPLE__ )
        ::vm_deallocate(
            mach_task_self(), reinterpret_cast<vm_address_t>( addr ), jSize
        );
#elif defined( __WINDOWS__ )
        ::VirtualFree( addr, jSize, MEM_RELEASE );
#else
        delete[] addr;
#endif
    }
}

/* vim:set et sw=4 ts=4: */
