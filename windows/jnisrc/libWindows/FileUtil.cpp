/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstring>
#include <string>

// windows
#include <windows.h>
#include <shlobj.h>

#ifdef DEBUG
#include <iostream>
#endif

// local
#include "LC_CPPUtils.h"
#include "LC_JNIUtils.h"
#include "LC_WinError.h"
#include "LC_WinUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_windows_WindowsFileUtil.h"
#endif

using namespace std;
using namespace LightCrafts;

/**
 * The time-out value (in milliseconds) that specifies the maximum amount of
 * time to spend resolving a link.
 */
int const SHORTCUT_RESOLVE_TIMEOUT = 1000;

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsFileUtil_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_windows_WindowsFileUtil,_,method)

/**
 * Make a file hidden so that it doesn't show up in Windows Explorer.
 */
JNIEXPORT jstring JNICALL WindowsFileUtil_METHOD(getFolderPathOf)
    ( JNIEnv *env, jclass, jint jFolderID )
{
    WCHAR wPath[ MAX_PATH ];
    HRESULT result =
        ::SHGetFolderPath( NULL, jFolderID, NULL, SHGFP_TYPE_CURRENT, wPath );
    return SUCCEEDED( result ) ? LC_wTojstring( env, wPath ) : NULL;
}

/**
 * Make a file hidden so that it doesn't show up in Windows Explorer.
 */
JNIEXPORT void JNICALL WindowsFileUtil_METHOD(hideFile)
    ( JNIEnv *env, jclass, jstring jFileName )
{
    jstring_to_w const wFileName( env, jFileName );
    DWORD attributes = ::GetFileAttributes( wFileName );
    if ( attributes != INVALID_FILE_ATTRIBUTES ) {
        attributes |= FILE_ATTRIBUTE_HIDDEN;
        if ( ::SetFileAttributes( wFileName, attributes ) ) 
            return;
    }
    LC_throwIOException(
        env, LC_formatError( "hideFile()", ::GetLastError() )
    );
}

/**
 * Move a set of files to the Recycle Bin.
 */
JNIEXPORT jboolean JNICALL WindowsFileUtil_METHOD(moveToRecycleBin)
    ( JNIEnv *env, jclass, jobjectArray jFiles )
{
    int const nFiles = env->GetArrayLength( jFiles );

    //
    // Compute size of "from" buffer.
    //
    int fromBufSize = 0;
    for ( int i = 0; i < nFiles; ++i ) {
        jstring const jFile = (jstring)env->GetObjectArrayElement( jFiles, i );
        fromBufSize += env->GetStringLength( jFile ) + 1;
    }
    ++fromBufSize;                      // for final '\0'

    //
    // Build "from" string.
    //
    auto_vec<WCHAR> fromBuf( new WCHAR[ fromBufSize ] );
    WCHAR *p = fromBuf;
    for ( int i = 0; i < nFiles; ++i ) {
        jstring const jFile = (jstring)env->GetObjectArrayElement( jFiles, i );
        jstring_to_w const wFile( env, jFile );
        ::wcscpy( p, wFile );
        p += env->GetStringLength( jFile ) + 1;
    }
    *p = '\0';

    ////////// Populate SHFILEOPSTRUCT ////////////////////////////////////////

    SHFILEOPSTRUCT fop;
    ::memset( &fop, 0, sizeof fop );
    fop.wFunc = FO_DELETE;
    fop.pFrom = fromBuf;
    fop.fFlags =
        // Move to Recycle Bin instead of immediate deletion.
        FOF_ALLOWUNDO

        // Respond with "Yes to All" for any dialog box that is displayed.
        | FOF_NOCONFIRMATION

        // Do not display a user interface if an error occurs.
        | FOF_NOERRORUI

        // Give the file being operated on a new name in a move, copy, or
        // rename operation if a file with the target name already exists.
        | FOF_RENAMEONCOLLISION

        // Do not display a progress dialog box.
        // | FOF_SILENT

        // Display a progress dialog box but do not show the file names.
        // | FOF_SIMPLEPROGRESS

        // Send a warning if a file is being destroyed during a delete
        // operation rather than recycled.  This flag partially overrides
        // FOF_NOCONFIRMATION.
        // | FOF_WANTNUKEWARNING
        ;

    ////////// Move files to Recycle bin //////////////////////////////////////

    int const result = ::SHFileOperation( &fop );
    return !result ? JNI_TRUE : JNI_FALSE;
}

/**
 * Display the native open-file dialog.
 */
JNIEXPORT jstring JNICALL WindowsFileUtil_METHOD(openFile)
    ( JNIEnv *env, jclass, jstring jInitialDir,
      jobjectArray jFilterDisplayStrings, jobjectArray jFilterPatterns )
{
    jstring_to_w const wInitialDir( env, jInitialDir );

    WCHAR wFileName[ MAX_PATH ];
    ::memset( wFileName, 0, sizeof wFileName );

    ////////// Build filter ///////////////////////////////////////////////////

    //
    // Ensure lengths of arrays match.
    //
    int const nDisplayStrings = env->GetArrayLength( jFilterDisplayStrings );
    int const nPatterns = env->GetArrayLength( jFilterPatterns );
    if ( nDisplayStrings != nPatterns ) {
        LC_throwIllegalArgumentException( env, "nDisplayStrings != nPatterns" );
        return NULL;
    }

    //
    // Compute size of filter buffer.
    //
    int filterBufSize = 0;
    for ( int i = 0; i < nDisplayStrings; ++i ) {
        jstring const jDisplayString =
            (jstring)env->GetObjectArrayElement( jFilterDisplayStrings, i );
        filterBufSize += env->GetStringLength( jDisplayString ) + 1;
        jstring const jPattern =
            (jstring)env->GetObjectArrayElement( jFilterPatterns, i );
        filterBufSize += env->GetStringLength( jPattern ) + 1;
    }
    ++filterBufSize;                    // for final '\0'

    //
    // Build filter string.
    //
    auto_vec<WCHAR> filterBuf( new WCHAR[ filterBufSize ] );
    WCHAR *p = filterBuf;
    for ( int i = 0; i < nDisplayStrings; ++i ) {
        jstring const jDisplayString =
            (jstring)env->GetObjectArrayElement( jFilterDisplayStrings, i );
        jstring_to_w const wDisplayString( env, jDisplayString );
        ::wcscpy( p, wDisplayString );
        p += env->GetStringLength( jDisplayString ) + 1;

        jstring const jPattern =
            (jstring)env->GetObjectArrayElement( jFilterPatterns, i );
        jstring_to_w const wPattern( env, jPattern );
        ::wcscpy( p, wPattern );
        p += env->GetStringLength( jPattern ) + 1;
    }
    *p = '\0';

    ////////// Populate OPENFILENAME struct ///////////////////////////////////

    OPENFILENAME ofn;
    ::memset( &ofn, 0, sizeof ofn );
    ofn.Flags = OFN_PATHMUSTEXIST | OFN_FILEMUSTEXIST;
    ofn.lpstrFile = wFileName;
    ofn.lpstrFilter = filterBuf;
    ofn.lpstrInitialDir = wInitialDir;
    ofn.lStructSize = sizeof ofn;
    ofn.nFilterIndex = 1;
    ofn.nMaxFile = sizeof wFileName / sizeof wFileName[0];

    ////////// Open a file ////////////////////////////////////////////////////

    if ( !::GetOpenFileName( &ofn ) ) {
        DWORD const error = ::CommDlgExtendedError();
        if ( error )
            LC_throwIOException(
                env, LC_formatError( "GetOpenFileName()", error )
            );
        return NULL;
    }

    return LC_wTojstring( env, wFileName );
}

/**
 * Resolve a Windows shortcut file.
 *
 * See: http://msdn.microsoft.com/library/default.asp?url=/library/en-us/shellcc/platform/shell/programmersguide/shell_int/shell_int_programming/shortcuts/shortcut.asp
 */
JNIEXPORT jstring JNICALL WindowsFileUtil_METHOD(resolveShortcutImpl)
    ( JNIEnv *env, jclass, jstring jPath )
{
    IShellLink *link = NULL;
    IPersistFile *ipf = NULL;
    jstring_to_w const wPath( env, jPath );
    DWORD flags_high, flags_low;

    HRESULT result = NULL;
    // Initialize the COM system.
    if ( FAILED( ::CoInitialize( NULL ) ) )
        goto error;

    // Get a pointer to the IShellLink interface.
    result = ::CoCreateInstance(
        CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER, IID_IShellLink,
        (LPVOID*)&link
    );
    if ( FAILED( result ) )
        goto error;

    // Get a pointer to the IPersistFile interface.
    if ( FAILED( link->QueryInterface( IID_IPersistFile, (void**)&ipf ) ) )
        goto error;

    // Load the shortcut.
    if ( FAILED( ipf->Load( wPath, STGM_READ ) ) )
        goto error;

    //
    // Resolve the link.
    //
    // When SLR_NO_UI is set, the high-order word of flags can be set to a
    // time-out value that specifies the maximum amount of time in milliseconds
    // to be spent resolving the link.
    //
    flags_high = SHORTCUT_RESOLVE_TIMEOUT << sizeof( DWORD ) / 2 * 8;
    flags_low =
        SLR_NOLINKINFO |
        SLR_NOSEARCH   |
        SLR_NOTRACK    |
        SLR_NOUPDATE   |
        SLR_NO_UI      ;

    if ( FAILED( link->Resolve( NULL, flags_high | flags_low ) ) )
        goto error;

    // Get the path to the link target.
    WCHAR wResolvedPath[ MAX_PATH ];
    result = link->GetPath( wResolvedPath, MAX_PATH, NULL, SLGP_UNCPRIORITY );
    if ( FAILED( result ) )
        goto error;

    jPath = LC_wTojstring( env, wResolvedPath );
    goto done;

error:
    jPath = NULL;
done:
    if ( ipf )
        ipf->Release();
    if ( link )
        link->Release();
    ::CoUninitialize();
    return jPath;
}
/* vim:set et sw=4 ts=4: */
