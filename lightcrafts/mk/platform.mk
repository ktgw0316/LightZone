##
# Platform Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ifndef PLATFORM0
  include $(ROOT)/lightcrafts/mk/platform0.mk
endif

ifndef JAVA_HOME
  $(error "JAVA_HOME" must be set)
endif

PROCESSOR:=		$(shell uname -m)
ifeq ($(PROCESSOR),i486)
  PROCESSOR:=		i386
endif
ifeq ($(PROCESSOR),i586)
  PROCESSOR:=		i386
endif
ifeq ($(PROCESSOR),i686)
  PROCESSOR:=		i386
endif
ifeq ($(PROCESSOR),amd64)
  PROCESSOR:=		x86_64
endif
ifeq ($(PROCESSOR),"Power Macintosh")
  PROCESSOR:=		powerpc
endif

TOOLS_BIN:=		$(abspath $(ROOT)/lightcrafts/tools/bin)

# Default to a normal (Unix) classpath seperator.
CLASSPATH_SEP:=		:

# The default C and C++ compilers.
CC:=			gcc
CXX:=			g++

# Unset USE_ICC_HERE if the overall USE_ICC flags != 1.
ifneq ($(USE_ICC),1)
  USE_ICC_HERE:=
endif

# The initial set of CFLAGS.  (Must not use := here!)
PLATFORM_CFLAGS=	-g

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
  MACOSX_DEPLOYMENT_TARGET:= 	$(shell sw_vers -productVersion | cut -d. -f-2)
  SDKROOT:=		$(shell xcodebuild -version -sdk macosx${MACOSX_DEPLOYMENT_TARGET} | sed -n '/^Path:/p' | sed 's/^Path: //')
  ifndef EXECUTABLE
    PLATFORM_CFLAGS+=	-fPIC
  endif
  ALTIVEC_CFLAGS:=	-DLC_USE_ALTIVEC

  ifdef USE_ICC_HERE
    ICC_ROOT:=		/opt/intel/Compiler/11.1/067
    ICC:=		$(ICC_ROOT)/bin/ia32/icc
    XIAR:=		$(ICC_ROOT)/bin/ia32/xiar
  endif

  ##
  # Don't use := here so other makefiles can override SDKROOT.
  ##
  ifdef USE_ICC_HERE
    MACOSX_ISYSROOT=	-isysroot $(SDKROOT)
  else
    MACOSX_ISYSROOT=	-isysroot$(SDKROOT)
  endif
  MACOSX_SYSLIBROOT=	-Wl,-syslibroot,$(SDKROOT)
  PLATFORM_LDFLAGS=	$(MACOSX_SYSLIBROOT)

  ##
  # These are to be only the bare minimum architecture-specific CFLAGS.  High-
  # performance CFLAGS go in the FAST_CFLAGS_* variables below.
  ##
  MACOSX_CFLAGS_PPC:=	-mcpu=G4 -mtune=G5
  MACOSX_CFLAGS_X86:=	-march=core2 -mtune=generic

  ifdef HIGH_PERFORMANCE
    ##
    # High-performance architecture-specific CFLAGS only.
    ##
    FAST_CFLAGS_PPC:=	-fast -Wstrict-aliasing -Wstrict-aliasing=2

    ifdef USE_ICC_HERE
      FAST_CFLAGS_X86:=	-O3 -no-prec-div -xP -fp-model fast=2 -ipo -vec-report0 -fno-common # -fno-alias
      ifeq ($(UNIVERSAL),1)
        CC_X86:=	$(ICC)
	AR_X86:=	$(XIAR)
        CXX_X86:=	$(ICC)
      else
        ifneq ($(PROCESSOR),powerpc)
	  AR:=		$(XIAR)
          CC:=		$(ICC)
          CXX:=		$(ICC)
        endif
      endif
    else
      FAST_CFLAGS_X86:=	-O3 \
			-fno-trapping-math \
			-fomit-frame-pointer \
			-msse2 -mfpmath=sse
    endif
    MACOSX_CFLAGS_PPC+=	$(FAST_CFLAGS_PPC)
    MACOSX_CFLAGS_X86+=	$(FAST_CFLAGS_X86)
  else
    PLATFORM_CFLAGS+=	-Os
  endif

  ifeq ($(UNIVERSAL),1)
    PLATFORM_CFLAGS_PPC:= $(PLATFORM_CFLAGS) -arch ppc7400 $(MACOSX_CFLAGS_PPC)
    PLATFORM_CFLAGS_X86:= $(PLATFORM_CFLAGS) -arch i386 $(MACOSX_CFLAGS_X86)

    ifeq ($(PROCESSOR),powerpc)
      OTHER_PROCESSOR:=	i386
    else
      OTHER_PROCESSOR:=	powerpc
    endif
    DARWIN_RELEASE:=	$(shell uname -r)
    CONFIG_HOST:=	$(PROCESSOR)-apple-darwin$(DARWIN_RELEASE)
    CONFIG_TARGET:=	$(OTHER_PROCESSOR)-apple-darwin$(DARWIN_RELEASE)
  else
    ifeq ($(PROCESSOR),powerpc)
      PLATFORM_CFLAGS+=	$(MACOSX_CFLAGS_PPC)
      PLATFORM_CFLAGS_PPC:= $(PLATFORM_CFLAGS)
    else
      PLATFORM_CFLAGS+=	$(MACOSX_CFLAGS_X86)
      PLATFORM_CFLAGS_X86:= $(PLATFORM_CFLAGS)
    endif
  endif

  LIPO:=		lipo

  ##
  # Note that JAVA_INCLUDES is treated as relative to SDKROOT.
  ##
  JAVA_HOME=		/Library/Java/Home
  JAVA_INCLUDES=	-I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
  JAVA_LDFLAGS=		-L/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Libraries
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
    P4_CPU_FLAGS:=	-march=pentium4
  endif

  SSE_FLAGS_OFF:=	$(P4_CPU_FLAGS) -mno-sse
  SSE_FLAGS_ON:=	$(P4_CPU_FLAGS) -msse2
  SSE_FLAGS:=		$(SSE_FLAGS_OFF)

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

  NUM_PROCESSORS:=	$(shell grep '^processor' /proc/cpuinfo | wc -l)
  ifeq ($(NUM_PROCESSORS),0)
    NUM_PROCESSORS:=	1
  endif

  MSSDK_HOME_W32:=	$(shell cygpath -w "$(MSSDK_HOME)")

  ifeq ($(PROCESSOR),x86_64)
    ARCH:=		x64
    CC:=		x86_64-w64-mingw32-gcc
    CXX:=		x86_64-w64-mingw32-g++
  else
    ARCH:=		x86
    CC:=		i686-w64-mingw32-gcc
    CXX:=		i686-w64-mingw32-g++
  endif

  RC:=			"$(MSSDK_HOME)/Bin/$(ARCH)/RC.Exe"
  RC_INCLUDES:=		-i "$(shell cygpath -w /usr/include/w32api)"
  RC_FLAGS=		$(RC_INCLUDES) -n -fo

  PLATFORM_CFLAGS+=	$(SSE_FLAGS) # "-D__int64=long long"
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
# Linux
##
ifeq ($(PLATFORM),Linux)
  ifeq ($(PROCESSOR),x86_64)
    PLATFORM_CFLAGS+=	-march=athlon64 -mtune=generic $(SSE_FLAGS_ON) -fPIC
  else
    PLATFORM_CFLAGS+=	-march=pentium4 -mtune=generic $(SSE_FLAGS_ON) -fPIC
  endif

  ifdef HIGH_PERFORMANCE
    PLATFORM_CFLAGS+=	-O3 \
			-fno-trapping-math \
			-fomit-frame-pointer \
			-msse2 -mfpmath=sse
  else
    PLATFORM_CFLAGS+=	-Os
  endif
  JAVA_INCLUDES:=	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
  JAVA_LDFLAGS:=	-L$(JAVA_HOME)/lib
  JNILIB_PREFIX:=	lib
  JNILIB_EXT:=		.so
  DYLIB_PREFIX:=	$(JNILIB_PREFIX)
  DYLIB_EXT:=		$(JNILIB_EXT)

  NUM_PROCESSORS:=	$(shell grep '^processor' /proc/cpuinfo | wc -l)
endif

##
# Miscellaneous stuff.
##

MAKE+=			--no-print-directory #-j $(NUM_PROCESSORS)

# vim:set noet sw=8 ts=8:
