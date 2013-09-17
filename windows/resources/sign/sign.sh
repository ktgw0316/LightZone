#! /usr/bin/bash

##
# Set just enough PATH so that make, pvk2pfx, and signtool are found.
##
#PATH=\
#/usr/bin:\
#/usr/local/bin:\
#"${MSSDK_HOME}/bin"

make EXE_TO_SIGN="$1"
