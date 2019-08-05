# Development guide

## Install required packages
Building the LightZone source requires (at least) following packages:
- __ant__ version 1.9.8 or later to support nativeheaderdir parameter
- __fakeroot__ for linux package creation
- __g++__
- __gcc__ version 4.4 or later
- __git__
- __javahelp2__ for jhindexer
- __liblcms2-dev__
- __libjpeg-dev__ or __libjpeg-turbo-dev__
- __libtiff__
- __libxml2-utils__ for xmllint
- __make__
- __openjdk-8-jdk__ or later
- __pkg-config__
- __rsync__

### Debian and Ubuntu
_For Debian (>= squeeze, i386/amd64) and Ubuntu (>= 10.04 lucid). See also [Packaging on Debian or Ubuntu](#packaging_deb) below._

Install required packages:

    sudo apt-get install debhelper devscripts build-essential
 ant autoconf git-core javahelp2 default-jdk default-jre-headless rsync liblcms2-dev libjpeg-turbo8-dev libtiff5-dev libx11-dev libxml2-utils pkg-config

_(Note: gcc, g++, libc6-dev and make shall be installed with the build-essential.)_

Before start the build, you have to set JAVA_HOME environment variable, e.g.

    export JAVA_HOME=/usr/lib/jvm/default-java

### OpenSUSE (>= 12.2)
Install required packages:

    sudo zypper install ant autoconf gcc gcc-c++ make
    git javahelp2 liblcms2-devel libjpeg8-devel libtiff-devel libxml2-utils rsync libX11-devel java-1_8_0-openjdk-devel pkg-config

Set your `JAVA_HOME` variable to point to installed JDK, e.g.

    export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0

## Build
To start the build:

    ant -f linux/build.xml

_Note: If the build failed with a message_

    "/usr/share/ant/bin/antRun": java.io.IOException: error=2, No such file or directory

_manually download Apache Ant, unpack it somewhere (e.g. to your home directory) and append it to the path:_

    export PATH=/home/yourusername/apache-ant-1.10.5/bin/:$PATH

## Test Run
To check if it works fine before installing:

    ant -f linux/build.xml run

## Create a package and install
### <a name="packaging_deb"/>.deb package (Debian or Ubuntu)
You need to place an original source tarball in parent directory.
For instance, if you want to build v4.1.9 in /tmp directory,

    cd /tmp
    mkdir lightzone
    curl -L https://github.com/Aries85/LightZone/tarball/master > lightzone_4.1.9.orig.tar.gz
    tar xf lightzone_4.1.9.orig.tar.gz -C lightzone --strip-components=1
    cd lightzone

(If you want to build a package including your modification, you need to create its source tarball by yourself and place the tarball in the parent directory of the source code.)

Then

    debuild -uc -us

will create lightzone-*.deb package in the parent directory,
To install the package:

    sudo dpkg -i ../lightzone_*.deb

### .rpm package (Fedora, OpenSUSE, CentOS etc.)
### Re-packaging rpm from a source rpm
If you already have a .src.rpm for other distro, you can create .rpm for your distro
from the .src.rpm by yourself.

First of all, you need to install rpm-build using package manager of your distro.

Then extract the containts of the .src.rpm, and copy its source archive to SOURCES
directory:
    rpm2cpio lightzone-*.src.rpm | cpio -idmv --no-absolute-filenames
    cp lightzone-*.tar.bz2 ~/rpmbuild/SOURCES/

Then build an .rpm package using .spec file:

    rpmbuild -b lightzone.spec

If package list for unsatisfied dependency is shown, install the packages via apt-get,
then execute the rpmbuild command again. Your .rpm package will be created in
`~/rpmbuild/RPMS/i386/` or `~/rpmbuild/RPMS/x86_64/`. Install it with

    rpm -ivh ~/rpmbuild/RPMS/x86_64/lightzone-*.rpm

## Build from upstream source and install
### ebuild (Gentoo Linux)
Install Lightzone in a local overlay. First you need _app-portage/layman_:

    USE=git emerge app-portage/layman

There is a howto for the local overlay on Gentoo Wiki:
[Overlay/Local overlay](https://wiki.gentoo.org/wiki/Overlay/Local_overlay)

In the local overlay use the portage groups:

    mkdir media-gfx/lightroom

Put _linux/lightzone_9999.ebuild_ in the local overlay.
If you need to build specific version, replace the _9999_ in the filename with the version number such as _4.1.9_.

Move into the new directory _media-gfx/lightroom_ and do:

    repoman manifest
    popd

Now you are ready to 

    emerge media-gfx/lightzone

In my case I had to keyword lightzone....

### PKGBUILD (Arch Linux)
There is a build script: _linux/PKGBUILD_

### Ports (FreeBSD etc.)
There are build scripts in _freebsd-ports/graphics/lightzone/_ directory.

## Miscellaneous
### If you prefer Oracle Java to OpenJDK
You can use _Oracle Java JRE version 8 or later_ instead of openjdk-8-jdk.

Easiest way to setup one of these on Ubuntu is, for example:

    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt-get install oracle-java8-installer

Set your JAVA_HOME variable to point to installed JDK, e.g.

    export JAVA_HOME=/usr/lib/jvm/java-8-oracle
