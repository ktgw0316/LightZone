TARGET_BASE:=		LCJPEG

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_EXTRA_CFLAGS:=	-fexceptions -std=c++0x
JNI_EXTRA_PKGCFG:=	libjpeg
JNI_LINUX_LINK:=	-Wl,-rpath,'$$ORIGIN'

ROOT:=		../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
