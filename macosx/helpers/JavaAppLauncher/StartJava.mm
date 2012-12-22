/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * StartJava.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 *
 * This code is based on:
 * http://developer.apple.com/samplecode/simpleJavaLauncher/simpleJavaLauncher.html
 * http://developer.apple.com/samplecode/JavaSplashScreen/JavaSplashScreen.html
 */

// standard
#include <jni.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/resource.h>               /* for getrlimit(2) */
#ifdef DEBUG
#include <iostream>
#endif

// local
#include "JavaParamBlock.h"
#include "LC_JNIUtils.h"
#include "UI.h"

using namespace std;
using namespace LightCrafts;

JavaVM *g_jvm;

/**
 * Convert the array of char* pointers of the main() arguments to a Java
 * String[].
 */
static jobjectArray convertMainArgs( JNIEnv *env, JavaParamBlock const *jpb ) {
    jclass jString_class = env->FindClass( "java/lang/String" );
    if ( !jString_class )
        LC_die( @"Unexpected", @"Missing java.lang.String" );

    jobjectArray jArgs =
        env->NewObjectArray( jpb->main_argc, jString_class, 0 );
    if ( !jArgs )
        LC_die( @"Unexpected", @"Could not allocate main() array" );
    if ( jpb->main_argc ) {
        for ( int i = 0; i < jpb->main_argc; ++i ) {
            auto_jstring jArg( env, env->NewStringUTF( jpb->main_argv[i] ) );
            if ( !jArg )
                LC_die( @"Unexpected", @"Could not allocate main() string" );
            env->SetObjectArrayElement( jArgs, i, jArg );
        }
    } else {
        //
        // No "Arguments" key was found so leave the array empty and use that
        // for "no arguments" to main().
        //
    }

    return jArgs;
}

/**
 * This method is called when the Java application is ready to open files.
 */
JNIEXPORT void JNICALL readyToOpenFiles( JNIEnv*, jclass ) {
#ifdef DEBUG
    cout << "*** In readyToOpenFiles()" << endl;
#endif
    extern bool g_javaIsReady;
    g_javaIsReady = true;
}

/**
 * Start the JVM.  This must be called in its own thread.
 */
void startJava( JavaParamBlock const *jpb, jclass *pLauncher_class ) {
    //
    // Start a JVM.
    //
    JNIEnv *env;
    if ( JNI_CreateJavaVM( &g_jvm, (void**)&env, (void*)&jpb->jvm_args ) != 0 )
        LC_die( @"Unexpected", @"Error starting JVM" );

    //
    // Find the application's main class.
    //
    *pLauncher_class = env->FindClass( jpb->main_className );
    if ( LC_exceptionOccurred( env ) || !*pLauncher_class )
        LC_die( @"Corrupted", @"Missing main()" );
    *pLauncher_class = (jclass)env->NewGlobalRef( *pLauncher_class );

    //
    // Find the main class's main() method.
    //
    jmethodID const jMain_methodID = env->GetStaticMethodID(
        *pLauncher_class, "main", "([Ljava/lang/String;)V"
    );
    if ( LC_exceptionOccurred( env ) || !jMain_methodID )
        LC_die( @"Corrupted", @"Missing main() method" );

    //
    // Register native methods.
    //
    JNINativeMethod const jMethod = {
        "readyToOpenFiles", "()V", reinterpret_cast<void*>( &readyToOpenFiles )
    };
    if ( env->RegisterNatives( *pLauncher_class, &jMethod, 1 ) < 0 )
        LC_die( @"Unexpected", @"RegisterNatives() failed" );

    //
    // Call the main class's main() method.
    //
    env->CallStaticVoidMethod(
        *pLauncher_class, jMain_methodID, convertMainArgs( env, jpb )
    );
    if ( LC_exceptionOccurred( env ) )
        LC_die( @"Unexpected", @"main() threw exception" );

    //
    // Detach the current thread so that it appears to have exited when the
    // Java application's main() method exits.
    //
    if ( g_jvm->DetachCurrentThread() != 0 )
        LC_die( @"Unexpected", @"Could not detach thread" );

    //
    // Unloads a Java VM and reclaims its resources.  Only this thread can
    // unload the VM.  This call blocks until this thread is only remaining
    // user thread before it destroys the VM.
    //
    g_jvm->DestroyJavaVM();
}

/* vim:set et sw=4 ts=4: */
