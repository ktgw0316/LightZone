#!/bin/sh

DEPS=$(ntldd $@ | grep -o " => .*mingw.*\.dll" | sort | uniq | sed 's/ => \(.*\)/"\1"/g' | xargs -r cygpath)

if [ "${DEPS}" = "" ]; then exit 0; fi

cp -u -t . ${DEPS}
for DEP in ${DEPS}; do
  $0 $(basename ${DEP})
done
