ROOT:=			../..
include			$(ROOT)/lightcrafts/mk/platform.mk

RM:=			rm -fr

EXCLUDED_LIBS:=		../lib/ivy.jar $(wildcard ../lib/lombok*.jar)
COMMON_LIBS:=		$(filter-out $(EXCLUDED_LIBS),$(wildcard ../lib/*))
DEST_COMMON_LIBS:=	$(notdir $(COMMON_LIBS))

##
# Build rules
##

.PHONY: all

all: $(DEST_COMMON_LIBS)

ifeq ($(PLATFORM),Windows)
WINDOWS_FORCE:=		windows_force
.PHONY: windows_force
$(WINDOWS_FORCE):
endif

$(DEST_COMMON_LIBS): $(WINDOWS_FORCE)
	$(call SYMLINK,../lib/$@,$@)

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(RM) $(filter-out GNUmakefile,$(wildcard *))

# vim:set noet sw=8 ts=8:
