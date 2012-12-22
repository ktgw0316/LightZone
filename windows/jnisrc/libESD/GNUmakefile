ROOT:=		../../..

COMMON_DIR:=	$(ROOT)/macosx/jnisrc/libESD

##
# Build rules
##

.PHONY: all
all: eSellerate.cpp
	@$(MAKE) -f windows.mk

eSellerate.cpp:
	ln -s $(COMMON_DIR)/eSellerate.mm $@

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	rm -f eSellerate.cpp
	@$(MAKE) -f windows.mk $@

distclean mostlyclean: clean
	@$(MAKE) -f windows.mk $@

# vim:set noet sw=8 ts=8:
