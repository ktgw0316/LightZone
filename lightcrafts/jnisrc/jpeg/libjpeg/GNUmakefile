##
# libjpeg makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

HIGH_PERFORMANCE:=	true

ROOT:=			../../../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk
include			$(COMMON_DIR)/mk/sources.mk

LIB_SRC:=		$(wildcard libjpeg-*)
LIB:=			.libs/libjpeg.a

ARCHS:=			$(PROCESSOR) $(OTHER_PROCESSOR)
ARCH_SRCS:=		$(foreach arch,$(ARCHS),arch-$(arch)/$(LIB_SRC))
ARCH_INCLUDES:=		$(foreach src,$(ARCH_SRCS),$(wildcard $(src)/*.h))
ARCH_LIBS:=		$(foreach src,$(ARCH_SRCS),$(src)/$(LIB))
ARCH_MAKEFILES:=	$(foreach src,$(ARCH_SRCS),$(src)/Makefile)

TARGET_LIB:=		lib/$(LIB)
INCLUDE_DIRS:=		$(foreach arch,$(ARCHS),arch-$(arch)/include)
TARGET_INCLUDES:=	$(subst $(LIB_SRC),include,$(ARCH_INCLUDES))

##
# The -fexceptions is here since our JPEG JNI code is C++ and uses exceptions.
# Since C code doesn't have exceptions on by default, we have to turn it on
# manually so exceptions can propagate through C functions.
##
CFLAGS:=		$(MACOSX_ISYSROOT) -fexceptions

CONFIG_OPTIONS:=	--disable-dependency-tracking
CONFIG_CROSS:=		--host=$(CONFIG_HOST) --target=$(CONFIG_TARGET)

##
# Build rules.
##

.PHONY: all part1 part2 includes
all: part1 part2

part1: $(ARCH_SRCS) $(ARCH_MAKEFILES) $(ARCH_LIBS) lib $(TARGET_LIB)
part2:
	$(MAKE) includes

includes: $(INCLUDE_DIRS) $(TARGET_INCLUDES)

##### Symlink the sources.

ifeq ($(PLATFORM),MacOSX)
$(ARCH_SRCS):
	if [ ! -e $@ ] ; \
	then $(COMMON_DIR)/tools/bin/lc-symlinktree $(LIB_SRC) $@ ; \
	fi
else
$(ARCH_SRCS):
	if [ ! -e $@ ] ; \
	then $(MKDIR) $(@D) && ln -fs ../$(LIB_SRC) $@ ; \
	fi
endif

##### Create the sub-Makefiles via configure.

ifeq ($(UNIVERSAL),1)
  MAKEFILE_PPC:=		arch-powerpc/$(LIB_SRC)/Makefile
  MAKEFILE_X86:=		arch-i386/$(LIB_SRC)/Makefile

  $(MAKEFILE_PPC): CC:=		$(CC_PPC)
  $(MAKEFILE_PPC): CFLAGS+=	$(PLATFORM_CFLAGS_PPC) # $(ALTIVEC_CFLAGS) off for now, there is a bug in the color

  $(MAKEFILE_X86): CC:=		$(CC_X86)
  $(MAKEFILE_X86): CFLAGS+=	$(PLATFORM_CFLAGS_X86)

  arch-$(OTHER_PROCESSOR)/$(LIB_SRC)/Makefile: CONFIG_OPTIONS+= $(CONFIG_CROSS)
else
  CFLAGS+=		-fPIC
  # CFLAGS+=		$(PLATFORM_CFLAGS)
endif

$(ARCH_MAKEFILES):
	cd $(@D) && \
	autoreconf -fiv && \
	CC="$(CC)" CFLAGS="$(CFLAGS)" ./configure $(CONFIG_OPTIONS)

##### Make the libraries.

ifeq ($(UNIVERSAL),1)
$(TARGET_LIB):
	$(LIPO) -create $(ARCH_LIBS) -output $@
else
$(TARGET_LIB):
	$(call SYMLINK,../$(ARCH_LIBS),lib)
endif

$(ARCH_LIBS): FORCE
	$(MAKE) -C $(ARCH_SRCS) -j $(NUM_PROCESSORS)

##### Symlink the include files.

$(TARGET_INCLUDES):
	$(call SYMLINK,../$(LIB_SRC)/$(@F),$(@D))

lib $(INCLUDE_DIRS):
	$(MKDIR) $@

##
# Utility rules.
##

.PHONY: FORCE
FORCE:

.PHONY: clean distclean mostlyclean

clean:

distclean: clean
	if [ -f $(LIB_SRC)/Makefile ]; then $(MAKE) -C $(LIB_SRC) $@; fi
	$(RM) arch-* lib

mostlyclean:

# vim:set noet sw=8 ts=8:
