##
# JNI Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

##
# This makefile is to be included by other makefiles that build JNI source.
# At a minimum, a makefile including this makefile must do:
#
#	TARGET_BASE:=	Neato
#	JAVAH_CLASSES:=	com.lightcrafts.utils.Neato
#	include		../jni.mk
#
# in which case libNeato.jnilib (for Mac OS X), Neato.dll (for Windows), or
# libNeato.so (for Linux) would be built.
#
# If the makefile needs to override the default settings, it can define:
#
#	JNI_EXTRA_CFLAGS	Specify extra flags to the compiler.
#	JNI_EXTRA_DEFINES	Specify extra -D directives.
#	JNI_EXTRA_INCLUDES	Specify extra -I directives.
#	JNI_EXTRA_LDFLAGS	Specify extra -L directives.
#	JNI_EXTRA_LINK		Specify extra -l directives.
#
#	JNI_EXTRA_PKGCFG	Specify extra package name to set -I, _L,
#				and -l directives using pkg-config.
#
#	JNI_EXTRA_CLEAN		Specify extra files to clean.
#	JNI_EXTRA_DISTCLEAN	Specify extra files to distclean.
#
# The above apply to all platforms.  To specify extra stuff for a specific
# platform only, the makefile can replace "EXTRA" with one of "MACOSX",
# "WINDOWS" or "LINUX".
#
# In addition to the above, there are also JNI_ARM_CFLAGS, JNI_ARM_DEFINES, and
# JNI_ARM_LDFLAGS for arm64-specific directives, and JNI_X86_CFLAGS,
# JNI_X86_DEFINES, and JNI_X86_LDFLAGS for Intel-specific directives.
#
# If a makefile needs to override how the TARGET is build, it can do:
#
#	JNI_MANUAL_TARGET:=	1
#
# then define the TARGET rule itself.
#
# Also see the comments in sources.mk.
##

##
# Undefine all this stuff so we don't get any defaults.
##
DEFINES:=
INCLUDES:=
LINK:=

COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

ifeq ($(UNIVERSAL),1)
  CFLAGS_ARM:=		$(PLATFORM_CFLAGS_ARM) $(JNI_EXTRA_CFLAGS)
  CFLAGS_X86:=		$(PLATFORM_CFLAGS_X86) $(JNI_EXTRA_CFLAGS)
else
  CFLAGS:=		$(PLATFORM_CFLAGS) $(JNI_EXTRA_CFLAGS)
endif

DEFINES:=		-DJNILIB $(JNI_EXTRA_DEFINES)
INCLUDES:=		$(PLATFORM_INCLUDES) $(JAVA_INCLUDES) \
			-I$(COMMON_DIR) \
			-I$(COMMON_DIR)/jnisrc/jniutils $(JNI_EXTRA_INCLUDES)
LDFLAGS:=		$(PLATFORM_LDFLAGS) $(JAVA_LDFLAGS) \
			-L$(COMMON_DIR)/products $(JNI_EXTRA_LDFLAGS)
LINK:=			$(JNI_EXTRA_LINK)

ifdef JNI_EXTRA_PKGCFG
  LINK+=		$(shell $(PKGCFG) --libs-only-l $(JNI_EXTRA_PKGCFG))
  INCLUDES+=		$(shell $(PKGCFG) --cflags-only-I $(JNI_EXTRA_PKGCFG))
  LDFLAGS+=		$(shell $(PKGCFG) --libs-only-L $(JNI_EXTRA_PKGCFG))
endif

TARGET_DIR:=		../../products

ifeq ($(PLATFORM),MacOSX)
  DEFINES+=		$(JNI_MACOSX_DEFINES)
  INCLUDES:=		$(MACOSX_ISYSROOT) $(INCLUDES) $(JNI_MACOSX_INCLUDES)
  LDFLAGS+=		-dynamiclib $(JNI_MACOSX_LDFLAGS)
  LINK+=		$(JNI_MACOSX_LINK)
  ifdef JNI_MACOSX_DYLIB
    JNILIB_EXT:=	$(DYLIB_EXT)
    LINK+=		-install_name $(DYLIB_PREFIX)$(TARGET_BASE)$(DYLIB_EXT)
  else
    ifdef JNI_MACOSX_SHAREDLIB
      JNILIB_EXT:=	.a
    endif
  endif
  ifeq ($(UNIVERSAL),1)
    CFLAGS_ARM+=	$(JNI_MACOSX_CFLAGS) $(JNI_ARM_CFLAGS)
    CFLAGS_X86+=	$(JNI_MACOSX_CFLAGS) $(JNI_X86_CFLAGS)
  else
    CFLAGS+=		$(JNI_MACOSX_CFLAGS)
    ifeq ($(PROCESSOR),arm64)
      CFLAGS+=		$(JNI_ARM_CFLAGS)
      DEFINES+=		$(JNI_ARM_DEFINES)
      LDFLAGS+=		$(JNI_ARM_LDFLAGS)
      LINK+=		$(JNI_ARM_LINK)
    endif
    ifeq ($(PROCESSOR),x86_64)
      CFLAGS+=		$(JNI_X86_CFLAGS)
      DEFINES+=		$(JNI_X86_DEFINES)
      LDFLAGS+=		$(JNI_X86_LDFLAGS)
      LINK+=		$(JNI_X86_LINK)
    endif
  endif
  JNI_EXTRA_CLEAN+=	$(JNI_MACOSX_CLEAN)
  JNI_EXTRA_DISTCLEAN+=	$(JNI_MACOSX_DISTCLEAN)
endif

ifeq ($(PLATFORM),Windows)
  CFLAGS+=		$(JNI_WINDOWS_CFLAGS)
  DEFINES+=		$(JNI_WINDOWS_DEFINES)
  INCLUDES+=		$(JNI_WINDOWS_INCLUDES)
  LDFLAGS+=		-shared -Wl,--add-stdcall-alias $(JNI_WINDOWS_LDFLAGS)
  ifdef JNI_WINDOWS_IMPLIB
    TARGET_IMPLIB:=	$(TARGET_DIR)/$(TARGET_BASE)-implib.a
    ifeq ($(USE_ICC),1)
      LDFLAGS+=		-Wl,--out-implib=$(TARGET_IMPLIB)
    endif
  endif
  LINK+=		$(JNI_WINDOWS_LINK)
  JNI_EXTRA_CLEAN+=	$(JNI_WINDOWS_CLEAN)
  JNI_EXTRA_DISTCLEAN+=	$(JNI_WINDOWS_DISTCLEAN)
endif

ifeq ($(PLATFORM),$(filter $(PLATFORM),Linux FreeBSD SunOS))
  CFLAGS+= 		$(JNI_LINUX_CFLAGS)
  DEFINES+=		$(JNI_LINUX_DEFINES)
  INCLUDES+= 		$(JNI_LINUX_INCLUDES)
  LDFLAGS+=		-shared $(JNI_LINUX_LDFLAGS)
  LINK+=		$(JNI_LINUX_LINK)
  JNI_EXTRA_CLEAN+=	$(JNI_LINUX_CLEAN)
  JNI_EXTRA_DISTCLEAN+=	$(JNI_LINUX_DISTCLEAN)
endif

########## You shouldn't have to change anything below this line. #############

ifdef DEBUG
DEFINES+=	-DDEBUG
endif

ifeq ($(UNIVERSAL),1)
  CFLAGS_ARM+=		$(DEFINES) $(JNI_ARM_DEFINES)
  CFLAGS_X86+=		$(DEFINES) $(JNI_X86_DEFINES)
  INCLUDES_ARM:=	$(INCLUDES) $(JNI_ARM_INCLUDES)
  INCLUDES_X86:=	$(INCLUDES) $(JNI_X86_INCLUDES)
else
  CFLAGS+=		$(DEFINES)
endif

include			$(COMMON_DIR)/mk/sources.mk

LOCAL_LIBS:=	$(filter-out %-ranlib.a,$(wildcard *.a))
LOCAL_RANLIBS:=	$(foreach lib,$(LOCAL_LIBS),$(lib:.a=-ranlib.a))

BUILT_LIBS:=	$(wildcard */lib/*.a)

ifeq ($(PLATFORM),MacOSX)
# don't go directly to target dir; creates an invalid ../../ link in the jni library file itself
# we'll copy after CC instead
TARGET:=	$(JNILIB_PREFIX)$(TARGET_BASE)$(JNILIB_EXT)
else
TARGET:=	$(TARGET_DIR)/$(JNILIB_PREFIX)$(TARGET_BASE)$(JNILIB_EXT)
endif

##
# These are always defined even when UNIVERSAL is not set so a "make disclean"
# will remove them.
##
TARGET_ARM:=	$(JNILIB_PREFIX)$(TARGET_BASE)-arm64$(JNILIB_EXT)
TARGET_X86:=	$(JNILIB_PREFIX)$(TARGET_BASE)-x86$(JNILIB_EXT)

##
# Build rules
##

.PHONY: all

ifndef mk_target
##
# The first time through, we have to build the javah headers WITHOUT including
# the auto-dependencies because the auto-dependencies can't be generated until
# after the javah headers are built.  Once built, we build the phony target
# mk_target that recursively calls make with mk_target=true that will build the
# real target.
##
all: mk_target

.PHONY: mk_target
mk_target:
	@$(MAKE) mk_target=true
else
##
# Now that we've build the javah headers, we can build the real target and
# generate the auto-dependencies in the process.
##
all: $(TARGET) $(POST_TARGET)
include		$(COMMON_DIR)/mk/auto_dep.mk
endif

ifeq ($(PLATFORM),MacOSX)
ifdef JNI_MACOSX_SHAREDLIB
	USE_AR_RANLIB=yes
endif
endif

ifeq ($(UNIVERSAL),1)

$(TARGET): $(TARGET_ARM) $(TARGET_X86)
	-$(MKDIR) $(TARGET_DIR)
	$(LIPO) -create $(TARGET_ARM) $(TARGET_X86) -output $@
ifeq ($(PLATFORM),MacOSX)
	cp -p $@ $(TARGET_DIR)
endif

ifndef JNI_MANUAL_TARGET
ifdef USE_AR_RANLIB
$(TARGET_ARM): $(OBJECTS_ARM) $(BUILT_LIBS)
	ar -rc $@ *-arm64.o
	-ranlib $@
else
$(TARGET_ARM): $(OBJECTS_ARM) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	$(CC_LINK) $(CFLAGS_ARM) $(LDFLAGS) -o $@ *-arm64.o $(LINK)
endif

ifdef USE_AR_RANLIB
$(TARGET_X86): $(OBJECTS_X86) $(BUILT_LIBS)
	ar -rc $@ *-x86.o
	-ranlib $@
else
$(TARGET_X86): $(OBJECTS_X86) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	$(CC_LINK) $(CFLAGS_X86) $(LDFLAGS) -o $@ *-x86.o $(LINK)
endif
endif	# JNI_MANUAL_TARGET

else	# UNIVERSAL

ifndef JNI_MANUAL_TARGET
ifdef USE_AR_RANLIB
$(TARGET): $(OBJECTS) $(RC_OBJECTS) $(BUILT_LIBS)
	-$(MKDIR) $(TARGET_DIR)
	ar -rc $@ *.o
	-ranlib $@
else
$(TARGET): $(OBJECTS) $(RC_OBJECTS) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) $(RC_OBJECTS) -o $@ *.o $(LINK)
ifeq ($(PLATFORM),MacOSX)
	cp -p $@ $(TARGET_DIR)
endif
endif
endif	# JNI_MANUAL_TARGET

endif	# UNIVERSAL

%-ranlib.a : %.a
	cp $< $@
	-ranlib $@

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	$(RM) *.o .*.d javah *-ranlib.a *.dSYM *.res $(TARGET).dSYM $(JNI_EXTRA_CLEAN)

distclean mostlyclean: clean
	$(RM) $(TARGET) $(TARGET_IMPLIB) $(TARGET_ARM) $(TARGET_X86) $(POST_TARGET) $(JNI_EXTRA_DISTCLEAN)

# vim:set noet sw=8 ts=8:
