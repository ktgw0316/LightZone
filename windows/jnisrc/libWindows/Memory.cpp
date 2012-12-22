/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstring>

// windows
#include <windows.h>
#include <winbase.h>

// local
#include "LC_JNIUtils.h"
#include "LC_WinError.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_windows_WindowsMemory.h"
#endif

using namespace std;

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsMemory_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_windows_WindowsMemory,_,method)

/**
 * Gets the amount of physical memory installed in the computer in MB.
 */
JNIEXPORT jint JNICALL WindowsMemory_METHOD(getPhysicalMemoryInMB)
    ( JNIEnv *env, jclass )
{
    MEMORYSTATUSEX ms;
    ::memset( &ms, 0, sizeof ms );
    ms.dwLength = sizeof ms;
    if ( !::GlobalMemoryStatusEx( &ms ) ) {
        LC_throwIllegalStateException( env,
            LC_formatError( "GlobalMemoryStatusEx()", ::GetLastError() )
        );
        return 0;
    }
    return static_cast<jint>( ms.ullTotalPhys / 1048576 );
}
/* vim:set et sw=4 ts=4: */
