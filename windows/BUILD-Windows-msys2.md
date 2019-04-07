# Development guide

This short document will show you how to set up development environment for building LightZone.

## Windows
The working directory _can't have any spaces_ in the path to it. For example, don't put it under
"My Documents". It is problem for some scripts and command line utilities that do not enclose
parameter values in quotes and treat it as multiple parameters.

There is a problem with some files checked out from Git that do not work with Windows-standard CR/LF
line endings. There's a .gitattributes file to specify that those files need to have LF line endings.

Download and install (or unpack) following:
-   __Apache Ant__ version 1.9.8 or later to support nativeheaderdir parameter
-   __MSYS2__
    1. Install MSYS2 and update packages as described in https://msys2.github.io/
    1. Install required packages.
    -    `pacman -S autoconf git make pkg-config tar`
    1. Install target-specific toolchain.
    -    For 32-bit: `pacman -S mingw-w64-i686-toolchain`
    -    For 64-bit: `pacman -S mingw-w64-x86_64-toolchain`
    then select (at least) __binutils__, __gcc__, __gcc-libs__, and __pkg_config__.
    1. Install __lcms2__. This will also install __libtiff__ and __libjpeg-turbo__.
    -    For 32-bit: `pacman -S mingw-w64-i686-lcms2`
    -    For 64-bit: `pacman -S mingw-w64-x86_64-lcms2`
    1. Install __ntldd__.
    -    For 32-bit: `pacman -S mingw-w64-i686-ntldd-git`
    -    For 64-bit: `pacman -S mingw-w64-x86_64-ntldd-git`
-   __Oracle JDK SE 8__ (Java Development Kit)
-   __Microsoft Windows SDK__
    Pick the right version based on your Windows version. Information and download links are
    available at
    http://en.wikipedia.org/wiki/Microsoft_Windows_SDK#Versions
    Place at end of PATH environment variable.
-   __HTML Help Workshop__
-   __Install4J__ (free trial for 90 days, then we'll need to try to get open-source licenses)
    After installation, run Install4J, click on Project -> Download JREs, and go through wizard
    dialog.

Optionally, install following:
-   Java IDE - Eclipse, Netbeans or IntelliJ IDEA Community Edition

Few points for MSYS2 beginners
- It is POSIX evnironment for Windows (emulates a lot of stuff that is in Linux)
- It is case sensitive
- Computer drives under it are available under / (root) directory, e.g. /c/
- Home directory is abbreviated with ~

If you haven't changed anything, your default shell is Bash. Open `~/.bashrc` with an editor (nano
or vim) and enter following environmental variables. (Modify the paths to match your environment.):

    export JAVA_HOME="/c/Program Files/Java/jdk1.8.0_181";
    export ANT_HOME="/c/Program Files/apache-ant-1.9.8";
    export MSSDK_HOME="/c/Program Files (x86)/Windows Kits/8.0";
    export INSTALL4J_HOME="/c/Program Files/install4j5";
    export PATH=$PATH:${JAVA_HOME}/bin:${ANT_HOME}/bin:${MSSDK_HOME}/bin/x86:${INSTALL4J_HOME}/bin;

If you close and open your shell again it will automatically set these variables.

Do NOT set C_INCLUDE_PATH=/usr/include for mingw compilers.

Before starting your first build, you have to copy HtmlHelp.lib to mingw library path:

    cp "${MSSDK_HOME}/Lib/10.0.14393.0/um/x86/Htmlhelp.Lib" /mingw32/lib/libhtmlhelp.a
    cp "${MSSDK_HOME}/Lib/10.0.14393.0/um/x64/Htmlhelp.Lib" /mingw64/lib/libhtmlhelp.a

Checkout your project with Git. If you have problems with line endings in build (the \r stuff), do
following:

    git rm --cached -r .
    git reset --hard

To start the build:

    cd windows
    ant build-installer

If you want to build a 32-bit binary on 64-bit machine, specify TARGET variable:

    TARGET=i386 ant build-installer

### Known build issues
-   In LightZone there are now no version information. There was a problem with rc.exe from MSSDK
failing, windres can be used to compile it. There is a bug in windres that makes it fail on
compiling version info. A patch has been submitted to binutils already, so in next version (higher
than 2.22.51-2) it should work again. Here also versioning with Git needs to be considered.
-   bundled JRE version in Install4J is updated manually now
