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
  1. Install common packages:

    ```shell
    pacman -S diffutils git make tar pacboy
    ```

  1. Install target-specific packages:

    ```shell
    pacboy -S lcms2 libraw lensfun ntldd-git pkgconf toolchain
    ```

    For toolchain, select (at least) `binutils`, `gcc`, and `gcc-libs`.

- __Java Development Kit__ (JDK) version 21 from [Bellsoft](https://bell-sw.com/pages/downloads/).

- __Microsoft Windows SDK__
    Pick the right version based on your Windows version. Information and download links are
    available at
    <http://en.wikipedia.org/wiki/Microsoft_Windows_SDK#Versions>
    Place at end of `PATH` environment variable.

- __HTML Help Workshop__

Few points for MSYS2 beginners

- It is POSIX evnironment for Windows (emulates a lot of stuff that is in Linux)
- It is case-sensitive
- Computer drives under it are available under `/` (root) directory, e.g. `/c/`
- Home directory is abbreviated with `~`

If you haven't changed anything, your default shell is Bash. Open `~/.bashrc` with an editor (nano
or vim) and enter following environmental variables. (Modify the paths to match your environment.):

```shell
export JAVA_HOME="/c/Program Files/BellSoft/LibericaJDK-21"
export MSSDK_HOME="/c/Program Files (x86)/Windows Kits/10/Lib/10.0.22621.0"
export PATH=$PATH:/msys64/usr/bin/:/msys32/usr/bin/:${JAVA_HOME}/bin/
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
./gradlew build
```

To create the installer:

```shell
./gradlew jpackage
```
