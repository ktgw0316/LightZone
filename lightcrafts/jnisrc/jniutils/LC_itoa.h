/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_itoa_H
#define LC_itoa_H

#ifdef  __cplusplus
extern "C" {
#endif

/**
 * Convert a long integer to a string.  The string is a static buffer, so this
 * is not thread-safe.
 */
extern char const* LC_ltoa( long );

inline char const* LC_itoa( int n )     { return LC_ltoa( n ); }

#ifdef  __cplusplus
}
#endif

#endif	/* LC_itoa_H */
/* vim:set et sw=4 ts=4: */
