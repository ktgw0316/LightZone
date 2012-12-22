/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * StartJava.h
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

#ifndef StartJava_H
#define StartJava_H

// standard
#include <jni.h>

// local
#include "JavaParamBlock.h"

/**
 * The running JVM.
 */
extern JavaVM *g_jvm;

/**
 * Start a Java virtual machine and run the main class's main() method.
 */
void startJava( JavaParamBlock* );

#endif  /* StartJava_H */
/* vim:set et sw=4 ts=4: */
