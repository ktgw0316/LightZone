/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstring>

// windows
#include <windows.h>
#include <shlobj.h>
#include <shlwapi.h>

// local
#include "LC_JNIUtils.h"
#include "LC_WinError.h"
#include "LC_WinUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_windows_WindowsSavedSearch.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// Local //////////////////////////////////////////////////////////////

/**
 * A simple struct to hold onto things between calls to getNextResult().
 */
struct SavedSearch {
    SavedSearch();
    ~SavedSearch();

    LPENUMIDLIST    m_pEnumIDList;
    LPSHELLFOLDER   m_pFolder;          // the saved search virtual folder
    LPMALLOC        m_pMalloc;
};

SavedSearch::SavedSearch() {
    m_pEnumIDList = NULL;
    m_pFolder     = NULL;
    m_pMalloc     = NULL;
}

SavedSearch::~SavedSearch() {
    if ( m_pEnumIDList )
        m_pEnumIDList->Release();
    if ( m_pFolder )
        m_pFolder->Release();
    if ( m_pMalloc )
        m_pMalloc->Release();
}

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsSavedSearch_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_windows_WindowsSavedSearch,_,method)

/**
 * Begin a saved search.
 */
JNIEXPORT jlong JNICALL WindowsSavedSearch_METHOD(beginSearch)
    ( JNIEnv *env, jclass, jbyteArray jPathUtf8 )
{
    jbyteArray_to_c const cPath( env, jPathUtf8 );

    SavedSearch*    cSavedSearch = NULL;
    bool            error = false;
    LPITEMIDLIST    pidl = NULL;
    LPSHELLFOLDER   pDesktop = NULL;
    HRESULT         result;

    if ( FAILED( ::CoInitialize( NULL ) ) )
        goto error;

    cSavedSearch = new SavedSearch;

    if ( FAILED( ::SHGetMalloc( &cSavedSearch->m_pMalloc ) ) )
        goto error;

    //
    // We have to start at the filesystem root (the "Desktop" in Windows).
    //
    if ( FAILED( ::SHGetDesktopFolder( &pDesktop ) ) )
        goto error;

    //
    // ParseDisplayName() wants UTF-16, so convert the path first.
    //
    WCHAR wPath[ MAX_PATH ];
    if ( !LC_toWCHAR( cPath, wPath, sizeof wPath ) )
        goto error;

    result = pDesktop->ParseDisplayName( NULL, NULL, wPath, NULL, &pidl, NULL );
    if ( FAILED( result ) )
        goto error;

    //
    // Binding the path of what we want relative to the Desktop gets us the
    // PIDL for the path of the saved search file.
    //
    result = pDesktop->BindToObject(
        pidl, NULL, IID_IShellFolder,
        reinterpret_cast<void**>( &cSavedSearch->m_pFolder )
    );
    if ( FAILED( result ) )
        goto error;

    //
    // We can now start the enumeration of the saved search file which runs the
    // search.
    //
    result = cSavedSearch->m_pFolder->EnumObjects(
        NULL, SHCONTF_FOLDERS | SHCONTF_NONFOLDERS, &cSavedSearch->m_pEnumIDList
    );
    if ( result == S_FALSE || FAILED( result ) )
        goto error;

    goto done;

error:
    error = true;
done:
    if ( pDesktop )
        pDesktop->Release();
    if ( cSavedSearch && cSavedSearch->m_pMalloc && pidl )
        cSavedSearch->m_pMalloc->Free( pidl );
    if ( error ) {
        delete cSavedSearch;
        return 0;
    }
    return reinterpret_cast<jlong>( cSavedSearch );
}

/**
 * Get the next item from the saved search.
 */
JNIEXPORT jstring JNICALL WindowsSavedSearch_METHOD(getNextResult)
    ( JNIEnv *env, jclass, jlong jSavedSearch )
{
    SavedSearch const *const cSavedSearch =
        reinterpret_cast<SavedSearch const*>( jSavedSearch );

    jstring jPath = NULL;
    HRESULT result;

    //
    // Get the next search result.
    //
    LPITEMIDLIST pidl = NULL;
    if ( FAILED( cSavedSearch->m_pEnumIDList->Next( 1, &pidl, NULL ) ) )
        goto error;

    //
    // Get the search result's name.
    //
    STRRET dispName;
    result = cSavedSearch->m_pFolder->GetDisplayNameOf(
        pidl, SHGDN_FORPARSING, &dispName
    );
    if ( FAILED( result ) )
        goto error;

    //
    // GetDisplayNameOf() returns the path in a pain-in-the-ass STRRET struct
    // so we have to convert it to a UTF-16 string.
    //
    WCHAR wName[ MAX_PATH ];
    if ( FAILED( ::StrRetToBuf( &dispName, pidl, wName, sizeof( wName ) ) ) )
        goto error;

    jPath = LC_wTojstring( env, wName );

error:
    if ( pidl )
        cSavedSearch->m_pMalloc->Free( pidl );
    return jPath;
}

/**
 * End the saved search.
 */
JNIEXPORT void JNICALL WindowsSavedSearch_METHOD(endSearch)
    ( JNIEnv *env, jclass, jlong jSavedSearch )
{
    delete reinterpret_cast<SavedSearch*>( jSavedSearch );
    ::CoUninitialize();
}
/* vim:set et sw=4 ts=4: */
