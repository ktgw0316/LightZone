HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

ROOT:=			../../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

TARGET_BASE:=		fbf

FW_DIR:=		/System/Library/Frameworks

JNI_EXTRA_CFLAGS+=	-fopenmp -std=c++0x

ifeq ($(PLATFORM),MacOSX)
  JNI_EXTRA_LINK:=      -lstdc++
endif

# JNI_WINDOWS_LINK:=	-lLCJNI
JNI_LINUX_LINK:=	$(JNI_WINDOWS_LINK)

##
# Altivec stuff is specified in JNI_PPC_DEFINES rather than JNI_PPC_CFLAGS
# because otherwise gcc complains about it during dependency generation.
##
JNI_PPC_DEFINES:=	$(ALTIVEC_CFLAGS)

JAVAH_CLASSES:=		com.lightcrafts.jai.opimage.FastBilateralFilterOpImage

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
