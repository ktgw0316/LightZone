HIGH_PERFORMANCE:=	1

TARGET_BASE:=		LCJPEG

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_EXTRA_CFLAGS:=	-fexceptions -std=c++0x
JNI_EXTRA_PKGCFG:=	libjpeg
JNI_WINDOWS_LINK:=	-lLCJNI
JNI_LINUX_LINK:=	-lLCJNI
JNI_MACOSX_LINK:=	../jniutils/libLCJNI.a
JNI_MACOSX_LDFLAGS:=	-L/usr/local/opt/jpeg-turbo/lib
JNI_MACOSX_INCLUDES:=	-I/usr/local/opt/jpeg-turbo/include

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCJPEGReader \
			com.lightcrafts.image.libs.LCJPEGWriter

ROOT:=		../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
