/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * Light Crafts' Windows JNI Utilities.
 *
 * Paul J. Lucas [paul@lightcratfs.com]
 */

#ifdef  LC_USE_JAWT
// standard
#include <jawt_md.h>
#endif  /* LC_USE_JAWT */

// windows
#include <shlwapi.h>                    /* for Path*() functions */

// local
#include "LC_WinUtils.h"

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
            return NULL;
        ::PathStripPath( wPathBuf );
        ::PathRemoveExtension( wPathBuf );
    }
    return wPathBuf;
}

/**
 * Given a Java component, return an HWND.
 */
HWND LC_getHWNDFromAWTComponent( JNIEnv *env, jobject awtComponent ) {
    if ( !awtComponent )
        return ::GetForegroundWindow();
#ifdef  LC_USE_JAWT
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if ( !JAWT_GetAWT( env, &awt ) )
        return NULL;

    JAWT_DrawingSurface *const ds = awt.GetDrawingSurface( env, awtComponent );
    if ( !ds )
        return NULL;

    HWND hWnd = NULL;

    jint const lock = ds->Lock( ds );
    if ( lock & JAWT_LOCK_ERROR )
        goto error_1;

    JAWT_DrawingSurfaceInfo *const dsi = ds->GetDrawingSurfaceInfo( ds );
    if ( !dsi )
        goto error_2;

    { // local scope
        JAWT_Win32DrawingSurfaceInfo const *const dsiWin =
            static_cast<JAWT_Win32DrawingSurfaceInfo const*>(
                dsi->platformInfo
            );
        hWnd = dsiWin->hwnd;
    }

    ds->FreeDrawingSurfaceInfo( dsi );
error_2:
    ds->Unlock( ds );
error_1:
    awt.FreeDrawingSurface( ds );
    return hWnd;
#else
    return ::GetForegroundWindow();
#endif  /* LC_USE_JAWT */
}

/**
 * Convert a UTF-16 string into a jstring.
 */
jstring LC_wTojstring( JNIEnv *env, LPCWSTR w ) {
    int const len =
        ::WideCharToMultiByte( CP_UTF8, 0, w, -1, NULL, 0, NULL, NULL );
    if ( !len )
        return NULL;

    char *const s = new char[ len ];
    ::WideCharToMultiByte( CP_UTF8, 0, w, -1, s, len, NULL, NULL );
    jstring const jS = env->NewStringUTF( s );
    delete[] s;
    return jS;
}

/* vim:set et sw=4 ts=4: */
