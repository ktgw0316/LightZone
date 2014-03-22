HIGH_PERFORMANCE:=	1

TARGET_BASE:=		LCJPEG

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_EXTRA_CFLAGS:=	-fexceptions
JNI_EXTRA_LINK:=	-ljpeg -lLCJNI

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCJPEGReader \
			com.lightcrafts.image.libs.LCJPEGWriter

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
