#!/bin/sh
#
# Scan the "locale" tree of Java properties files, marshal them all into a
# single stream, and send the stream to standard output.  This is done in a
# way that can be reversed by locale-unflatten.
#
# Flattened properties files are just to provide readability for human
# translators.  They are not part of the LightZone build.

# These markers bracket the original properties file names:
separatorPre='# *** flattened from'
separatorPost='***'

# Properties files are discovered under the working directory:
files=`find . -name "*.properties" | grep -v "_" | sort`

# Stamp the flat form with its revision number:
echo -n "# "
git log -1 | grep '^commit '

for file in $files; do
    echo "$separatorPre $file $separatorPost"
    cat $file
done
