ROOT:=			../..
COMMON_DIR:=		$(ROOT)/lightcrafts

include			$(COMMON_DIR)/mk/platform.mk

COMMON_PRODUCTS:=	$(filter-out GNUmakefile,$(wildcard $(COMMON_DIR)/products/*))
DEST_COMMON_PRODUCTS:=	$(notdir $(COMMON_PRODUCTS))

LINUX_LIBS:=		$(wildcard ../lib/*)
DEST_LINUX_LIBS:=	$(notdir $(LINUX_LIBS))

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
	$(RM) *.jar *.so

# vim:set noet sw=8 ts=8:
