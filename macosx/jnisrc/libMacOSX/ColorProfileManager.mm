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

////////// Objective C interface //////////////////////////////////////////////

@interface ColorProfileInfo : NSObject {
    NSString *m_name;
    NSString *m_path;
}

- (id) initWithName:
    (NSString*)name
    path:(NSString*)path;

- (NSString*) name;

- (NSString*) path;

@end

@interface ProfIterProcArg : NSObject {
@public
    NSMutableArray *m_profileArray;
    OSType m_profileClass;
}

@end

////////// Objective C implementation /////////////////////////////////////////

@implementation ColorProfileInfo

- (id) initWithName:
    (NSString*)name
    path:(NSString*)path
{
    self = [super init];
    m_name = [name retain];
    m_path = [path retain];
    return self;
}

- (void) dealloc
{
    [m_name release];
    [m_path release];
    [super dealloc];
}

- (NSString*) name
{
    return m_name;
}

- (NSString*) path
{
    return m_path;
}

@end

@implementation ProfIterProcArg

@end

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

/**
 * This function is a callback for CMIterateColorSyncFolder().
 */
extern "C" OSErr profIterProc( CMProfileIterateData *data, void *vArg ) {
    CMProfileLocation const profileLoc = data->location;
    switch ( profileLoc.locType ) {
#ifndef __LP64__
        case cmFileBasedProfile:
#endif
        case cmPathBasedProfile:
            ProfIterProcArg *const arg = static_cast<ProfIterProcArg*>( vArg );
            if ( data->header.profileClass != arg->m_profileClass )
                break;
            if ( !data->uniCodeNameCount )
                break;
            char const *const path = getProfilePath( profileLoc );
            if ( !path )
                break;

            ColorProfileInfo *const cpi =
                [[ColorProfileInfo alloc]
                    initWithName:
                        [NSString
                            stringWithCharacters:data->uniCodeName
                            length:data->uniCodeNameCount]
                    path:[NSString stringWithUTF8String:path]];
            [arg->m_profileArray addObject:cpi];

            delete[] path;
            break;
    }
    return noErr;
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

/**
 * Search color profiles for a given class.
 */
JNIEXPORT jobjectArray JNICALL
MacOSXColorProfileManager_METHOD(searchProfilesForImpl)
    ( JNIEnv *env, jclass, jint profileClassID )
{
    auto_obj<NSAutoreleasePool> pool;

    NSMutableArray *const profileArray = [NSMutableArray arrayWithCapacity:0];

    ProfIterProcArg *const arg = [ProfIterProcArg alloc];
    arg->m_profileArray = profileArray;
    arg->m_profileClass = profileClassID;

    UInt32 seed = 0;
    // UInt32 count = 0;
    // CMError err = CMIterateColorSyncFolder( profIterProc, &seed, &count, arg );
    CFErrorRef err;
    ColorSyncIterateInstalledProfiles((ColorSyncProfileIterateCallback)profIterProc, &seed, arg, &err);
    int const profileCount = [profileArray count];
    if ( err || !profileCount )
        return NULL;
    //
    // Convert the array of ColorProfileInfo objects into a "flat" array of
    // Strings because it's easier to return via JNI.
    //
    int j = 0;
    jclass const jString_class = LC_findClassOrDie( env, "java/lang/String" );
    jobjectArray jArray =
        env->NewObjectArray( profileCount * 2, jString_class, NULL );
    if ( !jArray )
        goto error;

#ifdef DEBUG
    cerr << "profileCount=" << profileCount << endl;
#endif
    for ( int i = 0; i < profileCount; ++i ) {
        ColorProfileInfo *const cpi = [profileArray objectAtIndex:i];
#ifdef DEBUG
        cerr << "color profile name=" << [[cpi name] UTF8String] << endl;
        cerr << "              path=" << [[cpi path] UTF8String] << endl;
#endif
        auto_jstring js( env, LC_NSStringTojstring( env, [cpi name] ) );
        if ( !js )
            goto error;
        env->SetObjectArrayElement( jArray, j++, js );
        js = LC_NSStringTojstring( env, [cpi path] );
        if ( !js )
            goto error;
        env->SetObjectArrayElement( jArray, j++, js );
    }
    return jArray;

error:
    return NULL;
}
/* vim:set et sw=4 ts=4: */
