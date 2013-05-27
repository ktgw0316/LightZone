/**
 * Smart Folder
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 *
 * See: http://lists.apple.com/archives/spotlight-dev/2005/Jun/msg00017.html
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
#include "javah/com_lightcrafts_platform_macosx_MacOSXSmartFolder.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXSmartFolder_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXSmartFolder,_,method)

/**
 * Perform a smart query to get the virtual contents of a "Smart Folder."
 */
JNIEXPORT jobjectArray JNICALL MacOSXSmartFolder_METHOD(smartQuery)
    ( JNIEnv *env, jclass, jstring jSavedSearchPathname )
{
    auto_obj<NSAutoreleasePool> pool;

    NSString *const nsSavedSearchPathname =
        LC_jstringToNSString( env, jSavedSearchPathname );
    NSDictionary *const dict =
        [NSDictionary dictionaryWithContentsOfFile:nsSavedSearchPathname];
    NSString *const rawQuery = [dict objectForKey:@"RawQuery"];
    if ( !rawQuery )
        return NULL;

    jobjectArray results = NULL;

    MDQueryRef queryRef =
        MDQueryCreate( kCFAllocatorDefault, (CFStringRef)rawQuery, NULL, NULL );
    Boolean queryRan = MDQueryExecute( queryRef, kMDQuerySynchronous );
    if ( !queryRan )
        goto done;

    {
        CFIndex const resultCount = MDQueryGetResultCount( queryRef );
        jclass const jString_class = LC_findClassOrDie( env, "java/lang/String" );
        results = env->NewObjectArray( resultCount, jString_class, NULL );
        if ( !results )
            goto done;

        int resultIndex;
        resultIndex = 0;
        for ( CFIndex i = 0; i < resultCount; ++i ) {
            MDItemRef itemRef = (MDItemRef)MDQueryGetResultAtIndex( queryRef, i );
            CFStringRef pathRef =
                (CFStringRef)MDItemCopyAttribute( itemRef, kMDItemPath );
            if ( pathRef ) {
                auto_jstring jPath( env,
                        LC_NSStringTojstring( env, (NSString*)pathRef )
                        );
                env->SetObjectArrayElement( results, resultIndex++, jPath );
                CFRelease( pathRef );
            }
        }
    }

done:
    CFRelease( queryRef );
    return results;
}
/* vim:set et sw=4 ts=4: */
