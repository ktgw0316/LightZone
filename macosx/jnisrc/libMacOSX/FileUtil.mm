/**
 * Mac OS X file utilities.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 * See: http://developer.apple.com/documentation/Cocoa/Conceptual/LowLevelFileMgmt/Tasks/ResolvingAliases.html
 */

// standard
#include <CoreServices/CoreServices.h>

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
 * Check whether a file is a Mac OS X alias file.
 */
JNIEXPORT jboolean JNICALL MacOSXFileUtil_METHOD(isAlias)
    ( JNIEnv *env, jclass, jstring jPath )
{
    auto_obj<NSAutoreleasePool> pool;
    NSString *const nsPath = LC_jstringToNSString( env, jPath );
    CFURLRef cfURLRef = CFURLCreateWithFileSystemPath(
        NULL, (CFStringRef)nsPath, kCFURLPOSIXPathStyle, NO /* isDirectory */
    );
    if ( !cfURLRef )
        return JNI_FALSE;

    jboolean result = JNI_FALSE;

    FSRef fsRef;
    if ( CFURLGetFSRef( cfURLRef, &fsRef ) ) {
        Boolean isAlias, isFolder;
        if ( FSIsAliasFile( &fsRef, &isAlias, &isFolder ) == noErr && isAlias )
            result = JNI_TRUE;
    }

    CFRelease( cfURLRef );
    return result;
}

/**
 * Moves a set of files to the Trash.
 */
JNIEXPORT jboolean JNICALL MacOSXFileUtil_METHOD(moveToTrash)
    ( JNIEnv *env, jclass, jstring jDirectory, jobjectArray jFiles )
{
    auto_obj<NSAutoreleasePool> pool;
    NSInteger tag;
    BOOL const result =
        [[NSWorkspace sharedWorkspace]
            performFileOperation:NSWorkspaceRecycleOperation
            source:LC_jstringToNSString( env, jDirectory ) destination:@""
            files:LC_jStringArrayToNSArray( env, jFiles ) tag:&tag];
    return result ? JNI_TRUE : JNI_FALSE;
}

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
    CFURLRef cfURLRef = CFURLCreateWithFileSystemPath(
        NULL, (CFStringRef)nsPath, kCFURLPOSIXPathStyle, NO /* isDirectory */
    );
    if ( !cfURLRef )
        return NULL;

    FSRef fsRef;
    if ( !CFURLGetFSRef( cfURLRef, &fsRef ) )
        goto error;

    Boolean isAlias, isFolder;
    OSErr err;
        err = FSResolveAliasFileWithMountFlags(
        &fsRef, true /* resolveAliasChains */, &isFolder, &isAlias,
        kResolveAliasFileNoUI
    );
    if ( err != noErr )
        goto error;

    if ( isAlias ) {
        CFURLRef cfResolvedURL = CFURLCreateFromFSRef( NULL, &fsRef );
        if ( !cfResolvedURL )
            goto error;
        CFStringRef cfResolvedPath =
            CFURLCopyFileSystemPath( cfResolvedURL, kCFURLPOSIXPathStyle );
        jPath = LC_NSStringTojstring( env, (NSString*)cfResolvedPath );
        CFRelease( cfResolvedPath );
        CFRelease( cfResolvedURL );
    }
    goto done;

error:
    jPath = NULL;
done:
    CFRelease( cfURLRef );
    return jPath;
}
/* vim:set et sw=4 ts=4: */
