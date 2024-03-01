#!/bin/sh

DEPS=$(otool -L $@ | egrep -v "/(usr/lib|System)" | grep -o "/.*\.dylib" | sort -u)

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
