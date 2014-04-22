# Development guide

## Install required packages
Building the LightZone source requires (at least) following packages:
- __ant__
- __fakeroot__ for linux package creation
- __g++__
- __gcc__
- __git__
- __javahelp2__ for jhindexer
- __make__
- __openjdk-6-jdk__ or later
- __tidy__

### Debian and Ubuntu
_For Debian (>= squeeze, i386/amd64) and Ubuntu (>= 10.04 lucid). See also [Packaging on Debian or Ubuntu](#packaging_deb) below._

Install required packages:

    sudo apt-get install ant autoconf automake build-essential debhelper devscripts git javahelp2 libtool libx11-dev nasm default-jdk default-jre-headless tidy

_(Note: gcc, g++, libc6-dev and make shall be installed with the build-essential.)_

Before start the build, you have to set JAVA_HOME environment variable, e.g.

    export JAVA_HOME=/usr/lib/jvm/default-java

### OpenSUSE (>= 12.2)
Install required packages:

    sudo zypper install ant autoconf automake nasm gcc gcc-c++ libtool make tidy git javahelp2 libX11-devel

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

