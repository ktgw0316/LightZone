##
# JNI Mac OS X Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

# Different compilers required for Objective-C sources.
XCODE_PATH:=	$(shell xcode-select -p)
ifeq ($(findstring CommandLineTools,$(XCODE_PATH)),CommandLineTools)
  # Use command line tools.
  SDKROOT:=	$(XCODE_PATH)/SDKs/MacOSX${MACOSX_DEPLOYMENT_TARGET}.sdk
  XCODE_BIN_DIR=	$(XCODE_PATH)/usr/bin
else
  # Use Xcode.
  SDKROOT:=	$(shell xcodebuild -version -sdk macosx${MACOSX_DEPLOYMENT_TARGET} | sed -n '/^Path:/p' | sed 's/^Path: //')
  XCODE_BIN_DIR=	$(XCODE_PATH)/Toolchains/XcodeDefault.xctoolchain/usr/bin
endif
CC=	$(XCODE_BIN_DIR)/clang
CXX=	$(XCODE_BIN_DIR)/clang++
JNI_MACOSX_INCLUDES+=	-I$(SDKROOT)/usr/include
JNI_MACOSX_LDFLAGS+=	-framework Cocoa

##
# ROOT is defined by the makefile including this one.
##
include 		$(ROOT)/lightcrafts/jnisrc/jni.mk

# vim:set noet sw=8 ts=8:
