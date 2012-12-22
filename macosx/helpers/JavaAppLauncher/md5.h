/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef MD5_H
#define MD5_H

#include <stdint.h>

#ifdef  __cplusplus
extern "C" {
#endif

/*  The following tests optimise behaviour on little-endian
    machines, where there is no need to reverse the byte order
    of 32 bit words in the MD5 computation.  By default,
    HIGHFIRST is defined, which indicates we're running on a
    big-endian (most significant byte first) machine, on which
    the byteReverse function in md5.c must be invoked. However,
    byteReverse is coded in such a way that it is an identity
    function when run on a little-endian machine, so calling it
    on such a platform causes no harm apart from wasting time. 
    If the platform is known to be little-endian, we speed
    things up by undefining HIGHFIRST, which defines
    byteReverse as a null macro.  Doing things in this manner
    insures we work on new platforms regardless of their byte
    order.  */

#define HIGHFIRST

#ifdef __i386__
#undef HIGHFIRST
#endif

struct MD5Context {
    uint32_t buf[4];
    uint32_t bits[2];
    unsigned char in[64];
};


void MD5Init( struct MD5Context* );
void MD5Update( struct MD5Context*, unsigned char const*, unsigned len );
void MD5Final( unsigned char digest[16], struct MD5Context* );
void MD5Transform( uint32_t[4], uint32_t[16] );

/*
 * This is needed to make RSAREF happy on some MS-DOS compilers.
 */
typedef struct MD5Context MD5_CTX;

/*  Define CHECK_HARDWARE_PROPERTIES to have main.c verify
    byte order and uint32_t settings.  */
#define CHECK_HARDWARE_PROPERTIES

#ifdef  __cplusplus
}
#endif

#endif /* !MD5_H */
/* vim:set et sw=4 ts=4: */
