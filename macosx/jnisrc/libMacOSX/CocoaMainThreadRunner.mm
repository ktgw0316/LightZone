// standard
#import <Cocoa/Cocoa.h>
#import <jni.h>

// local
#include "LC_CocoaUtils.h"
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_CocoaMainThreadRunner.h"
#endif

using namespace std;
using namespace LightCrafts;

@interface JavaVoidMethodCaller : NSObject {
    jmethodID   m_jMethodID;
    jobject     m_jRunnable;
}

- (id) initWithMethodID:
    (jmethodID)jMethodID
    andRunnable:(jobject)jRunnable;

- (void) callJavaVoidMethod:
    (id)notUsed;

@end

////////// JNI ////////////////////////////////////////////////////////////////

#define CocoaMainThreadRunner_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_CocoaMainThreadRunner,_,method)

/**
 * Call the run() method of a Java Runnable on the Cocoa AppKit thread.
 */
JNIEXPORT void JNICALL CocoaMainThreadRunner_METHOD(perform)
    ( JNIEnv *env, jclass, jobject jRunnable )
{
    static jclass       jRunnable_class;
    static jmethodID    jRun_methodID;

    if ( !jRunnable_class ) {
        jRunnable_class = LC_findClassOrDie( env, "java/lang/Runnable" );
        jRunnable_class = (jclass)env->NewGlobalRef( jRunnable_class );

        jRun_methodID = env->GetMethodID( jRunnable_class, "run", "()V" );
        if ( !jRun_methodID )
            return;                     // NoSuchMethodException thrown by Java
    }

    auto_obj<NSAutoreleasePool> pool;

    JavaVoidMethodCaller *const javaVoidMethodCaller =
        [[JavaVoidMethodCaller alloc]
            initWithMethodID:jRun_methodID
            andRunnable:jRunnable];

    [javaVoidMethodCaller
        performSelectorOnMainThread:@selector(callJavaVoidMethod:)
        withObject:NULL waitUntilDone:YES];
}

////////// Objective C ////////////////////////////////////////////////////////

@implementation JavaVoidMethodCaller

/**
 * Call a Java void method.
 */
- (void) callJavaVoidMethod:
    (id)notUsed
{
    auto_JNIEnv const env;
    env->CallVoidMethod( m_jRunnable, m_jMethodID );
}

/**
 * Initialize a JavaVoidMethodCaller.
 */
- (id) initWithMethodID:
    (jmethodID)jMethodID
    andRunnable:(jobject)jRunnable
{
    self = [super init];
    m_jMethodID = jMethodID;
    m_jRunnable = jRunnable;
    return self;
}

@end
/* vim:set et sw=4 ts=4: */
