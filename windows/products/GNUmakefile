ROOT:=			../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

COMMON_PRODUCTS:=	$(wildcard $(COMMON_DIR)/products/*)
DEST_COMMON_PRODUCTS:=	$(filter-out GNUmakefile,$(notdir $(COMMON_PRODUCTS)))

WINDOWS_LIBS:=		$(wildcard ../lib/*.jar)
DEST_WINDOWS_LIBS:=	$(notdir $(WINDOWS_LIBS))

WINDOWS_PROC_LIBS:=		$(wildcard ../lib/$(PROCESSOR)/*.dll)
DEST_WINDOWS_PROC_LIBS:=	$(notdir $(WINDOWS_PROC_LIBS))

##
# Build rules
##

.PHONY: all

all: $(DEST_COMMON_PRODUCTS) $(DEST_WINDOWS_LIBS) $(DEST_WINDOWS_PROC_LIBS)

$(DEST_COMMON_PRODUCTS): FORCE
	$(call SYMLINK,$(COMMON_DIR)/products/$@,$@)

$(DEST_WINDOWS_LIBS): FORCE
	$(call SYMLINK,../lib/$@,$@)

$(DEST_WINDOWS_PROC_LIBS): FORCE
	$(call SYMLINK,../lib/${PROCESSOR}/$@,$@)

.PHONY: FORCE
FORCE:

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(RM) *.chm *.dll *.jar *.exe *.log

# vim:set noet sw=8 ts=8:
