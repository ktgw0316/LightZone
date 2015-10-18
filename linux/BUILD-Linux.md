# Development guide

## Install required packages
Building the LightZone source requires (at least) following packages:
- __ant__
- __fakeroot__ for linux package creation
- __g++__
- __gcc__
- __git__
- __javahelp2__ for jhindexer
- __liblcms2-dev__
- __libjpeg-dev__ or __libjpeg-turbo-dev__
- __libtiff__
- __make__
- __openjdk-6-jdk__ or later
- __pkg-config__

### Debian and Ubuntu
_For Debian (>= squeeze, i386/amd64) and Ubuntu (>= 10.04 lucid). See also [Packaging on Debian or Ubuntu](#packaging_deb) below._

Install required packages:

    sudo apt-get install debhelper devscripts build-essential ant autoconf git-core javahelp2 default-jdk default-jre-headless libjpeg-turbo8-dev libtiff5-dev libx11-dev

_(Note: gcc, g++, libc6-dev and make shall be installed with the build-essential.)_

Before start the build, you have to set JAVA_HOME environment variable, e.g.

    export JAVA_HOME=/usr/lib/jvm/default-java

### OpenSUSE (>= 12.2)
Install required packages:

    sudo zypper install ant autoconf gcc gcc-c++ make git javahelp2 libjpeg8-devel libtiff-devel libX11-devel java-1_7_0-openjdk-devel

Set your JAVA_HOME variable to point to installed JDK, e.g.

    export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0

## Build
To start the build:

    ant -f linux/build.xml

_Note: If the build failed with a message_

    "/usr/share/ant/bin/antRun": java.io.IOException: error=2, No such file or directory

_manually download Apache Ant, unpack it somewhere (e.g. to your home directory) and append it to the path:_

    export PATH=/home/yourusername/apache-ant-1.8.4/bin/:$PATH

## Test Run
To check if it works fine before installing:

    ant -f linux/build.xml run

## Create a tarball and install
To create a tarball (.tar.gz) of LightZone, you need Install4J.
Download and install its debian package from
http://www.ej-technologies.com/download/install4j/files
, or:

    wget http://download-aws.ej-technologies.com/install4j/install4j_linux_5_1_4.deb
    sudo dpkg -i install4j_linux_5_1_4.deb

If you have already installed Install4J, this will create linux/LightZone.tar.gz:

    ant -f linux/build.xml archive

To install LightZone, just extract the archive on a directory where you want to place it.
For example:

    mv linux/LightZone.tar.gz ~
    cd
    tar zxf LightZone.tar.gz
    ./LightZone/LightZone &

## Miscellaneous
### If you prefer Oracle Java to OpenJDK
You can use __Oracle Java JRE version 6, 7, or 8__ instead of openjdk-7-jdk.

Easiest way to setup one of these on Ubuntu is, for example:

    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt-get install oracle-java7-installer

Set your JAVA_HOME variable to point to installed JDK, e.g.

    export JAVA_HOME=/usr/lib/jvm/java-7-oracle

### <a name="packaging_deb"/>Packaging on Debian or Ubuntu
    debuild -uc -us

will create lightzone-*.deb package in parent directory,
To install the package:

    sudo dpkg -i ../lightzone-*.deb

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
~/rpmbuild/RPMS/i386/ or ~/rpmbuild/RPMS/x86_64/. Install it with

    rpm -ivh ~/rpmbuild/RPMS/x86_64/lightzone-*.rpm

