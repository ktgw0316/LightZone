#!/bin/sh
#
# Find the amount of physical memory, divide that number by two, and use the
# result to append a Java heap limit option to the install4j environment
# variable that determines VM options.
#
# If the physical memory exceeds 4GB, then cap the Java heap limit at its
# intrinsic limit of 2GB.  (GB figures are rounded down, to accomodate slop
# in how linux counts memory and how Java fails near its limit.)

PLATFORM=`uname`
if [ "${PLATFORM}" = "Linux" ]; then
  totalmem=`cat /proc/meminfo | grep 'MemTotal: ' | sed -r 's/.* ([0-9]+) .*/\1/'`
elif [ "${PLATFORM}" = "SunOS" ]; then
  totalmem=`prtconf | grep 'Memory size:' | sed -r 's/.* ([0-9]+) .*/\1/'`
  totalmem=`expr $totalmem \* 1024`
elif [ "${PLATFORM}" = "FreeBSD" ]; then
  totalmem=`dmesg | grep 'real memory' | sed -r 's/.* ([0-9]+) .*/\1/'`
  totalmem=`expr $totalmem / 1024`
fi
echo $totalmem

fourGB=4000000
twoGB=2000000
if [ $totalmem -ge $fourGB ]; then
  maxmem=$twoGB
else
  maxmem=$(( $totalmem / 2 ))
fi
option=-Xmx${maxmem}k

INSTALL4J_ADD_VM_PARAMS="$INSTALL4J_ADD_VM_PARAMS $option"
