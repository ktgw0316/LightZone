/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * The Light Crafts JNI library
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cstdlib>
#include <errno.h>
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

#if defined(__x86_64__) || defined(__i386__)

/* Registers for 32-bit x86. */
#define CPU_REG_b "ebx"
#define CPU_REG_S "esi"

#define CPUID(index,eax,ebx,ecx,edx)                            \
        asm volatile (                                          \
            "mov %%" CPU_REG_b ", %%" CPU_REG_S "\n\t"          \
            "cpuid\n\t"                                         \
            "xchg %%" CPU_REG_b ", %%" CPU_REG_S                \
            : "=a" (eax), "=S" (ebx), "=c" (ecx), "=d" (edx)    \
            : "0" (index)                                       \
        )

#endif  /* __x86_64__ || __i386__ */

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
 * Given a class, method name, and method signature, gets the jmethodID.
 */
jmethodID LC_getStaticMethodID( JNIEnv *env, jclass jClass,
                                char const *methodName,
                                char const *methodSignature ) {
    jmethodID const jMethodID = env->GetStaticMethodID(
        jClass, methodName, methodSignature
    );
    if ( LC_exceptionOccurred( env ) || !jMethodID ) {
        cerr << "GetStaticMethodID(" << methodName << ',' << methodSignature
             << ") failed" << endl;
        ::exit( 1 );
    }
    return jMethodID;
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

#if defined(__x86_64__) || defined(__i386__)
/**
 * Returns non-zero only if the CPU has SSE2 support.
 */
int LC_hasSSE2() {
    int max_std_level, std_caps;
    int eax, ebx, ecx, edx;

    CPUID( 0, max_std_level, ebx, ecx, edx );
    if ( max_std_level >= 1 ) {
        CPUID( 1, eax, ebx, ecx, std_caps );
        if ( std_caps & (1 << 26) /* SSE2 */ )
            return 1;
    }
    return 0;
}
#endif  /* __x86_64__ || __i386__ */

/**
 * Sets a float field of a Java class to a given value.
 */
void LC_setFloatField( JNIEnv *env, jobject jObject, char const *fieldName,
                       float value ) {
    auto_jclass const jClass( env, jObject );
    jfieldID const fieldID = env->GetFieldID( jClass, fieldName, "F" );
    if ( !fieldID )
        return;                         // NoSuchFieldError was thrown by Java
    env->SetFloatField( jObject, fieldID, value );
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
 * Sets a static int field of a Java class to a given value.
 */
void LC_setStaticIntField( JNIEnv *env, jclass jClass, char const *fieldName,
                           int value ) {
    jfieldID const fieldID = env->GetStaticFieldID( jClass, fieldName, "I" );
    if ( !fieldID )
        return;                         // NoSuchFieldError was thrown by Java
    env->SetStaticIntField( jClass, fieldID, value );
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
 * Throw an IOException back on the Java side based on errno.
 */
void LC_throwIOExceptionForErrno( JNIEnv *env ) {
#ifdef __APPLE__
    char errorBuf[ 80 ];
    ::strerror_r( errno, errorBuf, sizeof errorBuf );
#else
    char const *const errorBuf = ::strerror( errno );
#endif
    LC_throwIOException( env, errorBuf );
}

/**
 * Throw an IOException back on the Java side for the given error code.
 */
void LC_throwIOExceptionForErrorCode( JNIEnv *env, int errorCode ) {
    char errorBuf[ 20 ];
    snprintf( errorBuf, sizeof errorBuf, "Error code %d", errorCode );
    LC_throwIOException( env, errorBuf );
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
