#!/bin/sh

# Remove library paths that depends on a build environment
# from libraries under specified directory.

if [ $# -ne 1 ]; then
  echo "Usage: "`basename $0`" DIRECTORY" 1>&2
  exit 1
fi

SCRIPT_DIR=$(cd $(dirname $0) && pwd)

cd $1
FILES=$(find . -name "*.jnilib" -o -name "dcraw_lz")

# Copy required dylibs, and change their ids
sh ${SCRIPT_DIR}/copydylibs.sh ${FILES}

# Change the local library paths in each file
for FILE in ${FILES}; do
  otool -L ${FILE} | egrep -v "$(otool -D ${FILE})" | egrep -v "/(usr/lib|System)" | grep -o "/.*\.dylib" | while read; do
    install_name_tool -change ${REPLY} @loader_path/$(basename ${REPLY}) ${FILE}
  done
done

# vim:set noet sw=8 ts=8:
