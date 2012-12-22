/* Copyright (C) 2005-2011 Fabio Riccardi */

// local
#include "LC_JNIUtils.h"

/**
 * This is called by the Java class-loader.  We use it to set our global
 * pointer to the JVM instance.
 */
JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *jvm, void* ) {
    g_jvm = jvm;
    return JNI_VERSION_1_4;
}

/* vim:set et sw=4 ts=4: */
