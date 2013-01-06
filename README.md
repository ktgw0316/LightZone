LightZone
=========

LightZone is a photo editor for Mac, Windows, and Linux.

Windows SDK 7.0 not working
When compiling there is an error thrown by resource compiler from MSSDK. Haven't you seen this when building?
     [exec] "/cygdrive/c/MSSDK/v7.0/Bin/RC.Exe" -i "C:\cygwin\usr\include\w32api" -n -fo temp.RES lightzone.rc
     [exec] Microsoft (R) Windows (R) Resource Compiler Version 6.1.7600.16385
     [exec] Copyright (C) Microsoft Corporation.  All rights reserved.
     [exec]
     [exec] C:\cygwin\usr\include\w32api\_mingw_mac.h(217) : warning RC4011: identifier truncated to '_CRT_SECURE_CPP_OVERLOAD_STANDA'
     [exec] C:\cygwin\usr\include\w32api\_mingw_secapi.h(57) : warning RC4011: identifier truncated to '_CRT_SECURE_CPP_OVERLOAD_SECURE'
     [exec] C:\cygwin\usr\include\w32api\_mingw_secapi.h(73) : warning RC4011: identifier truncated to '_CRT_SECURE_CPP_OVERLOAD_SECURE'
     [exec] C:\cygwin\usr\include\w32api\_mingw_secapi.h(90) : warning RC4011: identifier truncated to '_CRT_SECURE_CPP_OVERLOAD_STANDA'
     [exec] C:\cygwin\usr\include\w32api\_mingw_secapi.h(123) : warning RC4011: identifier truncated to '_CRT_SECURE_CPP_OVERLOAD_STANDA'
     [exec] C:\cygwin\usr\include\w32api\_mingw_secapi.h(123) : warning RC4011: identifier truncated to '_CRT_SECURE_CPP_OVERLOAD_STANDA'
     [exec] C:\cygwin\usr\include\w32api\_mingw_secapi.h(184) : warning RC4011: identifier truncated to '_CRT_SECURE_CPP_OVERLOAD_STANDA'
     [exec] C:\cygwin\usr\include\w32api\vadefs.h(33) : error RC2188: C:\LightZoneAries\windows\helpers\JavaAppLauncher\RCa03532(98) : fatal error RC1116: RC terminating after preprocessor errors
     [exec] ../../../lightcrafts/mk/sources.mk:247: recipe for target `lightzone.res' failed
     [exec] ../../lightcrafts/mk/recurse.mk:24: recipe for target `all' failed
     [exec] make[1]: *** [lightzone.res] Error 2
     [exec] make: *** [all] Error 1