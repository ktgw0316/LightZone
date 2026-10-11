CC:=	clang
CXX:=	clang++

MACOSX_DEPLOYMENT_TARGET:= 	$(shell sw_vers -productVersion | cut -d. -f-2)
ifndef EXECUTABLE
  PLATFORM_CFLAGS+=	-fPIC
endif
ALTIVEC_CFLAGS:=	-DLC_USE_ALTIVEC

BREW_DIR?=  /opt/homebrew
PKGCFG:=	$(BREW_DIR)/bin/pkg-config
LIBOMP_PATH?= $(shell $(BREW_DIR)/bin/brew --prefix libomp)
PLATFORM_INCLUDES+=	-I$(LIBOMP_PATH)/include
PLATFORM_LDFLAGS+=	-L$(LIBOMP_PATH)/lib

##
# Don't use := here so other makefiles can override SDKROOT.
##
SDKROOT?=
MACOSX_ISYSROOT=
MACOSX_SYSLIBROOT=

PLATFORM_LDFLAGS+=	$(MACOSX_SYSLIBROOT)

##
# These are to be only the bare minimum architecture-specific CFLAGS.
##
PLATFORM_CFLAGS+=	-target arm64-apple-macos11

FAST_CFLAGS:=	-O3 \
			-fno-trapping-math \
			-fomit-frame-pointer \
			-mcpu=apple-m1
PLATFORM_CFLAGS+=	$(FAST_CFLAGS)

LIPO:=		lipo

JAVA_INCLUDES+=	-I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/darwin"
JNILIB_PREFIX:=	lib
JNILIB_EXT:=		.jnilib
DYLIB_PREFIX:=	$(JNILIB_PREFIX)
DYLIB_EXT:=		.dylib
NUM_PROCESSORS:=	$(shell /usr/sbin/sysctl -n hw.ncpu)
##
# Since macOS @rpath doesn't follow symlinks, we have to copy files instead.
# We want to copy files only if they've changed; hence the cmp below.
##
SYMLINK=		$(TOOLS_BIN)/lc-cmp "$1" "$2"
