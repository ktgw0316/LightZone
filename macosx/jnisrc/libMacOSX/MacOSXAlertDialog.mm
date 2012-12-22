/**
 * Implementation file for the native side of the Sheets-in-Java example using
 * JAWT to attach a sheet to a Swing JFrame's NSWindow peer.
 * 
 * This code is based on sample code provided by Apple Computer.
 * http://developer.apple.com/samplecode/JSheets/JSheets.html
 */

// local
#import "LC_JNIUtils.h"
#import "LC_CocoaUtils.h"
#import "javah/com_lightcrafts_platform_macosx_MacOSXAlertDialog.h"

using namespace LightCrafts;

///////////////////////////////////////////////////////////////////////////////

@interface AlertSheetInfo : NSObject {
    jobject         m_AlertListener_object;
    NSAlertStyle    m_alertStyle;
    NSString*       m_msgText;
    NSString*       m_infoText;
    NSArray*        m_buttons;
}

- (void) dealloc;

- (id) initWithListener:
    (jobject)listener
    alertStyle:(NSAlertStyle)alertStyle
    msgText:(NSString*)msgText
    infoText:(NSString*)infoText
    buttons:(NSArray*)buttons
    env:(JNIEnv*)env;

- (void) showAlertForWindow:
    (NSWindow*)parentWindow;

- (void) alertEnded:
    (NSAlert*)sheet
    returnCode:(int)returnCode
    contextInfo:(void*)contextInfo;

@end

///////////////////////////////////////////////////////////////////////////////

#define MacOSXAlertDialog_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXAlertDialog,_,method)

#define MacOSXAlertDialog_CONSTANT(constant) \
        name3(com_lightcrafts_platform_macosx_MacOSXAlertDialog,_,constant)

static jclass MacOSXAlertDialog_class;
static jmethodID sheetDone_methodID;

/**
 * Initialize a MacOSXAlertDialog.
 */
JNIEXPORT void JNICALL MacOSXAlertDialog_METHOD(init)
    ( JNIEnv *env, jclass clazz )
{
    //
    // Cache class callback methods.
    //
    sheetDone_methodID = env->GetStaticMethodID(
        clazz, "sheetDoneCallback",
        "(Lcom/lightcrafts/platform/macosx/sheets/AlertListener;I)V"
    );

    //
    // Prevent the MacOSXAlertDialog class from being unloaded so our
    // methodIDs stay valid.
    //
    MacOSXAlertDialog_class = (jclass)env->NewGlobalRef( clazz );
}

/**
 * Show an alert sheet to the user.
 * This is the main entry-point from the Java side.
 */
JNIEXPORT void JNICALL MacOSXAlertDialog_METHOD(showNativeSheet)
    ( JNIEnv *env, jclass caller, jint alertType, jobject parent,
      jstring msgText, jstring infoText, jobjectArray buttons,
      jobject listener )
{
    auto_obj<NSAutoreleasePool> pool;

    //
    // Map the Java-side alert-style constants to those used by NSAlert.
    //
    NSAlertStyle alertStyle;
    switch ( alertType ) {
        case MacOSXAlertDialog_CONSTANT(WARNING_ALERT):
            alertStyle = NSWarningAlertStyle;
            break;
        case MacOSXAlertDialog_CONSTANT(ERROR_ALERT):
            alertStyle = NSCriticalAlertStyle;
            break;
        default:
            LC_throwIllegalArgumentException( env, "bad alertType value" );
    }

    AlertSheetInfo *const sheetInfo =
        [[AlertSheetInfo alloc]
            initWithListener:listener
            alertStyle:alertStyle
            msgText:LC_jstringToNSString( env, msgText )
            infoText:LC_jstringToNSString( env, infoText )
            buttons:LC_jStringArrayToNSArray( env, buttons )
            env:env
        ];

    // Bump the retain count on the delegate until the sheet goes away.
    [sheetInfo retain];

    //
    // It is extremely important to show the sheet from the main AppKit thread
    // WITHOUT BLOCKING using performSelectorOnMainThread with a waitUntilDone
    // value of NO.
    //
    [sheetInfo
        performSelectorOnMainThread:@selector(showAlertForWindow:)
        withObject:LC_getNSWindowFromAWTComponent( env, parent )
        waitUntilDone:NO
    ];
}

///////////////////////////////////////////////////////////////////////////////

@implementation AlertSheetInfo

/**
 * Initialize an AlertSheetInfo.
 */
- (id) initWithListener:
    (jobject)listener
    alertStyle:(NSAlertStyle)alertStyle
    msgText:(NSString*)msgText
    infoText:(NSString*)infoText
    buttons:(NSArray*)buttons
    env:(JNIEnv*)env
{
    self = [super init];

    m_alertStyle = alertStyle;
    //
    // We need to make this stuff stick around until the sheet is done
    // otherwise it will go away once showNativeSheet() returns (since it's
    // non-blocking) and that will cause the app. to crash.  We need to release
    // this stuff in the dealloc() method.
    //
    m_msgText  = [msgText retain];
    m_infoText = [infoText retain];
    m_buttons  = [buttons retain];

    //
    // I don't think this global reference is necessary because the code on the
    // Java side is now in wait() so there's a strong reference to the sheet
    // listener; but I don't think it hurts anything.  The original Apple
    // comment is:
    //
    //      Obtain a global ref to the Java listener for this sheet's results.
    //      This prevents the listener from being GC'd until we are done with
    //      it.
    //
    m_AlertListener_object = env->NewGlobalRef( listener );
    if ( !m_AlertListener_object )
        LC_throwIllegalStateException( env,
            "NewGlobalRef() failed for sheetListener"
        );
    return self;
}

/**
 * Deallocate an AlertSheetInfo.
 */
- (void) dealloc
{
    [m_buttons release];
    [m_infoText release];
    [m_msgText release];
    [super dealloc];
}

/**
 * Given a Java-derived NSWindow, show an NSAlert as a sheet.
 * Register a response selector so Java can be called back with the resulting
 * filename.
 */
- (void) showAlertForWindow:
    (NSWindow*)parentWindow
{
    NSAlert *const alert = [[NSAlert alloc] init];
    [alert setAlertStyle:m_alertStyle];
    [alert setShowsHelp:NO];
    if ( m_msgText )
        [alert setMessageText:m_msgText];
    if ( m_infoText )
        [alert setInformativeText:m_infoText];

    int const n = [m_buttons count];
    for ( int i = 0; i < n; ++i )
        [alert addButtonWithTitle:[m_buttons objectAtIndex:i]];

    [alert
        beginSheetModalForWindow:parentWindow
        modalDelegate:self
        didEndSelector:@selector(alertEnded:returnCode:contextInfo:)
        contextInfo:nil
    ];
}

/**
 * The alert has ended because the user clicked a button.  Call back to Java
 * with the index of the button the user clicked.
 */
- (void) alertEnded:
    (NSAlert*)sheet
    returnCode:(int)returnCode
    contextInfo:(void*)contextInfo
{
    auto_JNIEnv const env;

    int buttonClicked;
    switch ( returnCode )  {
        case NSAlertFirstButtonReturn:
            buttonClicked = 0;
            break;
        case NSAlertSecondButtonReturn:
            buttonClicked = 1;
            break;
        case NSAlertThirdButtonReturn:
            buttonClicked = 2;
            break;
        default:
            buttonClicked = returnCode - NSAlertThirdButtonReturn + 2;
            break;
    }

    env->CallStaticVoidMethod(
        MacOSXAlertDialog_class, sheetDone_methodID, m_AlertListener_object,
        buttonClicked
    );

    // We're done with the listener; release the global ref.
    env->DeleteGlobalRef( m_AlertListener_object );

    //
    // This delegate was was retained in showNativeSheet(); since this callback
    // occurs on the AppKit thread, which always has a pool in place, it can be
    // autoreleased rather than released.
    //
    [self autorelease];
}

@end
/* vim:set et sw=4 ts=4: */
