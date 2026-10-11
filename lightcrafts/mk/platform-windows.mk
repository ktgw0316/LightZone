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
  PLATFORM_LDFLAGS+=	-L"$(MSSDK_HOME_W32)/um/x64"
else
  MINGW:=		i686-w64-mingw32
  PLATFORM_LDFLAGS+=	-L"$(MSSDK_HOME_W32)/um/x86"
endif
CC:=		$(MINGW)-gcc
CXX:=		$(MINGW)-g++
PKGCFG:=	$(MINGW)-pkg-config

ifeq ($(CYGWIN),1)
  MINGW_DIR?=		/usr/$(MINGW)/sys-root/mingw/
else
  # MSYS2
  ifeq ($(PROCESSOR),x86_64)
    MINGW_DIR?=	/ucrt64/
    PKG_CONFIG_PATH:=	/ucrt64/lib/pkgconfig/
  else
    MINGW_DIR?=	/mingw32/
    PKG_CONFIG_PATH:=	/mingw32/lib/pkgconfig/
  endif
endif

DLL_DIR:=		$(MINGW_DIR)bin/

PLATFORM_CFLAGS:=	$(SSE_FLAGS) $(PLATFORM_CFLAGS)

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
