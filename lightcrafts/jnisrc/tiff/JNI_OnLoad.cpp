/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <jni.h>

// local
#include "LC_JNIUtils.h"

/**
 * This is called by the Java class-loader.
 */
JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *jvm, void* ) {
    g_jvm = jvm;
    return JNI_VERSION_1_4;
}

/* vim:set et sw=4 ts=4: */
