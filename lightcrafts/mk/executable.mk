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
# In addition to the above, there are also EXEC_PPC_CFLAGS, EXEC_PPC_DEFINES,
# and EXEC_PPC_LDFLAGS for PowerPC-specific directives, and EXEC_X86_CFLAGS,
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
CC:=
CFLAGS:=
CXX:=
DEFINES:=
INCLUDES:=
LDFLAGS:=
LINK:=

COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

ifeq ($(UNIVERSAL),1)
  CFLAGS_PPC:=		$(PLATFORM_CFLAGS_PPC) $(EXEC_EXTRA_CFLAGS)
  CFLAGS_X86:=		$(PLATFORM_CFLAGS_X86) $(EXEC_EXTRA_CFLAGS)
else
  CFLAGS:=		$(PLATFORM_CFLAGS) $(EXEC_EXTRA_CFLAGS)
endif

DEFINES:=		$(EXEC_EXTRA_DEFINES)
INCLUDES:=		$(EXEC_EXTRA_INCLUDES)
LDFLAGS:=		$(PLATFORM_LDFLAGS) -L$(COMMON_DIR)/products \
			$(EXEC_EXTRA_LDFLAGS)
LINK:=			$(EXEC_EXTRA_LINK)

ifeq ($(PLATFORM),MacOSX)
  DEFINES+=		$(EXEC_MACOSX_DEFINES)
  INCLUDES:=		$(MACOSX_ISYSROOT) $(INCLUDES) $(EXEC_MACOSX_INCLUDES)
  LDFLAGS+=		$(EXEC_MACOSX_LDFLAGS)
  LINK+=		$(EXEC_MACOSX_LINK)
  ifeq ($(UNIVERSAL),1)
    CFLAGS_PPC+=	$(EXEC_MACOSX_CFLAGS) $(EXEC_PPC_CFLAGS)
    CFLAGS_X86+=	$(EXEC_MACOSX_CFLAGS) $(EXEC_X86_CFLAGS)
  else
    CFLAGS+=		$(EXEC_MACOSX_CFLAGS)
    ifeq ($(PROCESSOR),powerpc)
      CFLAGS+=		$(EXEC_PPC_CFLAGS)
      DEFINES+=		$(EXEC_PPC_DEFINES)
      LDFLAGS+=		$(EXEC_PPC_LDFLAGS)
    endif
    ifeq ($(PROCESSOR),i386)
      CFLAGS+=		$(EXEC_X86_CFLAGS)
      DEFINES+=		$(EXEC_X86_DEFINES)
      LDFLAGS+=		$(EXEC_X86_LDFLAGS)
    endif
  endif
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

ifeq ($(UNIVERSAL),1)
  CFLAGS_PPC+=		$(DEFINES) $(EXEC_PPC_DEFINES)
  CFLAGS_X86+=		$(DEFINES) $(EXEC_X86_DEFINES)
  INCLUDES_PPC:=	$(INCLUDES) $(EXEC_PPC_INCLUDES)
  INCLUDES_X86:=	$(INCLUDES) $(EXEC_X86_INCLUDES)
else
  CFLAGS+=		$(DEFINES)
endif

include			$(COMMON_DIR)/mk/sources.mk

LOCAL_LIBS:=	$(filter-out %-ranlib.a,$(wildcard *.a))
LOCAL_RANLIBS:=	$(foreach lib,$(LOCAL_LIBS),$(lib:.a=-ranlib.a))

BUILT_LIBS:=	$(wildcard */lib/*.a)

TARGET:=	$(TARGET_DIR)/$(TARGET_BASE)$(EXEC_EXT)

##
# These are always defined even when UNIVERSAL is not set so a "make disclean"
# will remove them.
##
TARGET_PPC:=	$(TARGET_BASE)-ppc$(EXEC_EXT)
TARGET_X86:=	$(TARGET_BASE)-x86$(EXEC_EXT)

##
# Build rules
##

.PHONY: all

all: $(TARGET) $(POST_TARGET)
include		$(COMMON_DIR)/mk/auto_dep.mk

ifeq ($(UNIVERSAL),1)

$(TARGET): $(TARGET_PPC) $(TARGET_X86)
	-$(MKDIR) $(TARGET_DIR)
	$(LIPO) -create $(TARGET_PPC) $(TARGET_X86) -output $@

ifndef JNI_MANUAL_TARGET
$(TARGET_PPC): $(OBJECTS_PPC) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	$(CC_LINK) $(CFLAGS_PPC) $(LDFLAGS) -o $@ *-ppc.o $(LINK)

$(TARGET_X86): $(OBJECTS_X86) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	$(CC_LINK) $(CFLAGS_X86) $(LDFLAGS) -o $@ *-x86.o $(LINK)
endif

else	# UNIVERSAL

$(TARGET): $(OBJECTS) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *.o $(LINK)

endif	# UNIVERSAL

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	$(RM) *.o .*.d

distclean mostlyclean: clean
	$(RM) $(TARGET) $(TARGET_PPC) $(TARGET_X86) $(POST_TARGET)

# vim:set noet sw=8 ts=8:
