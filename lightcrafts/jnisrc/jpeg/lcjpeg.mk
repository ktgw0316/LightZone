ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

HIGH_PERFORMANCE:=	1

TARGET_BASE:=		LCJPEG

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_EXTRA_CFLAGS:=	-fexceptions
ifeq ($(PLATFORM),Windows)
  JNI_EXTRA_LINK:=	-ljpeg.dll -lLCJNI
else
  JNI_EXTRA_LINK:=	-ljpeg -lLCJNI
endif
JNI_MACOSX_LDFLAGS:=	-L/usr/local/opt/jpeg-turbo/lib
JNI_MACOSX_INCLUDES:=	-I/usr/local/opt/jpeg-turbo/include

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCJPEGReader \
			com.lightcrafts.image.libs.LCJPEGWriter

include			../jni.mk

# vim:set noet sw=8 ts=8:
