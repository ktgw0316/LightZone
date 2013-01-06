# Development guide

## Ubuntu 12.04.1 LTS (Precise Pangolin)
### Install required packages
Building the LightZone source requires (at least) following packages:
- __ant__
- __gcc__
- __make__
- __libx11-dev__ for X11/xlib.h
- __tidy__
- __javahelp2__ for jhindexer
- __git__
- __subversion__

To install these packages:

    sudo aptitude install ant gcc make libx11-dev tidy javahelp2 git subversion 

__Oracle Java 6 JRE__ is also required. (LightZone cannot build with Oracle Java 7 nor any version of openjdk.)
Easiest way to install this is:

    sudo add-apt-repository ppa:webupd8team/java
    sudo aptitude update
    sudo aptitude install oracle-java6-installer

### Build
Before start the build, you have to set JAVA_HOME environment variable:

    export JAVA_HOME=/usr/lib/jvm/java-6-oracle

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
    cd ~
    tar zxf LightZone.tar.gz
    ./LightZone/LightZone &

