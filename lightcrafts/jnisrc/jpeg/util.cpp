/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <jni.h>
#ifdef DEBUG
#include <iostream>
using namespace std;
#endif

// local
#include "LC_JNIUtils.h"
#include "LC_JPEGException.h"
#include "util.h"

/**
 * Check to see if a Java exception occurred.  If so, throw an LC_JPEGException
 * to stop what we're doing.
 */
void LC_checkForJavaException( JNIEnv *env ) {
    if ( env->ExceptionCheck() ) {
        env->ExceptionDescribe();
        throw LC_JPEGException();
    }
}

/**
 * Our own version of the JPEG library's error_exit function that instead
 * throws a Java exception rather than calling exit(3).
 */
extern "C" void LC_error_exit( j_common_ptr cinfo ) {
    char msgBuf[ JMSG_LENGTH_MAX ];
    (*cinfo->err->format_message)( cinfo, msgBuf );
#ifdef DEBUG
    cerr << "LC_error_exit(): " << msgBuf << endl;
#endif
    JNIEnv *const env = LC_getJNIEnv( NULL );
    LC_throwLCImageLibException( env, msgBuf );
}
/**
 * Throw a Java LCImageLibException.
 */
void LC_throwLCImageLibException( JNIEnv *env, char const *msg ) {
#ifdef DEBUG
    cerr << "LC_throwLCImageLibException()" << endl;
#endif
    static char const LCImageLibExceptionClass[] =
        "com/lightcrafts/image/libs/LCImageLibException";
    env->ThrowNew( LC_findClassOrDie( env, LCImageLibExceptionClass ), msg );
    throw LC_JPEGException();
}

/* vim:set et sw=4 ts=4: */
