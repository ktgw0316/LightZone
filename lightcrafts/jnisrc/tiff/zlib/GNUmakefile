##
# zlib makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ROOT:=			../../../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform0.mk

HIGH_PERFORMANCE:=	1
ifeq ($(PLATFORM),MacOSX)
  USE_ICC_HERE:=	1
endif

include			$(COMMON_DIR)/mk/platform.mk
include			$(COMMON_DIR)/mk/sources.mk

LIB_SRC:=		$(wildcard zlib-*)
LIB:=			libz.a

ARCHS:=			$(PROCESSOR) $(OTHER_PROCESSOR)
ARCH_SRCS:=		$(foreach arch,$(ARCHS),arch-$(arch)/$(LIB_SRC))
ARCH_INCLUDES:=		$(foreach src,$(ARCH_SRCS),$(wildcard $(src)/*.h))
ARCH_LIBS:=		$(foreach src,$(ARCH_SRCS),$(src)/$(LIB))
ARCH_MAKEFILES:=	$(foreach src,$(ARCH_SRCS),$(src)/Makefile)

TARGET_LIB:=		lib/$(LIB)
INCLUDE_DIRS:=		$(foreach arch,$(ARCHS),arch-$(arch)/include)
TARGET_INCLUDES:=	$(subst $(LIB_SRC),include,$(ARCH_INCLUDES))

CFLAGS:=		$(MACOSX_ISYSROOT)

CONFIG_OPTIONS:=	

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
  $(MAKEFILE_PPC): CFLAGS+=	$(PLATFORM_CFLAGS_PPC) $(ALTIVEC_CFLAGS)

  $(MAKEFILE_X86): AR:=		$(AR_X86)
  $(MAKEFILE_X86): CC:=		$(CC_X86)
  $(MAKEFILE_X86): CFLAGS+=	$(PLATFORM_CFLAGS_X86)
else
  CFLAGS+=			$(PLATFORM_CFLAGS)
endif

$(ARCH_MAKEFILES):
	cd $(@D) && \
	AR="$(AR)" CC="$(CC)" CFLAGS='$(CFLAGS)' ./configure $(CONFIG_OPTIONS)

##### Make the libraries.

ifeq ($(UNIVERSAL),1)
$(TARGET_LIB):
	$(LIPO) -create $(ARCH_LIBS) -output $@
else
$(TARGET_LIB):
	$(call SYMLINK,../$(ARCH_LIBS),lib)
endif

$(ARCH_LIBS): FORCE
	$(MAKE) -C $(@D) -j $(NUM_PROCESSORS) $(@F)

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
	$(RM) arch-* lib $(LIB_SRC)/Makefile

mostlyclean:

# vim:set noet sw=8 ts=8:
