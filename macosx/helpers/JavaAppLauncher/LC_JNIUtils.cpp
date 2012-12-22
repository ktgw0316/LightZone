/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * LC_JNIUtils.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cstdlib>
#include <iostream>

// local
#include "LC_JNIUtils.h"

using namespace std;

extern JavaVM *g_jvm;

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
bool LC_exceptionOccurred( JNIEnv *env ) {
    bool const exceptionOccurred = env->ExceptionCheck();
    if ( exceptionOccurred ) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    return exceptionOccurred;
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

/* vim:set et sw=4 ts=4: */
