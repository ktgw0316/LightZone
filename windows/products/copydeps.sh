#!/bin/sh

DEPS=$({ ldd $@; ntldd $@; } | sort | sed -n 's/.* \([^ ]*mingw.*\.dll\) .*/"\1"/p' | xargs -r cygpath | uniq)

if [ "${DEPS}" = "" ]; then exit 0; fi

echo Copying ${DEPS}
cp -u -t . ${DEPS}
for DEP in ${DEPS}; do
  $0 $(basename ${DEP})
done
