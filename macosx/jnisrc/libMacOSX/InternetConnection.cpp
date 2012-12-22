/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <SystemConfiguration/SCNetwork.h>

// local
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_platform_macosx_MacOSXInternetConnection.h"
#endif

using namespace std;
using namespace LightCrafts;

int const ConnectionRequiredFlags =
        kSCNetworkFlagsConnectionAutomatic |
        kSCNetworkFlagsConnectionRequired  |
        kSCNetworkFlagsInterventionRequired;

////////// JNI ////////////////////////////////////////////////////////////////

#define MacOSXInternetConnection_METHOD(method) \
        name4(Java_,com_lightcrafts_platform_macosx_MacOSXInternetConnection,_,method)

/**
 * Checks whether we have an internet connection to the given host.
 */
JNIEXPORT jboolean JNICALL MacOSXInternetConnection_METHOD(hasConnectionTo)
    ( JNIEnv *env, jclass, jstring jHostName )
{
    jstring_to_c const cHostName( env, jHostName );
    SCNetworkConnectionFlags flags;
    return  ::SCNetworkCheckReachabilityByName( cHostName, &flags ) &&
            !(flags & ConnectionRequiredFlags) &&
            flags & kSCNetworkFlagsReachable;
}

/* vim:set et sw=4 ts=4: */
