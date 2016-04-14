##
# JNI Mac OS X Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

JNI_MACOSX_LDFLAGS+=	-framework Cocoa

##
# ROOT is defined by the makefile including this one.
##
include 		$(ROOT)/lightcrafts/jnisrc/jni.mk

# Different compilers required for Objective-C sources.
XCODE_BIN_DIR=	$(shell xcode-select -p)/usr/bin
CC=	$(XCODE_BIN_DIR)/clang
CXX=	$(XCODE_BIN_DIR)/clang++

# vim:set noet sw=8 ts=8:
