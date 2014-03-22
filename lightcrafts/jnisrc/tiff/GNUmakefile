ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

##
# Build rules
##

.PHONY: all
all:
	$(MAKE) -f lctiff.mk

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(MAKE) -f lctiff.mk $@

# vim:set noet sw=8 ts=8:
