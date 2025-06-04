# Development guide

This short document will show you how to set up development environment for building LightZone.

## Windows

The working directory _can't have any spaces_ in the path to it. For example, don't put it under
"My Documents". It is problem for some scripts and command line utilities that do not enclose
parameter values in quotes and treat it as multiple parameters.

There is a problem with some files checked out from Git that do not work with Windows-standard CR/LF
line endings. There's a .gitattributes file to specify that those files need to have LF line endings.

Download and install (or unpack) following:

- __MSYS2__
  1. Install MSYS2 and update packages as described in <https://msys2.github.io/>
  1. Install required packages.
  - `pacman -S diffutils git gradle make pactoys tar`
  1. Install target-specific toolchain.
  - `pacboy -S toolchain:m`
  then select (at least) __binutils__, __gcc__, and __gcc-libs__.
  1. Install required packages (__lcms2__ will also install __libtiff__ and __libjpeg-turbo__).
  - `pacboy -S lcms2:m libraw:m lensfun:m ntldd-git:m pkg-config:m`
- __Java Development Kit__ (JDK) version 21 LTS from [Bellsoft](https://bell-sw.com/pages/downloads/).
- __Microsoft Windows SDK__
    Pick the right version based on your Windows version. Information and download links are
    available at
    <http://en.wikipedia.org/wiki/Microsoft_Windows_SDK#Versions>
    Place at end of PATH environment variable.
- __HTML Help Workshop__

Optionally, install following:

- Java IDE - Eclipse, Netbeans or IntelliJ IDEA Community Edition

Few points for MSYS2 beginners

- It is POSIX evnironment for Windows (emulates a lot of stuff that is in Linux)
- It is case-sensitive
- Computer drives under it are available under `/` (root) directory, e.g. `/c/`
- Home directory is abbreviated with `~`

If you haven't changed anything, your default shell is Bash. Open `~/.bashrc` with an editor (nano
or vim) and enter following environmental variables. (Modify the paths to match your environment.):

```shell
export JAVA_HOME="/c/Program Files/Java/jdk21"
export MSSDK_HOME="/c/Program Files (x86)/Windows Kits/10/Lib/10.0.22000.0"
export MINGW_DIR="/c/msys64/mingw64"
export PATH=$PATH:${JAVA_HOME}/bin
```

If you close and open your shell again it will automatically set these variables.

Do NOT set `C_INCLUDE_PATH=/usr/include` for mingw compilers.

Checkout your project with Git. If you have problems with line endings in build (the `\r` stuff), do
following:

```shell
git rm --cached -r .
git reset --hard
```

To start the build:

```shell
gradle windows:jpackage -x test
```

The installer will be generated under `windows/build/jpackage/`.

If you want to build a 32-bit binary on 64-bit machine, specify `TARGET_ARCH` variable:

```shell
TARGET_ARCH=i386 gradle windows:jpackage -x test
```
