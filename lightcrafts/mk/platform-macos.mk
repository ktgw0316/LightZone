CC:=	clang
CXX:=	clang++

MACOSX_DEPLOYMENT_TARGET:= 	$(shell sw_vers -productVersion | cut -d. -f-2)
ifndef EXECUTABLE
  PLATFORM_CFLAGS+=	-fPIC
endif
ALTIVEC_CFLAGS:=	-DLC_USE_ALTIVEC

ifeq ($(PROCESSOR),arm64)
  BREW_DIR?=  /opt/homebrew
else
  BREW_DIR?=  /usr/local
endif
PKGCFG:=	$(BREW_DIR)/bin/pkg-config
LIBOMP_PATH?= $(shell $(BREW_DIR)/bin/brew --prefix libomp)
PLATFORM_INCLUDES+=	-I$(LIBOMP_PATH)/include
PLATFORM_LDFLAGS+=	-L$(LIBOMP_PATH)/lib

##
# Don't use := here so other makefiles can override SDKROOT.
##
ifeq ($(UNIVERSAL),1)
  SDKROOT:=		$(shell xcodebuild -version -sdk macosx${MACOSX_DEPLOYMENT_TARGET} | sed -n '/^Path:/p' | sed 's/^Path: //')
  MACOSX_ISYSROOT=	-isysroot $(SDKROOT)
  MACOSX_SYSLIBROOT=	-Wl,-syslibroot,$(SDKROOT)
else
  SDKROOT?=
  MACOSX_ISYSROOT=
  MACOSX_SYSLIBROOT=
endif
PLATFORM_LDFLAGS+=	$(MACOSX_SYSLIBROOT)

##
# These are to be only the bare minimum architecture-specific CFLAGS.  High-
# performance CFLAGS go in the FAST_CFLAGS_* variables below.
##
MACOSX_CFLAGS_ARM:=	-target arm64-apple-macos11
MACOSX_CFLAGS_X86:=	-target x86_64-apple-macos10.12

ifdef HIGH_PERFORMANCE
  ##
  # High-performance architecture-specific CFLAGS only.
  ##
  FAST_CFLAGS_ARM:=	-O3 # TODO
  FAST_CFLAGS_X86:=	-O3 \
			-fno-trapping-math \
			-fomit-frame-pointer \
			-msse2 -mfpmath=sse
  MACOSX_CFLAGS_ARM+=	$(FAST_CFLAGS_ARM)
  MACOSX_CFLAGS_X86+=	$(FAST_CFLAGS_X86)
else
  PLATFORM_CFLAGS+=	-Os
endif

ifeq ($(UNIVERSAL),1)
  PLATFORM_CFLAGS_ARM:= $(PLATFORM_CFLAGS) $(MACOSX_CFLAGS_ARM)
  PLATFORM_CFLAGS_X86:= $(PLATFORM_CFLAGS) $(MACOSX_CFLAGS_X86)

  ifeq ($(PROCESSOR),arm64)
    CONFIG_HOST:= $(MACOSX_CFLAGS_ARM)
    CONFIG_TARGET:= $(MACOSX_CFLAGS_X86)
  else
    CONFIG_HOST:= $(MACOSX_CFLAGS_X86)
    CONFIG_TARGET:= $(MACOSX_CFLAGS_ARM)
  endif
else
  ifeq ($(PROCESSOR),arm64)
    PLATFORM_CFLAGS+=	$(MACOSX_CFLAGS_ARM)
    PLATFORM_CFLAGS_ARM:= $(PLATFORM_CFLAGS)
  else
    PLATFORM_CFLAGS+=	$(MACOSX_CFLAGS_X86)
    PLATFORM_CFLAGS_X86:= $(PLATFORM_CFLAGS)
  endif
endif

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
