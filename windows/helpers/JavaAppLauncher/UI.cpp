/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * UI.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cstdlib>                      /* for exit(3) */

// local
#include "UI.h"

using namespace std;

/**
 * Show an alert dialog box with the given message in it and quit.
 */
void LC_die( UINT stringID ) {
    LC_warn( stringID );
    ::exit( 1 );
}

/**
 * Show an alert dialog box with the given message in it and quit.
 */
void LC_die( LPCWSTR wMsg ) {
    LC_warn( wMsg );
    ::exit( 1 );
}

/**
 * Show an alert dialog box with the given message, error code, and error
 * string in it and quit.
 */
void LC_die( LPCWSTR wWhat, DWORD errorCode, int lineNo ) {
    LC_warn( wWhat, errorCode, lineNo );
    ::exit( errorCode );
}

/**
 * Show an alert dialog box with the given localized message in it.
 */
void LC_warn( UINT stringID ) {
    WCHAR wBuf[ 512 ];
    int const nChars = ::LoadString(
        ::GetModuleHandle( NULL ), stringID, wBuf,
        sizeof( wBuf ) / sizeof( wBuf[0] )
    );
    if ( !nChars )
        LC_die( TEXT("Load string resource"), ::GetLastError() );
    LC_warn( wBuf );
}

/**
 * Show an alert dialog box with the given message in it.
 */
void LC_warn( LPCWSTR wMsg ) {
    ::MessageBox(
        NULL, wMsg, NULL,
        MB_OK | MB_ICONERROR | MB_SETFOREGROUND | MB_TOPMOST
    );
}

/**
 * Show an alert dialog box with the given message, error code, and error
 * string in it.
 */
void LC_warn( LPCWSTR wWhat, DWORD errorCode, int lineNo ) {
    LPVOID lpMsgBuf;
    ::FormatMessageW(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM, NULL,
        errorCode, MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
        (LPWSTR)&lpMsgBuf, 0,
        NULL
    );

    WCHAR wMsgBuf[ 512 ]; 
    if ( lineNo )
        ::wsprintf(
            wMsgBuf, TEXT("%ls (line %d) failed (error %d): %ls"),
            wWhat, lineNo, errorCode, lpMsgBuf
        ); 
    else
        ::wsprintf(
            wMsgBuf, TEXT("%ls failed (error %d): %ls"),
            wWhat, errorCode, lpMsgBuf
        ); 

    ::LocalFree( lpMsgBuf );
    LC_warn( wMsgBuf );
}

/* vim:set et sw=4 ts=4: */
