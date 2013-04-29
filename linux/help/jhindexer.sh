#! /bin/sh

PLATFORM=`uname`

if [ "${PLATFORM}" = "Linux" ] ; then
  java -cp "/usr/share/java/*" com.sun.java.help.search.Indexer "$@"
elif [ "${PLATFORM}" = "FreeBSD" ] ; then
  java -cp "/usr/local/share/java/classes/*" com.sun.java.help.search.Indexer "$@"
fi
