##
# libMacOSX Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ROOT:=                  ../../..
COMMON_DIR:=            $(ROOT)/lightcrafts

TARGET_BASE:=		MacOSX

# Uncomment to compile in debug mode.
#DEBUG:=			true

#JNI_MACOSX_INCLUDES+=	-I/System/Library/Frameworks/AppKit.framework/Versions/C/Headers
JNI_MACOSX_LDFLAGS+=	-framework SystemConfiguration
JNI_MACOSX_LINK:=	$(COMMON_DIR)/jnisrc/jniutils/libLCJNI.a

JAVAH_CLASSES:=	com.lightcrafts.platform.macosx.AppleScript \
		com.lightcrafts.platform.macosx.CocoaMainThreadRunner \
		com.lightcrafts.platform.macosx.MacOSXAlertDialog \
		com.lightcrafts.platform.macosx.MacOSXColorProfileManager \
		com.lightcrafts.platform.macosx.MacOSXFileChooser \
		com.lightcrafts.platform.macosx.MacOSXFileUtil \
		com.lightcrafts.platform.macosx.MacOSXHelp \
		com.lightcrafts.platform.macosx.MacOSXInternetConnection \
		com.lightcrafts.platform.macosx.MacOSXMemory \
		com.lightcrafts.platform.macosx.MacOSXPrinter \
		com.lightcrafts.platform.macosx.MacOSXProgressDialog \
		com.lightcrafts.platform.macosx.MacOSXSmartFolder \
		com.lightcrafts.platform.macosx.sheets.SaveExportSheet

ROOT:=		../../..
include		../jni-macosx.mk

# vim:set noet sw=8 ts=8:
