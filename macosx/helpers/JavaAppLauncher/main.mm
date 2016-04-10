/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * main.mm
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 *
 * This code is based on:
 * http://developer.apple.com/samplecode/simpleJavaLauncher/simpleJavaLauncher.html
 */

/**
 * Undefine to cause this application to crash immediately to test the Crash
 * Reporter application.
 */
/* #define TEST_CRASH_REPORTER */

// standard
#import <Cocoa/Cocoa.h>
#import <CoreFoundation/CoreFoundation.h>
#import <cstdlib>
#import <cstring>
#import <dirent.h>
#import <fcntl.h>                       /* for open(2) */
#import <fstream>
#import <iostream>
#import <mach-o/arch.h>                 /* for NXGetLocalArchInfo(3) */
#import <signal.h>
#import <sys/types.h>
#import <unistd.h>

// local
#import "JavaParamBlock.h"
#import "SetJVMVersion.h"
#import "LC_CocoaUtils.h"
#import "md5.h"
#import "UI.h"

using namespace std;
using namespace LightCrafts;

/**
 * The path relative to the application bundle of where the Crash Reporter
 * application is.
 */
#define CRASH_REPORTER_PATH "/Contents/Resources/CrashReporter.app"

/**
 * The full path to where the current Mac OS X version information is.
 */
#define SYSTEM_VERSION_PLIST \
    "/System/Library/CoreServices/SystemVersion.plist"

//
// Obfuscate function names in object code.
//
#define checkCPUType    cC
#define checkJar        cJ
#define checkJars       cJs
#define checkOSXVersion cO
#define rot47           r

cpu_type_t lc_cpuType;

////////// Local functions ////////////////////////////////////////////////////

/**
 * This is a do-nothing call-back function needed to ensure the CFRunLoop
 * doesn't exit right away.  This call-back is called when the source has
 * fired.
 */
extern "C" void doNothing( void* ) {
    // do nothing
}

/**
 * Catch a signal and launch our custom Crash Reporter.
 */
extern "C" void catchSignal( int sigID ) {
    NSString *const bundlePath = [[NSBundle mainBundle] bundlePath];
    NSString *const crashReporterPath =
        [bundlePath stringByAppendingPathComponent:@CRASH_REPORTER_PATH];

    [[NSWorkspace sharedWorkspace]
        openFile:bundlePath withApplication:crashReporterPath
    ];
    ::exit( -1 );
}

/**
 * Check the CPU type.
 */
static void checkCPUType() {
    NXArchInfo const *const arch = NXGetLocalArchInfo();
    if ( !arch ) {
        //
        // This should never return null (how can the info not be available?),
        // but the manual page arch(3) says it's possible.  So err on the side
        // of allowing the application to run and hope for the best.
        //
        return;
    }
    lc_cpuType = arch->cputype;
    switch ( arch->cputype ) {
        case CPU_TYPE_I386:
            cout << "CPU = i386" << endl;
            return;
        case CPU_TYPE_POWERPC:
            if ( arch->cpusubtype >= CPU_SUBTYPE_POWERPC_7400 ) {
                cout << "CPU = ppc" << endl;
                return;
            }
    }
    LC_die( @"Newer CPU required", @"CPU requirements" );
}

/**
 * Check a jar to see if its hash still matches what it did at the time it was
 * built.
 */
static void checkJar( char const *jarPath, char const *correctMD5 ) {
    typedef unsigned char UCHAR;
    typedef UCHAR const UCHARC;

    int const MD5_BUF_SIZE = 16;        // MD5 encodes to 128 bits or 16 bytes

    //
    // The "salt" is the secret key.  First, it's broken into two big pieces
    // because it's better cryptographically speaking to put a salt both before
    // and after the actual data to be encrypted by MD5 since, even if you know
    // what the salt is, you don't know where the "split" between the two
    // pieces is.  (A null separates the 2 pieces.)
    //
    // Second, it's broken into 4-character chunks so the output of the Unix
    // "strings" command on the library shows at most the chunks and not the
    // entire salt as a single string.
    //
    char const *const SALT[] = {
        "Quid", "quid", " lat", "ine ", "dict", "um", 0,
        " sit", " alt", "um v", "idit", "ur", 0 
    };  

    //
    // This SEASON macro is used to add salt to the MD5 data.
    // Season ... salt: get it?  :-)
    //
#   define SEASON(STR) MD5Update( &md5_ctx, (UCHARC*)(STR), ::strlen( STR ) )

    MD5Context md5_ctx;
    MD5Init( &md5_ctx );

    char const *const *salt;
    for ( salt = SALT; *salt; ++salt )
        SEASON( *salt );

    ifstream ifs( jarPath );
    if ( !ifs )
        LC_die( @"Corrupted", @"Missing Jar" );
    char buf[ 8192 ];
    while ( !ifs.eof() ) {
        ifs.read( buf, sizeof buf );
        MD5Update( &md5_ctx, (UCHARC*)buf, ifs.gcount() );
    }

    for ( ++salt; *salt; ++salt )
        SEASON( *salt );

    UCHAR md5_buf[ MD5_BUF_SIZE ];
    MD5Final( md5_buf, &md5_ctx );

    char md5_char_buf[ MD5_BUF_SIZE * 2 + 1 ];
    char *p = md5_char_buf;
    for ( int i = 0; i < MD5_BUF_SIZE; ++i, p += 2 )
        sprintf( p, "%02x", md5_buf[i] );
    *p = '\0';

    if ( ::strcmp( md5_char_buf, correctMD5 ) != 0 )
        LC_die( @"Corrupted", @"Bad Jar" );
}

/**
 * ROT-47 cipher.
 */
static char* rot47( char *s, char const *s47 ) {
    char *const s0 = s;
    do {
        char c = *s47;
        if ( c >= '!' && c <= '~' )
            c = (c - '!' + 47) % 94 + '!';
        *s++ = c;
    } while ( *s47++ );
    return s0;
}

/**
 * Ensure the jars haven't been altered since build-time.
 */
static void checkJars() {
    auto_obj<NSAutoreleasePool> pool;
    NSString const *const javaPath =
        [[[NSBundle mainBundle] bundlePath]
            stringByAppendingPathComponent:@"/Contents/Resources/Java"];

    char const *const jar_md5[] = {
#       include "jar_md5_include"
        0
    };
    for ( char const *const *s = jar_md5; *s; s += 2 ) {
        char jar_buf[ 50 ];
        NSString const *const jarPath =
            [javaPath stringByAppendingPathComponent:
                [NSString stringWithUTF8String:rot47( jar_buf, s[0] )]];
        checkJar( [jarPath UTF8String], s[1] );
    }
}

/**
 * Check the Mac OS X version.
 */
static void checkOSXVersion() {
    auto_obj<NSAutoreleasePool> pool;
    NSDictionary *const versionDict =
        [NSDictionary dictionaryWithContentsOfFile:@SYSTEM_VERSION_PLIST];
    if ( !versionDict )
        return;
    NSString *const osVersion = [versionDict objectForKey:@"ProductVersion"];
    if ( !osVersion )
        return;
    cout << "This is Mac OS X " << [osVersion UTF8String] << endl;
    int major, minor = 0, point = 0;
    ::sscanf( [osVersion UTF8String], "%d.%d.%d", &major, &minor, &point );
    switch ( minor ) {
        case 0:     // Cheetah
        case 1:     // Puma
        case 2:     // Jaguar
        case 3:     // Panther
            break;
        case 4:     // Tiger
            if ( point >= 3 )
                return;
            break;
        default:    // Assume any future Mac OS X is OK.
            return;
    }
    LC_die( @"Newer Mac OS X required", @"OS requirements" );
}

/**
 * Redirect standard output and standard error to a log file.  The name of the
 * log file is <CFBundleName>.log.
 */
static void redirectOutput() {
    auto_obj<NSAutoreleasePool> pool;

    NSString *const bundleName =
        [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleName"];
    if ( !bundleName )
        LC_die( @"Corrupted", @"Missing CFBundleName" );

    NSString const *const logFile =
        [[[[NSString stringWithString:@"~/Library/Logs"]
            stringByAppendingPathComponent:bundleName]
            stringByAppendingString:@".log"]
            stringByExpandingTildeInPath];

    int const logFD =
        ::open( [logFile UTF8String], O_WRONLY | O_CREAT | O_TRUNC, 0644 );
    if ( logFD == -1 ) {
        //
        // If for whatever reason we couldn't open the log file, just return
        // and sacrifice logging rather than prevent the application from
        // running.
        //
        return;
    }
    ::close( STDOUT_FILENO );
    ::dup( logFD );                         // stdout -> log
    ::close( logFD );
    ::dup2( STDOUT_FILENO, STDERR_FILENO ); // stderr -> log
}

////////// main ///////////////////////////////////////////////////////////////

/**
 * Fire up the app.
 */
int main( int argc, char const **argv ) {
    //
    // Set up to catch nasty signals.
    //
    ::signal( SIGILL , &catchSignal );
    ::signal( SIGTRAP, &catchSignal );
    ::signal( SIGEMT , &catchSignal );
    ::signal( SIGFPE , &catchSignal );
    ::signal( SIGBUS , &catchSignal );
    ::signal( SIGSEGV, &catchSignal );
    ::signal( SIGSYS , &catchSignal );
    ::signal( SIGPIPE, SIG_IGN );

#ifdef  TEST_CRASH_REPORTER
    //
    // We want to test the Crash Reporter application, so do something to make
    // this application crash.  Writing to memory location 0 works.
    //
    int *p = 0;
    *p = 0;
#endif

    redirectOutput();

#ifdef DEBUG
    ::setenv( "AEDebugSends", "1", 1 );
    ::setenv( "AEDebugReceives", "1", 1 );
#endif

    checkCPUType();
    checkOSXVersion();
    checkJars();

    return NSApplicationMain( argc, argv );
}
/* vim:set et sw=4 ts=4: */
