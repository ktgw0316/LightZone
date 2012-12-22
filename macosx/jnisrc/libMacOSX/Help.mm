// standard
#import <Cocoa/Cocoa.h>

// local
#include "LC_CocoaUtils.h"
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_MacOSXHelp.h"
#endif

using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXHelp_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXHelp,_,method)


JNIEXPORT void JNICALL
MacOSXHelp_METHOD(showHelpTopic)
    ( JNIEnv *env, jclass, jstring jAnchor )
{
    if ( jAnchor ) {
        auto_obj<NSAutoreleasePool> pool;

        NSString *const locBookName =
            [[NSBundle mainBundle]
                objectForInfoDictionaryKey:@"CFBundleHelpBookName"];

        [[NSHelpManager sharedHelpManager]
            openHelpAnchor:LC_jstringToNSString( env, jAnchor )
            inBook:locBookName];
    } else {
        [[NSApplication sharedApplication]
            performSelectorOnMainThread:@selector(showHelp:)
            withObject:NULL waitUntilDone:NO];
    }
}

/* vim:set et sw=4 ts=4: */
