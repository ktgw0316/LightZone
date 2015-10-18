/*
 * JavaAppLauncher: a simple Java application launcher for Mac OS X.
 * JavaParamBlock.mm
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 *
 * This code is heavily based on:
 * http://developer.apple.com/samplecode/JavaSplashScreen/JavaSplashScreen.html
 */

#import <stdlib.h>                      /* for getenv(3) */
#import <string.h>
#import <unistd.h>                      /* for chdir(2) */
#import <Cocoa/Cocoa.h>
#import <mach-o/arch.h>                 /* for NXGetLocalArchInfo(3) */
#import <sys/sysctl.h>                  /* for sysctl(3) */

#import "JavaParamBlock.h"
#import "LC_CocoaUtils.h"
#import "SetJVMVersion.h"
#import "UI.h"

using namespace std;
using namespace LightCrafts;

/**
 * If there is no previously set user preference for the maximum amount of
 * memory to use, then this is the percentage of the physical memory that
 * should be used.
 */
float const DefaultMaxMemoryPercentage = 0.3;

/**
 * Insist on at least this much RAM.
 */
int const   JavaMinMemoryInMB = 512;

/**
 * Limit the JVM heap size because we now use non-heap memory for things like
 * tile caches.
 */
#ifdef __LP64__
int const   JavaMaxMemoryInMB = 32768;
#else
int const   JavaMaxMemoryInMB = 2048;
#endif

extern cpu_type_t lc_cpuType;

/**
 * Copy a C string to a newly allocated C buffer.
 */
static void new_strcpy( char **dest, char const *src ) {
    if ( src ) {
        *dest = new char[ ::strlen( src ) + 1 ];
        if ( !*dest )
            LC_die( @"Unexpected", @"Could not allocate memory" );
        ::strcpy( *dest, src );
    } else {
        *dest = new char[1];
        if ( !*dest )
            LC_die( @"Unexpected", @"Could not allocate memory" );
        **dest = '\0';
    }
}

/**
 * Convert/copy an NSString to a newly allocated C string.
 */
static void new_strcpy( char **dest, NSString const *src ) {
    if ( src )
        new_strcpy( dest, [src UTF8String] );
    else
        new_strcpy( dest, static_cast<char const*>( 0 ) );
}

/**
 * Allocate and copy a JVM option string.
 */
inline void newJVMOption( JavaVMOption *jvm_opt, char const *s ) {
    new_strcpy( &jvm_opt->optionString, s );
    jvm_opt->extraInfo = 0;
}

/**
 * Allocate and copy a JVM option string.
 */
static void newJVMOption( JavaVMOption *jvm_opt, NSString const *s ) {
    if ( s )
        newJVMOption( jvm_opt, [s UTF8String] );
    else
        newJVMOption( jvm_opt, static_cast<char const*>( 0 ) );
}

/**
 * Resolve all references to $APP_PACKAGE, $JAVAROOT, and $LC_JVM_VERSION.
 */
static NSString* resolveString( NSString *in, NSDictionary const *javaDict ) {
    if ( in == nil )
        return in;

    // Make a mutable copy of the string to work on.
    NSMutableString *const temp = [NSMutableString string];
    [temp appendString:in];

    //
    // First do $APP_PACKAGE.
    //
    NSString *const appPackage = [[NSBundle mainBundle] bundlePath];
    if ( appPackage )
        [temp replaceOccurrencesOfString:@"$APP_PACKAGE" withString:appPackage
            options:NSLiteralSearch range:NSMakeRange( 0, [temp length] )];

    //
    // Next do $JAVAROOT.
    //
    NSMutableString *const javaRoot = [NSMutableString string];
    NSString *const javaRootProp = [javaDict objectForKey:@"$JAVAROOT"];
    if ( javaRootProp )
        [javaRoot appendString:javaRootProp];
    else
        [javaRoot appendString:@"Contents/Resources/Java"];
    [temp replaceOccurrencesOfString:@"$JAVAROOT" withString:javaRoot
        options:NSLiteralSearch range:NSMakeRange( 0, [temp length] )];

    //
    // Finally, $LC_JVM_VERSION.
    //
    NSString *const jvmVersion =
        [NSString stringWithUTF8String: ::getenv( "JAVA_JVM_VERSION" )];
    [temp replaceOccurrencesOfString:@"$LC_JVM_VERSION" withString:jvmVersion
        options:NSLiteralSearch range:NSMakeRange( 0, [temp length] )];

    return temp;
}

/**
 * Set the given JVM option string.
 */
inline void setJVMOption( JavaVMOption *jvm_opt, char const *s ) {
    jvm_opt->optionString = const_cast<char*>( s );
    jvm_opt->extraInfo = 0;
}

/**
 * Set the given JVM option string.
 */
static void setJVMOption( JavaVMOption *jvm_opt, NSString const *s ) {
    if ( s )
        setJVMOption( jvm_opt, [s UTF8String] );
    else
        setJVMOption( jvm_opt, static_cast<char const*>( 0 ) );
}

/**
 * Gets the class path from the "ClassPath" key of the Java dictionary in the
 * application's Info.plist file.
 */
static NSString* getClassPath( NSDictionary const *javaDict ) {
    id classPathProp = [javaDict objectForKey:@"ClassPath"];
    if ( classPathProp == nil )
        return nil;

    //
    // The class path must be passed to the JVM using the java.class.path
    // property.  The JVM doesn't accept either the -cp or -classpath options.
    //
    NSMutableString *const javaClassPath = [NSMutableString string];
    [javaClassPath appendString:@"-Djava.class.path="];

    if ( [classPathProp isKindOfClass:[NSString class]] ) {
        //
        // It's just a single string.
        //
        [javaClassPath appendString:classPathProp];
    } else if ( [classPathProp isKindOfClass:[NSArray class]] ) {
        //
        // It's an array of strings.
        //
        int const n = [classPathProp count];
        for ( int i = 0; i < n; ++i ) {
            if ( i > 0 )
                [javaClassPath appendString:@":"];
            [javaClassPath appendString:[classPathProp objectAtIndex:i]];
        }
    } else
        LC_die( @"Corrupted", @"Bad ClassPath" );

    return resolveString( javaClassPath, javaDict );
}

/**
 * Gets the working directory from the "WorkingDirectory" key, if any, of the
 * Java dictionary in the application's Info.plist file.
 *
 * Also sets the current working directory to "WorkingDirectory" if specified
 * or the application bundle's path if not.
 */
static NSString* getCWD( NSDictionary const *javaDict ) {
    //
    // First check to see if the key WorkingDirectory is defined in the Java
    // dictionary.
    //
    NSString *cwd = [javaDict objectForKey:@"WorkingDirectory"];

    if ( cwd )
        cwd = resolveString( cwd, javaDict );
    else // Default to the path to the application's bundle.
        cwd = [[NSBundle mainBundle] bundlePath];

    if ( ::chdir( [cwd fileSystemRepresentation] ) != 0 )
        LC_die( @"Unexpected", @"Set CWD failed" );

    return cwd;
}

/**
 * Add the VM options for the given key.
 */
static void addVMOptions( NSMutableArray *options,
                          NSDictionary const *javaDict,
                          NSString const *vmOptionsKey ) {
    if ( id const vmArgs = [javaDict objectForKey:vmOptionsKey] )
        if ( [vmArgs isKindOfClass:[NSString class]] )
            [options addObject:vmArgs];
        else if ( [vmArgs isKindOfClass:[NSArray class]] )
            [options addObjectsFromArray:vmArgs];
        else
            LC_die( @"Corrupted", @"Bad VMOptions" );
}

/**
 * Allocate and initialize an array of JavaVMOption data structures from the
 * Java dictionary in the application's Info.plist file.
 */
static int getJVMOptions( JavaVMOption **jvm_options,
                          NSDictionary const *javaDict ) {
    NSMutableArray *const options = [NSMutableArray arrayWithCapacity:1];

    //
    // Process the VMOptions.
    //
    addVMOptions( options, javaDict, @"VMOptions" );
    switch ( lc_cpuType ) {
        case CPU_TYPE_I386:
            addVMOptions( options, javaDict, @"LC_VMOptionsX86" );
            break;
        case CPU_TYPE_POWERPC:
            addVMOptions( options, javaDict, @"LC_VMOptionsPPC" );
            break;
    }

    //
    // Add the java.class.path property.
    //
    NSString *const classPath = getClassPath( javaDict );
    if ( classPath )
        [options addObject:classPath];

    //
    // Set the working directory (pwd).
    //
    NSMutableString *const userDir = [NSMutableString string];
    [userDir appendString:@"-Duser.dir="];
    [userDir appendString:getCWD( javaDict )];
    [userDir appendString:@"/"];
    [options addObject:userDir];

    //
    // Add the properties defined in Properties dictionary.
    //
    NSDictionary const *const propDict = [javaDict objectForKey:@"Properties"];
    if ( propDict ) {
        NSArray const *const keys = [propDict allKeys];
        int const n = [keys count];
        for ( int i = 0; i < n; ++i ) {
            NSString *const key = [keys objectAtIndex:i];
            NSMutableString *const prop = [NSMutableString string];
            [prop appendString:@"-D"];
            [prop appendString:key];
            [prop appendString:@"="];
            [prop appendString:
                resolveString( [propDict objectForKey:key], javaDict )];
            [options addObject:prop];
        }
    }

    //
    // Convert the NSMutableArray into an array of JavaVMOptions.
    //
    int const n = [options count];
    if ( n > 0 ) {
        *jvm_options = new JavaVMOption[ n ];
        for ( int i = 0; i < n; ++i )
            newJVMOption( &(*jvm_options)[i], [options objectAtIndex:i] );
    } else
        *jvm_options = 0;

    return n;
}

/**
 * Gets the desired Java version from the "JVMVersion" key of the Java
 * dictionary in the application's Info.plist file.
 */
static void getJVMVersion( char **dest, NSDictionary const *javaDict ) {
    NSString const *const jvmVersion = [javaDict objectForKey:@"JVMVersion"];
    if ( !jvmVersion )
        LC_die( @"Corrupted", @"Missing JVMVersion" );
    new_strcpy( dest, jvmVersion );
}

/**
 * Get the arguments to main() from the "Arguments" key of the Java dictionary
 * in the application's Info.plist file.
 */
static int getMainArgs( char ***main_argv, NSDictionary const *javaDict ) {
    int n;
    id const args = [javaDict objectForKey:@"Arguments"];
    if ( args ) {
        if ( [args isKindOfClass:[NSString class]] ) {
            //
            // The "Arguments" key has only a single string value.
            //
            n = 1;
            *main_argv = new char*[1];
            if ( !*main_argv )
                LC_die( @"Unexpected", @"Could not allocate main() array" );
            new_strcpy( &(*main_argv)[0], args );
        } else if ( [args isKindOfClass:[NSArray class]] ) {
            //
            // The "Arguments" key is an array of strings.
            //
            n = [args count];
            *main_argv = new char*[n];
            if ( !*main_argv )
                LC_die( @"Unexpected", @"Could not allocate main() array" );
            for ( int i = 0; i < n; ++i ) {
                id const arg = [args objectAtIndex:i];
                if ( ![arg isKindOfClass:[NSString class]] )
                    LC_die( @"Corrupted", @"Bad argument array" );
                new_strcpy( &(*main_argv)[i], arg );
            }
        } else
            LC_die( @"Corrupted", @"Bad Arguments" );
    } else {
        *main_argv = 0;
        n = 0;
    }
    return n;
}

/**
 * Get the name of the class whose main() is to be executed from the Java
 * dictionary in the application's Info.plist file.
 */
static void getMainClassName( char **dest, NSDictionary const *javaDict ) {
    NSString const *const mainClassName = [javaDict objectForKey:@"MainClass"];
    if ( !mainClassName )
        LC_die( @"Corrupted", @"Missing MainClass" );
    new_strcpy( dest, mainClassName );
    //
    // Convert a class name of the form com.foo.bar to com/foo/bar because the
    // latter is how FindClass() wants it.  Curiously, this step isn't in
    // Apple's sample code.  Hmmm....
    //
    for ( char *c = *dest; *c; ++c )
        if ( *c == '.' )
            *c = '/';
}

/**
 * Read the maxmemory preference from Java.
 */
static int getMaxMemoryFromPreference() {
    //
    // TODO: this should be replaced with the proper Cocoa API for accessing
    // user preferences since the path ~/Library/Preferences is probably in the
    // user's native language.
    //
    char const *const home = ::getenv( "HOME" );
    if ( !home )
        return 0;
    NSMutableString *const prefFile =
        [NSMutableString stringWithUTF8String:home];
    [prefFile appendString:@"/Library/Preferences/com.lightcrafts.app.plist"];
    NSDictionary const *const plistDict =
        [NSDictionary dictionaryWithContentsOfFile:prefFile];
    if ( !plistDict )
        return 0;
    NSDictionary const *const appDict =
        [plistDict objectForKey:@"/com/lightcrafts/app/"];
    if ( !appDict )
        return 0;
    NSString const *const maxMemory = [appDict objectForKey:@"MaxMemory"];
    if ( !maxMemory )
        return 0;
    return (int)::strtol( [maxMemory UTF8String], NULL, 10 );
}

/**
 * Get the value of the MaxMemory user preference in megabytes; if none,
 * default to some percentage of physical memory (but do not exceed what the
 * JVM can handle).
 */
static int getMaxMemory() {
    int memInMB = getMaxMemoryFromPreference();
    if ( !memInMB ) {
        int sysParam[] = { CTL_HW, HW_MEMSIZE };
        //
        // Be defensive and allow for the possibility that sysctl(3) might
        // return either a 32- or 64-bit result by using a union and checking
        // the size of the result and using the correct union member.
        //
        // See:
        // http://www.cocoabuilder.com/archive/message/cocoa/2004/5/6/106388
        //
        union {
            uint32_t ui32;
            uint64_t ui64;
        } result;
        size_t resultSize = sizeof( result );

        ::sysctl( sysParam, 2, &result, &resultSize, NULL, 0 );
        memInMB = (int)(
            ( resultSize == sizeof( result.ui32 ) ?
                (result.ui32 / 1048576) : (result.ui64 / 1048576)
            ) * DefaultMaxMemoryPercentage
        );
    }

    if ( memInMB < JavaMinMemoryInMB )
        return JavaMinMemoryInMB;
    if ( memInMB > JavaMaxMemoryInMB )
        return JavaMaxMemoryInMB;
    return memInMB;
}

/**
 * Replace any -Xmx option that may have been specified in the Info.plist file
 * with one generated from the user preference.
 */
static void replaceXmxOption( JavaVMInitArgs *jvm_args ) {
    NSString const *const jvmXmxOption =
        [NSString stringWithFormat:@"-Xmx%dm", getMaxMemory()];
    if ( jvm_args->nOptions ) {
        //
        // There is at lease one JVM option: loop through them all looking for
        // an -Xmx option.  If found, replace it.
        //
        for ( int i = 0; i < jvm_args->nOptions; ++i ) {
            char **const optionString = &jvm_args->options[i].optionString;
            if ( ::strncmp( *optionString, "-Xmx", 4 ) == 0 ) {
                delete[] *optionString;
                new_strcpy( optionString, jvmXmxOption );
                return;
            }
        }
        //
        // No existing -Xmx option was found: we need to append one.
        //
        int const new_nOptions = jvm_args->nOptions + 1;
        JavaVMOption *const new_options = new JavaVMOption[ new_nOptions ];
        int i;
        for ( i = 0; i < jvm_args->nOptions; ++i )
            setJVMOption( &new_options[i], jvm_args->options[i].optionString );
        newJVMOption( &new_options[i], jvmXmxOption );
        delete[] jvm_args->options;
        jvm_args->options = new_options;
        jvm_args->nOptions = new_nOptions;
    } else {
        //
        // There are no JVM options: create one for the -Xmx option.
        //
        jvm_args->nOptions = 1;
        jvm_args->options = new JavaVMOption[1];
        newJVMOption( &jvm_args->options[0], jvmXmxOption );
    }
}

/**
 * Initialize the given JavaParamBlock from the application's Info.plist file.
 */
void initJavaParamBlock( JavaParamBlock *jpb ) {
    auto_obj<NSAutoreleasePool> pool;

    NSDictionary const *const javaDict =
        [[[NSBundle mainBundle] infoDictionary] objectForKey:@"Java"];
    if ( !javaDict )
        LC_die( @"Corrupted", @"Missing Java dictionary" );

    //
    // Set-up the desired JVM version.
    //
    getJVMVersion( &jpb->jvm_version, javaDict );
    setJVMVersion( jpb->jvm_version );

    //
    // Set-up the JVM initialization options.
    //
    jpb->jvm_args.version = JNI_VERSION_1_4;
    jpb->jvm_args.nOptions = getJVMOptions( &jpb->jvm_args.options, javaDict );
    jpb->jvm_args.ignoreUnrecognized = JNI_TRUE;

    //
    // We need to replace any -Xmx option that may have been specified in the
    // Info.plist file with one generated from the user preference.
    //
    replaceXmxOption( &jpb->jvm_args );

    //
    // Set-up the main class name and main()'s arguments.
    //
    getMainClassName( &jpb->main_className, javaDict );
    jpb->main_argc = getMainArgs( &jpb->main_argv, javaDict );
}

/* vim:set et sw=4 ts=4: */
