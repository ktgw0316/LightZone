// standard
#include <cstring>
#include <jni.h>

#ifdef  DEBUG
#include <iostream>
#endif

#include "LC_JNIUtils.h"                /* defines __WINDOWS__ */

// platform
#ifdef  __APPLE__
#   import <Cocoa/Cocoa.h>
#   include <sys/types.h>
#elif defined( __WINDOWS__ )
#   include <windows.h>
#   include <wininet.h>

    //
    // These WinInet error codes aren't currently defined by either MinGW or
    // Cygwin, so define them ourselves.
    //
#   ifndef ERROR_INTERNET_DISCONNECTED
#   define ERROR_INTERNET_DISCONNECTED              12163
#   endif

#   ifndef ERROR_INTERNET_FAILED_DUETOSECURITYCHECK
#   define ERROR_INTERNET_FAILED_DUETOSECURITYCHECK 12171
#   endif

#   ifndef ERROR_INTERNET_NOT_INITIALIZED
#   define ERROR_INTERNET_NOT_INITIALIZED           12172
#   endif

#   ifndef ERROR_INTERNET_PROXY_SERVER_UNREACHABLE
#   define ERROR_INTERNET_PROXY_SERVER_UNREACHABLE  12165
#   endif

#   ifndef ERROR_INTERNET_SERVER_UNREACHABLE
#   define ERROR_INTERNET_SERVER_UNREACHABLE        12164
#   endif

#else
#   error   Can compile only for either Mac OS X or Windows.
#endif  /* __APPLE__ | __WINDOWS__ */

// eSellerate
#ifdef  __APPLE__
#   include "EWSLib.h"
#   define SUCCEEDED( errorCode ) ( (errorCode) >= 0 )
#else
#   include "eWebLibrary.h"
#   define OSStatus HRESULT
#   define eWeb_ValidateSerialNumber eSellerate_ValidateSerialNumber
#endif
#include "validate.h"

// local
#include "LC_CPPUtils.h"
#ifdef __WINDOWS__
#include "LC_WinUtils.h"
#endif
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_license_eSellerateLicenseLibrary.h"
#endif

using namespace std;
using namespace LightCrafts;

//
// eSellerate constants for the Light Crafts account.
//
#define ES_PUBLISHER_ID     "PUB8462586885"
#define ES_PUBLISHER_KEY    "53781"
#define ES_ACTIVATION_ID    "ACT424162180"

//
// The file that the eSellerate engine is contained in.
//
#ifdef  __APPLE__
#   define ES_ENGINE_FILE  "EWSMacCompress.tar.gz"
#else   /* __WINDOWS__ */
#   define ES_ENGINE_FILE  "eWebClient.dll"
#endif  /* __APPLE__ | __WINDOWS__ */

#define eSellerate_METHOD(method) \
        name4(Java_,com_lightcrafts_license_eSellerateLicenseLibrary,_,method)

#define eSellerate_CONSTANT(constant) \
        name3(com_lightcrafts_license_eSellerateLicenseLibrary,_,constant)

////////// Local functions ////////////////////////////////////////////////////

#ifdef  __WINDOWS__
/**
 * Given an error code from eSellerate, extract the Windows' error code from
 * it.
 */
inline DWORD getWindowsErrorCode( DWORD errorCode ) {
    return errorCode & 0x0000FFFF;
}

/**
 * Given a Windows error code, format an error message string.  The string
 * returned is from a static buffer, so this is not thread-safe.
 */
static char const* getWindowsErrorMessage( DWORD errorCode ) {
    LPVOID lpMsgBuf;
    DWORD const nChars = ::FormatMessage(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM, NULL,
        errorCode, MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
        (LPTSTR)&lpMsgBuf, 0, NULL
    );
    if ( !nChars )
        return NULL;

    WCHAR wMsgBuf[ 256 ]; 
    ::wsprintf( wMsgBuf, TEXT("LOCALIZED:%ls"), lpMsgBuf ); 

    ::LocalFree( lpMsgBuf );

    static char aMsgBuf[ 256 ];
    LC_toUTF8( wMsgBuf, aMsgBuf, sizeof aMsgBuf );
    return aMsgBuf;
}
#endif  /* __WINDOWS__ */

/**
 * Converts an eSellerate error code to an error message.
 */
static char const* getESErrorMessage( OSStatus errorCode ) {
    switch ( errorCode ) {

        ////////// Activate SN

        case E_ACTIVATESN_BLACKLISTED_SN:
            return "E_ACTIVATESN_BLACKLISTED_SN";

        case E_ACTIVATESN_FINALIZATION_ERROR:
            return "E_ACTIVATESN_FINALIZATION_ERROR";

        case E_ACTIVATESN_IMPROPER_USAGE:
            return "E_ACTIVATESN_IMPROPER_USAGE";

        case E_ACTIVATESN_INVALID_ORDER:
            return "E_ACTIVATESN_INVALID_ORDER";

        case E_ACTIVATESN_LIMIT_MET:
            return "E_ACTIVATESN_LIMIT_MET";

        case E_ACTIVATESN_NO_SN_FROM_SERVER:
            return "E_ACTIVATESN_NO_SN_FROM_SERVER";

        case E_ACTIVATESN_NOT_UNIQUE:
            return "E_ACTIVATESN_NOT_UNIQUE";

#ifdef  __APPLE__
        case E_ACTIVATESN_UNKNOWN_ACTIVATION_KEY:
#else
        case E_ACTIVATION_UNKNOWN_ACTIVATION_KEY:
#endif
            return "E_ACTIVATESN_UNKNOWN_ACTIVATION_KEY";

        case E_ACTIVATESN_UNKNOWN_SERVER_ERROR:
            return "E_ACTIVATESN_UNKNOWN_SERVER_ERROR.";

        case E_ACTIVATESN_UNKNOWN_SN:
            return "E_ACTIVATESN_UNKNOWN_SN";

#ifdef  __WINDOWS__
        case E_ACTIVATION_INVALID_ACTIVATION_KEY:
            return "E_ACTIVATION_INVALID_ACTIVATION_KEY";
#endif

        ////////// Activation

        case E_ACTIVATION_MANUAL_CANCEL:
            return "E_ACTIVATION_MANUAL_CANCEL";

        ////////// Engine

#ifdef  __WINDOWS__
        case E_ENGINE_BAD_SDK_INPUT:
            return "E_ENGINE_BAD_SDK_INPUT";
#endif

        case E_ENGINE_INTERNAL_ERROR:
            return "E_ENGINE_INTERNAL_ERROR";

        case E_ENGINE_PURCHASE_NOTSUCCESSFUL:
            return "E_ENGINE_PURCHASE_NOTSUCCESSFUL";

        ////////// SDK

        case E_SDK_BAD_PARAMETER:
            return "E_SDK_BAD_PARAMETER";

        case E_SDK_CREATE_ENTRY_ERROR:
            return "E_SDK_CREATE_ENTRY_ERROR";

#ifdef  __APPLE__
        case E_SDK_ENGINE_BUSY:
            return "E_SDK_ENGINE_BUSY";
#endif

        case E_SDK_ENGINE_CORRUPTED:
            return "E_SDK_ENGINE_CORRUPTED";

        case E_SDK_ENGINE_NOT_INSTALLED:
            return "E_SDK_ENGINE_NOT_INSTALLED";

#ifdef  __APPLE__
        case E_SDK_ENGINE_NOT_LOADED:
            return "E_SDK_ENGINE_NOT_LOADED";
#endif

#ifdef  __WINDOWS__
        case E_SDK_ENGINE_RES_ERR:
            return "E_SDK_ENGINE_RES_ERR";

        case E_SDK_ERROR_REGISTERING_ENGINE:
            return "E_SDK_ERROR_REGISTERING_ENGINE";
#endif

        case E_SDK_OBJECT_NOT_FOUND:
            return "E_SDK_OBJECT_NOT_FOUND";

        ////////// Inet

        case E_INET_CONNECTION_FAILURE:
            return "E_INET_CONNECTION_FAILURE";

#ifdef  __APPLE__
        case E_INET_DOWNLOAD_ENGINE_FAILURE:
            return "E_INET_DOWNLOAD_ENGINE_FAILURE";

        case E_INET_ESELLERATE_FAILURE:
            return "E_INET_ESELLERATE_FAILURE";

        case E_INET_ESTATE_NOT_FOUND:
            return "E_INET_ESTATE_NOT_FOUND";
#endif

#ifdef  __WINDOWS__
        case E_INET_DEVICE_CONNECTION_FAILURE:
            return "E_INET_DEVICE_CONNECTION_FAILURE";
#endif

        ////////// OS

        case E_OS_FUNCTIONALITY_UNSUPPORTED:
            return "E_OS_FUNCTIONALITY_UNSUPPORTED";

        ////////// Validation

        case E_VALIDATEACTIVATION_MACHINE_MISMATCH:
            return "E_VALIDATEACTIVATION_MACHINE_MISMATCH";

#ifdef  __APPLE__
        case E_VALIDATEACTIVATION_OLD_ACTIVATION_KEY:
            return "E_VALIDATEACTIVATION_OLD_ACTIVATION_KEY";
#endif

        ////////// Light Crafts error codes ///////////////////////////////////

        case eSellerate_CONSTANT(LC_INVALID_LICENSE_FILE):
            return "LC_INVALID_LICENSE_FILE";

        case eSellerate_CONSTANT(LC_INVALID_LICENSE_KEY):
            return "LC_INVALID_LICENSE_KEY";

        case eSellerate_CONSTANT(LC_WRITE_LICENSE_KEY_FAILED):
            return "LC_WRITE_LICENSE_KEY_FAILED";

        default:
#ifdef  __WINDOWS__
            char const *const errorMessage =
                getWindowsErrorMessage( getWindowsErrorCode( errorCode ) );
            if ( errorMessage )
                return errorMessage;
#endif
            return NULL;
    }
}

/**
 * In some cases, eSellerate library functions return codes >= 0 for success
 * and < 0 for failure.  This is dumb.  If the function succeeds, we don't
 * really care what the code is, so just change it to E_SUCCESS.
 */
inline OSStatus esError( OSStatus errorCode ) {
    return SUCCEEDED( errorCode ) ? E_SUCCESS : errorCode;
}

#ifdef __APPLE__
/**
 * The eWeb_ActivateSerialNumber() makes use of WebKit.  Starting with the
 * Safari 4 beta, eWeb_ActivateSerialNumber() would crash the app because
 * eSellerate claims it must be run on the main thread.  An attempt was made to
 * use a Cocoa object proxy with performSelectorOnMainThread, but that
 * deadlocked.  The only known solution is to call eWeb_ActivateSerialNumber()
 * in another process.
 */
static OSStatus activate( char const *cKey ) {
    NSTask *const task = [[NSTask alloc] init];
    [task setLaunchPath:@"./eSellerateActivate"];
    NSString *const nsKey = [NSString stringWithUTF8String:cKey];
    NSArray *const args = [NSArray arrayWithObjects: nsKey, nil];
    [task setArguments:args];
    NSPipe *const pipe = [NSPipe pipe];
    [task setStandardOutput:pipe];
    NSFileHandle *const file = [pipe fileHandleForReading];
    [task launch];
    NSData *const data = [file readDataToEndOfFile];
    NSString *const out =
        [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    return [out intValue];
}
#endif  /* __APPLE__ */

////////// JNI ////////////////////////////////////////////////////////////////

/**
 * This is called by the Java class-loader.  We use it to set our global
 * pointer to the JVM instance.
 */
JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *jvm, void* ) {
    return JNI_VERSION_1_4;
}

/**
 * JNI wrapper around eWeb_ActivateSerialNumber().
 */
JNIEXPORT jint JNICALL eSellerate_METHOD(ESactivateSerialNumber)
    ( JNIEnv *env, jclass, jstring jKey )
{
    jstring_to_c const cKey( env, jKey );
#ifdef DEBUG
    cout << "eWeb_ActivateSerialNumber(" << cKey << ')' << endl;
#endif
#ifdef  __APPLE__
    NSAutoreleasePool *const pool = [[NSAutoreleasePool alloc] init];
    OSStatus errorCode = activate( cKey );
    [pool release];
#else
    OSStatus errorCode = eWeb_ActivateSerialNumber(
        ES_PUBLISHER_ID, ES_ACTIVATION_ID, cKey,
        0 // do not do manual activation
    );
#endif  /* __APPLE__ */
#ifdef DEBUG
    cout << "  result = " << errorCode << endl;
#endif
    return esError( errorCode );
}

#if 0
/**
 * JNI wrapper around eWeb_DeactivateSerialNumber().
 */
JNIEXPORT jint JNICALL eSellerate_METHOD(ESdeactivateSerialNumber)
    ( JNIEnv *env, jclass, jstring jSN )
{
    jstring_to_c const cSN( env, jSN );
#ifdef DEBUG
    cout << "eWeb_DeactivateSerialNumber(" << cSN << ')' << endl;
#endif
    OSStatus const errorCode = eWeb_DeactivateSerialNumber(
        ES_PUBLISHER_ID, ES_ACTIVATION_ID, cSN
#ifdef  __WINDOWS__
        , 0 // do not do manual deactivation
#endif
    );

#ifdef DEBUG
    cout << "  result = " << errorCode << endl;
#endif
    return esError( errorCode );
}
#endif

/**
 * Get the error message for an eSellerate error code.
 */
JNIEXPORT jstring JNICALL eSellerate_METHOD(ESgetErrorMessage)
    ( JNIEnv *env, jclass, jint errorCode )
{
    char const *const esErrorMsg = getESErrorMessage( errorCode );
    return esErrorMsg ? env->NewStringUTF( esErrorMsg ) : NULL;
}

/**
 * Initialize.
 */
JNIEXPORT jint JNICALL eSellerate_METHOD(ESinitialize)
    ( JNIEnv *env, jclass )
{
#ifdef  __APPLE__
    //
    // This relies upon the fact that, when the application is running, its
    // current working directory is set to where the eSellerate engine file is.
    //
    return esError( eWeb_InstallEngineFromPath( ES_ENGINE_FILE ) );
#else
    //
    // For Windows, the eSellerate engine is installed by the installer.
    //
    return 0;
#endif
}

/**
 * Checks whether a given eSellerate error is "forgivable."
 */
JNIEXPORT jboolean JNICALL eSellerate_METHOD(ESisForgivableError)
    ( JNIEnv *env, jclass, jint errorCode )
{
    switch ( errorCode ) {

        case E_ACTIVATESN_FINALIZATION_ERROR:
        case E_ACTIVATESN_NO_SN_FROM_SERVER:
        case E_ACTIVATESN_UNKNOWN_SERVER_ERROR:
        case E_ACTIVATESN_UNKNOWN_SN:
        case E_ENGINE_INTERNAL_ERROR:
        case E_INET_CONNECTION_FAILURE:
        case E_OS_FUNCTIONALITY_UNSUPPORTED:
        case E_SDK_BAD_PARAMETER:
        case E_SDK_CREATE_ENTRY_ERROR:
        case E_SDK_ENGINE_CORRUPTED:
        case E_SDK_ENGINE_NOT_INSTALLED:
        case E_SDK_OBJECT_NOT_FOUND:

#ifdef  __APPLE__
        case E_INET_DOWNLOAD_ENGINE_FAILURE:
        case E_INET_ESELLERATE_FAILURE:
        case E_INET_ESTATE_NOT_FOUND:
        case E_SDK_ENGINE_BUSY:
        case E_SDK_ENGINE_NOT_LOADED:
#endif

#ifdef  __WINDOWS__
        case E_ENGINE_BAD_SDK_INPUT:
        case E_SDK_ENGINE_RES_ERR:
        case E_SDK_ERROR_REGISTERING_ENGINE:
        case E_INET_DEVICE_CONNECTION_FAILURE:
#endif
            return JNI_TRUE;

        default:
            return JNI_FALSE;
    }
}

/**
 * Checks whether a given eSellerate error is internet-related
 */
JNIEXPORT jboolean JNICALL eSellerate_METHOD(ESisInternetError)
    ( JNIEnv *env, jclass, jint errorCode )
{
    switch ( errorCode ) {
#ifdef  __WINDOWS__
        //
        // If the error isn't one of the eSellerate internet errors listed
        // below, assume the error is a Windows' error and convert the error
        // code.
        //
        // Note: there is no requirement in either C or C++ that the default
        // case of a switch statement be last.  It's handy in this situation
        // where we want to convert the error code and fall through.
        //
        default:
            errorCode = getWindowsErrorCode( errorCode );
            // no break;

        //
        // Relevant Windows' internet errors
        //
        case ERROR_INTERNET_CANNOT_CONNECT:
        case ERROR_INTERNET_DISCONNECTED:
        case ERROR_INTERNET_FAILED_DUETOSECURITYCHECK:
        case ERROR_INTERNET_INTERNAL_ERROR:
        case ERROR_INTERNET_ITEM_NOT_FOUND:
        case ERROR_INTERNET_NAME_NOT_RESOLVED:
        case ERROR_INTERNET_NOT_INITIALIZED:
        case ERROR_INTERNET_NO_DIRECT_ACCESS:
        case ERROR_INTERNET_OUT_OF_HANDLES:
        case ERROR_INTERNET_PROXY_SERVER_UNREACHABLE:
        case ERROR_INTERNET_SERVER_UNREACHABLE:
        case ERROR_INTERNET_SHUTDOWN:
        case ERROR_INTERNET_TCPIP_NOT_INSTALLED:
        case ERROR_INTERNET_TIMEOUT:

        //
        // Relevant Windows-specific eSellerate errors
        //
        case E_INET_DEVICE_CONNECTION_FAILURE:
#endif  /* __WINDOWS__ */

#ifdef  __APPLE__
        //
        // Relevant Mac-specific eSellerate errors
        //
        case E_INET_ESELLERATE_FAILURE:
#endif
        //
        // Relevant cross-platform eSellerate errors
        //
        case E_INET_CONNECTION_FAILURE:

        /*
        case E_ACTIVATESN_UNKNOWN_SERVER_ERROR:
        case E_ACTIVATESN_FINALIZATION_ERROR:
        case E_ACTIVATESN_NO_SN_FROM_SERVER:
        */
            return JNI_TRUE;

    }
    return JNI_FALSE;
}

/**
 * JNI wrapper around eWeb_ManualActivateSerialNumber().
 */
JNIEXPORT jint JNICALL eSellerate_METHOD(ESmanualActivateSerialNumber)
    ( JNIEnv *env, jclass, jstring jKey )
{
    jstring_to_c const cKey( env, jKey );
#ifdef DEBUG
    cout << "eWeb_ManualActivateSerialNumber(" << cKey << ')' << endl;
#endif
    OSStatus const errorCode = eWeb_ManualActivateSerialNumber(
        ES_PUBLISHER_ID, ES_ACTIVATION_ID, cKey, "LightZone", NULL
    );
#ifdef DEBUG
    cout << "  result = " << errorCode << endl;
#endif
    return esError( errorCode );
}

/**
 * JNI wrapper around eWeb_ValidateActivation().
 */
JNIEXPORT jint JNICALL eSellerate_METHOD(ESvalidateActivation)
    ( JNIEnv *env, jclass, jstring jKey )
{
    jstring_to_c const cKey( env, jKey );
#   ifdef DEBUG
    cout << "eWeb_ValidateActivation(" << cKey << ')' << endl;
#   endif
    OSStatus const errorCode =
        eWeb_ValidateActivation( ES_PUBLISHER_ID, ES_ACTIVATION_ID, cKey );
#   ifdef DEBUG
    cout << "  result = " << errorCode << endl;
#   endif
    return esError( errorCode );
}

/**
 * JNI wrapper around eWeb_ValidateSerialNumber().
 */
JNIEXPORT jint JNICALL eSellerate_METHOD(ESvalidateSerialNumber)
    ( JNIEnv *env, jclass, jstring jKey )
{
    jstring_to_c const cKey( env, jKey );
#   ifdef DEBUG
    cout << "eWeb_ValidateSerialNumber(" << cKey << ')' << endl;
#   endif
    eSellerate_DaysSince2000 const days = eWeb_ValidateSerialNumber(
#ifdef  __WINDOWS__
        //
        // The Windows version of this function has a non-const char* as an
        // argument even though it should have a const char*.
        //
        const_cast<char*>( static_cast<char const*>( cKey ) ),
#else
        cKey,
#endif  /* __WINDOWS__ */
        0, 0, ES_PUBLISHER_KEY
    );
    OSStatus const errorCode = days > 0 ?
        E_SUCCESS : eSellerate_CONSTANT(LC_INVALID_LICENSE_KEY);
#   ifdef DEBUG
    cout << "  result = " << errorCode << endl;
#   endif
    return errorCode;
}

/* vim:set et sw=4 ts=4: */
