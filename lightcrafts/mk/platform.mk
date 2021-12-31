##
# Platform Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
# Masahiro Kitagawa [arctica0316@gmail.com]
##

ifndef PLATFORM0
  include $(ROOT)/lightcrafts/mk/platform0.mk
endif

ifndef JAVA_HOME
  $(error "JAVA_HOME" must be set)
endif

##
# Target architecture
##
ifdef TARGET_ARCH
  PROCESSOR:=		$(TARGET_ARCH)
else
  PROCESSOR:=		$(shell uname -m)
endif

ifeq ($(PROCESSOR),$(filter $(PROCESSOR),i486 i586 i686 i86pc))
  PROCESSOR:=		i386
endif
ifeq ($(PROCESSOR),amd64)
  PROCESSOR:=		x86_64
endif
ifeq ($(PROCESSOR),$(filter $(PROCESSOR),aarch64 armv8l arm64))
  PROCESSOR:=		arm64
endif

TOOLS_BIN:=		$(abspath $(ROOT)/lightcrafts/tools/bin)

# Default to a normal (Unix) classpath seperator.
CLASSPATH_SEP:=		:

# The default C and C++ compilers for Linux, FreeBSD, or OpenIndiana
CC?=			gcc
CXX?=			g++

# Unset USE_ICC_HERE if the overall USE_ICC flags != 1.
ifneq ($(USE_ICC),1)
  USE_ICC_HERE:=
endif

# The initial set of CFLAGS and LDFLAGS.  (Must not use := here!)
PLATFORM_CFLAGS=	$(CFLAGS) -g
PLATFORM_LDFLAGS=	$(LDFLAGS)

# Default symlink command.  This needs to be defined as a function variable
# rather than just a simple variable because of the way it's overridden for
# Windows.  (Must not use := here!)
SYMLINK=		ln -fs "$1" "$2"

# Miscellaneous commands.
AR:=			ar
MKDIR:=			mkdir -p
RM:=			rm -fr

##
# Mac OS X
##
ifeq ($(PLATFORM),MacOSX)
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
else
  ##
  # JNI on non-Mac platforms doesn't do proper stack alignment when SSE
  # instructions are being used.  Therefore, we turn it off unless it's needed
  # for high performance code.
  #
  # To turn it on on a per-file basis, name the source file such that it has
  # "_sse" just before the extension, e.g., "foo_sse.c".
  #
  # See Sun bug ID: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5102720
  ##
  ifeq ($(PROCESSOR),x86_64)
    P4_CPU_FLAGS:=	-march=athlon64
  else
    P4_CPU_FLAGS:=	-march=pentium4 -m32
  endif

  SSE_FLAGS_OFF:=	$(P4_CPU_FLAGS) -mno-sse
  SSE_FLAGS_ON:=	$(P4_CPU_FLAGS) -msse2 -mfpmath=sse
  # SSE_FLAGS:=		$(SSE_FLAGS_OFF)
  SSE_FLAGS:=		$(SSE_FLAGS_ON)

  %_sse.o:
    SSE_FLAGS:= $(SSE_FLAGS_ON)
endif

##
# Windows
##
ifeq ($(PLATFORM),Windows)
  ifndef MSSDK_HOME
    $(error "MSSDK_HOME" must be set)
  endif
  MSSDK_HOME_W32:=	$(shell cygpath -w "$(MSSDK_HOME)")

  NUM_PROCESSORS:=	$(shell grep '^processor' /proc/cpuinfo | wc -l)
  ifeq ($(NUM_PROCESSORS),0)
    NUM_PROCESSORS:=	1
  endif

  ifeq ($(PROCESSOR),x86_64)
    MINGW:=		x86_64-w64-mingw32
  else
    MINGW:=		i686-w64-mingw32
  endif
  CC:=		$(MINGW)-gcc
  CXX:=		$(MINGW)-g++
  PKGCFG:=	$(MINGW)-pkg-config

  ifeq ($(CYGWIN),1)
    MINGW_DIR?=		/usr/$(MINGW)/sys-root/mingw/
  else
    # MSYS2
    ifeq ($(PROCESSOR),x86_64)
      MINGW_DIR?=	/mingw64/
      PKG_CONFIG_PATH:=	/mingw64/lib/pkgconfig/
    else
      MINGW_DIR?=	/mingw32/
      PKG_CONFIG_PATH:=	/mingw32/lib/pkgconfig/
    endif
  endif

  DLL_DIR:=		$(MINGW_DIR)bin/

  PLATFORM_CFLAGS+=	$(SSE_FLAGS)

  ifdef HIGH_PERFORMANCE
    ifdef USE_ICC_HERE
      ICC:=		$(TOOLS_BIN)/lc-icc-w32
      CC:=		$(ICC)
      CXX:=		$(ICC)
      PLATFORM_CFLAGS:=	-g -O3 /QxW /Qipo \
      			-D__MMX__ -D__SSE__ -D__SSE2__ -D_USE_MATH_DEFINES
    else
      PLATFORM_CFLAGS+=	-O3 \
			-fno-trapping-math \
			-fomit-frame-pointer
    endif
  else
    PLATFORM_CFLAGS+=	-Os
  endif
  CLASSPATH_SEP:=	;
  JAVA_INCLUDES:=	-I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32"
  JAVA_LDFLAGS:=	-L"$(JAVA_HOME)/lib"
  JNILIB_PREFIX:=	# nothing
  JNILIB_EXT:=		.dll
  DYLIB_PREFIX:=	$(JNILIB_PREFIX)
  DYLIB_EXT:=		$(JNILIB_EXT)
  EXEC_EXT:=		.exe
  ##
  # Since Windows doesn't have symlinks (symlinks via Cygwin don't count when
  # using non-cygwin software like javac), we have to copy files instead.  But
  # we want to copy files only if they've changed; hence the cmp below.
  ##
  SYMLINK=		$(TOOLS_BIN)/lc-cmp "$1" "$2"
endif

##
# Linux, FreeBSD or OpenIndiana
##
ifeq ($(PLATFORM),$(filter $(PLATFORM),Linux FreeBSD SunOS))
  PLATFORM_CFLAGS+=	-fPIC

  ifeq ($(PROCESSOR),$(filter $(PROCESSOR),x86_64 i386))
    PLATFORM_CFLAGS+=	$(SSE_FLAGS)
  else ifeq ($(PROCESSOR),arm64)
    PLATFORM_CFLAGS+=	-march=armv8-a
  else ifeq ($(PROCESSOR),$(filter $(PROCESSOR),armhf armv7l))
    PLATFORM_CFLAGS+=	-march=armv7-a+fp
  else ifeq ($(PROCESSOR),ppc)
    PLATFORM_CFLAGS+=	-mcpu=powerpc
  else ifeq ($(PROCESSOR),ppc64)
    PLATFORM_CFLAGS+=	-mcpu=powerpc64
  endif

  ifdef HIGH_PERFORMANCE
    PLATFORM_CFLAGS+=	-O3 \
			-fno-trapping-math \
			-fomit-frame-pointer
  else
    PLATFORM_CFLAGS+=	-Os
  endif

  JAVA_LDFLAGS:=	-L$(JAVA_HOME)/lib
  JNILIB_PREFIX:=	lib
  JNILIB_EXT:=		.so
  DYLIB_PREFIX:=	$(JNILIB_PREFIX)
  DYLIB_EXT:=		$(JNILIB_EXT)

  PKGCFG:=		pkg-config

  ifeq ($(PLATFORM),Linux)
    JAVA_INCLUDES:=	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
    NUM_PROCESSORS:=	$(shell grep '^processor' /proc/cpuinfo | wc -l)
  else ifeq ($(PLATFORM),FreeBSD)
    PKGCFG:=		pkgconf
    JAVA_INCLUDES:=	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/freebsd
    NUM_PROCESSORS:=	$(shell dmesg | grep '^cpu' | wc -l)
    PLATFORM_INCLUDES=	-I/usr/local/include
    PLATFORM_LDFLAGS+=	-L/usr/local/lib
  else ifeq ($(PLATFORM),SunOS)
    JAVA_INCLUDES:=	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/solaris
    NUM_PROCESSORS:=	$(shell psrinfo -p)
  endif
endif

##
# Miscellaneous stuff.
##

MAKE+=			--no-print-directory #-j $(NUM_PROCESSORS)

# vim:set noet sw=8 ts=8:
