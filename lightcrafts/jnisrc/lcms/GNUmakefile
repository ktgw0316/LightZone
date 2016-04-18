HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

TARGET_BASE:=		LCLCMS

# Uncomment to compile in debug mode.
#DEBUG:=			true

ROOT:=			../../..
include		$(ROOT)/lightcrafts/mk/platform.mk
JNI_EXTRA_LINK:=	$(shell $(PKGCFG) --libs-only-l lcms2)
ifneq ($(PLATFORM),MacOSX)
  JNI_EXTRA_INCLUDES:=	$(shell $(PKGCFG) --cflags lcms2)
  JNI_EXTRA_LDFLAGS:=	$(shell $(PKGCFG) --libs-only-L lcms2)
endif
JNI_MACOSX_INCLUDES:=	-I/usr/local/include
JNI_MACOSX_LDFLAGS:=	-L/usr/local/lib

##
# This is to prevent LCMS from using the MessageBox() function to report errors
# under Windows and just use stderr instead.
##
JNI_WINDOWS_DEFINES:=	-D__CONSOLE__

JAVAH_CLASSES:=		com.lightcrafts.utils.LCMSNative

include			../jni.mk

# vim:set noet sw=8 ts=8:
