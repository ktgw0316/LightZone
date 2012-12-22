This subversion directory (linux/jre) holds the custom-built JRE bundle that is
used by LightZone for Linux.  For the linux build to work, this JRE bundle must
be in one of the special Install4J directories:

either 

<install4j-install-dir>/jres

or

<home-dir>/.install4j3/jres

The bundle is grabbed by Install4J at build time and included in the LightZone
archive.

This bundle differs from the stock client-only JRE bundle distributed by EJ
Technologies in three ways.

First, inside lib/i386/jvm.cfg, it has one extra line:

-client KNOWN

immediately after the comments section.  This line prevents the JRE from
selecting a server VM under circumstances where it otherwise would, for
instance on 64-bit architectures.  This way, the bundle does not need to
include server VM libraries, which would take up space and perform worse with
LightZone.

Second, inside lib/i386/xawt/libmawt.so, Xinerama recognition has been
crippled by

sed -i 's/XINERAMA/FAKEEXTN/g' lib/i386/xawt/libmawt.so

This works around Sun's use of a non-thread safe, statically-linked copy of
libXinerama in their JRE:

http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6532373

Third, inside lib/rt.jar.pack, the sun.print.IPPPrintService class has been
patched and recompiled, to prevent an NPE at print time:

--- IPPPrintService.java-orig   2007-12-29 14:18:34.000000000 -0800
+++ IPPPrintService.java        2007-12-29 13:57:36.000000000 -0800
@@ -1144,7 +1144,10 @@
                                              DocFlavor flavor,
                                              AttributeSet attributes) {
        if (attr == null) {
-           throw new NullPointerException("null attribute");
+//         throw new NullPointerException("null attribute");
+// See Sun bug ID 6633656:
+// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6633656
+        return false;
        }
        if (flavor != null) {
            if (!isDocFlavorSupported(flavor)) {

Source code for the current JDK (1.6.0_03), including the source for
sun.print.IPPPrintService, is available from http://jdk6.dev.java.net.  It
can be compiled without compiling the rest of the JDK by doing like this:

    cd $JDK_BUILD_HOME/j2se/src/solaris/classes
    javac sun/print/IPPPrintDialog.java

Once the Java source has been patched and compiled, the procedure to
get the patch into the bundle is:

    cd $LIGHTZONE_HOME/linux/jre
    mkdir temp
    cd temp
    tar zxf ../linux-x86-1.6.0_03-lightzone.tar.gz
    cd lib
    unpack200 rt.jar.pack rt.jar
    mkdir temp
    cd temp
    jar xf ../rt.jar
    cp $JDK_BUILD_HOME/j2se/src/solaris/classes/sun/print/IPPPrintService.class sun/print
    jar uf ../rt.jar sun/print/IPPPrintService.class
    cd ..
    pack200 --no-gzip -J-Xmx512M rt.jar.pack rt.jar
    rm rt.jar
    rm -rf temp
    cd ..
    tar zcf ../linux-x86-1.6.0_03-lightzone.tar.gz *
    cd ..
    rm -rf temp

Note the Sun bug ID in the comment,

http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6633656
