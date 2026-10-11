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
else ifeq ($(PLATFORM),Windows)
  include $(ROOT)/lightcrafts/mk/platform-windows.mk
else ifeq ($(PLATFORM),$(filter $(PLATFORM),Linux FreeBSD SunOS))
  include $(ROOT)/lightcrafts/mk/platform-linux.mk
else
  $(error Unsupported platform: $(PLATFORM))
endif

MAKE+=			--no-print-directory #-j $(NUM_PROCESSORS)

# vim:set noet sw=8 ts=8:
