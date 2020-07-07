HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

TARGET_BASE:=		FASTJAI

JNI_WINDOWS_CFLAGS:=	-fopenmp
JNI_LINUX_CFLAGS:=	$(JNI_WINDOWS_CFLAGS)
JNI_MACOSX_CFLAGS:=	-Xpreprocessor -fopenmp
JNI_EXTRA_CFLAGS+=	-std=c++11
JNI_EXTRA_INCLUDES+=	-I../include

# Must not use := here!
JNI_MACOSX_LINK=	-lstdc++ -lomp
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
			com.lightcrafts.jai.opimage.NonLocalMeansFilterOpImage \
			com.lightcrafts.jai.opimage.HDROpImage2

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
