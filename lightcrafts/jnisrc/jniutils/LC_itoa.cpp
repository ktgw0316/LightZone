/* Copyright (C) 2005-2011 Fabio Riccardi */

#include "LC_itoa.h"

/**
 * Convert a long integer to a string.  The string is a static buffer, so this
 * is not thread-safe.
 *
 * See also: Brian W. Kernighan, Dennis M. Ritchie.  "The C Programming
 * Language, 2nd ed."  Addison-Wesley, Reading, MA, 1988.  pp. 63-64.
 */
char const* LC_ltoa( register long n ) {
    static char buf[ 20 ];
    register char *s = buf;
    bool const is_neg = n < 0;

    if ( is_neg ) n = -n;
    do {                                // generate digits in reverse
        *s++ = n % 10 + '0';
    } while ( n /= 10 );
    if ( is_neg ) *s++ = '-';
    *s = '\0';

    // now reverse the string
    for ( register char *t = buf; t < s; ++t ) {
        char const tmp = *--s;
        *s = *t;
        *t = tmp;
    }

    return buf;
}

/* vim:set et sw=4 ts=4: */
