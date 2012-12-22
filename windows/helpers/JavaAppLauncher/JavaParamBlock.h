/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * JavaParamBlock.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef JavaParamBlock_H
#define JavaParamBlock_H

// standard
#include <jni.h>

struct JavaParamBlock {
    /**
     * This is the signature of the JNI_CreateJavaVM() function.
     */
    typedef jint (JNICALL *CreateJavaVM_t)(JavaVM**, void**, void*);

    /////////////// Input

    CreateJavaVM_t  CreateJavaVM_func;
    JavaVMInitArgs  jvm_args;
    char*           main_className;
    int             main_argc;
    char**          main_argv;

    /////////////// Output

    jclass          main_class;
};

/**
 * Initialize the given JavaParamBlock.
 */
void initJavaParamBlock( JavaParamBlock*, char const *const jvmArgs[] );

#endif  /* JavaParamBlock_H */
/* vim:set et sw=4 ts=4: */
