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
else ifeq ($(PROCESSOR),amd64)
  PROCESSOR:=		x86_64
else ifeq ($(PROCESSOR),$(filter $(PROCESSOR),aarch64 armv8l arm64))
  PROCESSOR:=		arm64
endif

TOOLS_BIN:=		$(abspath $(ROOT)/lightcrafts/tools/bin)

# Default to a normal (Unix) classpath separator.
CLASSPATH_SEP:=		:

# Unset USE_ICC_HERE if the overall USE_ICC flags != 1.
ifneq ($(USE_ICC),1)
  USE_ICC_HERE:=
endif

# The initial set of CFLAGS and LDFLAGS.  (Must not use := here!)
PLATFORM_CFLAGS=	$(CFLAGS) -g
PLATFORM_LDFLAGS=	$(LDFLAGS)

# Default symlink command.  This needs to be defined as a function variable
# rather than just a simple variable because of the way it's overridden for
# Windows and macOS.  (Must not use := here!)
SYMLINK=		ln -fs "$1" "$2"

# Miscellaneous commands.
AR:=			ar
MKDIR:=			mkdir -p
RM:=			rm -fr

ifeq ($(PLATFORM),MacOSX)
  include $(ROOT)/lightcrafts/mk/platform-macos.mk
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
    P4_CPU_FLAGS:=	-mtune=generic -m64
  else
    P4_CPU_FLAGS:=	-march=pentium4 -m32
  endif

  SSE_FLAGS_OFF:=	$(P4_CPU_FLAGS) -mno-sse
  SSE_FLAGS_ON:=	$(P4_CPU_FLAGS) -msse2 -mfpmath=sse
  # SSE_FLAGS:=		$(SSE_FLAGS_OFF)
  SSE_FLAGS:=		$(SSE_FLAGS_ON)

  %_sse.o:
    SSE_FLAGS:= $(SSE_FLAGS_ON)

  ifeq ($(PLATFORM),Windows)
    include $(ROOT)/lightcrafts/mk/platform-windows.mk
  else ifeq ($(PLATFORM),$(filter $(PLATFORM),Linux FreeBSD SunOS))
    include $(ROOT)/lightcrafts/mk/platform-linux.mk
  else
    $(error Unsupported platform: $(PLATFORM))
  endif
endif

MAKE+=			--no-print-directory #-j $(NUM_PROCESSORS)

# vim:set noet sw=8 ts=8:
