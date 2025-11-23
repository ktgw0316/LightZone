/**
 * Mac OS X file utilities.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 * See: http://developer.apple.com/documentation/Cocoa/Conceptual/LowLevelFileMgmt/Tasks/ResolvingAliases.html
 */

// standard
#include <CoreServices/CoreServices.h>
#import <Foundation/Foundation.h>

#ifdef DEBUG
#include <iostream>
#endif

// local
#include "LC_CPPUtils.h"
#include "LC_JNIUtils.h"
#include "LC_CocoaUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_MacOSXFileUtil.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXFileUtil_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXFileUtil,_,method)

/**
  * Resolve a Mac OS X alias file.
  * Returns the resolved file (or the original file if it wasn't an alias), or
  * null if there was an error.
  */
JNIEXPORT jstring JNICALL MacOSXFileUtil_METHOD(resolveAlias)
    ( JNIEnv *env, jclass, jstring jPath )
{
    auto_obj<NSAutoreleasePool> pool;
    NSString *const nsPath = LC_jstringToNSString( env, jPath );
    jstring result = NULL;

    @autoreleasepool {
        NSURL *url = [NSURL fileURLWithPath: nsPath];
        NSNumber *isAlias = nil;
        NSError *err = nil;
        if ( ![url getResourceValue: &isAlias forKey: NSURLIsAliasFileKey error: &err] ) {
            // Could not read resource value; return NULL to indicate error
            return NULL;
        }

        if ( isAlias != nil && [isAlias boolValue] ) {
            // Try to read bookmark data from the alias file and resolve it.
            NSError *bErr = nil;
            NSData *bookmark = [NSURL bookmarkDataWithContentsOfURL: url error: &bErr];
            if ( bookmark ) {
                BOOL stale = NO;
                NSError *resErr = nil;
                NSURL *resolved = [NSURL URLByResolvingBookmarkData: bookmark
                                                            options: NSURLBookmarkResolutionWithoutUI | NSURLBookmarkResolutionWithoutMounting
                                                      relativeToURL: nil
                                                bookmarkDataIsStale: &stale
                                                              error: &resErr];
                if ( resolved ) {
                    result = LC_NSStringTojstring( env, [resolved path] );
                } else {
                    // If resolving bookmark failed, fall back to returning NULL
                    result = NULL;
                }
            } else {
                // If we couldn't get bookmark data, return NULL
                result = NULL;
            }
        } else {
            // Not an alias: return the original path
            result = LC_NSStringTojstring( env, nsPath );
        }
    }

    return result;
}
