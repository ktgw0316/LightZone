// standard
#import <Cocoa/Cocoa.h>
#import <jni.h>

// local
#include "LC_CocoaUtils.h"
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_AppleScript.h"
#endif

using namespace LightCrafts;

////////// Objective C interface //////////////////////////////////////////////

@interface AppleScriptProxy : NSObject {
    NSAppleScript *m_script;
}

- (id) init;

- (void) compileScript:
    (NSString*)script;

- (BOOL) compiledScript;

- (void) dealloc;

- (void) runScript:
    (id)notUsed;

@end

////////// Objective C implementation /////////////////////////////////////////

@implementation AppleScriptProxy

/**
 * Initialize an AppleScriptProxy.
 */
- (id) init
{
    self = [super init];
    m_script = nil;
    return self;
}

/**
 * Deallocate an AppleScriptProxy.
 */
- (void) dealloc
{
    [m_script release];
    [super dealloc];
}

/**
 * Returns true only if the script compiled successfully.
 */
- (BOOL) compiledScript
{
    return m_script != nil;
}

/**
 * Compile an AppleScript.
 */
- (void) compileScript:
    (NSString*)source
{
    m_script = [[NSAppleScript alloc] initWithSource:source];
    if ( [m_script compileAndReturnError:nil] )
        [m_script retain];
    else
        m_script = nil;
}

/**
 * Run the given AppleScript on the main thread.
 */
- (void) runScript:
    (id)notUsed
{
    [m_script executeAndReturnError:nil];
}

@end

////////// JNI ////////////////////////////////////////////////////////////////

#define AppleScript_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_AppleScript,_,method)

/**
 * Run an AppleScript.
 */
JNIEXPORT void JNICALL AppleScript_METHOD(run)
    ( JNIEnv *env, jclass, jstring jScript )
{
    auto_obj<NSAutoreleasePool> pool;

    AppleScriptProxy *const proxy = [[AppleScriptProxy alloc] init];

    [proxy
        performSelectorOnMainThread:@selector(compileScript:)
        withObject:LC_jstringToNSString( env, jScript )
        waitUntilDone:YES];

    if ( ![proxy compiledScript] ) {
        LC_throwIllegalArgumentException(
            env, "AppleScript compilation failed"
        );
        return;
    }

    [proxy retain];

    [proxy
        performSelectorOnMainThread:@selector(runScript:)
        withObject:nil waitUntilDone:NO];

    [proxy autorelease];
}

/* vim:set et sw=4 ts=4: */
