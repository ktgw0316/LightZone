HIGH_PERFORMANCE:=	1
#USE_ICC_HERE:=		1

TARGET_BASE:=		JAI

FW_DIR:=		/System/Library/Frameworks

JNI_WINDOWS_LINK:=	-lLCJNI
JNI_LINUX_LINK:=	$(JNI_WINDOWS_LINK)
JNI_MACOSX_LINK:=	../jniutils/libLCJNI.a

##
# Altivec stuff is specified in JNI_PPC_DEFINES rather than JNI_PPC_CFLAGS
# because otherwise gcc complains about it during dependency generation.
##
JNI_PPC_DEFINES:=	$(ALTIVEC_CFLAGS)

JAVAH_CLASSES:=		com.lightcrafts.jai.opimage.Convolutions

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
