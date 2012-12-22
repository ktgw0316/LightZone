ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

DEP_LIBS:=	libjpeg

##
# Build rules
##

.PHONY: all $(DEP_LIBS)
all: $(DEP_LIBS)
	$(MAKE) -f lcjpeg.mk

$(DEP_LIBS):
	$(MAKE) -C $@

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(MAKE) -f lcjpeg.mk $@
	for lib in $(DEP_LIBS); do $(MAKE) -C $$lib $@; done

# vim:set noet sw=8 ts=8:
