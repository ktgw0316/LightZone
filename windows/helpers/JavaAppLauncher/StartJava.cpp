/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * StartJava.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <jni.h>
#ifdef DEBUG
#include <iostream>
#endif

// windows
#include <windows.h>

// local
#include "JavaParamBlock.h"
#include "LC_JNIUtils.h"
#include "LC_WinUtils.h"
#include "UI.h"

using namespace std;
using namespace LightCrafts;

/**
 * The running JVM.
 */
JavaVM *g_jvm;

/**
 * A semaphore used to suspend the main thread until the Java application (on
 * the JVM thread) is initialized and ready to open files.
 */
static HANDLE g_javaIsReadySemaphore;

/**
 * Convert the array of char* pointers of the main() arguments to a Java
 * String[].
 */
static jobjectArray convertMainArgs( JNIEnv *env, JavaParamBlock const *jpb ) {
    jclass jString_class = LC_findClassOrDie( env, "java/lang/String" );

    jobjectArray jArray;
    if ( jpb->main_argc ) {
        jArray = env->NewObjectArray( jpb->main_argc, jString_class, 0 );
        if ( !jArray )
            LC_die( TEXT("Could not allocate main() argument array.") );
        for ( int i = 0; i < jpb->main_argc; ++i ) {
            auto_jstring jArg( env, env->NewStringUTF( jpb->main_argv[i] ) );
            if ( !jArg )
                LC_die( TEXT("Could not allocate main() argument string.") );
            env->SetObjectArrayElement( jArray, i, jArg );
        }
    } else {
        //
        // There are no arguments to main(), so create an empty array of
        // strings and use that for "no arguments" to main().
        //
        jArray = env->NewObjectArray( 0, jString_class, 0 );
        if ( !jArray )
            LC_die( TEXT("Could not allocate main() argument array.") );
    }

    return jArray;
}

/**
 * This method is called when the Java application is ready to open files.
 */
JNIEXPORT void JNICALL readyToOpenFiles( JNIEnv*, jclass, ... ) {
#ifdef DEBUG
    cout << "*** In readyToOpenFiles()" << endl;
#endif
    //
    // Signal the main thread that we've finished starting up the JVM.
    //
    if ( !::ReleaseSemaphore( g_javaIsReadySemaphore, 1, NULL ) )
        LC_die( TEXT("Could not signal semaphore.") );
}

/**
 * Start a Java virtual machine and run the main class's main() method.
 */
DWORD WINAPI jvmThreadMain( LPVOID param ) {
    JavaParamBlock *const jpb = static_cast<JavaParamBlock*>( param );
    //
    // Start a JVM.
    //
    JNIEnv *env;
    if ( jpb->CreateJavaVM_func( &g_jvm, (void**)&env, &jpb->jvm_args ) != 0 )
        LC_die( TEXT("Error starting Java virtual machine.") );

    //
    // Find the application's main class.
    //
    jpb->main_class = env->FindClass( jpb->main_className );
    if ( LC_exceptionOccurred( env ) || !jpb->main_class )
        LC_die( TEXT("Could not find main class.") );
    jpb->main_class = (jclass)env->NewGlobalRef( jpb->main_class );

    //
    // Find the main class's main() method.
    //
    jmethodID const jMain_methodID = env->GetStaticMethodID(
        jpb->main_class, "main", "([Ljava/lang/String;)V"
    );
    if ( LC_exceptionOccurred( env ) || !jMain_methodID )
        LC_die( TEXT("Main class has no main() method.") );

    //
    // Register native methods.  This must be done before the main class's
    // main() method is called to guard against the possibility of a native
    // method being called before it's been registered.
    //
    JNINativeMethod const jMethod = {
        "readyToOpenFiles", "()V", (void*)&readyToOpenFiles
    };
    if ( env->RegisterNatives( jpb->main_class, &jMethod, 1 ) != 0 )
        LC_die( TEXT("RegisterNatives() failed") );

    //
    // Call main class's main() method.
    //
    env->CallStaticVoidMethod(
        jpb->main_class, jMain_methodID, convertMainArgs( env, jpb )
    );
    if ( LC_exceptionOccurred( env ) )
        LC_die( TEXT("Error calling main().") );

    //
    // Detach the current thread so that it appears to have exited when the
    // Java application's main() method exits.
    //
    if ( g_jvm->DetachCurrentThread() != 0 )
        LC_die( TEXT("Could not detach thread.") );

    //
    // Unload the JVM and reclaim its resources.  Only this thread can unload
    // the JVM.  This call blocks until this thread is only remaining user
    // thread before it destroys the JVM.
    //
    g_jvm->DestroyJavaVM();
    return 0;
}

/**
 * Load the JVM and start the JVM in its own thread.
 */
void startJava( JavaParamBlock *jpb ) {
    //
    // Load our private jvm.dll and get the pointer to the JNI_CreeateJavaVM()
    // function to use create the JVM.
    //
    HINSTANCE libHandle = ::LoadLibrary( TEXT("jre\\bin\\client\\jvm.dll") );
    if ( !libHandle )
        libHandle = ::LoadLibrary( TEXT("jre\\bin\\server\\jvm.dll") );
    if ( !libHandle )
        LC_die( TEXT("Could not load JVM library.") );
    jpb->CreateJavaVM_func = (JavaParamBlock::CreateJavaVM_t)
        ::GetProcAddress( libHandle, "JNI_CreateJavaVM" );
    if ( !jpb->CreateJavaVM_func )
        LC_die( TEXT("Could not find JNI methods in jvm.dll.") );

    //
    // Create a semaphore to block returning from this function until the JVM
    // thread has finished starting up the JVM.
    //
    g_javaIsReadySemaphore = ::CreateSemaphore( NULL, 0, 1, NULL );
    if ( !g_javaIsReadySemaphore )
        LC_die( TEXT("Could not create semaphore.") );

    if ( !::CreateThread( NULL, 0, &jvmThreadMain, jpb, 0, NULL ) )
        LC_die( TEXT("Could not start JVM thread.") );

    ::WaitForSingleObject( g_javaIsReadySemaphore, INFINITE );
    ::CloseHandle( g_javaIsReadySemaphore );
}

/* vim:set et sw=4 ts=4: */
