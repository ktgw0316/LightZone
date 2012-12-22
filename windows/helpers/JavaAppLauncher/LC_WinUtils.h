/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * LC_WinUtils.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef LC_WinUtils_H
#define LC_WinUtils_H

// windows
#include <windows.h>
#include <winnls.h>

/**
 * Gets the name of the running .exe without the .exe extension.
 */
LPCWSTR LC_getAppName();

/**
 * Gets the full path to the directory the running .exe is in.
 */
LPCWSTR LC_getExeDirectory();

/**
 * Make a directory if it doesn't exist.
 */
bool LC_makeDir( LPCWSTR wPath );

/**
 * Convert a UTF-16 string to UTF-8.
 */
inline bool LC_toUTF8( LPCWSTR w, char *s, int sSize ) {
    return ::WideCharToMultiByte( CP_UTF8, 0, w, -1, s, sSize, NULL, NULL );
}

/**
 * Convert a UTF-8 string to UTF-16.
 */
inline bool LC_toWCHAR( char const *s, LPWSTR w, int wSize ) {
    return ::MultiByteToWideChar( CP_UTF8, 0, s, -1, w, wSize );
}

#endif  /* LC_WinUtils_H */
/* vim:set et sw=4 ts=4: */
