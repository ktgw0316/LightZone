ROOT:=		../../..

COMMON_DIR:=	../../jnisrc/libESD
COMMON_FILES:=	eWebLibrary.h eWebClient.lib

##
# Build rules
##

.PHONY: all
all: $(COMMON_FILES)
	@$(MAKE) -f installer.mk

$(COMMON_FILES):
	ln -s $(COMMON_DIR)/$@ $@

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	rm -f $(COMMON_FILES)
	@$(MAKE) -f installer.mk $@

distclean mostlyclean: clean
	@$(MAKE) -f installer.mk $@

# vim:set noet sw=8 ts=8:
