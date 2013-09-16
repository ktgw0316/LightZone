/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * JavaAppLauncher: a simple Java application launcher for Windows.
 * main.cpp
 *
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <cctype>
#include <cstdio>
#include <cstdlib>                      /* for exit(3) */
#include <cstring>
#include <fstream>
#include <io.h>                         /* for _open_osfhandle() */
#include <iostream>

// windows
#include <shlobj.h>
#include <tlhelp32.h>
#include <w32api.h>
#include <windows.h>
#include <winbase.h>

// local
#include "JavaParamBlock.h"
#include "LC_CPPUtils.h"
#include "LC_JNIUtils.h"
#include "LC_WinUtils.h"
#include "md5.h"
#include "StartJava.h"
#include "UI.h"

using namespace std;
using namespace LightCrafts;

/* Registers for 32-bit x86. */
#define CPU_REG_b "ebx"
#define CPU_REG_S "esi"

#define CPUID(index,eax,ebx,ecx,edx)                            \
        asm volatile (                                          \
            "mov %%" CPU_REG_b ", %%" CPU_REG_S "\n\t"          \
            "cpuid\n\t"                                         \
            "xchg %%" CPU_REG_b ", %%" CPU_REG_S                \
            : "=a" (eax), "=S" (ebx), "=c" (ecx), "=d" (edx)    \
            : "0" (index)                                       \
        )

//
// Obfuscate function names in object code.
//
#define checkCPUType        cC
#define checkJar            cJ
#define checkJars           cJs
#define checkWindowsVersion cW
#define rot47               r

// An acknowledgement message from the first running instance to the second.
char const ACK[] = "ACK";

// The maximum number of JVM arguments we allow.
int const MaxJVMArgs = 16;

// The size of the pipe buffer between running instances.
int const PipeBufSize = 1024;

static JavaParamBlock g_jpb;

/**
 * Checks whether the given window is a LightZone window.
 */
static bool isLightZoneWindow( HWND window ) {
    WCHAR wTextBuf[ 80 ];
    if ( !::GetWindowText( window, wTextBuf, sizeof wTextBuf / sizeof(WCHAR) ) )
        return false;
    if ( !::wcsstr( wTextBuf, TEXT("LightZone") ) )
        return false;
    DWORD pid;
    ::GetWindowThreadProcessId( window, &pid );
    return pid == ::GetCurrentProcessId();
}

/**
 * Find a LightZone window and bring it to the front.  This function is a
 * call-back for EnumWindows() and shouldn't be called directly.
 */
BOOL CALLBACK bringAppWindowToFront( HWND window, LPARAM ) {
    if ( !isLightZoneWindow( window ) )
        return TRUE;

    //
    // First, starting from the found window, traverse the stack of windows
    // looking for the one with the least Z-order (top-most).
    //
    for ( HWND w = window; w = ::GetNextWindow( w, GW_HWNDPREV ); )
        if ( isLightZoneWindow( w ) )
            window = w;

    //
    // Next, starting from the least Z-order window, traverse the list of
    // parent windows to find the eldest parent.
    //
    for ( HWND w = window; w = ::GetParent( w ); )
        if ( isLightZoneWindow( w ) )
            window = w;

    ::ShowWindow( window, SW_RESTORE ); // unminimize, if necessary

    HWND const fgWindow = ::GetForegroundWindow();
    if ( window != fgWindow ) {
#ifndef USE_SwitchToThisWindow
        //
        // Windows 98 and later deny background applications the ability to set
        // the foreground window.  Usually, this is a good thing since it
        // prevents background applications from popping-up a window and
        // stealing focus while the user is doing something else.
        //
        // However, in our case, the user either just double-clicked our
        // application icon or some other application requested that we open
        // some image files; hence, bringing our window to the front really is
        // the right thing to do.  Honest!
        //
        // To get around Windows' prohibition, we can trick it into thinking
        // this thread is the thread that currently owns the current foreground
        // window by attaching our input to it temporarily.  Once attached, we
        // can set the foreground window.  After setting it, we detach.
        //
        // However, there are potentially 3 threads involved: (1) the current
        // thread, (2) the thread that owns the LightZone window (which may or
        // may not be the current thread), and (3) the thread that owns the
        // foreground window.  We've discovered that the aforementioned
        // trickery works only if we attach both threads 1 and 2 to thread 3.
        //
        DWORD const curTID = ::GetCurrentThreadId();
        DWORD const lznTID = ::GetWindowThreadProcessId( window, NULL );
        DWORD const fgwTID = ::GetWindowThreadProcessId( fgWindow, NULL );

        if ( curTID != fgwTID )
            ::AttachThreadInput( curTID, fgwTID, TRUE );
        if ( lznTID != curTID && lznTID != fgwTID )
            ::AttachThreadInput( lznTID, fgwTID, TRUE );

        ::SetForegroundWindow( window );

        if ( curTID != fgwTID )
            ::AttachThreadInput( curTID, fgwTID, FALSE );
        if ( lznTID != curTID && lznTID != fgwTID )
            ::AttachThreadInput( lznTID, fgwTID, FALSE );
#else
        //
        // This code has been placed here for safe keeping.  It is an alternate
        // to the above if it ever stops working.  The reason for using the
        // above is because SwitchToThisWindow() has been deprecated.
        //
        SwitchToThisWindow( window, FALSE );
#endif  /* USE_SwitchToThisWindow */
    }

    return FALSE;                       // stop enumerating
}

/**
 * Check the CPU type.
 */
static void checkCPUType() {
    //
    // Before we can check for anything about the CPU, we first need to check
    // for cpuid instruction support.
    //
#if _WIN64 || __amd64__
    long long a, c;
#else
    long a, c;
#endif
    asm volatile (
        /* Copy EFLAGS into eax and ecx. */
        "pushf\n\t"
        "pop %0\n\t"
        "mov %0, %1\n\t"

        /* Toggle the ID bit in one copy and store to the EFLAGS register. */
        "xor $0x200000, %0\n\t"
        "push %0\n\t"
        "popf\n\t"

        /* Get the (hopefully modified) EFLAGS. */
        "pushf\n\t"
        "pop %0\n\t"
        : "=a" (a), "=c" (c)
        :
        : "cc"
    );
    if ( a == c )
        LC_die( LS_REQ_CPUID );

    //
    // We need to check for SSE2 instruction support.  We'd simply like to be
    // able to use:
    //
    //      IsProcessorFeaturePresent( PF_XMMI64_INSTRUCTIONS_AVAILABLE )
    //
    // but this isn't supported on Windows 2000 (non-Professional).
    //
    int max_std_level, std_caps;
    int eax, ebx, ecx, edx;

    CPUID( 0, max_std_level, ebx, ecx, edx );
    if ( max_std_level >= 1 ) {
        CPUID( 1, eax, ebx, ecx, std_caps );
        if ( std_caps & (1 << 26) /* SSE2 */ )
            return;
    }
    LC_die( LS_REQ_SSE2 );
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

    ifstream ifs( jarPath, ios::binary );
    if ( !ifs )
        LC_die( TEXT("Missing jar.") );
    char buf[ 8192 ];
    while ( !ifs.eof() ) {
        ifs.read( buf, sizeof buf );
        MD5Update( &md5_ctx, reinterpret_cast<UCHARC*>( buf ), ifs.gcount() );
    }

    for ( ++salt; *salt; ++salt )
        SEASON( *salt );

    UCHAR md5_buf[ MD5_BUF_SIZE ];
    MD5Final( md5_buf, &md5_ctx );

    char md5_char_buf[ MD5_BUF_SIZE * 2 + 1 ];
    char *p = md5_char_buf;
    for ( int i = 0; i < MD5_BUF_SIZE; ++i, p += 2 )
        ::sprintf( p, "%02x", md5_buf[i] );
    *p = '\0';

    if ( ::strcmp( md5_char_buf, correctMD5 ) != 0 )
        LC_die( TEXT("Corrupted jar.") );
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
    char const *const jarMD5[] = {
#       include "jar_md5_include"
        0
    };
    for ( char const *const *s = jarMD5; *s; s += 2 ) {
        char jarBuf[ 50 ];
        checkJar( rot47( jarBuf, s[0] ), s[1] );
    }
}

/**
 * Check the Windows version.
 */
static void checkWindowsVersion() {
    OSVERSIONINFOEX info;
    ::ZeroMemory( &info, sizeof( OSVERSIONINFOEX ) );
    info.dwOSVersionInfoSize = sizeof( OSVERSIONINFOEX );
    if ( !::GetVersionEx( reinterpret_cast<OSVERSIONINFO*>( &info ) ) )
        LC_die( TEXT("Unable to determine Windows version.") );
    cout << "JavaAppLauncher checkWindowsVersion()"
         << "\n    dwMajorVersion=" << info.dwMajorVersion
         << "\n    dwMinorVersion=" << info.dwMinorVersion
         << "\n    wServicePackMajor=" << info.wServicePackMajor << endl;
    //
    // dwMajorVersion:
    //  4 = Windows NT 4.0
    //  5 = Windows Server 2003, Windows 2000, or Windows XP
    //  6 = Windows Vista
    //
    if ( info.dwMajorVersion > 5 ) {
        //
        // Be optimistic and assume we'll run on Vista or anything later.
        //
        if ( info.dwMajorVersion == 6 )
            cout << "  = Windows Vista" << endl;
        return;
    }
    if ( info.dwMajorVersion == 5 ) {
        switch ( info.dwMinorVersion ) {
            case 0: // Windows 2000
                cout << "  = Windows 2000" << endl;
                if ( info.wServicePackMajor >= 4 )
                    return;
                break;
            case 1: // Windows XP
            case 2: // Windows XP Pro x64
                cout << "  = Windows XP";
                if ( info.dwMinorVersion == 2 )
                    cout << " x64";
                cout << endl;
                if ( info.wServicePackMajor >= 2 )
                    return;
                break;
            default:
                //
                // Assume any future minor versions are OK.
                //
                return;
        }
    }
    LC_die( LS_REQ_WIN_VER );
}

/**
 * Gets the name of our parent application's executable.  Note that the caller
 * of this function is responsible for delete[]'ing the string returned.
 */
static LPCWSTR getParentExe() {
    //
    // Take a snap-shot of all the processes.
    //
    HANDLE hSnapShot = ::CreateToolhelp32Snapshot( TH32CS_SNAPPROCESS, 0 );
    if ( hSnapShot == INVALID_HANDLE_VALUE )
        return NULL;

    DWORD const pid = ::GetCurrentProcessId();
    DWORD ppid = 0;

    PROCESSENTRY32 pe;
    ::memset( &pe, 0, sizeof( PROCESSENTRY32 ) );
    pe.dwSize = sizeof( PROCESSENTRY32 );
    //
    // Iterate through all the processes in the snap-shot looking for this
    // process to get our parent proccess ID.
    //
    BOOL next = ::Process32First( hSnapShot, &pe );
    while ( next ) {
        if ( pid == pe.th32ProcessID ) {
            ppid = pe.th32ParentProcessID;
            break;
        }
        pe.dwSize = sizeof( PROCESSENTRY32 );
        next = ::Process32Next( hSnapShot, &pe );
    }

    LPWSTR wParentExe = NULL;
    if ( ppid ) {
        ::memset( &pe, 0, sizeof( PROCESSENTRY32 ) );
        pe.dwSize = sizeof( PROCESSENTRY32 );
        //
        // Now iterate through all the processes again looking for our
        // parent process ID so we can get its exe name.
        //
        next = ::Process32First( hSnapShot, &pe );
        while ( next ) {
            if ( ppid == pe.th32ProcessID ) {
                wParentExe = new WCHAR[ ::wcslen( pe.szExeFile ) + 1 ];
                ::wcscpy( wParentExe, pe.szExeFile );
                break;
            }
            pe.dwSize = sizeof( PROCESSENTRY32 );
            next = ::Process32Next( hSnapShot, &pe );
        }
    }

    ::CloseHandle( hSnapShot );
    return wParentExe;
}

/**
 * Open the given file by calling a call-back method on the Java side.
 */
static void openFile( LPCWSTR wPathToFile, LPCWSTR wParentExe ) {
#ifdef DEBUG
    cout << "*** In openFile()" << endl;
#endif
    JNIEnv *const env = LC_attachCurrentThread();

    static jmethodID openFile_methodID;
    if ( !openFile_methodID ) {
        openFile_methodID = env->GetStaticMethodID(
            g_jpb.main_class, "openFile",
            "(Ljava/lang/String;Ljava/lang/String;)V"
        );
        if ( !openFile_methodID )
            LC_die( TEXT("Could not find openFile() method.") );
    }

    char aPathToFile[ MAX_PATH ];
    if ( !LC_toUTF8( wPathToFile, aPathToFile, sizeof aPathToFile ) )
        LC_die( TEXT("WideCharToMultiByte() failed.") );
    jstring jPathToFile = env->NewStringUTF( aPathToFile );
    if ( !jPathToFile )
        LC_die( TEXT("NewStringUTF() failed.") );

    jstring jParentExe = NULL;
    if ( wPathToFile && *wPathToFile ) {
        char aParentExe[ 80 ];
        if ( !LC_toUTF8( wParentExe, aParentExe, sizeof aParentExe ) )
            LC_die( TEXT("WideCharToMultiByte() failed.") );
        jParentExe = env->NewStringUTF( aParentExe );
        if ( !jParentExe )
            LC_die( TEXT("NewStringUTF() failed.") );
    }

    env->CallStaticVoidMethod(
        g_jpb.main_class, openFile_methodID, jPathToFile, jParentExe
    );
}

/**
 * Open one or more files on the given command-line.
 */
static void openFiles( LPCWSTR wCommandLine, LPCWSTR wParentExe ) {
#ifdef DEBUG
    cout << "*** In openFiles()" << endl;
#endif
    int argc;
    LPWSTR *const wArgv = ::CommandLineToArgvW( wCommandLine, &argc );
    if ( wArgv ) {
        //
        // Start at wArgv[1] because wArgv[0] is the name of the executable
        // (just like in Unix).
        //
        for ( int i = 1; i < argc; ++i )
            openFile( wArgv[i], wParentExe );
        ::LocalFree( wArgv );
    }
}

/**
 * Remove all leading and trailing whitespace from the given string and return
 * it.  The string is modified in-place.
 */
static char* trim( char *s ) {
    while ( isspace( *s ) )
        ++s;
    for ( int i = ::strlen( s ) - 1; i >= 0; --i )
        if ( isspace( s[i] ) )
            s[i] = '\0';
    return s;
}

/**
 * Read the JVM arguments, one per line, from a text file.  Comments and blank
 * lines are ignored.
 */
static char const** readJVMArgs() {
    static char const *jvmArgs[ MaxJVMArgs ];

    ifstream argsFile( "lightzone.jvmargs" );
    if ( !argsFile )
        LC_die( TEXT("Could not open lightzone.jvmargs file.") );

    char buf[ 1024 ];
    int i = 0;
    while ( argsFile.getline( buf, sizeof buf ) ) {
        char const *const arg = trim( buf );
        if ( !*arg || *arg == '#' )
            continue;
        if ( i >= MaxJVMArgs )
            LC_die( TEXT("Too many JVM arguments.") );
        jvmArgs[i++] = new_strdup( arg );
    }
    argsFile.close();
    return jvmArgs;
}

/**
 * Redirect standard output and standard error to a log file.
 */
static void redirectOutput() {
    //
    // Get the path to the "My Documents" folder (pre-Vista) or "Documents"
    // folder (Vista).
    //
    WCHAR wLogFile[ MAX_PATH ];
    HRESULT hResult = ::SHGetFolderPath(
        NULL, CSIDL_PERSONAL, NULL, SHGFP_TYPE_CURRENT, wLogFile
    );
    if ( FAILED( hResult ) )
        return;

    //
    // Create the directory (if necessary) where the log file will go.
    //
    LPCWSTR const wAppName = LC_getAppName();
    ::wcscat( wLogFile, TEXT("\\") );
    ::wcscat( wLogFile, wAppName );
    if ( !LC_makeDir( wLogFile ) )
        return;

    //
    // Complete the full path of the log file.
    //
    ::wcscat( wLogFile, TEXT("\\") );
    ::wcscat( wLogFile, wAppName );
    ::wcscat( wLogFile, TEXT(".log") );

    //
    // Attempt to create the log file.
    //
    HANDLE logHandle = ::CreateFile(
        wLogFile, GENERIC_WRITE, FILE_SHARE_READ, NULL, CREATE_ALWAYS,
        FILE_ATTRIBUTE_NOT_CONTENT_INDEXED | FILE_FLAG_WRITE_THROUGH, NULL
    );
    if ( logHandle == INVALID_HANDLE_VALUE ) {
        //
        // An instance of the application might already be running in which
        // case the log file would already be open for writing exclusively to
        // the first running instance.  Trying to create the log file in this
        // case will result in an ERROR_SHARING_VIOLATION.  If that's the case,
        // just return without either creating or redirecting to the log file.
        //
        DWORD const errorCode = ::GetLastError();
        if ( errorCode == ERROR_SHARING_VIOLATION )
            return;
        //
        // For any other error, however, die.
        //
        LC_die( TEXT("Create log file"), errorCode );
    }

    //
    // Redirect stdout and stderr to the log file.  Note, however, that this
    // will only redirect Java's System.out and System.err.
    //
    if ( !::SetStdHandle( STD_OUTPUT_HANDLE, logHandle ) )
        LC_die( TEXT("Redirect to standard output"), ::GetLastError() );
    if ( !::SetStdHandle( STD_ERROR_HANDLE, logHandle ) )
        LC_die( TEXT("Redirect to standard error"), ::GetLastError() );

    //
    // To redirect C's stdout and stderr as well as C++'s cout and cerr to the
    // same log file, we have to convert logHandle to a FILE* then replace
    // stdout and stderr with it.
    //
#if _WIN64 || __amd64__
    int const logFD = _open_osfhandle( reinterpret_cast<long long>( logHandle ), 0 );
#else
    int const logFD = _open_osfhandle( reinterpret_cast<long>( logHandle ), 0 );
#endif
    FILE *const logFile = ::_fdopen( logFD, "w" );
    ::setvbuf( logFile, NULL, _IONBF, 0 );
    ::fclose( stdout ); *stdout = *logFile;
    ::fclose( stderr ); *stderr = *logFile;
}

/**
 * Run our application.
 */
int APIENTRY WinMain( HINSTANCE, HINSTANCE, LPSTR, int ) {
    redirectOutput();
    checkWindowsVersion();
    checkCPUType();

    if ( !::SetCurrentDirectory( LC_getExeDirectory() ) )
        LC_die( TEXT("Set current directory"), ::GetLastError() );

    checkJars();

    LPCWSTR const wCommandLine = ::GetCommandLine();

    ///////////////////////////////////////////////////////////////////////////

    //
    // Create a named pipe to use for communication between the second and the
    // first running application instances.
    //
    // When a second instance is launched by Windows (either by the user
    // double-clicking the LightZone.exe icon or by the user dragging image
    // files onto the LightZone.exe icon), the second instance detects that
    // it is a second instance and then sends the files to open to the first
    // instance via the pipe.
    //
    // By specifying FILE_FLAG_FIRST_PIPE_INSTANCE and maxInstances of 1, the
    // first instance successfully creates the pipe.  Secondary instances will
    // fail because the pipe already exists and thus know they're secondary
    // instances.
    //
    WCHAR wPipeName[ 32 ] = TEXT("\\\\.\\pipe\\");
    ::wcscat( wPipeName, LC_getAppName() );
    HANDLE pipe = ::CreateNamedPipe(
        wPipeName,
        PIPE_ACCESS_DUPLEX | FILE_FLAG_FIRST_PIPE_INSTANCE,
        PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT,
        1,                              // maxInstances
        sizeof ACK,                     // output buffer size
        PipeBufSize,                    // input buffer size
        NMPWAIT_USE_DEFAULT_WAIT,
        NULL
    );
    if ( pipe == INVALID_HANDLE_VALUE ) {
        DWORD const error = ::GetLastError();
        if ( error == ERROR_ACCESS_DENIED || error == ERROR_PIPE_BUSY ) {
            //
            // We're a second instance of the appplication.
            //
            int const wCommandLineLen = ::wcslen( wCommandLine );
            if ( wCommandLineLen ) {
                //
                // We have files to open: send our command-line to the first
                // instance to have it open the files instead of us.
                //
                LPWSTR wPipeBuf;
                int wPipeBufLen;

                if ( LPCWSTR const wParentExe = getParentExe() ) {
                    //
                    // We have a parent process that requested the files to be
                    // opened: prepend the name of the parent process's exe
                    // followed by a ':' to the command-line.
                    //
                    int const wParentExeLen = ::wcslen( wParentExe );
                    wPipeBufLen = wParentExeLen + 1 + wCommandLineLen;
                    wPipeBuf = new WCHAR[ wPipeBufLen + 1 /* for null */ ];
                    ::wcscpy( wPipeBuf, wParentExe );
                    ::wcscat( wPipeBuf, TEXT(":" ) );
                    ::wcscat( wPipeBuf, wCommandLine );
                    //
                    // Note: we don't care about delete[]'ing wParentExe since
                    // this process is going to die very soon.
                    //
                } else {
                    //
                    // No parent process (I don't think this is ever the case
                    // under Windows, but it's defensive programming to assume
                    // that it can happen): just send the command-line as-is.
                    //
                    wPipeBuf = const_cast<LPWSTR>( wCommandLine );
                    wPipeBufLen = wCommandLineLen;
                }

                char ackBuf[ sizeof ACK ];
                DWORD bytesRead;
                BOOL const sent = ::CallNamedPipe(
                    wPipeName, wPipeBuf,
                    (wPipeBufLen + 1 /* include null */) * sizeof( WCHAR ),
                    ackBuf, sizeof ackBuf, &bytesRead, NMPWAIT_USE_DEFAULT_WAIT
                );
                if ( !sent )
                    LC_die(
                        TEXT("Communication with running instance"),
                        ::GetLastError()
                    );
            } else {
                //
                // We have no files to open (the user must have just double-
                // clicked the application icon or a shortcut thereto), so just
                // bring the top-most window to the front.
                //
                ::EnumWindows( &bringAppWindowToFront, 0 );
            }
            ::exit( 0 );                // There can be only one....
        }
        LC_die( TEXT("CreateNamedPipe()"), error );
    }

    ///////////////////////////////////////////////////////////////////////////

    //
    // If we get here, we're the first instance of the application: fire up a
    // JVM.
    //
    initJavaParamBlock( &g_jpb, readJVMArgs() );
    startJava( &g_jpb );

    //
    // If we were given any files to open, open them.
    //
    LPCWSTR wParentExe = getParentExe();
    openFiles( wCommandLine, wParentExe );
    delete[] wParentExe;

    ///////////////////////////////////////////////////////////////////////////

    //
    // Now wait for secondary application instances to connect to us and read
    // their command-lines and open the files they specify.
    //
    while ( true ) {
        BOOL const connected = ::ConnectNamedPipe( pipe, NULL ) ?
            TRUE : (::GetLastError() == ERROR_PIPE_CONNECTED);
        if ( connected ) {
            ::EnumWindows( &bringAppWindowToFront, 0 );

            WCHAR wPipeBuf[ PipeBufSize ];
            DWORD byteCount;
            BOOL success = ::ReadFile(
                pipe, wPipeBuf, sizeof wPipeBuf, &byteCount, NULL
            );
            if ( success ) {
                LPCWSTR wCommandLine, wParentExe;
                //
                // We got a command-line from a secondary application instance:
                // first see if it's preceeded by an exe name.
                //
                WCHAR *const colon = ::wcschr( wPipeBuf, TEXT(':') );
                if ( colon ) {
                    wParentExe = wPipeBuf;
                    wCommandLine = colon + 1;
                    *colon = 0;
                } else {
                    wParentExe = NULL;
                    wCommandLine = wPipeBuf;
                }

                openFiles( wCommandLine, wParentExe );
                //
                // Even though we have no need to send anything back to the
                // second instance, apparently Windows isn't happy unless we do
                // (the second instance will get an ERROR_PIPE_NOT_CONNECTED
                // error), so just send it a tiny packet containing "ACK".
                //
                success = ::WriteFile(
                    pipe, ACK, sizeof ACK, &byteCount, NULL
                );
                if ( !success )
                    LC_warn(
                        TEXT("Instance acknowledgement"), ::GetLastError()
                    );
            } else
                LC_warn( TEXT("Opening files"), ::GetLastError() );
        }
        ::FlushFileBuffers( pipe );
        ::DisconnectNamedPipe( pipe );
    }

    return 0;
}

/* vim:set et sw=4 ts=4: */
