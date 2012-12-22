/**
 * Implement a progress indicator dialog as a sheet.
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// local
#import "LC_Arg.h"
#import "LC_CocoaUtils.h"
#import "LC_CPPUtils.h"
#import "LC_JNIUtils.h"

#import "ProgressController.h"
#import "javah/com_lightcrafts_platform_macosx_MacOSXProgressDialog.h"

using namespace LightCrafts;

///////////////////////////////////////////////////////////////////////////////

@interface ProgressSheetInfo : NSObject {
@public
    jobject             m_ProgressListener_object;
    NSString*           m_message;
    BOOL                m_indeterminate;
    int                 m_minValue, m_maxValue;
    BOOL                m_hasCancelButton;
    ProgressController* m_controller;
}

- (void) dealloc;

- (void) endSheet:
    (NSPanel*)panel;

- (void) incrementBy:
    (LC_Arg*)arg;

- (id) initWithListener:
    (jobject)listener
    message:(NSString*)message
    indeterminate:(BOOL)indeterminate
    minValue:(int)minValue
    maxValue:(int)maxValue
    hasCancelButton:(BOOL)hasCancelButton
    env:(JNIEnv*)env;

- (void) setIndeterminate:
    (LC_Arg*)arg;

- (void) setMaxValue:
    (LC_Arg*)arg;

- (void) setMinValue:
    (LC_Arg*)arg;

- (void) sheetEnded:
    (NSPanel*)sheet
    returnCode:(int)returnCode
    contextInfo:(void*)contextInfo;

- (void) showProgressForWindow:
    (NSWindow*)parentWindow;

@end

////////// Local functions ////////////////////////////////////////////////////

/**
 * Get the pointer value previously stored inside the MacOSXProgressDialog
 * object on the Java side.
 */
static void* getNativePtr( JNIEnv *env, jobject jMacOSXProgressDialog ) {
    auto_jclass const jClass( env, jMacOSXProgressDialog );
    jfieldID const fieldID = env->GetFieldID( jClass, "m_nativePtr", "J" );
    if ( !fieldID )
        LC_throwIllegalStateException( env, "Can not find m_nativePtr" );
    void *const ptr = reinterpret_cast<void*>(
        env->GetLongField( jMacOSXProgressDialog, fieldID )
    );
    if ( !ptr )
        LC_throwIllegalStateException( env, "NULL m_nativePtr" );
    return ptr;
}

/**
 * Store a pointer inside the MacOSXProgressDialog object on the Java side.
 */
static void setNativePtr( JNIEnv *env, jobject jMacOSXProgressDialog,
                          void *ptr ) {
    auto_jclass const jClass( env, jMacOSXProgressDialog );
    jfieldID const fieldID = env->GetFieldID( jClass, "m_nativePtr", "J" );
    if ( !fieldID )
        LC_throwIllegalStateException( env, "Can not find m_nativePtr" );
    env->SetLongField(
        jMacOSXProgressDialog, fieldID, reinterpret_cast<jlong>( ptr )
    );
}

/**
 * Convenience method for getting the pointer to the ProgressSheetInfo stored
 * inside the MacOSXProgressDialog object on the Java side.
 */
inline ProgressSheetInfo*
getSheetInfo( JNIEnv *env, jobject jMacOSXProgressDialog ) {
    return reinterpret_cast<ProgressSheetInfo*>(
        getNativePtr( env, jMacOSXProgressDialog )
    );
}

/**
 * Convenience method for fetting the pointer to the ProgressController stored
 * via the MacOSXProgressDialog object on the Java side.
 */
inline ProgressController*
getProgressController( JNIEnv *env, jobject jMacOSXProgressDialog ) {
    return getSheetInfo( env, jMacOSXProgressDialog )->m_controller;
}

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXProgressDialog_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXProgressDialog,_,method)

#define MacOSXProgressDialog_CONSTANT(constant) \
        name3(com_lightcrafts_platform_macosx_MacOSXProgressDialog,_,constant)

static jclass MacOSXProgressDialog_class;
static jmethodID progressCanceled_methodID;
static char const* nibPath;

/**
 * Initialize a MacOSXProgressDialog's native side.
 */
JNIEXPORT void JNICALL MacOSXProgressDialog_METHOD(init)
    ( JNIEnv *env, jclass jClass, jstring jNIBPath )
{
    //
    // Cache class callback methods.
    //
    progressCanceled_methodID = env->GetStaticMethodID(
        jClass, "progressCanceledCallback",
        "(Lcom/lightcrafts/utils/ProgressListener;)V"
    );

    //
    // Prevent the MacOSXProgressDialog class from being unloaded so our
    // methodIDs stay valid.
    //
    MacOSXProgressDialog_class = (jclass)env->NewGlobalRef( jClass );

    jstring_to_c const cNIBPath( env, jNIBPath );
    nibPath = new_strdup( cNIBPath );
}

/**
 * Hide the sheet.  This is called when the ProgressThread on the Java side has
 * terminated naturally, i.e., it wasn't cancelled by the user clicking the
 * Cancel button.
 */
JNIEXPORT void JNICALL MacOSXProgressDialog_METHOD(hideSheet)
    ( JNIEnv *env, jobject jMacOSXProgressDialog )
{
    //
    // We have to end the sheet from the main AppKit thread, not whatever
    // arbitrary thread we happen to have been called on from the Java side.
    //
    [getSheetInfo( env, jMacOSXProgressDialog )
        performSelectorOnMainThread:@selector(endSheet:)
        withObject:getProgressController( env, jMacOSXProgressDialog )->panel
        waitUntilDone:YES
    ];
}

/**
 * Increment the progress indicator's value.  If the indicator is
 * indeterminate, do nothing.
 */
JNIEXPORT void JNICALL MacOSXProgressDialog_METHOD(incrementBy)
    ( JNIEnv *env, jobject jMacOSXProgressDialog, jint increment )
{
    // See comment in hideSheet.
    [getSheetInfo( env, jMacOSXProgressDialog )
        performSelectorOnMainThread:@selector(incrementBy:)
        withObject:[LC_Arg allocInt:increment]
        waitUntilDone:YES
    ];
}

/**
 * Set whether the progress indicator is indeterminate or not.
 */
JNIEXPORT void JNICALL MacOSXProgressDialog_METHOD(setIndeterminate)
    ( JNIEnv *env, jobject jMacOSXProgressDialog, jboolean indeterminate )
{
    // See comment in hideSheet.
    [getSheetInfo( env, jMacOSXProgressDialog )
        performSelectorOnMainThread:@selector(setIndeterminate:)
        withObject:[LC_Arg allocBool:indeterminate]
        waitUntilDone:YES
    ];
}

/**
 * Set the maximum value of the progress indicator.
 */
JNIEXPORT void JNICALL MacOSXProgressDialog_METHOD(setMaximum)
    ( JNIEnv *env, jobject jMacOSXProgressDialog, jint maxValue )
{
    // See comment in hideSheet.
    [getSheetInfo( env, jMacOSXProgressDialog )
        performSelectorOnMainThread:@selector(setMaxValue:)
        withObject:[LC_Arg allocInt:maxValue]
        waitUntilDone:YES
    ];
}

/**
 * Set the minimum value of the progress indicator.
 */
JNIEXPORT void JNICALL MacOSXProgressDialog_METHOD(setMinimum)
    ( JNIEnv *env, jobject jMacOSXProgressDialog, jint minValue )
{
    // See comment in hideSheet.
    [getSheetInfo( env, jMacOSXProgressDialog )
        performSelectorOnMainThread:@selector(setMinValue:)
        withObject:[LC_Arg allocInt:minValue]
        waitUntilDone:YES
    ];
}

/**
 * Show an progress sheet to the user.
 * This is the main entry-point from the Java side.
 */
JNIEXPORT void JNICALL MacOSXProgressDialog_METHOD(showNativeSheet)
    ( JNIEnv *env, jobject jMacOSXProgressDialog, jobject parent,
      jstring jMessage, jboolean indeterminate, jint minValue, jint maxValue,
      jboolean hasCancelButton, jobject listener )
{
    auto_obj<NSAutoreleasePool> pool;

    ProgressSheetInfo *const sheetInfo =
        [[ProgressSheetInfo alloc]
            initWithListener:listener
            message:LC_jstringToNSString( env, jMessage )
            indeterminate:indeterminate
            minValue:minValue
            maxValue:maxValue
            hasCancelButton:hasCancelButton
            env:env
        ];

    setNativePtr( env, jMacOSXProgressDialog, sheetInfo );

    // Bump the retain count on the delegate until the sheet goes away.
    [sheetInfo retain];

    // Show the sheet from the main AppKit thread.
    [sheetInfo
        performSelectorOnMainThread:@selector(showProgressForWindow:)
        withObject:LC_getNSWindowFromAWTComponent( env, parent )
        waitUntilDone:YES
    ];
}

////////// Objective C ////////////////////////////////////////////////////////

@implementation ProgressSheetInfo

/**
 * Initialize a ProgressSheetInfo.
 */
- (id) initWithListener:
    (jobject)listener
    message:(NSString*)message
    indeterminate:(BOOL)indeterminate
    minValue:(int)minValue
    maxValue:(int)maxValue
    hasCancelButton:(BOOL)hasCancelButton
    env:(JNIEnv*)env
{
    self = [super init];
    m_message = [message retain];
    m_indeterminate = indeterminate;
    m_minValue = minValue;
    m_maxValue = maxValue;
    m_hasCancelButton = hasCancelButton;

    //
    // Obtain a global ref to the Java listener for this sheet's results.  This
    // prevents the listener from being GC'd until we are done with it.
    //
    m_ProgressListener_object = env->NewGlobalRef( listener );
    if ( !m_ProgressListener_object )
        LC_throwIllegalStateException( env,
            "NewGlobalRef() failed for sheetListener"
        );
    return self;
}

/**
 * Deallocate a ProgressSheetInfo.
 */
- (void) dealloc
{
    [m_message release];
    [super dealloc];
}

/**
 * A proxy method called via performSelectorOnMainThread to call sheetEnded
 * with all the arguments it needs.
 */
- (void) endSheet:
    (NSPanel*)panel
{
    //
    // Even though the sheet doesn't have an OK button, we use its return code
    // to mean that the ProgressThread (and thus the sheet) are terminating
    // naturally, i.e., not because the user clicked the Cancel button.
    //
    [NSApp endSheet:panel returnCode:NSOKButton];
}

/**
 * Increment the progress indicator's value (only if it's determinate).
 */
- (void) incrementBy:
    (LC_Arg*)arg
{
    if ( ![m_controller->progressIndicator isIndeterminate] ) {
        [m_controller->progressIndicator incrementBy:arg->i];
        [m_controller->progressIndicator displayIfNeeded];
    }
    [arg release];
}

/**
 * Set whether the progress indicator is indeterminate.
 */
- (void) setIndeterminate:
    (LC_Arg*)arg
{
    [m_controller->progressIndicator setIndeterminate:arg->b];
    if ( arg->b ) {
        [m_controller->progressIndicator setUsesThreadedAnimation:YES];
        [m_controller->progressIndicator startAnimation:nil];
    } else
        [m_controller->progressIndicator stopAnimation:nil];
    [arg release];
}

/**
 * Set the maximum value of the progress indicator.
 */
- (void) setMaxValue:
    (LC_Arg*)arg
{
    [m_controller->progressIndicator setMaxValue:arg->i];
    [arg release];
}

/**
 * Set the minimum value of the progress indicator.
 */
- (void) setMinValue:
    (LC_Arg*)arg
{
    [m_controller->progressIndicator setMinValue:arg->i];
    [arg release];
}

/**
 * The panel has ended.  Make the sheet go away and notify the ProgressListener
 * only if the ProgressThread was terminated prematurely because the user
 * clicked the Cancel button.
 */
- (void) sheetEnded:
    (NSPanel*)panel
    returnCode:(int)returnCode
    contextInfo:(void*)contextInfo
{
    [panel orderOut:self];              // make the sheet go away

    auto_JNIEnv const env;
    if ( returnCode == NSCancelButton )
        env->CallStaticVoidMethod(
            MacOSXProgressDialog_class, progressCanceled_methodID,
            m_ProgressListener_object
        );

    // We're done with the listener; release the global ref.
    env->DeleteGlobalRef( m_ProgressListener_object );

    //
    // This delegate was was retained in showNativeSheet(); since this callback
    // occurs on the AppKit thread, which always has a pool in place, it can be
    // autoreleased rather than released.
    //
    [self autorelease];
}

/**
 * Show the progress sheet.
 */
- (void) showProgressForWindow:
    (NSWindow*)parentWindow
{
    // Load the NIB file.
    NSNib *const nib =
        [[NSNib alloc]
            initWithContentsOfURL:
                [NSURL fileURLWithPath:[NSString stringWithUTF8String:nibPath]]
        ];
    if ( !nib )
        LC_raiseIllegalStateException( @"Failed to initialize NIB." );

    // Get the top level objects from the NIB.
    NSMutableArray *topLevelObjects;
    BOOL ok =
        [nib
            instantiateNibWithOwner:parentWindow
            topLevelObjects:&topLevelObjects
        ];
    if ( !ok )
        LC_raiseIllegalStateException( @"Failed to instantiate NIB." );

    // Find the controller.
    for ( int i = [topLevelObjects count] - 1; i >= 0; --i ) {
        NSObject *const object = [topLevelObjects objectAtIndex:i];
        if ( [object isKindOfClass:[ProgressController class]] ) {
            m_controller = static_cast<ProgressController*>( object );
            break;
        }
    }
    if ( !m_controller )
        LC_raiseIllegalStateException( @"Couldn't find ProgressController." );

    // Tweak the various things in the dialog.
    if ( !m_hasCancelButton )
        [m_controller->cancelButton setEnabled:NO];
    [m_controller->message setStringValue:m_message];
    [m_controller->progressIndicator setIndeterminate:m_indeterminate];
    [m_controller->progressIndicator setMinValue:m_minValue];
    [m_controller->progressIndicator setMaxValue:m_maxValue];
    if ( m_indeterminate ) {
        [m_controller->progressIndicator setUsesThreadedAnimation:YES];
        [m_controller->progressIndicator startAnimation:nil];
    }

    // Make sure the panel can get mouse clicks no matter what.
    [m_controller->panel setWorksWhenModal:YES];

    [NSApp
        beginSheet:m_controller->panel
        modalForWindow:parentWindow
        modalDelegate:self
        didEndSelector:@selector(sheetEnded:returnCode:contextInfo:)
        contextInfo:nil
    ];
}

@end
/* vim:set et sw=4 ts=4: */
