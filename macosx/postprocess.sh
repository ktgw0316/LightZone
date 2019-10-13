#!/bin/sh

# Remove library paths that depends on a build environment
# from libraries under specified directory.

if [ $# -ne 1 ]; then
  echo "Usage: "`basename $0`" DIRECTORY" 1>&2
  exit 1
fi

PATTERN="/usr/local/"

cd $1

# Copy required dylibs, and change their ids
DEPS=$(otool -L dcraw_lz *.jnilib | egrep -v "/(usr/lib|System)" | grep -o "/.*dylib" | sort -u)
for DEP in ${DEPS}; do
  LIB=$(basename ${DEP})
  cp -f ${DEP} ${LIB}
  chmod 755 ${LIB}
  install_name_tool -id ${LIB} ${LIB}
done

# Change the local library paths in each file
FILES=$(find . -name "*.jnilib" -a ! -name "libquaqua*" -o -name "*.dylib" -o -name "dcraw_lz")
for FILE in ${FILES}; do
  otool -L ${FILE} | egrep -v "$(otool -D ${FILE})" | egrep -v "/(usr/lib|System)" | grep -o "/.*\.dylib" | while read; do
    install_name_tool -change ${REPLY} @executable_path/$(basename ${REPLY}) ${FILE}
  done
done

# # Hack for homebrew libtiff, which uses libjpeg instead of libjpeg-turbo
# tiff=$(find . -name "libtiff*.dylib")
# turbo=$(find . -name "libjpeg*.dylib" | sed -e "s%^\.\/%%")
# jpeg=$(otool -L ${tiff} | grep -o "@executable_path/libjpeg.*\.dylib")
# install_name_tool -change ${jpeg} @executable_path/${turbo} ${tiff}

# Hack for older macOS than High Sierra
install_name_tool -change /System/Library/Frameworks/ColorSync.framework/Versions/A/ColorSync /System/Library/Frameworks/ApplicationServices.framework/Frameworks/ColorSync.framework/Versions/A/ColorSync libMacOSX.jnilib

# vim:set noet sw=8 ts=8:
