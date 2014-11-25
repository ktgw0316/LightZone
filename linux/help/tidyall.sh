#!/bin/sh

files=`ls $@/*.html`
echo tidyall $@

for file in $files; do
    tidy -m -i -q -ashtml -utf8 --show-warnings false $file
    if [ $? -eq 2 ]; then
        echo "tidy error: $file"
        exit 1
    fi
done
