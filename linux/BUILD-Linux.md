# Development guide

## Install required packages
Building the LightZone source requires (at least) following packages:
- __ant__
- __autoconf__ 2.56 or later
- __automake__ 1.7 or later
- __fakeroot__ for linux package creation
- __g++__
- __gcc__
- __git__
- __javahelp2__ for jhindexer
- __libtool__ 1.4 or later
- __libx11-dev__ for X11/xlib.h
- __make__
- __nasm__ 2.07 or later
- __openjdk-7-jdk__
- __tidy__

### Ubuntu 12.04.1 LTS (Precise Pangolin)
Install required packages:

    sudo apt-get install ant autoconf automake devscripts g++ gcc git javahelp2 libtool libx11-dev make nasm openjdk-7-jdk tidy

Before start the build, you have to set JAVA_HOME environment variable:

    export JAVA_HOME=/usr/lib/jvm/java-7-openjdk

#### If you prefer Oracle Java to OpenJDK
You can use __Oracle Java JRE version 6, 7, or 8__ instead of openjdk-7-jdk.
Easiest way to setup one of these is, for example:

    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt-get install oracle-java7-installer

Set your JAVA_HOME variable to point to installed JDK, e.g.

    export JAVA_HOME=/usr/lib/jvm/java-7-oracle

### OpenSUSE 12.2
Install required packages:
    sudo zypper install gcc gcc-c++ make libX11-devel tidy javahelp2 git

Manually download Apache Ant, unpack it somewhere (e.g. to your home directory) and append it to the path:
    export PATH=/home/yourusername/apache-ant-1.8.4/bin/:$PATH

Set your JAVA_HOME variable to point to installed JDK, e.g.
    export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0

## Build
To start the build:

    ant -f linux/build.xml

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

