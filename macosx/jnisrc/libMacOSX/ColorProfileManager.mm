// standard
#import <Carbon/Carbon.h>
#import <Cocoa/Cocoa.h>
#import <CoreServices/CoreServices.h>

#ifdef DEBUG
#include <iostream>
#endif

// local
#include "LC_CocoaUtils.h"
#include "LC_CPPUtils.h"
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_MacOSXColorProfileManager.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// Local functions ////////////////////////////////////////////////////

#ifndef __LP64__
/**
 * Get the full path of a file (as a UTF-8 string) from an FSSpec.
 */
static bool getPathFromFSSpec( FSSpec const *pFSSpec, char *buf, int bufSize ) {
    FSRef fsRef;
    FSpMakeFSRef( pFSSpec, &fsRef );

    CFURLRef cfURLRef = CFURLCreateFromFSRef( NULL, &fsRef );
    if ( !cfURLRef )
        return false;

    CFStringRef cfPath =
        CFURLCopyFileSystemPath( cfURLRef, kCFURLPOSIXPathStyle );

    bool const result =
        CFStringGetCString( cfPath, buf, bufSize, kCFStringEncodingUTF8 );

    CFRelease( cfPath );
    CFRelease( cfURLRef );
    return result;
}
#endif /* __LP64__ */

/**
 * Get a color profile's path from a CMProfileLocation.  Note that is the
 * caller's responsibility to delete the string returned.
 */
static char* getProfilePath( CMProfileLocation const &loc ) {
    switch ( loc.locType ) {
#ifndef __LP64__
        case cmFileBasedProfile:
            char path[ PATH_MAX ];
            if ( getPathFromFSSpec( &loc.u.fileLoc.spec, path, sizeof path ) )
                return new_strdup( path );
            break;
#endif
        case cmPathBasedProfile:
            return new_strdup( loc.u.pathLoc.path );
    }
    return NULL;
}

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXColorProfileManager_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXColorProfileManager,_,method)

/**
 * Gets the path to the system display profile.
 */
JNIEXPORT jstring JNICALL
MacOSXColorProfileManager_METHOD(getSystemDisplayProfilePath)
    ( JNIEnv *env, jclass )
{
#ifdef MAC_OS_X_VERSION_10_11
    CGDirectDisplayID displayID = CGMainDisplayID();
    CFUUIDRef displayUUID = CGDisplayCreateUUIDFromDisplayID(displayID);
    if (!displayUUID) {
#ifdef DEBUG
        cerr << "Cannot get display UUID." << endl;
#endif
        return NULL;
    }

    CFDictionaryRef displayInfo =
        ColorSyncDeviceCopyDeviceInfo(kColorSyncDisplayDeviceClass, displayUUID);
    CFRelease(displayUUID);
    if (!displayInfo) {
#ifdef DEBUG
        cerr << "Cannot get display info." << endl;
#endif
        return NULL;
    }

    CFDictionaryRef factoryInfo =
        (CFDictionaryRef)CFDictionaryGetValue(displayInfo, kColorSyncFactoryProfiles);
    if (!factoryInfo) {
#ifdef DEBUG
        cerr << "Cannot get display factory info." << endl;
#endif
        return NULL;
    }

    CFStringRef defaultProfileID =
        (CFStringRef)CFDictionaryGetValue(factoryInfo, kColorSyncDeviceDefaultProfileID);
    if (!defaultProfileID) {
#ifdef DEBUG
        cerr << "Cannot get display default profile ID." << endl;
#endif
        return NULL;
    }

    CFURLRef profileURL;
    CFDictionaryRef customProfileInfo =
        (CFDictionaryRef)CFDictionaryGetValue(displayInfo, kColorSyncCustomProfiles);
    if (customProfileInfo) {
        profileURL =
            (CFURLRef)CFDictionaryGetValue(customProfileInfo, defaultProfileID);
        if (!profileURL) {
#ifdef DEBUG
            cerr << "Cannot get display custom profile URL." << endl;
#endif
            return NULL;
        }
    }
    else {
        // try to use factoryInfo
        CFDictionaryRef factoryProfileInfo =
            (CFDictionaryRef)CFDictionaryGetValue(factoryInfo, defaultProfileID);
        if (!factoryProfileInfo) {
#ifdef DEBUG
            cerr << "Cannot get display factory profile info." << endl;
#endif
            return NULL;
        }

        profileURL =
            (CFURLRef)CFDictionaryGetValue(factoryProfileInfo, kColorSyncDeviceProfileURL);
        if (!profileURL) {
#ifdef DEBUG
            cerr << "Cannot get display factory profile URL." << endl;
#endif
            return NULL;
        }
    }

    char path[ PATH_MAX ];
    bool const result =
        CFURLGetFileSystemRepresentation(profileURL, true, (UInt8*)path, PATH_MAX);
    CFRelease(profileURL);
    if (!result) {
#ifdef DEBUG
        cerr << "Cannot get display profile path." << endl;
#endif
        return NULL;
    }
    char const *cPath = new_strdup( path );
#else // for Snow Leopard or older
    CMError err;
    CMProfileRef profRef;
    CMProfileLocation profLoc;
    UInt32 locSize = cmCurrentProfileLocationSize;

    CGDirectDisplayID const displayID = CGMainDisplayID();
    err = CMGetProfileByAVID( (CMDisplayIDType)displayID, &profRef );
    if ( err != noErr )
        return NULL;
    err = NCMGetProfileLocation( profRef, &profLoc, &locSize );
    if ( err != noErr )
        return NULL;
    char const *const cPath = getProfilePath( profLoc );
    CMCloseProfile( profRef );
    if ( !cPath )
        return NULL;
#endif
    jstring const jPath = env->NewStringUTF( cPath );
    delete[] cPath;
    return jPath;
}

/* vim:set et sw=4 ts=4: */
