/* Copyright (C) 2005-2011 Fabio Riccardi */

// windows
#include <windows.h>
#include <wingdi.h>

// local
#include "LC_JNIUtils.h"
#include "LC_WinUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_windows_WindowsColorProfileManager.h"
#endif

using namespace std;

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsColorProfileManager_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_windows_WindowsColorProfileManager,_,method)

/**
 * Gets the path to the system display profile.
 */
JNIEXPORT jstring JNICALL
WindowsColorProfileManager_METHOD(getSystemDisplayProfilePath)
    ( JNIEnv *env, jclass )
{
    HDC const dc = ::GetDC( NULL /* entire screen */ );
    if ( dc ) {
        WCHAR wPath[ MAX_PATH ];
        DWORD len = sizeof wPath;
        BOOL const gotProfile = ::GetICMProfile( dc, &len, wPath );
        ::ReleaseDC( NULL, dc );
        if ( gotProfile )
            return LC_wTojstring( env, wPath );
    }
    return NULL;
}
/* vim:set et sw=4 ts=4: */
