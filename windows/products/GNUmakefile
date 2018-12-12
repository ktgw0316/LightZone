ROOT:=			../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

COMMON_PRODUCTS:=	$(wildcard $(COMMON_DIR)/products/*)
DEST_COMMON_PRODUCTS:=	$(filter-out GNUmakefile,$(notdir $(COMMON_PRODUCTS)))

##
# Build rules
##

.PHONY: all libs

all: libs

$(DEST_COMMON_PRODUCTS): FORCE
	$(call SYMLINK,$(COMMON_DIR)/products/$@,$@)

libs: $(DEST_COMMON_PRODUCTS)
	$(shell ldd *.dll | grep -o "/.*mingw.*\.dll" | sort | uniq | xargs -r cp -u -t .)

.PHONY: FORCE
FORCE:

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(RM) *.chm *.dll *.jar *.exe *.log

# vim:set noet sw=8 ts=8:
