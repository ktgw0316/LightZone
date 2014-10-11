/**
 * Implementation file for the native side of the Sheets-in-Java example using
 * JAWT to attach a sheet to a Swing JFrame's NSWindow peer.
 *
 * This code is based on sample code provided by Apple Computer.
 * http://developer.apple.com/samplecode/JSheets/
 */

// local
#import "ExportController.h"
#import "KeyStrokeNumberFormatter.h"
#import "LC_CocoaUtils.h"
#import "LC_CPPUtils.h"
#import "LC_JNIUtils.h"
#import "ResizeListener.h"
#import "javah/com_lightcrafts_platform_macosx_sheets_SaveExportSheet.h"

// If defined, return to using sheets the way they were, i.e., modeless.
/*#define USE_BEGIN_SHEET /**/

// If defined, use actual sheets (window modal); otherwise regular modal.
/*#define USE_ACTUAL_SHEET /**/

using namespace LightCrafts;

///////////////////////////////////////////////////////////////////////////////

@interface SheetInfo : NSObject {
    NSString*           m_windowTitle;
    NSString*           m_initDirName;
    NSString*           m_initFileName;
    jobject             m_SaveExportListener_object;

    int                 m_sheetType;

    // Used only by the save sheet.
    bool                m_multilayer;

    // Used only by the save & export sheets.
    int                 m_bitsPerChannel;
    bool                m_blackPointCompensation;
    ExportController*   m_controller;
    NSArray*            m_colorProfileNames;
    NSString*           m_defaultColorProfileName;
    NSString*           m_defaultExtension;
    int                 m_jpegQuality;
    bool                m_lzwCompression;
    NSString*           m_nibPath;
    int                 m_originalWidth;
    int                 m_originalHeight;
    int                 m_renderingIntent;
    int                 m_resizeWidth;
    int                 m_resizeHeight;
    int                 m_resolution;
    int                 m_resolutionUnit;
    NSString*           m_saveButtonLabel;
}

- (id) initWithListener:
    (jobject)listener
    sheetType:(int)sheetType
    windowTitle:(NSString*)windowTitle
    saveButtonLabel:(NSString*)saveButtonLabel
    initDirName:(NSString*)initDirName
    initFileName:(NSString*)initFileName
    defaultExtension:(NSString*)defaultExtension
    bitsPerChannel:(int)bitsPerChannel
    blackPointCompensation:(BOOL)blackPointCompensation
    colorProfileNames:(NSArray*)colorProfileNames
    defaultColorProfileName:(NSString*)defaultColorProfileName
    jpegQuality:(int)jpegQuality
    lzwCompression:(BOOL)lzwCompression
    multilayer:(BOOL)multilayer
    nibPath:(NSString*)nibPath
    originalWidth:(int)originalWidth
    originalHeight:(int)originalHeight
    renderingIntent:(int)renderingIntent
    resizeWidth:(int)resizeWidth
    resizeHeight:(int)resizeHeight
    resolution:(int)resolution
    resolutionUnit:(int)resolutionUnit
    env:(JNIEnv*)env;

- (void) dealloc;

- (void) showSaveOrExportPanelForWindow:
    (NSWindow*)parentWindow;

- (void) saveOrExportPanelEnded:
    (NSSavePanel*)panel
    returnCode:(int)returnCode
    contextInfo:(void*)contextInfo;

@end

////////// Local functions ////////////////////////////////////////////////////

/**
 * Encode an export option: take the label and the integer value and construct
 * a jstring of the form <label>:<value>.
 */
static jstring encodeExportOption( JNIEnv *env, NSString const *label,
                                   int value ) {
    NSNumber *n = [NSNumber numberWithInt:value];
    NSString *const s = [label stringByAppendingString:[n stringValue]];
    return LC_NSStringTojstring( env, s );
}

/**
 * Encode an export option: take the label and the integer value and construct
 * a jstring of the form <label>:<value>.
 */
inline jstring encodeExportOption( JNIEnv *env, NSString const *label,
                                   NSString *value ) {
    NSString *const s = [label stringByAppendingString:value];
    return LC_NSStringTojstring( env, s );
}

/**
 * Encode an export option: take the label and the integer value and construct
 * a jstring of the form <label>:<value>.
 */
inline jstring encodeExportOption( JNIEnv *env, NSString const *label,
                                   char const *value ) {
    return encodeExportOption(
        env, label, [NSString stringWithUTF8String:value]
    );
}

/**
 * Load a NIB file and return its controller.
 */
id LC_loadNIB( NSString *nibPath, NSSavePanel *panel, Class controllerClass ) {
    // Load the NIB file.
    NSNib *const nib =
        [[NSNib alloc] initWithContentsOfURL:[NSURL fileURLWithPath:nibPath]];
    if ( !nib )
        LC_raiseIllegalStateException( @"Failed to initialize NIB." );

    // Get the top level objects from the NIB.
    NSMutableArray *topLevelObjects;
    if ( ![nib instantiateNibWithOwner:panel topLevelObjects:&topLevelObjects] )
        LC_raiseIllegalStateException( @"Failed to instantiate NIB." );

    // Find the controller.
    for ( int i = [topLevelObjects count] - 1; i >= 0; --i ) {
        NSObject *const object = [topLevelObjects objectAtIndex:i];
        if ( [object isKindOfClass:controllerClass] )
            return object;
    }
    LC_raiseIllegalStateException( @"Couldn't find controller." );
}

////////// JNI ////////////////////////////////////////////////////////////////

#define SaveExportSheet_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_sheets_SaveExportSheet,_,method)

#define SaveExportSheet_CONSTANT(constant) \
        name3(com_lightcrafts_platform_macosx_sheets_SaveExportSheet,_,constant)

static jclass SaveExportSheet_class;
static jmethodID sheetOK_methodID, sheetCanceled_methodID;

/**
 * Initialize a SaveExportSheet.
 */
JNIEXPORT void JNICALL SaveExportSheet_METHOD(init)
    ( JNIEnv *env, jclass clazz )
{
    //
    // Cache class callback methods.
    //
    sheetOK_methodID = env->GetStaticMethodID(
        clazz, "sheetOKCallback",
        "(Lcom/lightcrafts/platform/macosx/sheets/SaveExportListener;Ljava/lang/String;[Ljava/lang/String;)V"
    );
    sheetCanceled_methodID = env->GetStaticMethodID(
        clazz, "sheetCanceledCallback",
        "(Lcom/lightcrafts/platform/macosx/sheets/SaveExportListener;)V"
    );

    //
    // Prevent the SaveExportSheet class from being unloaded so our methodIDs
    // stay valid.
    //
    SaveExportSheet_class = (jclass)env->NewGlobalRef( clazz );
}

/**
 * Show a sheet to the user.
 * This is the main entry-point from the Java side.
 */
JNIEXPORT void JNICALL SaveExportSheet_METHOD(showNativeSheet)
    ( JNIEnv *env, jclass caller, jint sheetType, jobject parent,
      jstring windowTitle, jstring saveButtonLabel, jstring initDirName,
      jstring initFileName, jstring defaultExtension, jint bitsPerChannel,
      jboolean blackPointCompensation, jobjectArray colorProfileNames, jstring
      defaultColorProfileName, jint jpegQuality, jboolean lzwCompression,
      jboolean multilayer, jint originalWidth, jint originalHeight, jint
      renderingIntent, jint resizeWidth, jint resizeHeight, jint resolution,
      jint resolutionUnit, jstring nibPath, jobject listener )
{
    auto_obj<NSAutoreleasePool> pool;

    SheetInfo *const sheetInfo =
        [[SheetInfo alloc]
            initWithListener:listener
            sheetType:sheetType
            windowTitle:LC_jstringToNSString( env, windowTitle )
            saveButtonLabel:LC_jstringToNSString( env, saveButtonLabel )
            initDirName:LC_jstringToNSString( env, initDirName )
            initFileName:LC_jstringToNSString( env, initFileName )
            defaultExtension:LC_jstringToNSString( env, defaultExtension )
            bitsPerChannel:bitsPerChannel
            blackPointCompensation:blackPointCompensation
            colorProfileNames:LC_jStringArrayToNSArray( env, colorProfileNames )
            defaultColorProfileName:
                LC_jstringToNSString( env, defaultColorProfileName )
            jpegQuality:jpegQuality
            lzwCompression:lzwCompression
            multilayer:multilayer
            nibPath:LC_jstringToNSString( env, nibPath )
            originalWidth:originalWidth
            originalHeight:originalHeight
            renderingIntent:renderingIntent
            resizeWidth:resizeWidth
            resizeHeight:resizeHeight
            resolution:resolution
            resolutionUnit:resolutionUnit
            env:env
        ];

    // Bump the retain count on the delegate until the sheet goes away.
    [sheetInfo retain];

    //
    // Take the parent component (passed via Java call) and get the parent
    // NSWindow from it.
    //
    NSWindow *parentWindow = LC_getNSWindowFromAWTComponent( env, parent );

    //
    // It is extremely important to show the sheet from the main AppKit thread
    // WITHOUT BLOCKING using performSelectorOnMainThread with a waitUntilDone
    // value of NO.
    //
    [sheetInfo
        performSelectorOnMainThread:@selector(showSaveOrExportPanelForWindow:)
        withObject:parentWindow
        waitUntilDone:NO
    ];
}

////////// Objective C ////////////////////////////////////////////////////////

@implementation SheetInfo

/**
 * Initialize a SheetInfo.
 */
- (id) initWithListener:
    (jobject)listener
    sheetType:(int)sheetType
    windowTitle:(NSString*)windowTitle
    saveButtonLabel:(NSString*)saveButtonLabel
    initDirName:(NSString*)initDirName
    initFileName:(NSString*)initFileName
    defaultExtension:(NSString*)defaultExtension
    bitsPerChannel:(int)bitsPerChannel
    blackPointCompensation:(BOOL)blackPointCompensation
    colorProfileNames:(NSArray*)colorProfileNames
    defaultColorProfileName:(NSString*)defaultColorProfileName
    jpegQuality:(int)jpegQuality
    lzwCompression:(BOOL)lzwCompression
    multilayer:(BOOL)multilayer
    nibPath:(NSString*)nibPath
    originalWidth:(int)originalWidth
    originalHeight:(int)originalHeight
    renderingIntent:(int)renderingIntent
    resizeWidth:(int)resizeWidth
    resizeHeight:(int)resizeHeight
    resolution:(int)resolution
    resolutionUnit:(int)resolutionUnit
    env:(JNIEnv*)env
{
    self = [super init];

    //
    // We need to make this stuff stick around until the sheet is done
    // otherwise it will go away once showNativeSheet() returns (since it's
    // non-blocking) and that will cause the app. to crash.  We need to release
    // this stuff in the dealloc() method.
    //

    // All sheets.
    m_initDirName             = [initDirName retain];
    m_initFileName            = [initFileName retain];
    m_sheetType               = sheetType;
    m_windowTitle             = [windowTitle retain];

    // Save sheet only.
    m_multilayer              = multilayer;

    // Save and export sheets only.
    m_bitsPerChannel          = bitsPerChannel;
    m_blackPointCompensation  = blackPointCompensation;
    m_colorProfileNames       = colorProfileNames ?
                                    [colorProfileNames retain] : nil;
    m_controller              = nil;
    m_defaultColorProfileName = defaultColorProfileName ?
                                    [defaultColorProfileName retain] : nil;
    m_defaultExtension        = defaultExtension ?
                                    [defaultExtension retain] : nil;
    m_jpegQuality             = jpegQuality;
    m_lzwCompression          = lzwCompression;
    m_nibPath                 = [nibPath retain];
    m_originalHeight          = originalHeight;
    m_originalWidth           = originalWidth;
    m_renderingIntent         = renderingIntent;
    m_resizeHeight            = resizeHeight;
    m_resizeWidth             = resizeWidth;
    m_resolution              = resolution;
    m_resolutionUnit          = resolutionUnit;
    m_saveButtonLabel         = saveButtonLabel ?
                                    [saveButtonLabel retain] : nil;

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
    m_SaveExportListener_object = env->NewGlobalRef( listener );
    if ( !m_SaveExportListener_object )
        LC_throwIllegalStateException( env,
            "NewGlobalRef() failed for sheetListener"
        );
    return self;
}

/**
 * Deallocate a SheetInfo.
 */
- (void) dealloc
{
    [m_colorProfileNames release];
    [m_defaultColorProfileName release];
    [m_defaultExtension release];
    [m_initDirName release];
    [m_initFileName release];
    [m_nibPath release];
    [m_saveButtonLabel release];
    [m_windowTitle release];
    [super dealloc];
}

/**
 * Given a Java-derived NSWindow, show an NSSavePanel as a sheet.
 * Register a response selector so Java can be called back with the resulting
 * filename.
 */
- (void) showSaveOrExportPanelForWindow:
    (NSWindow*)parentWindow
{
    NSSavePanel *const panel = [NSSavePanel savePanel];
    [panel setTitle:m_windowTitle];
    [panel setAllowsOtherFileTypes:NO];
    [panel setCanCreateDirectories:YES];
    [panel setCanSelectHiddenExtension:YES];
    [panel setExtensionHidden:NO];
    [panel setPrompt:m_saveButtonLabel];
    [panel setRequiredFileType:@"lzn"];

    // Load the NIB file.
    m_controller = (ExportController*)
        LC_loadNIB( m_nibPath, panel, [ExportController class] );

    // Set the controller's panel to the one we created.
    m_controller->m_panel = panel;

    // Set the default subview values in the custom view.
    if ( m_defaultExtension != nil ) {

        char tag = 'L';
        if ( [m_defaultExtension isEqualToString:@"jpg"] )
            tag = 'J';
        else if ( [m_defaultExtension isEqualToString:@"tif"] )
            tag = 'T';

        [m_controller->fileTypePopUp selectItemWithTag:tag];
        [m_controller setImageType:tag];

    } else {
        [m_controller setImageType:'L'];
    }

    // Set the bit per channel pop-up menu.
    [m_controller->bitsPerChannelPopUp selectItemWithTag:m_bitsPerChannel];

    // Set the black-point compensation checkbox.
    [m_controller->blackPointCheckBox
        setState:m_blackPointCompensation ? NSOnState: NSOffState];

    // Fix file-type pop-up menu and multilayer checkbox
    if ( m_sheetType == SaveExportSheet_CONSTANT(EXPORT_SHEET) ) {
        [m_controller->fileTypePopUp removeItemAtIndex:0];
        [m_controller->multilayerCheckBox setHidden:TRUE];
    } else {
        // Set the multilayer checkbox.
        [m_controller->multilayerCheckBox
            setState:m_multilayer ? NSOnState: NSOffState];
    }

    // Set the LZW compression checkbox.
    [m_controller->lzwCompressionCheckBox
        setState:m_lzwCompression ? NSOnState: NSOffState];

    // Set the resolution text field.
    NSTextField *const resolutionField = m_controller->resolutionField;
    [resolutionField setIntValue:m_resolution];
    KeyStrokeNumberFormatter *const nFormatter =
        [[[KeyStrokeNumberFormatter alloc] init] autorelease];
    [nFormatter setFormat:@"0"];
    [[resolutionField cell] setFormatter:nFormatter];

    // Set the resolution unit pop-up menu.
    [m_controller->resolutionUnitPopUp selectItemWithTag:m_resolutionUnit];

    // Set the JPEG quality slider/text.
    [m_controller->qualitySlider setIntValue:m_jpegQuality];
    [m_controller->qualityField setIntValue:m_jpegQuality];

    // Set the rendering intent.
    [m_controller->renderingIntentPopUp selectItemWithTag:m_renderingIntent];

    ////////// Add the color profiles to the Color Profile menu ///////////////

    NSPopUpButton *const colorProfilePopUp = m_controller->colorProfilePopUp;
    [colorProfilePopUp removeAllItems];
    int const n = [m_colorProfileNames count];
    for ( int i = 0; i < n; ++i ) {
        NSString *const name = [m_colorProfileNames objectAtIndex:i];
        if ( [name isEqualToString:@"nil"] )
            [[colorProfilePopUp menu] addItem:[NSMenuItem separatorItem]];
        else
            [colorProfilePopUp addItemWithTitle:name];
    }
    if ( m_defaultColorProfileName )
        [colorProfilePopUp selectItemWithTitle:m_defaultColorProfileName];

    ////////// Configure the Resize width (X) and height (Y) text fields //////

    NSTextField *const resizeXField = m_controller->resizeXField;
    NSTextField *const resizeYField = m_controller->resizeYField;

    ResizeListener *const xListener =
        [ResizeListener
            allocForField:resizeYField
            aspectRatio:(double)m_resizeHeight / m_resizeWidth];
    ResizeListener *const yListener =
        [ResizeListener
            allocForField:resizeXField
            aspectRatio:(double)m_resizeWidth / m_resizeHeight];

    KeyStrokeNumberFormatter *const xFormatter =
        [[[KeyStrokeNumberFormatter alloc] init] autorelease];
    KeyStrokeNumberFormatter *const yFormatter =
        [[[KeyStrokeNumberFormatter alloc] init] autorelease];

    [xFormatter setFormat:@"0"];
    [xFormatter setListener:xListener];
    [yFormatter setFormat:@"0"];
    [yFormatter setListener:yListener];

    [resizeXField setIntValue:m_resizeWidth];
    [resizeYField setIntValue:m_resizeHeight];

    [[resizeXField cell] setFormatter:xFormatter];
    [[resizeYField cell] setFormatter:yFormatter];

    m_controller->m_originalWidth  = m_originalWidth;
    m_controller->m_originalHeight = m_originalHeight;

    ////////// Display the sheet //////////////////////////////////////////////

    // Set the save panel accessory view to the custom view.
    [panel setAccessoryView:m_controller->mainView];

#ifdef USE_BEGIN_SHEET
    [panel
        beginSheetForDirectory:m_initDirName
        file:m_initFileName
#       ifdef USE_ACTUAL_SHEET
            modalForWindow:parentWindow
#       else
            modalForWindow:nil
#       endif
        modalDelegate:self
        didEndSelector:@selector(saveOrExportPanelEnded:returnCode:contextInfo:)
        contextInfo:nil
    ];
#else
    int const returnCode =
        [panel runModalForDirectory:m_initDirName file:m_initFileName];
    [self saveOrExportPanelEnded:panel returnCode:returnCode contextInfo:nil];
#endif  /* USE_BEGIN_SHEET */
}

/**
 * Delegate selector for savePanel or exportPanel selection.
 * Calls back to Java with result status: either cancellation or the path for a
 * file to open and export options.
 */
- (void) saveOrExportPanelEnded:
    (NSSavePanel*)panel
    returnCode:(int)returnCode
    contextInfo:(void*)contextInfo
{
    auto_JNIEnv const env;

    switch ( returnCode ) {

        case NSFileHandlingPanelOKButton: {
            jstring jFilename = LC_NSStringTojstring( env, [panel filename] );
            //
            // For simplicity, export options are passed back to the Java side
            // as a simple String array where each String is of the form
            // <key>:<value>.
            //
            // The 0th String is guaranteed to be Type:<type> where <type> is
            // the digits of the ASCII code of the first letter of the image
            // type, e.g., "74" = 'J' for JPEG.
            //
            jclass const jString_class =
                LC_findClassOrDie( env, "java/lang/String" );
            jobjectArray jExportOptionArray =
                env->NewObjectArray( 12, jString_class, NULL );

            //
            // Store the selected image type.
            //
            int opt = 0;
            int const selectedImageTypeTag =
                [[m_controller->fileTypePopUp selectedItem] tag];
            env->SetObjectArrayElement(
                jExportOptionArray, opt++,
                encodeExportOption( env, @"Type:", selectedImageTypeTag )
            );

            //
            // The fileTypePopUp is expected to have the ASCII code of the
            // first letter of the image type encoded as its tag, e.g., 74 =
            // 'J' for JPEG.
            //
            switch ( selectedImageTypeTag ) {

                case 'J':   // JPEG
                    env->SetObjectArrayElement(
                        jExportOptionArray, opt++,
                        encodeExportOption(
                            env, @"Quality:",
                            [m_controller->qualitySlider intValue]
                        )
                    );
                    break;

                case 'L':   // LZN
                    break;

                case 'T':   // TIFF
                    //
                    // The bitsPerChannelPopUp is expected to have the number
                    // of bits per channel encoded as its tag.
                    //
                    env->SetObjectArrayElement(
                        jExportOptionArray, opt++,
                        encodeExportOption(
                            env, @"BitsPerChannel:",
                            [[m_controller->bitsPerChannelPopUp selectedItem]
                                tag]
                        )
                    );
                    env->SetObjectArrayElement(
                        jExportOptionArray, opt++,
                        encodeExportOption(
                            env, @"LZWCompression:",
                            [m_controller->lzwCompressionCheckBox state]
                                == NSOnState
                        )
                    );
                    env->SetObjectArrayElement(
                        jExportOptionArray, opt++,
                        encodeExportOption(
                            env, @"Multilayer:",
                            [m_controller->multilayerCheckBox state]
                                == NSOnState
                        )
                    );
                    break;

                default:
                    LC_throwIllegalStateException( env, "bad imageType tag" );
            }

            if ( selectedImageTypeTag != 'L' ) {
                //
                // Store the Color Profile selection.
                //
                env->SetObjectArrayElement(
                    jExportOptionArray, opt++,
                    encodeExportOption(
                        env, @"ColorProfile:",
                        [m_controller->colorProfilePopUp titleOfSelectedItem]
                    )
                );

                //
                // Store the black-point compensation option.
                //
                env->SetObjectArrayElement(
                    jExportOptionArray, opt++,
                    encodeExportOption(
                        env, @"BlackPointCompensation:",
                        [m_controller->blackPointCheckBox state] == NSOnState
                    )
                );

                //
                // Store the rendering intent.
                //
                env->SetObjectArrayElement(
                    jExportOptionArray, opt++,
                    encodeExportOption(
                        env, @"RenderingIntent:",
                        [[m_controller->renderingIntentPopUp selectedItem] tag]
                    )
                );

                //
                // Store the Resize values.
                //
                env->SetObjectArrayElement(
                    jExportOptionArray, opt++,
                    encodeExportOption(
                        env, @"ResizeWidth:",
                        [m_controller->resizeXField intValue]
                    )
                );
                env->SetObjectArrayElement(
                    jExportOptionArray, opt++,
                    encodeExportOption(
                        env, @"ResizeHeight:",
                        [m_controller->resizeYField intValue]
                    )
                );

                //
                // Store the Resolution values.
                //
                env->SetObjectArrayElement(
                    jExportOptionArray, opt++,
                    encodeExportOption(
                        env, @"Resolution:",
                        [m_controller->resolutionField intValue]
                    )
                );
                env->SetObjectArrayElement(
                    jExportOptionArray, opt++,
                    encodeExportOption(
                        env, @"ResolutionUnit:",
                        [[m_controller->resolutionUnitPopUp selectedItem] tag]
                    )
                );
            }

            env->CallStaticVoidMethod(
                SaveExportSheet_class, sheetOK_methodID,
                m_SaveExportListener_object, jFilename, jExportOptionArray
            );
            env->DeleteLocalRef( jExportOptionArray );
            env->DeleteLocalRef( jFilename );
            break;
        }

        default:
            env->CallStaticVoidMethod(
                SaveExportSheet_class, sheetCanceled_methodID,
                m_SaveExportListener_object
            );
            break;
    }

    env->DeleteGlobalRef( m_SaveExportListener_object );
    [self autorelease];
}

@end
/* vim:set et sw=4 ts=4: */
