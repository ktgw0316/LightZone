ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

##
# Build rules
##

.PHONY: all
all:
	$(MAKE) -f lcjpeg.mk

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(MAKE) -f lcjpeg.mk $@

# vim:set noet sw=8 ts=8:
