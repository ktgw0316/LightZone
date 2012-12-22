##
# libESD (eSellerate for Windows) Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ROOT:=			../../..

TARGET_BASE:=		ESD

# Uncomment to compile in debug mode.
DEBUG:=			true

JNI_WINDOWS_DEFINES:=	-DWINVER=0x0500 -D_WIN32_WINNT=0x0500 -DUNICODE
JNI_WINDOWS_INCLUDES:=	-I../libWindows
JNI_WINDOWS_LDFLAGS:=	-L.
JNI_WINDOWS_LINK:=	-leWebClient -lvalidateLibrary -lshlwapi -lole32 -lLCJNI

JAVAH_CLASSES:=		com.lightcrafts.license.eSellerateLicenseLibrary

# Must not use := here!
POST_TARGET=		$(TARGET_DIR)/eWebClient.dll

include			$(ROOT)/lightcrafts/jnisrc/jni.mk

$(POST_TARGET): eWebClient-runtime.dll
	cp $^ $@

# vim:set noet sw=8 ts=8:
