CC?=			gcc
CXX?=			g++

PLATFORM_CFLAGS+=	-fPIC

ifeq ($(PROCESSOR),$(filter $(PROCESSOR),x86_64 i386))
  CPU_FLAGS=	$(SSE_FLAGS)
else ifeq ($(PROCESSOR),arm64)
  CPU_FLAGS=	-march=armv8-a
else ifeq ($(PROCESSOR),$(filter $(PROCESSOR),armhf armv7l))
  CPU_FLAGS=	-march=armv7-a+fp
else ifeq ($(PROCESSOR),ppc)
  CPU_FLAGS=	-mcpu=powerpc
else ifeq ($(PROCESSOR),ppc64)
  CPU_FLAGS=	-mcpu=powerpc64
endif
PLATFORM_CFLAGS:=	$(CPU_FLAGS) $(PLATFORM_CFLAGS)

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
