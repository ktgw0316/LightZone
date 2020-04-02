#!/bin/sh

DEPS=$(otool -L $@ | egrep -v "/(usr/lib|System)" | grep -o "/.*dylib" | sort -u)

if [ "${DEPS}" = "" ]; then exit 0; fi

for DEP in ${DEPS}; do
  LIB=$(basename ${DEP})
  cp -f ${DEP} ${LIB}
  chmod 755 ${DEP}
  install_name_tool -id ${LIB} ${LIB}
  $0 ${LIB}
done

