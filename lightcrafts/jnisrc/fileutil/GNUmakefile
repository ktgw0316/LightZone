TARGET_BASE:=	LCFileUtil

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_WINDOWS_LINK:=	-lLCJNI
JNI_LINUX_LINK:=	$(JNI_WINDOWS_LINK)
JNI_MACOSX_LINK:=	../jniutils/libLCJNI.a

JAVAH_CLASSES:=	com.lightcrafts.utils.file.FileUtil

ROOT:=		../../..
include		../jni.mk

# vim:set noet sw=8 ts=8:
