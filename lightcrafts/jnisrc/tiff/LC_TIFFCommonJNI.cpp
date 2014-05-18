/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <jni.h>
#include <stdarg.h>
#include <tiffio.h>

#ifdef WIN32
#include <windows.h>
#endif

// local
#include "LC_JNIUtils.h"
#include "util.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_image_libs_LCTIFFCommon.h"
#endif

using namespace std;
using namespace LightCrafts;

static jclass LCTIFFCommon_class;
static jmethodID throwException_methodID;

////////// Local functions ////////////////////////////////////////////////////

/**
 * Transform a libtiff error into a Java exception.
 */
extern "C" void LC_TIFFErrorHandler( char const*, char const *fmt, va_list ap ){
    char msgBuf[ 1024 ];
    vsnprintf( msgBuf, sizeof msgBuf, fmt, ap );

    JNIEnv *const env = LC_getJNIEnv( NULL );
    env->CallStaticVoidMethod(
        LCTIFFCommon_class, throwException_methodID, env->NewStringUTF( msgBuf )
    );
}

/**
 * Ignore all libtiff warnings
 */
extern "C" void LC_TIFFWarningHandler( char const*, char const *fmt, va_list ap ){
    return;
}

/**
 *  Open a TIFF file.  This function correctly handles path names containing
 *  UTF-8 on all platforms.
 */
TIFF* LC_TIFFOpen( char const *filename, char const *mode ) {
#ifdef WIN32
    wchar_t wFilename[ ::strlen( filename ) + 1 ];
    int const result = ::MultiByteToWideChar(
        CP_UTF8, 0, filename, -1, wFilename, sizeof( wFilename )
    );
    return result ? TIFFOpenW( wFilename, mode ) : NULL;
#else
    return TIFFOpen( filename, mode );
#endif
}

////////// JNI ////////////////////////////////////////////////////////////////

#define LCTIFFCommon_METHOD(method) \
        name4(Java_,com_lightcrafts_image_libs_LCTIFFCommon,_,method)

#define LCTIFFCommon_CONSTANT(constant) \
        name3(com_lightcrafts_image_libs_LCTIFFCommon,_,constant)

/**
 * Initialize LCTIFFCommon.
 */
JNIEXPORT void JNICALL LCTIFFCommon_METHOD(init)
    ( JNIEnv *env, jclass clazz )
{
    throwException_methodID = env->GetStaticMethodID(
        clazz, "throwException", "(Ljava/lang/String;)V"
    );
    LCTIFFCommon_class = (jclass)env->NewGlobalRef( clazz );

    TIFFSetWarningHandler( &LC_TIFFWarningHandler );
    TIFFSetErrorHandler( &LC_TIFFErrorHandler );
}

/**
 * Close the TIFF image file.
 */
JNIEXPORT void JNICALL LCTIFFCommon_METHOD(TIFFClose)
    ( JNIEnv *env, jobject jLCTIFFCommon )
{
    if ( TIFF *const tiff = getNativePtr( env, jLCTIFFCommon ) ) {
        TIFFClose( tiff );
        LC_setNativePtr( env, jLCTIFFCommon, 0 );
    }
}

/* vim:set et sw=4 ts=4: */
