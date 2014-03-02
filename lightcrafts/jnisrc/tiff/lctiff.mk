ROOT:=			../../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform0.mk

HIGH_PERFORMANCE:=	1
ifeq ($(PLATFORM),MacOSX)
  USE_ICC_HERE:=	1
endif

TARGET_BASE:=		LCTIFF

# Uncomment to compile in debug mode.
#DEBUG:=		true

ifeq ($(UNIVERSAL),1)
  JNI_PPC_INCLUDES:=	-Ilibtiff/arch-powerpc/include
  JNI_X86_INCLUDES:=	-Ilibtiff/arch-i386/include
else
  JNI_EXTRA_INCLUDES=	-Ilibtiff/arch-$(PROCESSOR)/include
endif

JNI_EXTRA_LDFLAGS:=	-L../jpeg/libjpeg/lib -Lzlib/lib -Llibtiff/lib
ifeq ($(PLATFORM),Windows)
  JNI_EXTRA_LINK:=	-Wl,-Bdynamic -lLCJNI -Wl,-Bstatic -ltiff -llzma -lz -lstdc++ 
else
  JNI_EXTRA_LINK:=	-lLCJNI -ltiff -lz -lstdc++ 
endif

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCTIFFCommon \
			com.lightcrafts.image.libs.LCTIFFReader \
			com.lightcrafts.image.libs.LCTIFFWriter

include			../jni.mk

# vim:set noet sw=8 ts=8:
