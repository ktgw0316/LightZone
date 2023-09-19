HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1
USE_X64_HERE:=		1

TARGET_BASE:=		LIBRAW

JNI_EXTRA_PKGCFG:=	libraw

JNI_EXTRA_CFLAGS:=	-std=c++11
JNI_WINDOWS_CFLAGS:=	-fopenmp -DLIBRAW_NODLL
JNI_LINUX_CFLAGS:=	-fopenmp
JNI_MACOSX_CFLAGS:=	-Xpreprocessor -fopenmp

JNI_EXTRA_DEFINES:=	-DNO_JPEG

JNI_EXTRA_INCLUDES+=	-I../include

JNI_WINDOWS_LINK:=      -lgomp.dll
JNI_MACOSX_LINK:=	-lstdc++ -single_module -lomp

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LibRaw

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8: