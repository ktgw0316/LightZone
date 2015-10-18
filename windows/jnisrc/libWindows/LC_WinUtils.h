/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * Light Crafts' Windows JNI Utilities.
 *
 * Paul J. Lucas [paul@lightcratfs.com]
 */

#ifndef LC_WinUtils_H
#define LC_WinUtils_H

// standard
#include <jni.h>

// windows
#include <windows.h>
#include <winnls.h>

// local
#include "LC_JNIUtils.h"

/**
 * Gets the name of the running .exe without the .exe extension.
 */
LPCWSTR LC_getAppName();

/**
 * Given a Java component, return an HWND.
 */
HWND LC_getHWNDFromAWTComponent( JNIEnv*, jobject awtComponent );

/**
 * Convert a UTF-16 string to UTF-8.
 */
inline bool LC_toUTF8( LPCWSTR w, char *s, int sSize ) {
    return ::WideCharToMultiByte( CP_UTF8, 0, w, -1, s, sSize, NULL, NULL );
}

/**
 * Convert a UTF-8 string to UTF-16.
 */
inline bool LC_toWCHAR( char const *s, LPWSTR w, int wSize ) {
    return ::MultiByteToWideChar( CP_UTF8, 0, s, -1, w, wSize );
}

/**
 * Convert a UTF-16 string into a jstring.
 */
jstring LC_wTojstring( JNIEnv*, LPCWSTR );

namespace LightCrafts {

    /**
     * Convert a jstring to a wide (UTF-16) string.
     */
    class jstring_to_w {
    public:
        jstring_to_w( JNIEnv *env, jstring js ) : m_cString( env, js ) {
            int const bufSize = ::strlen( m_cString ) + 1;
            m_wString = new WCHAR[ bufSize ];
            LC_toWCHAR( m_cString, m_wString, bufSize );
        }

        void release() {
            if ( m_wString ) {
                delete[] m_wString;
                m_wString = 0;
                m_cString.release();
            }
        }

        ~jstring_to_w() {
            release();
        }

        operator LPCWSTR() const {
            return m_wString;
        }
    private:
        jstring_to_c m_cString;
        LPWSTR m_wString;
    };

    /**
     * Lock a global handle (HGLOBAL) and ensure it's unlocked.
     */
    template<typename T> class auto_GlobalLock {
    public:
        explicit auto_GlobalLock( HGLOBAL g ) : m_g( g ), m_v( ::GlobalLock( g ) ) {
        }

        ~auto_GlobalLock() {
            ::GlobalUnlock( m_g );
        }

        T operator->() const {
            return static_cast<T>( m_v );
        }
    private:
        HGLOBAL m_g;
        LPVOID const m_v;
    };

} // namespace

#endif  /* LC_WinUtils_H */
/* vim:set et sw=4 ts=4: */
