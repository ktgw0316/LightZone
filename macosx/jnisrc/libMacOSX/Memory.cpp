/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <sys/sysctl.h>                 /* for sysctl(3) */

// local
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_MacOSXMemory.h"
#endif

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXMemory_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXMemory,_,method)

/**
 * Gets the amount of physical memory in this computer.
 */
JNIEXPORT jint JNICALL MacOSXMemory_METHOD(getPhysicalMemoryInMB)
    ( JNIEnv *env, jclass )
{
    int sysParam[] = { CTL_HW, HW_MEMSIZE };
    //
    // Be defensive and allow for the possibility that sysctl(3) might return
    // either a 32- or 64-bit result by using a union and checking the size of
    // the result and using the correct union member.
    //
    // See: http://www.cocoabuilder.com/archive/message/cocoa/2004/5/6/106388
    //
    union {
        uint32_t ui32;
        uint64_t ui64;
    } result;
    size_t resultSize = sizeof( result );

    ::sysctl( sysParam, 2, &result, &resultSize, NULL, 0 );
    return (jint)(
        resultSize == sizeof( result.ui32 ) ?
            (result.ui32 / 1048576) : (result.ui64 / 1048576)
    );
}

/* vim:set et sw=4 ts=4: */
