# Development guide

## Install required packages

Building the LightZone source requires (at least) following packages:

- __fakeroot__ for linux package creation
- __g++__
- __gcc__ version 4.4 or later
- __git__
- __libglib2.0-dev__
- __liblcms2-dev__
- __liblensfun-dev__ version 0.3.2
- __libjpeg-dev__ or __libjpeg-turbo-dev__
- __libraw-dev__
- __libtiff__
- __libxml2-utils__ for xmllint
- __make__
- __openjdk-17-jdk__ or later
- __pkg-config__
- __rsync__

### Debian (>= 11) and Ubuntu (>= 20.04)

_See also [Packaging on Debian or Ubuntu](#.deb-package-(debian-or-ubuntu)) below._

Install required packages:

```shell
sudo apt-get install debhelper devscripts build-essential git-core default-jdk default-jre-headless libglib2.0-dev libjiconfont-google-material-design-icons-java liblcms2-dev liblensfun-dev libjpeg-dev libraw-dev libtiff-dev libx11-dev libxml2-utils pkg-config rsync
```

_Note:_ gcc, g++, libc6-dev and make shall be installed with the build-essential.

Before start the build, you have to set `JAVA_HOME` environment variable, e.g.

```shell
export JAVA_HOME=/usr/lib/jvm/default-java
```

### Fedora (>= 42)

Install required packages:

```shell
sudo dnf install java-21-openjdk-devel java-21-openjdk-jmods git gcc-c++ lcms2-devel lensfun-devel LibRaw-devel libjpeg-turbo-devel libtiff-devel libX11-devel libxml2-devel make pkgconfig rsync
```

Set your `JAVA_HOME` variable to point to installed JDK, e.g.

```shell
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

### OpenSUSE (>= 15.2)

Install required packages:

```shell
sudo zypper install gcc gcc-c++ make
git liblcms2-devel lensfun-devel libjpeg8-devel libraw-devel libtiff-devel libxml2-utils rsync libX11-devel java-21-openjdk-devel pkg-config
```

Set your `JAVA_HOME` variable to point to installed JDK, e.g.

```shell
export JAVA_HOME=/usr/lib64/jvm/java
```

## Build

To start the build:

```shell
./gradlew build
```

## Test Run

To check if it works fine before installing:

```shell
./gradlew run
```

## Create a package and install

### .deb package (Debian or Ubuntu)

```shell
./gradlew jpackage
```

will create `lightzone*.deb` package in `linux/build/jpackage/` directory,
To install the package:

```shell
sudo dpkg -i linux/build/jpackage/lightzone*.deb
```

The `lightzone` command will be installed under `/opt/lightzone/bin/`.

### .rpm package (Fedora, OpenSUSE, CentOS etc.)

Modify `linux/build.gradle` file to set `rpm` instead of `deb` as the target package type, then:

```shell
./gradlew jpackage
```

will create `lightzone*.rpm` package in `linux/build/jpackage/` directory,
To install the package:

```shell
sudo rpm -i linux/build/jpackage/lightzone*.rpm
```

The `lightzone` command will be installed under `/opt/lightzone/bin/`.

### Re-packaging rpm from a source rpm

If you already have a .src.rpm for other distro, you can create .rpm for your distro
from the .src.rpm by yourself.

First of all, you need to install rpm-build using package manager of your distro.

Then extract the contents of the .src.rpm, and copy its source archive to SOURCES
directory:

```shell
rpm2cpio lightzone-*.src.rpm | cpio -idmv --no-absolute-filenames
cp lightzone-*.tar.xz ~/rpmbuild/SOURCES/
```

Then build an .rpm package using .spec file:

```shell
rpmbuild -b lightzone.spec
```

If package list for unsatisfied dependency is shown, install the packages via apt-get,
then execute the rpmbuild command again. Your .rpm package will be created in
`~/rpmbuild/RPMS/i386/` or `~/rpmbuild/RPMS/x86_64/`. Install it with

```shell
rpm -ivh ~/rpmbuild/RPMS/x86_64/lightzone-*.rpm
```

## Build from upstream source and install

### ebuild (Gentoo Linux)

Install LightZone in a local overlay. First you need `app-portage/layman`:

```shell
USE=git emerge app-portage/layman
```

There is a howto for the local overlay on Gentoo Wiki:
[Overlay/Local overlay](https://wiki.gentoo.org/wiki/Overlay/Local_overlay)

In the local overlay use the portage groups:

```shell
mkdir -p media-gfx/lightzone
```

Put `linux/lightzone_9999.ebuild` in the local overlay.
If you need to build specific version, replace the `9999` in the filename with the version number such as `4.2.4`.

Move into the new directory `media-gfx/lightzone` and do:

```shell
repoman manifest
popd
```

Now you are ready to

```shell
emerge media-gfx/lightzone
````

In my case I had to keyword lightzone....

### SlackBuild (Slackware)

There is a SlackBuild script to build a Slackware package.
You can use it like following:

```shell
sbopkg -i zulu-openjdk21
su -c linux/lightzone.SlackBuild
su -c "installpkg /tmp/lightzone-4.2.4-1_SBo.tgz"
```

### Ports (FreeBSD etc.)

There are build scripts in `freebsd-ports/graphics/lightzone/` directory.
