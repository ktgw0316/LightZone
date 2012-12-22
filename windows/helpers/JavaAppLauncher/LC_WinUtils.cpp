/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * LC_WinUtils.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cstring>

// windows
#include <shlwapi.h>                    /* for Path*() */
#include <windows.h>

// local
#include "LC_WinUtils.h"
#include "UI.h"

using namespace std;

/**
 * Gets the name of the running .exe without the .exe extension.
 */
LPCWSTR LC_getAppName() {
    static WCHAR wPathBuf[ MAX_PATH ];
    if ( !*wPathBuf ) {
        //
        // Get the full path to the running .exe file then chop off both the
        // path and the .exe extension leaving just the base name of the
        // application.
        //
        if ( !::GetModuleFileName( NULL, wPathBuf, sizeof wPathBuf ) )
            LC_die( TEXT("Could not determine path to executable.") );
        ::PathStripPath( wPathBuf );
        ::PathRemoveExtension( wPathBuf );
    }
    return wPathBuf;
}

/**
 * Get the full path to the directory the running .exe is in.
 */
LPCWSTR LC_getExeDirectory() {
    static WCHAR wPathBuf[ MAX_PATH ];
    if ( !*wPathBuf ) {
        //
        // Get the full path to the running .exe file then chop off the .exe
        // file leaving the directory it's in.
        //
        if ( !::GetModuleFileName( NULL, wPathBuf, sizeof wPathBuf ) )
            LC_die( TEXT("Could not determine path to executable.") );
        ::PathRemoveFileSpec( wPathBuf );
    }
    return wPathBuf;
}

/**
 * Make a directory if it doesn't exist.
 */
bool LC_makeDir( LPCWSTR wPath ) {
    return  ::CreateDirectory( wPath, 0 ) ||
            ::GetLastError() == ERROR_ALREADY_EXISTS;
}

/* vim:set et sw=4 ts=4: */
