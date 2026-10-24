TARGET_BASE:=		LCJPEG

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_EXTRA_CFLAGS:=	-fexceptions -std=c++0x
JNI_EXTRA_PKGCFG:=	libjpeg
JNI_WINDOWS_LINK:=	-lLCJNI
JNI_LINUX_LINK:=	-lLCJNI -Wl,-rpath,'$$ORIGIN'
JNI_MACOSX_LINK:=	../jniutils/libLCJNI.a

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCJPEGReader \
			com.lightcrafts.image.libs.LCJPEGWriter

ROOT:=		../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
