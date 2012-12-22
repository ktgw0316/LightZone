/**
 * eSellerateInstaller
 * main.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// windows
#include <shlwapi.h>                    /* for Path*() */
#include <windows.h>

// eSellerate
#include "eWebLibrary.h"

// local
#include "UI.h"

//
// The file that the eSellerate engine is contained in.
//
#define ES_ENGINE_FILE  "eWebClient.dll"

/**
 * In some cases, eSellerate library functions return codes >= 0 for success
 * and < 0 for failure.  This is dumb.  If the function succeeds, we don't
 * really care what the code is, so just change it to E_SUCCESS.
 */
inline HRESULT esError( HRESULT errorCode ) {
    return SUCCEEDED( errorCode ) ? E_SUCCESS : errorCode;
}

/**
 * Get the full path to the directory the running .exe is in.
 */
static LPCWSTR LC_getExeDirectory() {
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
 * Run our application.
 */
int APIENTRY WinMain( HINSTANCE, HINSTANCE, LPSTR, int ) {
    if ( !::SetCurrentDirectory( LC_getExeDirectory() ) )
        LC_die( TEXT("Set current directory"), ::GetLastError() );
    return esError( eWeb_InstallEngineFromPath( ES_ENGINE_FILE ) );
}

/* vim:set et sw=4 ts=4: */
