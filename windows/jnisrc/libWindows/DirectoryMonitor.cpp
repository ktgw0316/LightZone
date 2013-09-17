/* Copyright (C) 2005-2011 Fabio Riccardi */

// windows
#include <windows.h>

#ifdef DEBUG
#include <iostream>
#endif

// local
#include "LC_CPPUtils.h"
#include "LC_JNIUtils.h"
#include "LC_WinError.h"
#include "LC_WinUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_directory_WindowsDirectoryMonitor.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsDirectoryMonitor_METHOD(method) \
        name4(Java_,com_lightcrafts_utils_directory_WindowsDirectoryMonitor,_,method)

/**
 * Dispose of the change notification object.
 */
JNIEXPORT void JNICALL WindowsDirectoryMonitor_METHOD(disposeHandle)
    ( JNIEnv *env, jclass, jlong jHandle )
{
    HANDLE handle = reinterpret_cast<HANDLE>( jHandle );
#ifdef DEBUG
    cout << "disposeHandle(" << jHandle << ')' << endl;
#endif
    if ( !::FindCloseChangeNotification( handle ) )
        LC_throwIOException( env,
            LC_formatError( "FindCloseChangeNotification()", ::GetLastError() )
        );
}

/**
 * Checks whether the directory referred to by the given change notification
 * handle has changed.
 */
JNIEXPORT jboolean JNICALL WindowsDirectoryMonitor_METHOD(hasChanged)
    ( JNIEnv *env, jclass, jlong jHandle )
{
    HANDLE handle = reinterpret_cast<HANDLE>( jHandle );
    switch ( ::WaitForSingleObject( handle, 0 /* just check, don't wait */ ) ) {
        case WAIT_OBJECT_0:
            //
            // The directory changed: call FindNextChangeNotification() to tell
            // Windows that we've acknowledged this (otherwise we keep getting
            // told about the same change).
            //
            if ( !::FindNextChangeNotification( handle ) )
                LC_throwIOException( env,
                    LC_formatError(
                        "FindNextChangeNotification()", ::GetLastError()
                    )
                );
            return JNI_TRUE;
        case WAIT_TIMEOUT:
            break;
        case WAIT_FAILED:
            // no break;
        default:
            LC_throwIOException( env,
                LC_formatError( "WaitForSingleObject()", ::GetLastError() )
            );
    }
    return JNI_FALSE;
}

/**
 * Creates a new HANDLE that refers to a change notification object that is
 * used to monitor the given directory.
 */
JNIEXPORT jlong JNICALL WindowsDirectoryMonitor_METHOD(newHandle)
    ( JNIEnv *env, jclass, jstring jDirectory )
{
    jstring_to_w const wDirectory( env, jDirectory );
    HANDLE handle = ::FindFirstChangeNotification(
        wDirectory, FALSE,
        FILE_NOTIFY_CHANGE_FILE_NAME | FILE_NOTIFY_CHANGE_DIR_NAME
    );
    if ( handle == INVALID_HANDLE_VALUE ) {
        LC_throwIOException( env,
            LC_formatError( "FindFirstChangeNotification()", ::GetLastError() )
        );
        return 0;
    }
    return reinterpret_cast<jlong>( handle );
}
/* vim:set et sw=4 ts=4: */
