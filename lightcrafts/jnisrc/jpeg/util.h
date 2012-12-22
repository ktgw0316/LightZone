/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LCJPEG_util_H
#define LCJPEG_util_H

// standard
#include <jni.h>

extern "C" {
#   include <jpeglib.h>
}

/**
 * Check to see if a Java exception occurred.  If so, throw an LC_JPEGException
 * to stop what we're doing.
 */
void LC_checkForJavaException( JNIEnv* );

/**
 * Our own version of the JPEG library's error_exit function that instead
 * throws a Java exception rather than calling exit(3).
 */
extern "C" void LC_error_exit( j_common_ptr );

/**
 * Throw an LCImageLibException.
 */
void LC_throwLCImageLibException( JNIEnv*, char const *msg );

#endif  /* LCJPEG_util_H */
/* vim:set et sw=4 ts=4: */
