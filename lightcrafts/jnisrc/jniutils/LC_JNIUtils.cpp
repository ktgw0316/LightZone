/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * The Light Crafts JNI library
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cstdlib>
#include <iostream>
#include <jni.h>

#ifdef WIN32
#include <windows.h>
#else
#include <cstdio>
#endif

// local
#define LC_JNIUtils_IMPLEMENTATION
#include "LC_JNIUtils.h"

using namespace std;
using namespace LightCrafts;

extern "C" {

/**
 * The running JVM.
 */
JNIEXPORT JavaVM *g_jvm;

}

/**
 * Destruct an auto_JNIEnv.
 */
auto_JNIEnv::~auto_JNIEnv() {
    if ( m_mustDetach )
        g_jvm->DetachCurrentThread();
}

/**
 * Attach to the current JVM thread.
 */
JNIEnv* LC_attachCurrentThread() {
    JNIEnv *env;
    if ( g_jvm->AttachCurrentThread( (void**)&env, NULL ) != 0 ) {
        cerr << "AttachCurrentThread() failed" << endl;
        ::exit( 1 );
    }
    return env;
}

/**
 * Check to see if Java threw an exception: if so, report it, then clear it.
 */
int LC_exceptionOccurred( JNIEnv *env ) {
    bool const exceptionOccurred = env->ExceptionCheck();
    if ( exceptionOccurred ) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    return exceptionOccurred;
}

/**
 * Given a class name, find the actual Java class for it.
 */
jclass LC_findClassOrDie( JNIEnv *env, char const *className,
                          int newGlobalRef ) {
    jclass jClass = env->FindClass( className );
    if ( LC_exceptionOccurred( env ) || !jClass ) {
        cerr << "FindClass(" << className << ") failed" << endl;
        ::exit( 1 );
    }
    if ( newGlobalRef )
        jClass = (jclass)env->NewGlobalRef( jClass );
    return jClass;
}

/**
 * Open a file.  This function correctly handles path names containing Unicode
 * (UTF-8) on all platforms.
 */
FILE* LC_fopen( char const *path, char const *mode ) {
#ifdef WIN32
    //
    // The fopen() function as implemented under Cygwin doesn't handle UTF-8
    // paths.  However, there is a _wfopen() function that handles UTF-16, so
    // convert the UTF-8 to UTF-16 first.
    //
    int result = ::MultiByteToWideChar( CP_UTF8, 0, path, -1, NULL, 0 );
    WCHAR wPath[ result ];
    result = ::MultiByteToWideChar( CP_UTF8, 0, path, -1, wPath, result );
    if ( !result )
        return NULL;
    result = ::MultiByteToWideChar( CP_UTF8, 0, mode, -1, NULL, 0 );
    WCHAR wMode[ result ];
    result = ::MultiByteToWideChar( CP_UTF8, 0, mode, -1, wMode, result );
    if ( !result )
        return NULL;
    return ::_wfopen( wPath, wMode );
#else
    return ::fopen( path, mode );
#endif  /* WIN32 */
}

/**
 * Gets the JNI env for the current thread.
 */
JNIEnv* LC_getJNIEnv( int *mustDetach ) {
    JNIEnv *env;
    switch ( g_jvm->GetEnv( (void**)&env, JNI_VERSION_1_4 ) ) {
        case JNI_OK:
            if ( mustDetach )
                *mustDetach = false;
            return env;
        case JNI_EDETACHED:
            if ( mustDetach )
                *mustDetach = true;
            return LC_attachCurrentThread();
        default:
            cerr << "GetEnv() failed" << endl;
            ::exit( 1 );
    }
}

/**
 * Get the pointer value previously stored inside the Java object.
 */
void* LC_getNativePtr( JNIEnv *env, jobject jObject ) {
    auto_jclass const jClass( env, jObject );
    jfieldID const fieldID = env->GetFieldID( jClass, "m_nativePtr", "J" );
    if ( !fieldID )
        return 0;                       // NoSuchFieldError was thrown by Java
    return reinterpret_cast<void*>( env->GetLongField( jObject, fieldID ) );
}

/**
 * Sets an int field of a Java class to a given value.
 */
void LC_setIntField( JNIEnv *env, jobject jObject, char const *fieldName,
                     int value ) {
    auto_jclass const jClass( env, jObject );
    jfieldID const fieldID = env->GetFieldID( jClass, fieldName, "I" );
    if ( !fieldID )
        return;                         // NoSuchFieldError was thrown by Java
    env->SetIntField( jObject, fieldID, value );
}

/**
 * Store a pointer inside the LCJPEG object on the Java side.
 */
void LC_setNativePtr( JNIEnv *env, jobject jObject, void *ptr ) {
    auto_jclass const jClass( env, jObject );
    jfieldID const fieldID = env->GetFieldID( jClass, "m_nativePtr", "J" );
    if ( !fieldID )
        return;                         // NoSuchFieldError was thrown by Java
    env->SetLongField( jObject, fieldID, reinterpret_cast<jlong>( ptr ) );
}

/**
 * Throw a FileNotFoundException back on the Java side.
 */
void LC_throwFileNotFoundException( JNIEnv *env, char const *msg ) {
    if ( env->ExceptionCheck() ) {
        //
        // If an exception is already pending, don't throw another one.
        //
        return;
    }
    char const FileNotFoundExceptionClass[] = "java/io/FileNotFoundException";
    env->ThrowNew( LC_findClassOrDie( env, FileNotFoundExceptionClass ), msg );
}

/**
 * Throw an IllegalArgumentException back on the Java side.
 */
void LC_throwIllegalArgumentException( JNIEnv *env, char const *msg ) {
    if ( env->ExceptionCheck() ) {
        //
        // If an exception is already pending, don't throw another one.
        //
        return;
    }
    char const IllegalArgumentExceptionClass[] =
        "java/lang/IllegalArgumentException";
    env->ThrowNew( LC_findClassOrDie( env, IllegalArgumentExceptionClass ), msg );
}

/**
 * Throw an IllegalStateException back on the Java side.
 */
void LC_throwIllegalStateException( JNIEnv *env, char const *msg ) {
    if ( env->ExceptionCheck() ) {
        //
        // If an exception is already pending, don't throw another one.
        //
        return;
    }
    char const IllegalStateExceptionClass[] = "java/lang/IllegalStateException";
    env->ThrowNew( LC_findClassOrDie( env, IllegalStateExceptionClass ), msg );
}

/**
 * Throw an IOException back on the Java side with the given message.
 */
void LC_throwIOException( JNIEnv *env, char const *msg ) {
    if ( env->ExceptionCheck() ) {
        //
        // If an exception is already pending, don't throw another one.
        //
        return;
    }
    char const IOExceptionClass[] = "java/io/IOException";
    env->ThrowNew( LC_findClassOrDie( env, IOExceptionClass ), msg );
}

/**
 * Throw an OutOfMemoryError back on the Java side.
 */
void LC_throwOutOfMemoryError( JNIEnv *env, char const *msg ) {
    if ( env->ExceptionCheck() ) {
        //
        // If an exception is already pending, don't throw another one.
        //
        return;
    }
    char const OutOfMemoryErrorClass[] = "java/lang/OutOfMemoryError";
    env->ThrowNew( LC_findClassOrDie( env, OutOfMemoryErrorClass ), msg );
}

/* vim:set et sw=4 ts=4: */
