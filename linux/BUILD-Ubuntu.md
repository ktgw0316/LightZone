# Development guide

## Ubuntu 12.04.1 LTS (Precise Pangolin)
### Install required packages
Building the LightZone source requires (at least) following packages:
- __openjdk-7-jdk__
- __ant__
- __gcc__
- __g++__
- __make__
- __libx11-dev__ for X11/xlib.h
- __tidy__
- __javahelp2__ for jhindexer
- __git__

To install these packages:

    sudo apt-get install openjdk-7-jdk ant gcc g++ make libx11-dev tidy javahelp2 git

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

### Build
To start the build:

    ant -f linux/build.xml

### Test Run
To check if it works fine before installing:

    ant -f linux/build.xml run

### Create a tarball and install
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

