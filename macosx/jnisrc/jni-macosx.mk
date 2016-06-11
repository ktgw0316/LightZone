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

MACOSX_MINOR_VERSION:=	$(shell sw_vers -productVersion | cut -d. -f2-2)

# Different compilers required for Objective-C sources.
ifeq ($(MACOSX_MINOR_VERSION),6) # Snow Leopard
  CC=	gcc
  CXX=	g++
else
  XCODE_PATH:=	$(shell xcode-select -p)
  ifeq ($(findstring CommandLineTools,$(XCODE_PATH)),CommandLineTools)
    # Use command line tools.
    XCODE_BIN_DIR=	$(XCODE_PATH)/usr/bin
  else
    # Use Xcode.
    XCODE_BIN_DIR=	$(XCODE_PATH)/Toolchains/XcodeDefault.xctoolchain/usr/bin
  endif
  CC=	$(XCODE_BIN_DIR)/clang
  CXX=	$(XCODE_BIN_DIR)/clang++
endif

# vim:set noet sw=8 ts=8:
