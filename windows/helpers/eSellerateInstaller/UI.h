/**
 * eSellerateInstaller
 * UI.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef UI_H
#define UI_H

// windows
#include <windows.h>

/**
 * Show an alert dialog box with the given message in it and quit.
 */
void LC_die( LPCWSTR wMsg );

/**
 * Show an alert dialog box with the given message, and the standard Windows
 * error message for the given error code in it and quit.
 */
void LC_die( LPCWSTR wWhat, DWORD errorCode, int lineNo = 0 );

/**
 * Show an alert dialog box with the given message in it.
 */
void LC_warn( LPCWSTR wMsg );

/**
 * Show an alert dialog box with the given message, and the standard Windows
 * error message for the given error code in it.
 */
void LC_warn( LPCWSTR wWhat, DWORD errorCode, int lineNo = 0 );

#endif  /* UI_H */
/* vim:set et sw=4 ts=4: */
