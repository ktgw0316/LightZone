LANGUAGES = English Danish Dutch French Italian Japanese Spanish
BUILD_DIR = build
NEUTRAL_DIR = neutral
IMAGES_DIR = images
JAR_FILE = lightzonehelp.jar
PRODUCTS_DIR = ../products

.PHONY: all copy tidy index jar build clean

all: build

copy:
	mkdir -p $(BUILD_DIR)
	rsync -a ../../lightcrafts/help/ $(BUILD_DIR) --exclude '*.txt'
	rsync -a LightZone.hs $(BUILD_DIR)
	rsync -a LightZone_*.hs $(BUILD_DIR)
	rsync -a $(NEUTRAL_DIR)/ $(BUILD_DIR)/neutral
	rsync -a $(IMAGES_DIR)/ $(BUILD_DIR)/neutral/images
	for lang in $(LANGUAGES); do \
		rsync -a $(BUILD_DIR)/neutral/ $(BUILD_DIR)/$$lang; \
	done

tidy:
	for lang in $(LANGUAGES); do \
		./tidyall.sh $(BUILD_DIR)/$$lang; \
	done

index:
	for lang in $(LANGUAGES); do \
		./jhindexer.sh $(BUILD_DIR)/$$lang; \
	done

jar:
	jar cf $(JAR_FILE) -C $(BUILD_DIR) LightZone*.hs
	for lang in $(LANGUAGES); do \
		jar uf $(JAR_FILE) -C $(BUILD_DIR) $$lang; \
	done

build: clean copy index jar
	cp -f $(JAR_FILE) $(PRODUCTS_DIR)

clean:
	rm -f $(JAR_FILE)
	rm -rf $(BUILD_DIR)