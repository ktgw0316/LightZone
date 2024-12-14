#!/bin/sh

EXCLUDE_PATTERN="(/usr/lib|/System|@loader_path|@rpath)"
DEPS=$(otool -LX $@ | grep -v "$(otool -DX $@)"  | egrep -v "${EXCLUDE_PATTERN}" | grep -o "/.*\.dylib" | sort -u)

if [ "${DEPS}" = "" ]; then exit 0; fi

for DEP in ${DEPS}; do
  LIB=$(basename ${DEP})
  diff -q ${DEP} ${LIB} 2> /dev/null
  if [ $? -ne 0 ]; then
    cp -f ${DEP} ${LIB}
    chmod 755 ${LIB}
    $0 ${LIB}
  fi
done
