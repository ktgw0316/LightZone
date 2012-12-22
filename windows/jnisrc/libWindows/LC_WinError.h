/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_error_H
#define LC_error_H

// windows
#include <windows.h>

/**
 * Given a Windows error code, format an error message string.  The string
 * returned is from a static buffer, so this is not thread-safe.
 */
char const* LC_formatError( char const *what, DWORD errorCode );

#endif  /* LC_error_H */
/* vim:set et sw=4 ts=4: */
