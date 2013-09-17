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
#	JNI_EXTRA_CLEAN		Specify extra files to clean.
#	JNI_EXTRA_DISTCLEAN	Specify extra files to distclean.
#
# The above apply to all platforms.  To specify extra stuff for a specific
# platform only, the makefile can replace "EXTRA" with one of "MACOSX",
# "WINDOWS" or "LINUX".
#
# In addition to the above, there are also JNI_PPC_CFLAGS, JNI_PPC_DEFINES, and
# JNI_PPC_LDFLAGS for PowerPC-specific directives, and JNI_X86_CFLAGS,
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
CC:=
CFLAGS=
CXX:=
DEFINES:=
INCLUDES:=
LDFLAGS:=
LINK:=

COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

ifeq ($(UNIVERSAL),1)
  CFLAGS_PPC:=		$(PLATFORM_CFLAGS_PPC) $(JNI_EXTRA_CFLAGS)
  CFLAGS_X86:=		$(PLATFORM_CFLAGS_X86) $(JNI_EXTRA_CFLAGS)
else
  CFLAGS:=		$(PLATFORM_CFLAGS) $(JNI_EXTRA_CFLAGS)
endif

DEFINES:=		-DJNILIB $(JNI_EXTRA_DEFINES)
INCLUDES:=		$(JAVA_INCLUDES) \
			-I$(COMMON_DIR)/jnisrc/jniutils $(JNI_EXTRA_INCLUDES)
LDFLAGS:=		$(PLATFORM_LDFLAGS) $(JAVA_LDFLAGS) \
			-L$(COMMON_DIR)/products $(JNI_EXTRA_LDFLAGS)
LINK:=			$(JNI_EXTRA_LINK)

TARGET_DIR:=		../../products

ifeq ($(PLATFORM),MacOSX)
  DEFINES+=		$(JNI_MACOSX_DEFINES)
  INCLUDES:=		$(MACOSX_ISYSROOT) $(INCLUDES) $(JNI_MACOSX_INCLUDES)
  LDFLAGS+=		-dynamiclib -framework JavaVM $(JNI_MACOSX_LDFLAGS)
  LINK+=		$(JNI_MACOSX_LINK)
  ifdef JNI_MACOSX_DYLIB
    JNILIB_EXT:=	$(DYLIB_EXT)
    LINK+=		-install_name $(DYLIB_PREFIX)$(TARGET_BASE)$(DYLIB_EXT)
  endif
  ifeq ($(UNIVERSAL),1)
    CFLAGS_PPC+=	$(JNI_MACOSX_CFLAGS) $(JNI_PPC_CFLAGS)
    CFLAGS_X86+=	$(JNI_MACOSX_CFLAGS) $(JNI_X86_CFLAGS)
  else
    CFLAGS+=		$(JNI_MACOSX_CFLAGS)
    ifeq ($(PROCESSOR),powerpc)
      CFLAGS+=		$(JNI_PPC_CFLAGS)
      DEFINES+=		$(JNI_PPC_DEFINES)
      LDFLAGS+=		$(JNI_PPC_LDFLAGS)
      LINK+=		$(JNI_PPC_LINK)
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
  LDFLAGS+=		-shared -Wl,--add-stdcall-alias -static-libgcc -static-libstdc++ $(JNI_WINDOWS_LDFLAGS)
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

ifeq ($(PLATFORM),Linux)
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
  CFLAGS_PPC+=		$(DEFINES) $(JNI_PPC_DEFINES)
  CFLAGS_X86+=		$(DEFINES) $(JNI_X86_DEFINES)
  INCLUDES_PPC:=	$(INCLUDES) $(JNI_PPC_INCLUDES)
  INCLUDES_X86:=	$(INCLUDES) $(JNI_X86_INCLUDES)
else
  CFLAGS+=		$(DEFINES)
endif

include			$(COMMON_DIR)/mk/sources.mk

JAVAH_HEADERS:=	$(foreach class,$(subst .,_,$(JAVAH_CLASSES)),javah/$(class).h)

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
TARGET_PPC:=	$(JNILIB_PREFIX)$(TARGET_BASE)-ppc$(JNILIB_EXT)
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
all: $(JAVAH_HEADERS) mk_target

$(JAVAH_HEADERS):
	-$(MKDIR) javah
	javah -classpath "$(COMMON_DIR)/build$(CLASSPATH_SEP)$(COMMON_DIR)/extbuild$(CLASSPATH_SEP)$(PLATFORM_DIR)/build" \
	      -d javah $(basename $(subst _,.,$(@F)))

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


ifeq ($(UNIVERSAL),1)

$(TARGET): $(TARGET_PPC) $(TARGET_X86)
	-$(MKDIR) $(TARGET_DIR)
	$(LIPO) -create $(TARGET_PPC) $(TARGET_X86) -output $@
ifeq ($(PLATFORM),MacOSX)
	cp -p $@ $(TARGET_DIR)
endif

ifndef JNI_MANUAL_TARGET
$(TARGET_PPC): $(OBJECTS_PPC) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	$(CC_LINK) $(CFLAGS_PPC) $(LDFLAGS) -o $@ *-ppc.o $(LINK)

$(TARGET_X86): $(OBJECTS_X86) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	$(CC_LINK) $(CFLAGS_X86) $(LDFLAGS) -o $@ *-x86.o $(LINK)
endif

else	# UNIVERSAL

ifndef JNI_MANUAL_TARGET
$(TARGET): $(OBJECTS) $(RC_OBJECTS) $(LOCAL_RANLIBS) $(BUILT_LIBS)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) $(RC_OBJECTS) -o $@ *.o $(LINK)
ifeq ($(PLATFORM),MacOSX)
	cp -p $@ $(TARGET_DIR)
endif
endif

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
	$(RM) $(TARGET) $(TARGET_IMPLIB) $(TARGET_PPC) $(TARGET_X86) $(POST_TARGET) $(JNI_EXTRA_DISTCLEAN)

# vim:set noet sw=8 ts=8:
