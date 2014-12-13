#! /bin/sh
echo jhindexer $@
cd $@
java -cp "/usr/share/java/javahelp/*:/usr/share/java/*:/usr/share/javahelp/lib/*:/usr/local/share/java/classes/*:/usr/jdk/packages/lib/ext/*" com.sun.java.help.search.Indexer ./*.html
