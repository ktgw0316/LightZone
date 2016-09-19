#!/bin/sh

# Remove dylib path that depends on a build environment
# from libraries under specified directory.

if [ $# -ne 1 ]; then
  echo "Usage: "`basename $0`" DIRECTORY" 1>&2
  exit 1
fi

PATTERN="/usr/local/"

cd $1
for lib in *.dylib *.jnilib `find . -perm +0111 -type f`; do
  otool -L ${lib} | grep ${PATTERN} | while read line; do
    fullpath=`echo ${line} | sed -e "s% *\(${PATTERN}[^ ]*\.dylib\).*%\1%"`
    name=`echo ${fullpath} | sed -e "s%.*/\(.*\.dylib\)%\1%"`
    install_name_tool -change ${fullpath} @rpath/${name} ${lib}
  done
done
for lib in *.jnilib `find . -perm +0111 -type f`; do
  install_name_tool -add_rpath @loader_path ${lib}
done

