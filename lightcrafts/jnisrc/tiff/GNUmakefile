ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

DEP_LIBS:=	zlib libtiff

##
# Build rules
##

.PHONY: all $(DEP_LIBS)
all: $(DEP_LIBS)
	$(MAKE) -f lctiff.mk

$(DEP_LIBS):
	$(MAKE) -C $@

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(MAKE) -f lctiff.mk $@
	for lib in $(DEP_LIBS); do $(MAKE) -C $$lib $@; done

# vim:set noet sw=8 ts=8:
