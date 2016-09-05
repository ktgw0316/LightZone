// standard
#import <Cocoa/Cocoa.h>
#ifdef LC_USE_JAWT
#import <JavaVM/jawt_md.h>
#else
#import <AppKit/AppKit.h>
#endif

// local
#import "LC_CocoaUtils.h"
#import "LC_JNIUtils.h"

using namespace LightCrafts;

/**
 * Convert a C string array to an NSArray* of NSString*.
 */
NSMutableArray* LC_cStringArrayToNSArray( char const *const cArray[], int n ) {
    NSMutableArray *const nsArray = [NSMutableArray arrayWithCapacity:n];
    for ( int i = 0; i < n; ++i )
        [nsArray addObject:[NSString stringWithUTF8String:cArray[i]]];
    return nsArray;
}

/**
 * Given a Java component, return an NSWindow*.  The component must have been
 * made visible first otherwise this function crashes.
 */
NSWindow* LC_getNSWindowFromAWTComponent( JNIEnv *env, jobject awtComponent ) {
    if ( !awtComponent )
        return nil;

#ifdef LC_USE_JAWT
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if ( !JAWT_GetAWT( env, &awt ) )
        return nil;

    JAWT_DrawingSurface *const ds = awt.GetDrawingSurface( env, awtComponent );
    if ( !ds )
        return nil;

    NSWindow *nsWindow = nil;

    jint const lock = ds->Lock( ds );
    if ( lock & JAWT_LOCK_ERROR )
        goto error_1;

    {
        JAWT_DrawingSurfaceInfo *const dsi = ds->GetDrawingSurfaceInfo( ds );
        if ( !dsi )
            goto error_2;

        { // local scope
            JAWT_MacOSXDrawingSurfaceInfo const *const dsiMac =
                static_cast<JAWT_MacOSXDrawingSurfaceInfo const*>(
                        dsi->platformInfo
                        );
            nsWindow = [dsiMac->cocoaViewRef window];
        }

        ds->FreeDrawingSurfaceInfo( dsi );
    }
error_2:
    ds->Unlock( ds );
error_1:
    awt.FreeDrawingSurface( ds );
    return nsWindow;
#else
    return [[NSApplication sharedApplication] keyWindow];
#endif /* LC_USE_JAWT */
}

/**
 * Convert an NSString* to a jstring.
 */
jstring LC_NSStringTojstring( JNIEnv *env, NSString *in ) {
    if ( !in )
        return nil;
    char const *cstring = [in UTF8String];
    return env->NewStringUTF( cstring );
}

/**
 * Convert a jstring to an NSString*.
 */
NSString* LC_jstringToNSString( JNIEnv *env, jstring in ) {
    if ( !in )
        return nil;
    jstring_to_c const cString( env, in );
    return [NSString stringWithUTF8String:cString];
}

/**
 * Convert a jobjectArray of String[] to an NSArray* of NSString*.
 */
NSMutableArray* LC_jStringArrayToNSArray( JNIEnv *env, jobjectArray jArray ) {
    if ( !jArray )
        return nil;
    int const n = env->GetArrayLength( jArray );
    NSMutableArray *const nsArray = [NSMutableArray arrayWithCapacity:n];
    for ( int i = 0; i < n; ++i ) {
        jstring js = (jstring)env->GetObjectArrayElement( jArray, i );
        if ( js )
            [nsArray addObject:LC_jstringToNSString( env, js )];
        else
            [nsArray addObject:@"nil"];
    }
    return nsArray;
}

/**
 * Raise a Cocoa IllegalStateException.
 */
void LC_raiseIllegalStateException( NSString *msg ) {
    [NSException raise:@"IllegalStateException" format:msg];
}

/* vim:set et sw=4 ts=4: */
