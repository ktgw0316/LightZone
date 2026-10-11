##
# Executable Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

##
# This makefile is to be included by other makefiles that build executable
# source.  At a minimum, a makefile including this makefile must do:
#
#	TARGET_BASE:=	Neato
#	include		../mk/exec.mk
#
# in which case Neato (for Mac OS X and Linux) or Neato.exe (for Windows) would
# be built.
#
# If the makefile needs to override the default settings, it can define:
#
#	EXEC_EXTRA_CFLAGS	Specify extra flags to the compiler.
#	EXEC_EXTRA_DEFINES	Specify extra -D directives.
#	EXEC_EXTRA_INCLUDES	Specify extra -I directives.
#	EXEC_EXTRA_LDFLAGS	Specify extra -L directives.
#	EXEC_EXTRA_LINK		Specify extra -l directives.
#
# The above apply to all platforms.  To specify extra stuff for a specific
# platform only, the makefile can replace "EXTRA" with one of "MACOSX",
# "WINDOWS" or "LINUX".
#
# In addition to the above, there are also EXEC_ARM_CFLAGS, EXEC_ARM_DEFINES,
# and EXEC_ARM_LDFLAGS for arm64-specific directives, and EXEC_X86_CFLAGS,
# EXEC_X86_DEFINES, and EXEC_X86_LDFLAGS for Intel-specific directives.
#
# If a makefile needs to override how the TARGET is build, it can do:
#
#	EXEC_MANUAL_TARGET:=	1
#
# then define the TARGET rule itself.
#
# Also see the comments in sources.mk.
##

EXECUTABLE:=1

##
# Undefine all this stuff so we don't get any defaults.
##
DEFINES:=
INCLUDES:=
LINK:=

COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

CFLAGS:=		$(PLATFORM_CFLAGS) $(EXEC_EXTRA_CFLAGS)

DEFINES:=		$(EXEC_EXTRA_DEFINES)
INCLUDES:=		$(PLATFORM_INCLUDES) $(EXEC_EXTRA_INCLUDES)
LDFLAGS:=		$(PLATFORM_LDFLAGS) -L$(COMMON_DIR)/products \
			$(EXEC_EXTRA_LDFLAGS)
LINK:=			$(EXEC_EXTRA_LINK)

ifeq ($(PLATFORM),MacOSX)
  DEFINES+=		$(EXEC_MACOSX_DEFINES)
  INCLUDES:=		$(MACOSX_ISYSROOT) $(INCLUDES) $(EXEC_MACOSX_INCLUDES)
  LDFLAGS+=		$(EXEC_MACOSX_LDFLAGS)
  LINK+=		$(EXEC_MACOSX_LINK)

  CFLAGS+=		$(EXEC_MACOSX_CFLAGS)

  CFLAGS+=		$(EXEC_ARM_CFLAGS)
  DEFINES+=		$(EXEC_ARM_DEFINES)
  LDFLAGS+=		$(EXEC_ARM_LDFLAGS)
endif

ifeq ($(PLATFORM),Windows)
  CFLAGS+=		$(EXEC_WINDOWS_CFLAGS)
  DEFINES+=		$(EXEC_WINDOWS_DEFINES)
  INCLUDES+=		$(EXEC_WINDOWS_INCLUDES)
  LDFLAGS+=		$(EXEC_WINDOWS_LDFLAGS)
  LINK+=		$(EXEC_WINDOWS_LINK)
endif

ifeq ($(PLATFORM),$(filter $(PLATFORM),Linux FreeBSD SunOS))
  CFLAGS+= 		$(EXEC_LINUX_CFLAGS)
  DEFINES+=		$(EXEC_LINUX_DEFINES)
  INCLUDES+= 		$(EXEC_LINUX_INCLUDES)
  LDFLAGS+=		$(EXEC_LINUX_LDFLAGS)
  LINK+=		$(EXEC_LINUX_LINK)
endif

TARGET_DIR:=	../../products

########## You shouldn't have to change anything below this line. #############

ifdef DEBUG
DEFINES+=	-DDEBUG
endif

CFLAGS+=		$(DEFINES)

include			$(COMMON_DIR)/mk/sources.mk

LOCAL_LIBS:=	$(filter-out %-ranlib.a,$(wildcard *.a))
LOCAL_RANLIBS:=	$(foreach lib,$(LOCAL_LIBS),$(lib:.a=-ranlib.a))

BUILT_LIBS:=	$(wildcard */lib/*.a)

TARGET:=	$(TARGET_DIR)/$(TARGET_BASE)$(EXEC_EXT)

##
# Build rules
##

.PHONY: all

all: $(TARGET) $(POST_TARGET)
include		$(COMMON_DIR)/mk/auto_dep.mk

$(TARGET): $(OBJECTS) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *.o $(LINK)

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	$(RM) *.o .*.d

distclean mostlyclean: clean
	$(RM) $(TARGET) $(POST_TARGET)

# vim:set noet sw=8 ts=8:
