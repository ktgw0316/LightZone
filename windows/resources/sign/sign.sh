#! /usr/bin/bash

##
# Set just enough PATH so that make, pvk2pfx, and signtool are found.
##
PATH=\
/usr/bin:\
/usr/local/bin:\
/cygdrive/c/Program\ Files/Microsoft\ SDKs/Windows/v6.0/Bin

make EXE_TO_SIGN="$1"
