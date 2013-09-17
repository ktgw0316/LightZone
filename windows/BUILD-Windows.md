# Development guide

This short document will show you how to set up development environment for building LightZone.

## Windows
The working directory _can't have any spaces_ in the path to it. For example, don't put it under "My Documents". It is
problem for some scripts and command line utilities that do not enclose parameter values in quotes and treat it as
multiple parameters.

There is a problem with some files checked out from Git that do not work with Windows-standard CR/LF line endings.
There's a .gitattributes file to specify that those files need to have LF line endings.

Download and install (or unpack) following:
-   __Apache Ant__
-   __Cygwin__
    In Cygwin install following:
    -    __automake__
    -    __binutils__
    -    __git__
    -    __libtool__
    -    __make__
    -    __mingw64-i686-gcc-core__ (32-bit) or __mingw64-x86_64-gcc-core__ (64-bit)
    -    __mingw64-i686-gcc-g++__ (32-bit) or __mingw64-x86_64-gcc-g++__ (64-bit)
    -    __mingw64-i686-xz__ (32-bit) or __mingw64-x86_64-xz__ (64-bit)
    -    __nasm__
    -    __vim__ or __nano__ — These are editors for console, might come handy. If you are not familiar with vim, install
         nano instead, it is more user friendly.
-   __Oracle JDK SE 7__ (Java Development Kit)
-   __Microsoft Windows SDK__
    Pick the right version based on your Windows version. Information and download links are available at
    http://en.wikipedia.org/wiki/Microsoft_Windows_SDK#Versions
    Place at end of PATH
-   __HTML Help Workshop__
-   __Install4J__ (free trial for 90 days, then we'll need to try to get open-source licenses)
    After installation, run Install4J, click on Project -> Download JREs, and go through wizard dialog.

Optionally, install following:
-   __PortableGit__
    In case your IDE does depends on external Git client, install this and do not put it to the PATH. It would conflict
    with Git from Cygwin.
-   Java IDE - Eclipse, Netbeans or IntelliJ IDEA Community Edition

Few points for Cygwin beginners
- it is POSIX evnironment for Windows (emulates a lot of stuff that is in Linux)
- is is case sensitive
- computer drives under it are available under /cygdrive directory, e.g /cygdrive/c/
- home directory is abbreviated with ~

If you haven't changed anything, your default shell is Bash. Open `~/.bashrc` with an editor (nano or vim) and enter
following environmental variables. (Modify the paths to match your environment.):

    export JAVA_HOME="/cygdrive/c/Program Files/Java/jdk1.7.0_25";
    export ANT_HOME="/cygdrive/c/Program Files/apache-ant-1.9.1"
    export MSSDK_HOME="/cygdrive/c/Program Files (x86)/Windows Kits/8.0";
    export INSTALL4J_HOME="/cygdrive/c/Program Files/install4j5";
    export PATH=$PATH:${JAVA_HOME}/bin:${ANT_HOME}/bin:${MSSDK_HOME}/bin/x86:${INSTALL4J_HOME}/bin;

If you close and open your shell again it will automatically set these variables.

Do NOT set C_INCLUDE_PATH=/usr/include for mingw compilers.

Before starting your first build, you have to copy HtmlHelp.lib to mingw library path:

    cp ${MSSDK_HOME}/Lib/win8/um/x86/Htmlhelp.Lib /usr/i686-w64-mingw32/sys-root/mingw/lib/libhtmlhelp.a
    cp ${MSSDK_HOME}/Lib/win8/um/x64/Htmlhelp.Lib /usr/x86_64-w64-mingw32/sys-root/mingw/lib/libhtmlhelp.a

Checkout your project with Git. If you have problems with line endings in build (the \r stuff), do following :

    git rm --cached -r .
    git reset --hard

To start the build:

    cd windows
    ant build-installer

Updates already made to the code, checked in on Windows-build branch in github:
-   .gitignore file added to ignore the built files
-   .gitattributes file added to force LF line endings on some text files
-   recurse.mk changed to work with cygwin make. I think this change will need to be backed out once we figure out a
    version of make that works.
-   Build process changed to use Git instead of svn to get version-type information
-   Java code changed to use Git instead of svn to get version-type information. This needs to be evaluated because Git
    uses bashes to identify current build. Version displays part of the hash now.
-   Removed need for MSVS_HOME variable. It was not used for anything.
-   For Install4J updated JRE version

### Known build issues
-   In LightZone there are now no version information. There was a problem with rc.exe from MSSDK failing, windres can
    be used to compile it. There is a bug in windres that makes it fail on compiling version info. A patch has been
    submitted to binutils already, so in next version (higher than 2.22.51-2) it should work again. Here also versioning
    with Git needs to be considered.
-   bundled JRE version in Install4J is updated manually now
