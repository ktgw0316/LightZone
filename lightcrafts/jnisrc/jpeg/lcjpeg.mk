HIGH_PERFORMANCE:=	1

TARGET_BASE:=		LCJPEG

# Uncomment to compile in debug mode.
#DEBUG:=		true

ifeq ($(UNIVERSAL),1)
  JNI_PPC_INCLUDES:=	-Ilibjpeg/arch-powerpc/include
  JNI_X86_INCLUDES:=	-Ilibjpeg/arch-i386/include
else
  JNI_EXTRA_INCLUDES=	-Ilibjpeg/arch-$(PROCESSOR)/include
endif

JNI_EXTRA_CFLAGS:=	-fexceptions
JNI_EXTRA_LDFLAGS:=	-Llibjpeg/lib
JNI_EXTRA_LINK:=	-ljpeg -lLCJNI

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCJPEGReader \
			com.lightcrafts.image.libs.LCJPEGWriter

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
