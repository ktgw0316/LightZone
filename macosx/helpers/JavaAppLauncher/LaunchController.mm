// standard
#import <Cocoa/Cocoa.h>
#import <CoreFoundation/CoreFoundation.h>
#import <CoreServices/CoreServices.h>
#import <iostream>
#import <pthread.h>
#import <queue>

// local
#include "JavaParamBlock.h"
#include "LC_CocoaUtils.h"
#include "LC_JNIUtils.h"
#include "UI.h"

using namespace std;
using namespace LightCrafts;

static JavaParamBlock jpb;

struct FileToOpen {
    AliasHandle m_fileHandle;
    OSType      m_senderSig;

    FileToOpen( AliasHandle fileHandle, OSType senderSig ) :
        m_fileHandle( fileHandle ),
        m_senderSig( senderSig )
    {
    }
};

static queue<FileToOpen>    g_filesToOpen;
bool                        g_javaIsReady;

static jclass               jLauncher_class;
static jmethodID            jOpenFile_methodID;
static jmethodID            jQuit_methodID;
static jmethodID            jShowAbout_methodID;
static jmethodID            jShowPreferences_methodID;

////////// Objective-C interface //////////////////////////////////////////////

/**
 * A LaunchController is the controlling object for the launcher.
 */
@interface LaunchController : NSObject {
}

- (void) handleOpenDocumentEvent:
    (NSAppleEventDescriptor*)event
    withReplyEvent:(NSAppleEventDescriptor*)replyEvent;

- (OSErr) parseOpenDocumentEvent:
    (NSAppleEventDescriptor const*)aeDesc
    senderSig:(OSType)senderSig;

- (OSErr) queueFile:
    (AliasHandle)fileHandle
    fromSender:(OSType)senderSig;

- (IBAction) quit:
    (id)sender;

- (IBAction) showAbout:
    (id)sender;

- (IBAction) showPreferences:
    (id)sender;

@end

////////// Local functions ////////////////////////////////////////////////////

/**
 * Get the bundle signature from a PSN.
 */
static OSType getSigFromPSN( ProcessSerialNumber const *psn ) {
    ProcessInfoRec info;
    info.processInfoLength = sizeof( info );
    info.processName = NULL;
#ifdef __LP64__
    info.processAppRef = NULL;
#else
    info.processAppSpec = NULL;
#endif
    if ( ::GetProcessInformation( psn, &info ) == noErr )
        return info.processSignature;
    return 0;
}

/**
 * Get the bundle signature from an AppleEvent.
 */
static OSType getSigFromEvent( NSAppleEventDescriptor const *event ) {
    NSData const *const data =
        [[[event attributeDescriptorForKeyword:keyAddressAttr]
            coerceToDescriptorType:typeProcessSerialNumber] data];
    if ( data && [data length] == sizeof( ProcessSerialNumber ) )
        return getSigFromPSN(
            static_cast<ProcessSerialNumber const*>( [data bytes] )
        );
    return 0;
}

/**
 * Get the bundle signature from the parent PSN.
 */
static OSType getSigFromParentPSN( ProcessSerialNumber const *psn ) {
    auto_CFRef<CFDictionaryRef> dict(
        ::ProcessInformationCopyDictionary(
            psn, kProcessDictionaryIncludeAllInformationMask
        )
    );
    if ( dict ) {
        CFNumberRef const parentPSN = static_cast<CFNumberRef>(
            ::CFDictionaryGetValue( dict, CFSTR("ParentPSN") )
        );
        if ( parentPSN ) {
            ProcessSerialNumber ppsn;
            ::CFNumberGetValue( parentPSN, kCFNumberLongLongType, &ppsn );
            return getSigFromPSN( &ppsn );
        }
    }
    return 0;
}

/**
 * Initialize the Java call-back methods.
 */
extern "C" void initJavaCallbacks() {
#ifdef DEBUG
    cout << "*** In initJavaCallbacks()" << endl;
#endif
    auto_JNIEnv env;

    jOpenFile_methodID = env->GetStaticMethodID(
        jLauncher_class, "openFile",
        "(Ljava/lang/String;Ljava/lang/String;)V"
    );
    if ( !jOpenFile_methodID )
        LC_die( @"Corrupted", @"Missing openFile() method" );

    jQuit_methodID = env->GetStaticMethodID(
        jLauncher_class, "quit", "()V"
    );
    if ( !jQuit_methodID )
        LC_die( @"Corrupted", @"Missing quit() method" );

    jShowAbout_methodID = env->GetStaticMethodID(
        jLauncher_class, "showAbout", "()V"
    );
    if ( !jShowAbout_methodID )
        LC_die( @"Corrupted", @"Missing showAbout() method" );

    jShowPreferences_methodID = env->GetStaticMethodID(
        jLauncher_class, "showPreferences", "()V"
    );
    if ( !jShowPreferences_methodID )
        LC_die( @"Corrupted", @"Missing showPreferences() method" );
}

/**
 * Open the given file.
 */
static OSErr openFile( AliasHandle fileHandle, OSType senderSig ) {
#ifdef DEBUG
    cout << "*** In openFile()" << endl;
#endif
    auto_obj<NSAutoreleasePool> pool;

    FSRef fsRef;
    Boolean wasChanged;
    OSErr err = ::FSResolveAliasWithMountFlags(
        NULL, fileHandle, &fsRef, &wasChanged, kResolveAliasFileNoUI
    );
    ::DisposeHandle( (Handle)fileHandle );
    if ( err == noErr ) {
        CFURLRef resolvedURL = ::CFURLCreateFromFSRef( NULL, &fsRef );
        NSString const *const resolvedPath = reinterpret_cast<NSString const*>(
            ::CFURLCopyFileSystemPath( resolvedURL, kCFURLPOSIXPathStyle )
        );
        ::CFRelease( resolvedURL );

        UInt32 senderSigBigEndian = EndianU32_NtoB( senderSig );
        NSString const *const senderSigString =
            [[NSString alloc]
                initWithBytes:&senderSigBigEndian
                length:sizeof senderSigBigEndian
                encoding:NSASCIIStringEncoding];

#ifdef DEBUG
        cout << "*** senderSig=" << [senderSigString UTF8String] << endl;
#endif

        JNIEnv *const env = LC_attachCurrentThread();
        env->CallStaticVoidMethod(
            jLauncher_class, jOpenFile_methodID,
            LC_NSStringTojstring( env, resolvedPath ),
            LC_NSStringTojstring( env, senderSigString )
        );
    }
    return err;
}

////////// Objective-C implementation /////////////////////////////////////////

@implementation LaunchController

/**
 * This gets called by the Cocoa framework when the application has finished
 * launching.  It's time to fire up the JVM.
 */
- (void) applicationDidFinishLaunching:
    (NSNotification*)notification
{
#ifdef DEBUG
    cout << "*** In applicationDidFinishLaunching()" << endl;
#endif
    initJavaParamBlock( &jpb );

#ifdef  DEBUG
    cout << "JVMVersion=" << jpb.jvm_version << endl;
    cout << "JVM args:" << endl;
    for ( int i = 0; i < jpb.jvm_args.nOptions; ++i )
        cout << "  " << jpb.jvm_args.options[i].optionString << endl;
    cout << "MainClass=" << jpb.main_className << endl;
    cout << "main() args:" << endl;
    for ( int i = 0; i < jpb.main_argc; ++i )
        cout << "  " << jpb.main_argv[i] << endl;
#endif

    //
    // The JVM must be started in a seperate thread.
    //
    [NSThread
        detachNewThreadSelector:@selector(startJava:)
        toTarget:self withObject:nil];

    //
    // Start a timer to poll for when the Java application is ready to open
    // files.
    //
    [NSTimer
        scheduledTimerWithTimeInterval:0.5 // seconds
        target:self selector:@selector(isJavaReady:)
        userInfo:nil repeats:YES];
}

/**
 * This gets called by the Cocoa framework when the application is about to
 * finish launching.  This is the right time to install custom event handlers.
 */
- (void) applicationWillFinishLaunching:
    (NSNotification*)notification
{
#ifdef DEBUG
    cout << "*** In applicationWillFinishLaunching()" << endl;
#endif
    [[NSAppleEventManager sharedAppleEventManager]
        setEventHandler:self
        andSelector:@selector(handleOpenDocumentEvent:withReplyEvent:)
        forEventClass:kCoreEventClass andEventID:kAEOpenDocuments];
}

/**
 * This is called by the Cocoa framework just after our main NIB is fully
 * loaded.
 */
- (void) awakeFromNib
{
#ifdef DEBUG
    cout << "*** In awakeFromNib()" << endl;
#endif
    [[NSApplication sharedApplication] setDelegate:self];
}

/**
 * Handle an "open document" AppleEvent.
 */
- (void) handleOpenDocumentEvent:
    (NSAppleEventDescriptor*)event
    withReplyEvent:(NSAppleEventDescriptor*)replyEvent
{
#ifdef DEBUG
    cout << "*** In handleOpenDocumentEvent()" << endl;
#endif

    ////////// Figure out the process that sent us the message ////////////////

    //
    // First, get our PSN and out signature for reference.
    //
    ProcessSerialNumber ourPSN;
    ::GetCurrentProcess( &ourPSN );
    OSType const ourSig = getSigFromPSN( &ourPSN );

    //
    // Next, try to get the sender's signature from the event.
    //
    OSType senderSig = getSigFromEvent( event );

    //
    // If we didn't get a signature or the signature is our own signature
    // (which is the case if we were just launched as opposed to already
    // running), get the signature from our parent PSN.
    //
    if ( !senderSig || senderSig == ourSig )
        senderSig = getSigFromParentPSN( &ourPSN );

#if 0
    if ( senderSig == 'dock' ) {
        //
        // Unfortunately, if the user drags & drops from another application
        // onto our icon in the Dock, we get 'dock' as the sender and the event
        // contains no information as to who the original sender is.
        //
        // As a hack work-around, iterate through all processes looking for one
        // we know about.  If we find more than one, abort.
        //
#ifdef DEBUG
        cout << "*** sender is Dock" << endl;
#endif
        senderSig = 0;
        ProcessSerialNumber psn = { kNoProcess, kNoProcess };
        while ( ::GetNextProcess( &psn ) != procNotFound ) {
            OSType sig = getSigFromPSN( &psn );
            switch ( sig ) {
                case 'AgHg':    // Lightroom
                case 'Brdg':    // Bridge
                case 'fstp':    // Aperture
                case 'iPho':    // iPhoto
                    if ( senderSig ) {
                        senderSig = 0;
                        goto done;
                    }
                    senderSig = sig;
            }
        }
done:   ;
    }
#endif

    ////////// Open the file(s) ///////////////////////////////////////////////

    OSErr err =
        [self
            parseOpenDocumentEvent:[event descriptorForKeyword:keyDirectObject]
            senderSig:senderSig];

    ////////// Report any error back //////////////////////////////////////////

    if ( err != noErr ) {
        NSAppleEventDescriptor *const errNumDesc =
            [NSAppleEventDescriptor descriptorWithInt32:err];
        [replyEvent setParamDescriptor:errNumDesc forKeyword:keyErrorNumber];

        NSAppleEventDescriptor *const errStrDesc =
            [NSAppleEventDescriptor descriptorWithString:@"Complain"];
        [replyEvent setParamDescriptor:errStrDesc forKeyword:keyErrorString];
    }
}

/**
 * Periodically check whether the Java application is ready, i.e., has
 * initialized itself sufficiently and is ready to open files.
 */
- (void) isJavaReady:
    (NSTimer*)timer
{
#ifdef DEBUG
    cout << "*** In isJavaReady()" << endl;
#endif
    if ( !jLauncher_class ) {
        //
        // The thread that is starting the JVM hasn't done so yet (otherwise
        // the jLauncher_class variable wouldn't be null).
        //
        return;
    }

    static pthread_once_t once = PTHREAD_ONCE_INIT;
    ::pthread_once( &once, initJavaCallbacks );

    @synchronized ( self ) {
        if ( g_javaIsReady ) {
            [timer invalidate];
#ifdef DEBUG
            cout << "------------> Java is ready!" << endl;
#endif
            //
            // Open any files that have been queued between the time the
            // application launched and the time the Java application became
            // ready to open files.
            //
            while ( !g_filesToOpen.empty() ) {
                FileToOpen const &fto = g_filesToOpen.front();
                openFile( fto.m_fileHandle, fto.m_senderSig );
                g_filesToOpen.pop();
            }
        }
    }
}

/**
 * Parses an Open Document (odoc) Apple Event.
 */
- (OSErr) parseOpenDocumentEvent:
    (NSAppleEventDescriptor const*)aeDesc
    senderSig:(OSType)senderSig
{
#ifdef DEBUG
    cout << "*** In parseOpenDocumentEvent()" << endl;
#endif
    DescType const descType = [aeDesc descriptorType];
    OSErr err = noErr;

    if ( descType == typeAlias ) {
        //
        // Open a single file specified as an alias.
        //
        NSData const *const data = [aeDesc data];
        AliasHandle const fileHandle =
            reinterpret_cast<AliasHandle>( ::NewHandle( [data length] ) );
        if ( fileHandle ) {
            [data getBytes:*fileHandle];
            err = [self queueFile:fileHandle fromSender:senderSig];
        }
    } else if ( descType == typeBookmarkData ) {
        //
        // Open a single file specified as a bookmark (Mac OS X 10.6 or later).
        //
        BOOL isStale;
        NSError *nsError = nil;
        NSURL const *const url =
            [NSURL
                URLByResolvingBookmarkData:[aeDesc data]
                options:
                    (NSURLBookmarkResolutionWithoutUI |
                     NSURLBookmarkResolutionWithoutMounting)
                relativeToURL:nil bookmarkDataIsStale:&isStale error:&nsError];
        if ( isStale )
            return fnfErr;
        if ( nsError ) {
            if ( [[nsError domain] isEqualToString:NSOSStatusErrorDomain] )
                return [nsError code];
            return errAECoercionFail;
        }
        FSRef fsRef;
        if ( !::CFURLGetFSRef( (CFURLRef)url, &fsRef ) )
            return memFullErr;
        AliasHandle fileHandle;
        err = ::FSNewAlias( NULL, &fsRef, &fileHandle );
        if ( err == noErr )
            err = [self queueFile:fileHandle fromSender:senderSig];
    } else if ( descType == typeAEList ) {
        //
        // Open multiple files.
        //
        int const numItems = [aeDesc numberOfItems];
        for ( int i = 1; i <= numItems; ++i ) {
            err = [self
                    parseOpenDocumentEvent:[aeDesc descriptorAtIndex:i]
                    senderSig:senderSig];
            if ( err != noErr )
                break;
        }
    }
    return err;
}

/**
 * Queue a file to be opened.
 */
- (OSErr) queueFile:
    (AliasHandle)fileHandle
    fromSender:(OSType)senderSig
{
#ifdef DEBUG
    cout << "*** In queueFile()" << endl;
#endif
    @synchronized ( self ) {
        if ( g_javaIsReady ) {
            //
            // The Java application is ready to open files: open the file
            // immediately and don't bother to queue it.
            //
            return openFile( fileHandle, senderSig );
        }
        FileToOpen const fileToOpen( fileHandle, senderSig );
        g_filesToOpen.push( fileToOpen );
    }
    return noErr;
}

/**
 * This is called when the user selects "Quit" from the application menu.
 */
- (IBAction) quit:
    (id)sender
{
    JNIEnv *const env = LC_attachCurrentThread();
    env->CallStaticVoidMethod( jLauncher_class, jQuit_methodID );
}

/**
 * This is called when the user selects "About ..." from the application menu.
 */
- (IBAction) showAbout:
    (id)sender
{
    JNIEnv *const env = LC_attachCurrentThread();
    env->CallStaticVoidMethod( jLauncher_class, jShowAbout_methodID );
}

/**
 * This is called when the user selects "Preferences..." from the applicaiton
 * menu.
 */
- (IBAction) showPreferences:
    (id)sender
{
    JNIEnv *const env = LC_attachCurrentThread();
    env->CallStaticVoidMethod( jLauncher_class, jShowPreferences_methodID );
}

/**
 * Fire up the JVM.  This method must be called in its own thread.
 */
- (void) startJava:
    (id)userData
{
    extern void startJava( JavaParamBlock const*, jclass* );
    auto_obj<NSAutoreleasePool> pool;

    //
    // Start the JVM: this call will block until the JVM exits.
    //
    startJava( &jpb, &jLauncher_class );

    //
    // Now that the JVM has shut down cleanly, quit.
    //
    [[NSApplication sharedApplication] terminate:self];
}

@end
/* vim:set et sw=4 ts=4: */
