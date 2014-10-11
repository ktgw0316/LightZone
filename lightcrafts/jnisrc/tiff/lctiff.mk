ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

HIGH_PERFORMANCE:=	1
ifeq ($(PLATFORM),MacOSX)
  USE_ICC_HERE:=	1
endif

TARGET_BASE:=		LCTIFF

# Uncomment to compile in debug mode.
#DEBUG:=		true

ifeq ($(PLATFORM),Windows)
  JNI_EXTRA_LINK:=	-Wl,-Bdynamic -lLCJNI -ltiff.dll -Wl,-Bstatic -lstdc++ 
else
  JNI_EXTRA_LINK:=	-lLCJNI -ltiff -lstdc++ 
endif
JNI_MACOSX_INCLUDES:=	-I/usr/local/include
JNI_MACOSX_LDFLAGS:=	-L/usr/local/lib

JNI_EXTRA_DISTCLEAN:=	autom4te.cache configure config.* tif_config.h

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCTIFFCommon \
			com.lightcrafts.image.libs.LCTIFFReader \
			com.lightcrafts.image.libs.LCTIFFWriter

include			../jni.mk

# vim:set noet sw=8 ts=8:
