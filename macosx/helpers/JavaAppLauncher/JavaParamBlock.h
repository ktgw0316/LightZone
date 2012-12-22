/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * JavaParamBlock.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef JavaParamBlock_H
#define JavaParamBlock_H

// standard
#include <jni.h>                        /* for JavaVMInitArgs */

/**
 * This contains the parameters from the Java dictionary in Info.plist.
 */
struct JavaParamBlock {
    char*           jvm_version;
    JavaVMInitArgs  jvm_args;
    char*           main_className;
    int             main_argc;
    char**          main_argv;
};

/**
 * Initialize the given JavaParamBlock.
 */
void initJavaParamBlock( JavaParamBlock* );

#endif  /* JavaParamBlock_H */
/* vim:set et sw=4 ts=4: */
