/* Copyright (C) 2005-2011 Fabio Riccardi */

// local
#include "LC_WinError.h"
#include "LC_WinUtils.h"

/**
 * Given a Windows error code, format an error message string.  The string
 * returned is from a static buffer, so this is not thread-safe.
 */
char const* LC_formatError( char const *what, DWORD errorCode ) {
    LPVOID lpMsgBuf;
    ::FormatMessage(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM, NULL,
        errorCode, MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
        (LPTSTR)&lpMsgBuf, 0, NULL
    );

    WCHAR wMsgBuf[ 256 ]; 
    ::wsprintf(
        wMsgBuf, TEXT("%s failed (error %d): %ls"),
        what, errorCode, lpMsgBuf
    ); 

    ::LocalFree( lpMsgBuf );

    static char aMsgBuf[ 256 ];
    LC_toUTF8( wMsgBuf, aMsgBuf, sizeof aMsgBuf );
    return aMsgBuf;
}

/* vim:set et sw=4 ts=4: */
