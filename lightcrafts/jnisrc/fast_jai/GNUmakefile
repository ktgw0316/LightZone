HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

ROOT:=			../../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

TARGET_BASE:=		FASTJAI

JNI_EXTRA_CFLAGS+=	-fopenmp -std=c++0x

# Must not use := here!
JNI_MACOSX_LINK=	-lstdc++
JNI_WINDOWS_LINK:=      -lgomp.dll
# JNI_WINDOWS_LINK:=	-lLCJNI
# JNI_LINUX_LINK:=	$(JNI_WINDOWS_LINK)

##
# Altivec stuff is specified in JNI_PPC_DEFINES rather than JNI_PPC_CFLAGS
# because otherwise gcc complains about it during dependency generation.
##
JNI_PPC_DEFINES:=	$(ALTIVEC_CFLAGS) -faltivec

JAVAH_CLASSES:=		com.lightcrafts.jai.opimage.HighlightRecoveryOpImage \
			com.lightcrafts.jai.opimage.PixelBlender \
			com.lightcrafts.jai.opimage.ColorSelectionMaskOpImage \
			com.lightcrafts.jai.opimage.RGBColorSelectionMaskOpImage \
			com.lightcrafts.jai.opimage.BilateralFilterRGBOpImage \
			com.lightcrafts.jai.opimage.HDROpImage2 \
			com.lightcrafts.jai.opimage.BilateralFilterOpImage

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
