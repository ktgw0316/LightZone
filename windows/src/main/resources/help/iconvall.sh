#!/bin/sh

HTML_CHARSET=$1
TARGET_HELP_DIR=$2

files=$(find ${TARGET_HELP_DIR} -name '*.html')

for f in ${files} TOC.hhc; do
  iconv -f UTF-8 -t ${HTML_CHARSET} $f > $f.tmp
  sed 's/\( charset=\)UTF-8/\1${HTML_CHARSET}/' $f.tmp > $f
  rm $f.tmp
done
