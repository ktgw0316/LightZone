HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

TARGET_BASE:=		LCLCMS

# Uncomment to compile in debug mode.
#DEBUG:=			true

JNI_EXTRA_LINK:=	$(shell $(PKGCFG) --libs-only-l lcms2)
JNI_LINUX_INCLUDES:=	$(shell $(PKGCFG) --cflags lcms2)
JNI_LINUX_LDFLAGS:=	$(shell $(PKGCFG) --libs-only-L lcms2)
JNI_WINDOWS_INCLUDES:=	$(JNI_LINUX_INCLUDES)
JNI_WINDOWS_LDFLAGS:=	$(JNI_LINUX_LDFLAGS)
JNI_MACOSX_INCLUDES:=	-I/usr/local/include
JNI_MACOSX_LDFLAGS:=	-L/usr/local/lib

##
# This is to prevent LCMS from using the MessageBox() function to report errors
# under Windows and just use stderr instead.
##
JNI_WINDOWS_DEFINES:=	-D__CONSOLE__

JAVAH_CLASSES:=		com.lightcrafts.utils.LCMSNative

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
