/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <X11/Xlib.h>
#include <X11/XKBlib.h>
#include <iostream>
#ifdef DEBUG
#include <cassert>
#endif

// local
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_linux_LinuxKeyUtil.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define LinuxKeyUtil_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_linux_LinuxKeyUtil,_,method)

/**
 * Find the index of the first nonzero bit in the given char, where the least
 * significant bit has index zero.  Used in keysToKeycode().
 */
int indexOfBit(char c) {
#ifdef DEBUG
    assert(c != 0);
#endif
    int n;
    for (n=0; n<8; n++) {
        if (c & 0x01) {
            break;
        }
        c = c >> 1;
    }
    return n;
}

/**
 * Determine the KeyCode of the first pressed key in the 32-character keys
 * array returned from XQueryKeymap.
 */
KeyCode keysToKeycode(char *keys) {
    int n;
    for (n=0; n<32; n++) {
        if (keys[n] != 0) {
            return 8 * n + indexOfBit(keys[n]);
        }
    }
    return 0;
}

/**
 * The X11 Display reference is a global variable, initialized in the first
 * call to isKeyPressed().
 */
Display *display = NULL;

/**
 * Detect whether the key corresponding to the given virtual key code is
 * currently pressed.  (For ASCII characters, the virtual key code is just
 * the ASCII code.)
 */
JNIEXPORT jboolean JNICALL LinuxKeyUtil_METHOD(isKeyPressed)
    ( JNIEnv *env, jclass, jint keyCode )
{
    if (display == NULL) {
        display = XOpenDisplay(NULL);
    }
    if (display == NULL) {
        cerr << "LinuxPlatform cannot connect to X server "
             << XDisplayName(NULL)
             << endl;
        return false;
    }
    char keys[32];
    XQueryKeymap(display, keys);

    KeyCode code = keysToKeycode(keys);

    KeySym sym = XkbKeycodeToKeysym(display, code, 0, 0);

    bool pressed = keyCode == sym;
#ifdef DEBUG
    cout << "keyCode " << keyCode << " is ";
    if ( ! pressed ) {
        cout << "not ";
    }
    cout << "pressed" << endl;
#endif
    return pressed;
}
/* vim:set et sw=4 ts=4: */
