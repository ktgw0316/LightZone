#! /bin/bash

COMPANY="Light Crafts, Inc."

########## You shouldn't have to change anything below this line. #############

COMMON_DIR=../../../lightcrafts

VERSION=$(< $COMMON_DIR/version.txt)
MAJOR=$(echo $VERSION | cut -f1 -d.)
MINOR=$(echo $VERSION | cut -f2 -d.)
#PATCH=$(echo $VERSION | cut -f3 -d.); [ -z "$PATCH" ] && PATCH=0
# FIXME - this is a quick fix
PATCH=0
#REVISION=$($COMMON_DIR/tools/bin/lc-svn-revision)
# FIXME - this is a quick fix
REVISION=0
YEAR="2005-$(date "+%Y")"

sed -e "s/@APP_NAME@/${APP_NAME:-LightZone}/g" \
    -e "s/@COMPANY@/$COMPANY/g" \
    -e "s/@MAJOR@/$MAJOR/g" \
    -e "s/@MINOR@/$MINOR/g" \
    -e "s/@PATCH@/$PATCH/g" \
    -e "s/@REVISION@/$REVISION/g" \
    -e "s/@VERSION@/$VERSION/g" \
    -e "s/@YEAR@/$YEAR/g" \
    $1 > $2

# vim:set et sw=4 ts=4:
