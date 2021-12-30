##
# LightZone Mac OS X help makefile
#
# Paul J. Lucs [paul@lightcrafts.com]
##

ROOT:=			../..
COMMON_DIR:=		$(ROOT)/lightcrafts

APP_NAME?=		LightZone
HELP:=			$(APP_NAME)_Help
LANG?=			English

TRANSLATIONS_FILE:=	$(COMMON_DIR)/help/index_translations.txt
LANG_LINE:=		$(shell grep '^$(LANG)' $(TRANSLATIONS_FILE))
ISO_LANG_CODE:=		$(strip $(shell echo $(LANG_LINE) | cut -f2 -d:))
HELP_TITLE:=		$(strip $(shell echo $(LANG_LINE) | cut -f5 -d:))
INDEX_WORD:=		$(strip $(shell echo $(LANG_LINE) | cut -f6 -d:))
HTML_CHARSET:=		$(strip $(shell echo $(LANG_LINE) | cut -f7 -d:))

SOURCE_LANG_DIR:=	../resources/Resources/$(LANG).lproj
SOURCE_HELP_DIR:=	$(SOURCE_LANG_DIR)/$(HELP)

APP_BUNDLE_DIR:=	../release/$(APP_NAME).app
TARGET_LANG_DIR:=	$(APP_BUNDLE_DIR)/Contents/Resources/$(LANG).lproj
TARGET_HELP_DIR:=	$(TARGET_LANG_DIR)/$(HELP)

IS_ALPHABETIC=		$(shell echo $(HTML_CHARSET) | grep -i 'iso-8859-\|koi8-')

ifneq ($(IS_ALPHABETIC),)
MIN_TERM_LENGTH:=	3
else
MIN_TERM_LENGTH:=	1
endif

HELP_INDEX:=		$(TARGET_HELP_DIR)/$(HELP).helpindex
HELP_INDEXER:=		hiutil
HELP_INDEXER_OPTIONS:=	-Cag -f $(HELP_INDEX) -m $(MIN_TERM_LENGTH) -s $(ISO_LANG_CODE)

INDEX_PAGE_COMPILER:=	$(COMMON_DIR)/tools/bin/lc-help-make_index
INDEX_PAGE_DIR:=	$(TARGET_HELP_DIR)/index
INDEX_PAGE:=		$(INDEX_PAGE_DIR)/index.html

COPY= 			cd "$1" && tar -c -f - --exclude LightZoneTOC.xml * | \
			( cd $(PWD)/$(TARGET_HELP_DIR) && tar xfB - )

RM:=			rm -fr
MKDIR:=			mkdir -p

##
# Build rules
##

.PHONY: all copy search_indexes index_page info_plist_strings
all: copy search_indexes index_page info_plist_strings

$(TARGET_HELP_DIR):
	-$(MKDIR) $@

copy: $(TARGET_HELP_DIR)
	$(call COPY,$(COMMON_DIR)/help/neutral)
	$(call COPY,$(COMMON_DIR)/help/$(LANG))
	$(call COPY,$(SOURCE_HELP_DIR))

info_plist_strings: $(TARGET_HELP_DIR)
	cp $(SOURCE_LANG_DIR)/InfoPlist.strings $(TARGET_LANG_DIR)

search_indexes: $(HELP_INDEX)
index_page: $(INDEX_PAGE)

$(HELP_INDEX): $(TARGET_HELP_DIR)
	$(RM) $(INDEX_PAGE_DIR)
	$(HELP_INDEXER) $(HELP_INDEXER_OPTIONS) $(TARGET_HELP_DIR)

$(INDEX_PAGE): $(TARGET_HELP_DIR)
	-$(MKDIR) $(@D)
	$(INDEX_PAGE_COMPILER) -a "$(APP_NAME)" -C $(^D) -h "$(HELP_TITLE)" -i "$(INDEX_WORD)" -2 $(ISO_LANG_CODE) $(HELP) > $@

##
# Utility rules
##

clean distclean mostlyclean:
	$(RM) $(HELP_INDEX) $(INDEX_PAGE_DIR) $(TARGET_LANG_DIR)
