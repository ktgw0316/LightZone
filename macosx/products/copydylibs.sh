#!/bin/sh

SRC_DIR=$(dirname "$1")
if [ ${SRC_DIR} = "." ] || [ ${SRC_DIR} = ${PWD} ]; then
  EXCLUDE_PATTERN="(/usr/lib|/System|@loader_path|@rpath)"
else
  EXCLUDE_PATTERN="(/usr/lib|/System)"
fi

DEPS=$(otool -LX "$@" | grep -v "$(otool -DX "$@")"  | egrep -v "${EXCLUDE_PATTERN}" | grep -o "/.*\.dylib" | sort -u)

if [ "${DEPS}" = "" ]; then exit 0; fi

for SRC in ${DEPS}; do
  LIB=$(basename ${SRC})
  DST=${PWD}/${LIB}
  if [ "$(dirname ${SRC})" = "/" ]; then
    # Hack for @rpath and @loader_path
    SRC=/opt/homebrew/lib/${LIB}
  fi

  diff -q ${SRC} ${DST} 2> /dev/null
  if [ $? -ne 0 ]; then
    echo "Copying ${SRC} to ${DST}"
    cp -f ${SRC} ${DST}
    chmod 755 ${DST}
    $0 ${SRC}
  fi
done
