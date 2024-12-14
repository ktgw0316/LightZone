#!/bin/sh

# Remove library paths that depends on a build environment
# from libraries under specified directory.

if [ $# -ne 1 ]; then
  echo "Usage: "`basename $0`" DIRECTORY" 1>&2
  exit 1
fi

SCRIPT_DIR=$(cd $(dirname $0) && pwd)

cd $1

# Copy required dylibs
FILES=$(find . -maxdepth 1 -name "*.jnilib" -o -name "dcraw_lz")
sh ${SCRIPT_DIR}/copydylibs.sh ${FILES}

# Change the local library paths in each file
EXCLUDE_PATTERN="(/usr/lib|/System|@loader_path|@rpath)"
DYLIBS=$(find . -maxdepth 1 -name "*.dylib")
for LIB in ${FILES} ${DYLIBS}; do
  otool -LX ${LIB} | grep -v "$(otool -DX ${LIB})" | egrep -v "${EXCLUDE_PATTERN}" | grep -o "/.*\.dylib" | while read; do
    sh -x -c "install_name_tool -change ${REPLY} @rpath/$(basename ${REPLY}) ${LIB}"
  done
done

otool -LX dcraw_lz | egrep -v "${EXCLUDE_PATTERN}" | grep -o "/.*\.dylib" | while read; do
  sh -x -c "install_name_tool -change ${REPLY} @rpath/$(basename ${REPLY}) dcraw_lz"
done

for LIB in ${DYLIBS}; do
    install_name_tool -id @rpath/$(basename ${LIB}) ${LIB}
done

# Sign the dylibs/executables
for FILE in ${FILES} ${DYLIBS}; do
  codesign --force --deep --sign - ${FILE}
done

# vim:set noet sw=8 ts=8:
