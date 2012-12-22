/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * SetJVMVersion.mm
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#import <Cocoa/Cocoa.h>
#ifdef  DEBUG
#import <iostream>
#endif
#import <sys/types.h>
#import <dirent.h>

// local
#import "LC_CocoaUtils.h"
#import "UI.h"

using namespace std;
using namespace LightCrafts;

/**
 * Set the preferred JVM version via the JAVA_JVM_VERSION environment variable.
 */
void setJVMVersion( char const *jvmVersion ) {
    auto_obj<NSAutoreleasePool> pool;

    enum match_mode_t {
        //
        // Must exactly match the specified version.
        //
        MatchSpecifiedOnly,

        //
        // Must match the specified version or any later version, but within
        // the same minor revision, e.g., "1.4.2" is a match for "1.4*" but
        // "1.5" is not.
        //
        MatchSpecifiedOrLater,

        //
        // Must match the specified version or any later version.
        //
        MatchSpecifiedOrLatest
    };
    match_mode_t matchMode;

    //
    // Determine the match mode we should use.
    //
    int const len = ::strlen( jvmVersion );
    switch ( jvmVersion[ len - 1 ] ) {
        case '*':
            matchMode = MatchSpecifiedOrLater;
            break;
        case '+':
            matchMode = MatchSpecifiedOrLatest;
            break;
        default:
            matchMode = MatchSpecifiedOnly;
    }

    //
    // Get the path to the Java Versions directory.
    //
    NSBundle *const javaBundle =
        [NSBundle bundleWithIdentifier:@"com.apple.JavaVM"];
    if ( !javaBundle )
        LC_die( @"Unexpected", @"Missing Java" );
    NSString *const javaVersionsPath =
        [[javaBundle bundlePath] stringByAppendingPathComponent:@"Versions"];

    //
    // Read the names of all the subdirectories (or symbolic links thereto)
    // that list the Java versions available and look for the best match.
    //
    DIR *const dir = ::opendir( [javaVersionsPath fileSystemRepresentation] );
    if ( !dir )
        LC_die( @"Unexpected", @"Couldn't read Versions directory" );

    int jvmMajor, jvmMinor = 0, jvmPoint = 0;
    ::sscanf( jvmVersion, "%d.%d.%d", &jvmMajor, &jvmMinor, &jvmPoint );

    char bestJVMVersion[ 16 ];          // more than enough
    *bestJVMVersion = '\0';
    int bestMajor = 0, bestMinor = 0, bestPoint = 0;

    for ( struct dirent const *entry; entry = ::readdir( dir ); ) {
        if ( !isdigit( *entry->d_name ) )
            continue;
        int dirMajor, dirMinor, dirPoint;
        ::sscanf( entry->d_name, "%d.%d.%d", &dirMajor, &dirMinor, &dirPoint );
        switch ( matchMode ) {

            case MatchSpecifiedOnly:
                if ( dirMajor == jvmMajor && dirMinor == jvmMinor &&
                     dirPoint == jvmPoint
                ) {
                    ::strcpy( bestJVMVersion, entry->d_name );
                    goto done;
                }
                break;

            case MatchSpecifiedOrLater:
                if ( dirMajor == jvmMajor && dirMinor == jvmMinor &&
                     dirPoint >= jvmPoint && dirPoint >= bestPoint
                ) {
                    ::strcpy( bestJVMVersion, entry->d_name );
                    bestPoint = dirPoint;
                }
                break;

            case MatchSpecifiedOrLatest:
                if ( dirMajor >= jvmMajor && dirMinor >= jvmMinor &&
                     dirPoint >= jvmPoint
                ) {
                    ::strcpy( bestJVMVersion, entry->d_name );
                    bestMajor = dirMajor;
                    bestMinor = dirMinor;
                    bestPoint = dirPoint;
                }
                break;
        }
    }

done:
    ::closedir( dir );
    if ( !*bestJVMVersion )
        LC_dieWithoutLocalizedInfo( @"Required Java version not found",
            [NSString stringWithFormat:
                NSLocalizedString( @"Couldn't match Java version", nil ),
                jvmVersion]
        );

#ifdef DEBUG
    cout << "Setting JAVA_JVM_VERSION=" << bestJVMVersion << endl;
#endif
    ::setenv( "JAVA_JVM_VERSION", bestJVMVersion, 1 );
}

/* vim:set et sw=4 ts=4: */
