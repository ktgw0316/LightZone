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
    jstring const jPath = env->NewStringUTF( cPath );
    delete[] cPath;
    return jPath;
}

/* vim:set et sw=4 ts=4: */
