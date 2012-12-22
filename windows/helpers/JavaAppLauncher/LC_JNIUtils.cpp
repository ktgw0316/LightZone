/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * LC_JNIUtils.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// local
#include "LC_JNIUtils.h"
#include "UI.h"

extern JavaVM *g_jvm;

/**
 * Attach to the current JVM thread.
 */
JNIEnv* LC_attachCurrentThread() {
    JNIEnv *env;
    if ( g_jvm->AttachCurrentThread( (void**)&env, NULL ) != 0 )
        LC_die( TEXT("AttachCurrentThread() failed.") );
    return env;
}

/**
 * Check to see if Java threw an exception: if so, report it, then clear it.
 */
bool LC_exceptionOccurred( JNIEnv *env ) {
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
jclass LC_findClassOrDie( JNIEnv *env, char const *className ) {
    jclass const jClass = env->FindClass( className );
    if ( !jClass )
        LC_die( TEXT("FindClass() failed") );
    return jClass;
}

/* vim:set et sw=4 ts=4: */
