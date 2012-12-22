#!/bin/bash

files=`ls *.html`

for file in $files; do
    echo tidy $file
    tidy -m -i -q -ashtml --show-warnings false $file
    if [ $? -eq 2 ]; then
        echo "tidy error"
        exit 1
    fi
done
