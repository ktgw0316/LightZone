/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <jni.h>

/**
 * This is called by the Java class-loader.
 */
JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM*, void* ) {
    return JNI_VERSION_1_4;
}

/* vim:set et sw=4 ts=4: */
