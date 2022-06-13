#!/bin/sh

# Remove library paths that depends on a build environment
# from libraries under specified directory.

if [ $# -ne 1 ]; then
  echo "Usage: "`basename $0`" DIRECTORY" 1>&2
  exit 1
fi

SCRIPT_DIR=$(cd $(dirname $0) && pwd)

cd $1

# Copy required dylibs, and change their ids
FILES=$(find . -maxdepth 1 -name "*.jnilib" -o -name "dcraw_lz")
sh ${SCRIPT_DIR}/copydylibs.sh ${FILES}

# Change the local library paths in each file
LIBS="${FILES} $(find . -maxdepth 1 -name "*.dylib")"
for LIB in ${LIBS}; do
  otool -LX ${LIB} | egrep -v "$(otool -DX ${LIB})" | egrep -v "/(usr/lib|System)" | grep -o "/.*\.dylib" | while read; do
    sh -x -c "install_name_tool -change ${REPLY} @loader_path/$(basename ${REPLY}) ${LIB}"
  done
done

# vim:set noet sw=8 ts=8:
