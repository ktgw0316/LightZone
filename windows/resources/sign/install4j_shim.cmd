@ECHO OFF

REM Start bash to get a sane (Unix) environment where it's easier to do things.

CHDIR resources\sign
C:\cygwin\bin\bash -c './sign.sh "%1"'
