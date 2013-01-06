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
    -    __gcc__ version 3 (not 4)
    -    __make__
    -    __vim__ or __nano__ — These are editors for console, might come handy. If you are not familiar with vim, install
         nano instead, it is more user friendly.
    -    __git__
    You cannot use MinGW. Also do not mix binaries or header files from Cygwin and MinGW. It is possible to do that,
    but it is another complication.
-   __Oracle JDK SE 6__ (Java Development Kit)
    Do not use version 7 for now, it does not work.
-   __PortableGit__
    In case your IDE does depends on external Git client, install this and do not put it to the PATH. It would conflict
    with Git from Cygwin.
-   __Microsoft .NET__
    This one is probably not necessary. The version depends on version of Windows SDK (next point).
-   __Microsoft Windows SDK__
    Pick the right version based on your Windows version. Information and download links are available at
    http://en.wikipedia.org/wiki/Microsoft_Windows_SDK#Versions
    Place at end of PATH
-   __HTML Help Workshop__
    On 64-bit system it must be copied to _Program Files_, or lang.mk line 18 modified.
    This issue will be addressed later.
-   __Install4J__ (free trial for 90 days, then we'll need to try to get open-source licenses)
    After installation, run Install4J, click on Project -> Download JREs, and go through wizard dialog.
-   Java IDE - Eclipse, Netbeans or IntelliJ IDEA Community Edition

Few points for Cygwin beginners
- it is POSIX evnironment for Windows (emulates a lot of stuff that is in Linux)
- is is case sensitive
- computer drives under it are available under /cygdrive directory, e.g /cygdrive/c/
- home directory is abbreviated with ~


If you haven't changed anything, your default shell is Bash. Open `~/.bashrc` with an editor (nano or vim) and enter
following (replace things in square brackets with Cygwin-style paths):

    export PATH=[ant]/bin:$PATH:[jdk]/bin:[git]/bin:[WinSDK]/bin:[Install4J]/bin
    export JAVA_HOME=[jdk]
    export MSSDK_HOME=[WinSDK]
    export C_INCLUDE_PATH=/usr/include

If you close and open your shell again it will automatically set these variables.

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
-   LightZone.exe signing has been disabled for now as it is not critical to build functionality.
    It can be resolved later.
-   HTML Help on 64-bit Windows must be manually copied to C:/Program Files
-   bundled JRE version in Install4J is updated manually now