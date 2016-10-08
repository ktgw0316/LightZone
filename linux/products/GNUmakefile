ROOT:=			../..
COMMON_DIR:=		$(ROOT)/lightcrafts

include			$(COMMON_DIR)/mk/platform.mk

COMMON_PRODUCTS:=	$(filter-out GNUmakefile,$(wildcard $(COMMON_DIR)/products/*))
DEST_COMMON_PRODUCTS:=	$(notdir $(COMMON_PRODUCTS))

LINUX_LIBS:=		$(wildcard ../lib/*.jar)
DEST_LINUX_LIBS:=	$(notdir $(LINUX_LIBS))

PROCESSOR:=		$(shell uname -m)
ifeq ($(PROCESSOR),i486)
  PROCESSOR:=		i386
endif
ifeq ($(PROCESSOR),i586)
  PROCESSOR:=		i386
endif
ifeq ($(PROCESSOR),i686)
  PROCESSOR:=		i386
endif
ifeq ($(PROCESSOR),x86_64)
  PROCESSOR:=		amd64
endif

##
# Build rules
##

.PHONY: all

all: $(DEST_COMMON_PRODUCTS) $(DEST_LINUX_LIBS)

$(DEST_COMMON_PRODUCTS):
	$(call SYMLINK,$(COMMON_DIR)/products/$@,$@)

$(DEST_LINUX_LIBS):
	$(call SYMLINK,../lib/$@,$@)

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(RM) *.jar
	find -L . -type l -delete # Delete dead symlinks

# vim:set noet sw=8 ts=8:
