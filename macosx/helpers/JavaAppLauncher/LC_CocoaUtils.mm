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
    else {
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
    return  [[NSApplication sharedApplication] keyWindow];
#endif /* LC_USE_JAWT */
}

/* vim:set et sw=4 ts=4: */
