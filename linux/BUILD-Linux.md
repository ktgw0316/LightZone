# Development guide

## Install required packages

Building the LightZone source requires (at least) following packages:

- __ant__ version 1.9.8 or later to support nativeheaderdir parameter
- __fakeroot__ for linux package creation
- __g++__
- __gcc__ version 4.4 or later
- __git__
- __javahelp2__ for jhindexer
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
sudo apt-get install debhelper devscripts build-essential ant git-core javahelp2 default-jdk default-jre-headless libglib2.0-dev libjiconfont-google-material-design-icons-java liblcms2-dev liblensfun-dev libjpeg-dev libraw-dev libtiff-dev libx11-dev libxml2-utils pkg-config rsync
```

_Note:_ gcc, g++, libc6-dev and make shall be installed with the build-essential.

Before start the build, you have to set `JAVA_HOME` environment variable, e.g.

```shell
export JAVA_HOME=/usr/lib/jvm/default-java
```

### OpenSUSE (>= 15.2)

Install required packages:

```shell
sudo zypper install ant gcc gcc-c++ make
git javahelp2 liblcms2-devel lensfun-devel libjpeg8-devel libraw-devel libtiff-devel libxml2-utils rsync libX11-devel java-11-openjdk-devel pkg-config
```

Set your `JAVA_HOME` variable to point to installed JDK, e.g.

```shell
export JAVA_HOME=/usr/lib64/jvm/java
```

## Build

To start the build:

```shell
ant -f linux/build.xml
```

_Note:_ If the build failed with a message:

```
"/usr/share/ant/bin/antRun": java.io.IOException: error=2, No such file or directory
```

manually download Apache Ant, unpack it somewhere (e.g. to your home directory) and append it to the path:

```shell
export PATH=/home/yourusername/apache-ant-1.10.5/bin/:$PATH
```

### Offline build

If you want to build LightZone without internet connection, you need to download
the dependency files into the directory `lightcrafts/lib`, then:

```shell
ant -f linux/build.xml -Dno-ivy=true -Dno-submodule=true
```

Alternatively, if you cannot use `ivy` but you can use git submodule:

```shell
ant -f linux/build.xml -Dno-ivy=true -Dno-submodule=false
```

## Test Run

To check if it works fine before installing:

```shell
ant -f linux/build.xml run
```

## Create a package and install

### .deb package (Debian or Ubuntu)

You need to place an original source tarball in parent directory.
For instance, if you want to build v4.2.4 in /tmp directory,

```shell
cd /tmp
curl -L https://github.com/ktgw0316/LightZone/tarball/4.2.4 > lightzone_4.2.4.orig.tar.gz
tar xf lightzone_4.2.4.orig.tar.gz -C lightzone --strip-components=1
mkdir lightzone
cd lightzone
```

(If you want to build a package including your modification, you need to create its source tarball by yourself and place the tarball in the parent directory of the source code.)

If you are on Debian >= 12 or Ubuntu >= 22.10, you need to do:

```shell
cp debian/lightzone-Debian_12.dsc debian/lightzone.dsc
cp debian/patches/series-Debian_12 debian/patches/series
```

otherwise:

```shell
cp debian/lightzone-Debian_11.dsc debian/lightzone.dsc
```

Then

```shell
debuild -uc -us
```

will create lightzone-*.deb package in the parent directory,
To install the package:

```shell
sudo dpkg -i ../lightzone_*.deb
```

### .rpm package (Fedora, OpenSUSE, CentOS etc.)

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
mkdir media-gfx/lightroom
```

Put `linux/lightzone_9999.ebuild` in the local overlay.
If you need to build specific version, replace the `9999` in the filename with the version number such as `4.2.4`.

Move into the new directory `media-gfx/lightroom` and do:

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

There is a SlackBuild script with a patch to build a Slackware package
on [Gist](https://gist.github.com/ktgw0316/1d178a800377b247a531a4b4e59bfac9).
You can use it like following:

```shell
sbopkg -i zulu-openjdk17
sbopkg -i apache-ant
sbopkg -i apache-ivy
wget https://github.com/ktgw0316/LightZone/archive/refs/tags/4.2.4.tar.gz
wget https://gist.github.com/ktgw0316/1d178a800377b247a531a4b4e59bfac9/ivy.patch
wget https://gist.github.com/ktgw0316/1d178a800377b247a531a4b4e59bfac9/lightzone.SlackBuild
chmod +x ./lightzone.SlackBuild
su -c ./lightzone.SlackBuild
su -c "installpkg /tmp/lightzone-4.2.4-1_SBo.tgz"
```

### Ports (FreeBSD etc.)

There are build scripts in `freebsd-ports/graphics/lightzone/` directory.
