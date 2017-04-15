ROOT:=			../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

COMMON_PRODUCTS:=	$(wildcard $(COMMON_DIR)/products/*)
DEST_COMMON_PRODUCTS:=	$(filter-out GNUmakefile,$(notdir $(COMMON_PRODUCTS)))

WINDOWS_LIBS:=		$(wildcard ../lib/*.jar)
DEST_WINDOWS_LIBS:=	$(notdir $(WINDOWS_LIBS))

WINDOWS_PROC_LIBS:=	$(wildcard $(DLL_DIR)/libgcc_s_*-1.dll) \
			libjpeg-8.dll \
			liblzma-5.dll \
			libwinpthread-1.dll \
			libgomp-1.dll \
			liblcms2-2.dll \
			libtiff-5.dll \
			zlib1.dll

DEST_WINDOWS_PROC_LIBS:=	$(notdir $(WINDOWS_PROC_LIBS))

DEST_WINDOWS_SHARES:=	share

##
# Build rules
##

.PHONY: all

all: $(DEST_COMMON_PRODUCTS) $(DEST_WINDOWS_LIBS) $(DEST_WINDOWS_PROC_LIBS) $(DEST_WINDOWS_SHARES)

$(DEST_COMMON_PRODUCTS): FORCE
	$(call SYMLINK,$(COMMON_DIR)/products/$@,$@)

$(DEST_WINDOWS_LIBS): FORCE
	$(call SYMLINK,../lib/$@,$@)

$(DEST_WINDOWS_PROC_LIBS): FORCE
	$(call SYMLINK,${DLL_DIR}/$@,$@)

$(DEST_WINDOWS_SHARES): FORCE
	$(call SYMLINK,../$@,$@)

.PHONY: FORCE
FORCE:

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean distclean mostlyclean:
	$(RM) *.chm *.dll *.jar *.exe *.log

# vim:set noet sw=8 ts=8:
