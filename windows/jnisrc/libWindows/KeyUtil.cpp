/* Copyright (C) 2005-2011 Fabio Riccardi */

// windows
#include <windows.h>

#ifdef DEBUG
#include <iostream>
#endif

// local
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_windows_WindowsKeyUtil.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define WindowsKeyUtil_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_windows_WindowsKeyUtil,_,method)

/**
 * Detect whether the key corresponding to the given virtual key code is
 * currently pressed.  (For ASCII characters, the virtual key code is just
 * the ASCII code.)
 */
JNIEXPORT jboolean JNICALL WindowsKeyUtil_METHOD(isKeyPressed)
    ( JNIEnv *env, jclass, jint keyCode )
{
    SHORT state = ::GetKeyState( keyCode );
    int pressed = (state & 0x8000);
#ifdef DEBUG
    cout << "keyCode " << keyCode << " is ";
    if ( ! pressed ) {
        cout << "not ";
    }
    cout << "pressed (state == " << state << ")" << endl;
#endif
    return pressed;
}
/* vim:set et sw=4 ts=4: */
