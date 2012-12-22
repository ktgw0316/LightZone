/* Copyright (C) 2005-2011 Fabio Riccardi */

// windows
#include <windows.h>
#include <wininet.h>

// local
#include "LC_CPPUtils.h"
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_windows_WindowsInternetConnection.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsInternetConnection_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_windows_WindowsInternetConnection,_,method)

/**
 * Checks whether we have an internet connection.
 */
JNIEXPORT jboolean JNICALL
WindowsInternetConnection_METHOD(hasConnection)
    ( JNIEnv *env, jclass )
{
    DWORD flags;
    return ::InternetGetConnectedState( &flags, 0 );
}
/* vim:set et sw=4 ts=4: */
