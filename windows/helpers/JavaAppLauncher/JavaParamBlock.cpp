/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * JavaParamBlock.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cstring>

// windows
#include <windows.h>
#include <winbase.h>
#include <winreg.h>                     /* for Registry functions */

// local
#include "JavaParamBlock.h"
#include "UI.h"

using namespace std;

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
#if _WIN64 || __amd64__
int const   JavaMaxMemoryInMB = 32768;
#else
int const   JavaMaxMemoryInMB = 2048;
#endif

/**
 * Copy a C string to a newly allocated C buffer.
 */
static void new_strcpy( char **dest, char const *src ) {
    if ( src ) {
        *dest = new char[ ::strlen( src ) + 1 ];
        if ( !*dest )
            LC_die( TEXT("Could not allocate memory.") );
        ::strcpy( *dest, src );
    } else {
        *dest = new char[1];
        if ( !*dest )
            LC_die( TEXT("Could not allocate memory.") );
        **dest = '\0';
    }
}

/**
 * Allocate and copy a JVM option string.
 */
inline void newJVMOption( JavaVMOption *jvm_opt, char const *s ) {
    new_strcpy( &jvm_opt->optionString, s );
    jvm_opt->extraInfo = 0;
}

/**
 * Set the given JVM option string.
 */
inline void setJVMOption( JavaVMOption *jvm_opt, char const *s ) {
    jvm_opt->optionString = const_cast<char*>( s );
    jvm_opt->extraInfo = 0;
}

/**
 * Allocate and initialize an array of JavaVMOption data structures from the
 * JVM arguments.
 */
static int getJVMOptions( JavaVMOption **jvm_options,
                          char const *const jvmArgs[] ) {
    //
    // First, count the number of options.
    //
    int n = 0;
    for ( char const *const *arg = jvmArgs; *arg && **arg == '-'; ++arg )
        ++n;

    if ( n > 0 ) {
        //
        // Then, convert the options.
        //
        *jvm_options = new JavaVMOption[n];
        for ( int i = 0; i < n; ++i )
            newJVMOption( &(*jvm_options)[i], jvmArgs[i] );
    } else
        *jvm_options = 0;

    return n;
}

/**
 * Get the arguments to main() from the JVM arguments.
 */
static int getMainArgs( char ***main_argv, char const *const jvmArgs[] ) {
    int n = 0;
    *main_argv = 0;
    //
    // First, skip the JVM options.
    //
    char const *const *arg;
    for ( arg = jvmArgs; *arg && **arg == '-'; ++arg )
        ;

    if ( *arg && *++arg ) {             // skip the main class name
        //
        // Second, count the number of main() arguments.
        //
        for ( char const *const *temp = arg; *temp; ++temp )
            ++n;
        if ( n ) {
            //
            // Finally, convert the arguments.
            //
            *main_argv = new char*[n];
            if ( !*main_argv )
                LC_die( TEXT("Could not allocate main() argument array.") );
            for ( int i = 0; i < n; ++i )
                new_strcpy( &(*main_argv)[i], arg[i] );
        }
    }

    return n;
}

/**
 * Get the name of the class whose main() is to be executed from the Java
 * dictionary in the application's Info.plist file.
 */
static void getMainClassName( char **mainClassName,
                              char const *const jvmArgs[] ) {
    char const *const *arg;
    //
    // First, skip the JVM options.
    //
    for ( arg = jvmArgs; *arg && **arg == '-'; ++arg )
        ;

    if ( !*arg )
        LC_die( TEXT("Main class not specified.") );
    //
    // *arg is now pointing to the main class name.
    //
    new_strcpy( mainClassName, *arg );

    //
    // Convert a class name of the form com.foo.bar to com/foo/bar because the
    // latter is how FindClass() wants it.  Curiously, this step isn't in
    // Apple's sample code.  Hmmm....
    //
    for ( char *c = *mainClassName; *c; ++c )
        if ( *c == '.' )
            *c = '/';
}

/**
 * Get the value of the MaxMemory user preference in megabytes.
 */
static int getMaxMemoryFromPreference() {
    HKEY key;
    LONG status = ::RegOpenKeyEx(
        HKEY_CURRENT_USER,
        TEXT("Software\\JavaSoft\\Prefs\\com\\lightcrafts\\app"),
        0, KEY_QUERY_VALUE, &key
    );
    if ( status != ERROR_SUCCESS )
        return 0;

    DWORD keyType;
    BYTE  buf[ 10 ];
    DWORD bufSize = sizeof( buf );
    status = ::RegQueryValueExA(
        key, "/Max/Memory", NULL, &keyType, buf, &bufSize
    );
    if ( status != ERROR_SUCCESS || keyType != REG_SZ )
        return 0;
    return ::atoi( reinterpret_cast<char*>( buf ) );
}

/**
 * Get the value of the MaxMemory user preference in megabytes; if none,
 * default to some percentage of physical memory (but do not exceed what the
 * JVM can handle).
 */
static int getMaxMemory() {
    int memInMB = getMaxMemoryFromPreference();
    if ( !memInMB ) {
        MEMORYSTATUSEX ms;
        ms.dwLength = sizeof( MEMORYSTATUSEX );
        if ( !::GlobalMemoryStatusEx( &ms ) )
            LC_die( TEXT("Determining memory"), ::GetLastError() );
        memInMB = (int)(ms.ullTotalPhys / 1048576 * DefaultMaxMemoryPercentage);
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
    char jvmXmxOption[ 12 ];
    ::sprintf( jvmXmxOption, "-Xmx%dm", getMaxMemory() );
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
void initJavaParamBlock( JavaParamBlock *jpb, char const *const jvmArgs[] ) {
    //
    // Set-up the JVM initialization options.
    //
    jpb->jvm_args.version = JNI_VERSION_1_4;
    jpb->jvm_args.nOptions = getJVMOptions( &jpb->jvm_args.options, jvmArgs );
    jpb->jvm_args.ignoreUnrecognized = JNI_TRUE;

    //
    // We need to replace any -Xmx option that may have been specified in the
    // Info.plist file with one generated from the user preference.
    //
    replaceXmxOption( &jpb->jvm_args );

    //
    // Set-up the main class name and main()'s arguments.
    //
    getMainClassName( &jpb->main_className, jvmArgs );
    jpb->main_argc = getMainArgs( &jpb->main_argv, jvmArgs );
}

/* vim:set et sw=4 ts=4: */
