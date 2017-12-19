#!/bin/sh

# Remove library paths that depends on a build environment
# from libraries under specified directory.

if [ $# -ne 1 ]; then
  echo "Usage: "`basename $0`" DIRECTORY" 1>&2
  exit 1
fi

PATTERN="/usr/local/"

cd $1

FILES=$(find . -name "*.jnilib" -o -name "dcraw_lz")

# Copy required dylibs, and change its id
for lib in ${FILES}; do
  otool -L ${lib} | grep ${PATTERN} | while read line; do
    fullpath=`echo ${line} | sed -e "s% *\(${PATTERN}[^ ]*\.dylib\).*%\1%"`
    name=`echo ${fullpath} | sed -e "s%.*/\(.*\.dylib\)%\1%"`
    cp -f ${fullpath} ${name}
    chmod 755 ${name}
    install_name_tool -id ${name} ${name} || continue
  done
done

# Rewrite the library paths
for lib in ${FILES}; do
  otool -L ${lib} | grep ${PATTERN} | while read line; do
    fullpath=`echo ${line} | sed -e "s% *\(${PATTERN}[^ ]*\.dylib\).*%\1%"`
    name=`echo ${fullpath} | sed -e "s%.*/\(.*\.dylib\)%\1%"`
    install_name_tool -change ${fullpath} @rpath/${name} ${lib} || continue
  done
done
for lib in ${FILES}; do
  install_name_tool -add_rpath @loader_path ${lib} || continue
done
