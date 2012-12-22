##
# libESD (eSellerate for Mac OS X) Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

TARGET_BASE:=		ESD

# Uncomment to compile in debug mode.
DEBUG:=			true

JNI_MACOSX_LDFLAGS:=	-L.
JNI_MACOSX_LINK:=	-lEWS-ranlib -lValidateUniversal-ranlib -lLCJNI

JAVAH_CLASSES:=		com.lightcrafts.license.eSellerateLicenseLibrary

# Must not use := here!
POST_TARGET=		$(TARGET_DIR)/EWSMacCompress.tar.gz

ROOT:=			../../..
include			../jni-macosx.mk

$(POST_TARGET): EWSMacCompress.tar.gz
	cp $^ $@

# vim:set noet sw=8 ts=8:
