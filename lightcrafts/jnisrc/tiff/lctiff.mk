TARGET_BASE:=		LCTIFF

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_EXTRA_PKGCFG:=	libtiff-4
JNI_EXTRA_CFLAGS:=	-DTIFF_DISABLE_DEPRECATED -Wall
JNI_WINDOWS_LINK:=	-lstdc++
JNI_LINUX_LINK:=	-lstdc++ -Wl,-rpath,'$$ORIGIN'

ROOT:=		../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
