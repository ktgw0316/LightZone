ROOT:=			../../..
COMMON_DIR:=		$(ROOT)/lightcrafts

APP_NAME?=		LightZone
HELP:=			$(APP_NAME)_Help
LANG?=			English

TRANSLATIONS_FILE:=	$(COMMON_DIR)/help/index_translations.txt
LANG_LINE:=		$(shell grep '^$(LANG)' $(TRANSLATIONS_FILE))
ISO_LANG_CODE:=		$(strip $(shell echo $(LANG_LINE) | cut -f2 -d:))
W32_LANG_CODE:=		$(strip $(shell echo $(LANG_LINE) | cut -f3 -d:))
W32_REGION:=		$(strip $(shell echo $(LANG_LINE) | cut -f4 -d:))
HELP_TITLE:=		$(strip $(shell echo $(LANG_LINE) | cut -f5 -d:))
INDEX_WORD:=		$(strip $(shell echo $(LANG_LINE) | cut -f6 -d:))
HTML_CHARSET:=		$(strip $(shell echo $(LANG_LINE) | cut -f7 -d:))

TARGET_HELP_DIR:=	/tmp/$(HELP)

# check for 64 bit version of windows
# to determine path to HTML Help Workshop
UNAME:= $(shell uname -ms | grep -e 64 -e WOW)
ifeq ($(strip $(UNAME)),)
HTML_HELP_DIR:=	/cygdrive/c/Program\ Files
else
HTML_HELP_DIR:=	/cygdrive/c/Program\ Files\ \(x86\)
endif
HTML_HELP_COMPILER:=	$(HTML_HELP_DIR)/HTML\ Help\ Workshop/hhc

HTML_HELP_PROJECT:=	$(APP_NAME).hhp
HTML_HELP_TOC:=		TOC.hhc
HTML_HELP_FILE:=	../../products/$(APP_NAME)-$(ISO_LANG_CODE).chm

INDEX_PAGE_COMPILER:=	$(COMMON_DIR)/tools/bin/lc-help-make_index
INDEX_PAGE_DIR:=	$(TARGET_HELP_DIR)/index
INDEX_PAGE:=		$(INDEX_PAGE_DIR)/index.html

COPY= 			cd "$1" && tar -c -f - --exclude LightZoneTOC.xml * | \
			( cd $(TARGET_HELP_DIR) && tar xfB - )

MKDIR:=			mkdir -p
RM:=			rm -fr

.PHONY: all copy index_page html_help test
all: copy index_page html_help
index_page: $(INDEX_PAGE)
html_help: $(HTML_HELP_FILE)
test: copy index_page copy_project

copy: distclean $(TARGET_HELP_DIR)
	$(call COPY,$(COMMON_DIR)/help/neutral)
	$(call COPY,$(COMMON_DIR)/help/$(LANG))
	$(call COPY,neutral/$(HELP))
#	$(call COPY,$(LANG)/$(HELP))
	$(call COPY,$(HELP))

copy_project: /tmp/$(HTML_HELP_PROJECT)
ifeq ($(HTML_CHARSET),ISO-8859-1)
	cp $(COMMON_DIR)/help/$(LANG)/$(HTML_HELP_TOC) /tmp
else
	iconv -f UTF-8 -t ${HTML_CHARSET} $(COMMON_DIR)/help/$(LANG)/$(HTML_HELP_TOC) > /tmp/$(HTML_HELP_TOC)
endif

/tmp/$(HTML_HELP_PROJECT): $(HTML_HELP_PROJECT).template
	./build-hhp.pl -c $(W32_LANG_CODE) -e $(HTML_CHARSET) -h "$(HELP_TITLE)" -i $(ISO_LANG_CODE) -l $(LANG) -r "$(W32_REGION)" < $< > $@

$(TARGET_HELP_DIR):
	$(MKDIR) $@

$(INDEX_PAGE): convert_charset
	-$(MKDIR) $(@D)
	$(INDEX_PAGE_COMPILER) -C /tmp -h"$(HELP_TITLE)" -i"$(INDEX_WORD)" -2 $(ISO_LANG_CODE) $(HELP) > $@

convert_charset:
ifneq ($(HTML_CHARSET),ISO-8859-1)
	./iconvall.sh $(HTML_CHARSET) $(TARGET_HELP_DIR)
endif

$(HTML_HELP_FILE): copy_project
	cd /tmp && $(HTML_HELP_COMPILER) $(HTML_HELP_PROJECT) || exit 0
	mv /tmp/$(@F) $@
	$(MAKE) clean

clean:
	$(RM) $(TARGET_HELP_DIR)
	$(RM) /tmp/$(HTML_HELP_PROJECT)
	$(RM) /tmp/$(HTML_HELP_TOC)

distclean mostlyclean: clean
	$(RM) $(HTML_HELP_FILE)
