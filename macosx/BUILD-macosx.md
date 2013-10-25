# Development guide

Written and tested on MacOSX 10.5.8 with Java 1.6 64-bit and Eclipse 3.6.

LightZone can be built with Ant, and edited with any java IDE; these instructions use Eclipse for convenience,
since it's a common IDE with Ant built in.

These instructions sometimes say to right-click on something; use Control+Click instead if necessary.

## General Info and Status
At this stage of development, the LightZone OSX port builds a 64-bit LightZone and runs it from Ant or from inside Eclipse.
It doesn't yet package the binary launcher or any OSX-specific extensions; that work is still to be done.

The application's startup java class is com.lightcrafts.app.Application.
There's also an OSX-specific startup class that isn't built yet.  Once ready, it will read its info
from LightZone/macosx/resources/Info.plist and launch the MainClass defined there.

## Install required softare
Building the LightZone source requires the following software:

- __Java 1.6__ from Apple or Oracle
- __gcc__ (which includes __g++__)
- __git__

If you need to install gcc and git, the easiest route is to download XCode's command-line tools; the link depends on your OS X version.

http://stackoverflow.com/questions/9353444/how-to-use-install-gcc-on-mac-os-x-10-8-xcode-4-4

http://stackoverflow.com/questions/4360110/installing-gcc-to-mac-os-x-leopard-without-installing-xcode

http://stackoverflow.com/questions/10904774/install-git-separately-from-xcode

## Pre-work
Open the Java Preferences app. (You can use Spotlight to search for it)

On the General tab, note the topmost Java version and type (64- or 32-bit).
Because the project builds its libraries and JARs using varied commands,
make sure you use this version consistently throughout the project settings.

The build flags in platform.mk currently require 64-bit Java, but originally required 32-bit;
it can run on 32-bit by reverting that platform.mk change.

## Build instructions for LightZone with Ant (if you're not using Eclipse)
If you're using Eclipse, skip this section.

- Make sure gcc, git, and ant are installed.

- Make sure your default java version is set in the Java Preferences app.

- Set the JAVA_HOME environment variable to /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home  (or 1.7/Home if that's your version)

- cd to LightZone/lightcrafts in the source folder.

- To build LightZone, run these commands; each run's output should end with "`BUILD SUCCESSFUL`" when you run it.
If you have errors, see the "Troubleshooting" section of this document.

		ant clean
		ant build
		ant jar

- You should now be able to run LightZone with:

		ant run

## Setup instructions for LightZone as an Eclipse project
(This is written for Eclipse 3.6, and should be applicable to other versions with minor changes.)

If you're already using Eclipse for other development, you may want to make a new eclipse workspace for LightZone only.

# Eclipse initial setup

Eclipse prefs -> java -> installed JREs -> whatever you selected in Java Preferences (1.6 for me)

If you can't find it in the list, click Add -> MacOS X JVM and browse to
/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home  (or 1.7/Home)

Eclipse prefs -> java -> compiler -> compliance level: 1.6

- Choose File -> New -> Project... -> Java Project from Existing Ant Buildfile.
- Browse to lightcrafts/build.xml
- Check [X] Link to the buildfile in the file system
- Select the "javac" task in target "javac"
- Click "Finish"

- In the Project Explorer view, right-click the project, choose Properties
- Resource: Text file encoding: Other: UTF-8 (not the default MacRoman)
- Hit OK

# Build Setup and Preparation

Because some necessary libraries are downloaded by the build process, at first you'll see lots of
syntax errors in the source from missing classes.  We'll set up and run the ant build through the External Tools menu
to get these libraries.  After that, we'll be able to run LightZone through inside Eclipse in the usual way.

We will create several build configs.  The first one will be from scratch, then to save time for the rest
we'll copy it and change the copy's build targets.

If you get error messages while running these builds, see the "Troubleshooting" section at the end of this document.

# First build config: clean

- Click the down-arrow next to the External Tools icon, or choose Run -> External Tools -> External Tools Configs
- Select Ant Build, click the New icon

Set up this config for the build:

- Main tab:

	- Name: clean
	- Buildfile: Browse workspace: lightcrafts -> build.xml
	- Base directory: Browse file system: lightcrafts (at top level of the source tree)
- Build tab: un-check "Build before launch"
- Targets tab: check clean  ; un-check the default "build"
- JRE tab: Separate JRE  ; in the dropdown, be sure to select the same one you're using throughout the project
- Environment tab:

	- New: JAVA_HOME = /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home (or 1.7/Home if you're using 1.7)
	- Choose "Append Environment", not Replace
- Click Apply, click Run

	The console output should end with:

		clean: BUILD SUCCESSFUL


# Main build config: build
This config will download other library jars and build LightZone.

- Open the External Tools Configurations window.
- Right-click our first one ("clean") and Duplicate
- Name: build
- Targets tab: un-check clean; check build
- Click Apply, click Run

	Run will take a while, as it downloads and compiles the libraries, then compiles LightZone itself.
	The ivy, jai-core, and jai-codec libraries will be downloaded by the ant builder,
	built, and installed to /lightcrafts/lib/ in the source directory:
		`jai-core-1.1.3.jar`, `jai-codec-1.1.3.jar`
	Some parts of LightZone are in c or c++, and gcc or gcc+ will compile them to JNI libraries.

	Eventually the console output should end with:

		build: BUILD SUCCESSFUL


# Add the built libraries to project
At this point we need to tell Eclipse about the newly built libraries.

- Project explorer: Open lightcrafts, right-click src, choose Refresh
- Project properties -> java build path -> libraries -> add external JARs
	- Browse to /lightcrafts/lib/ in the source directory and select both of these:

		`jai-core-1.1.3.jar`
		`jai-codec-1.1.3.jar`

	- hit OK
- Eclipse should now auto-rebuild the workspace, and the src folder's red error icon should disappear.

# Final build config: jar
This config will package LightZone as a JAR for execution.

- Open the External Tools Configurations window.
- Right-click our first one ("clean") and Duplicate
- Name: jar
- Build tab: [X] Build before launch; select "The project containing the selected resource"
- Targets tab: un-check clean; check jar
- Click Apply, click Run

	Run will quickly verify the build steps, then create a jar file.
	The console output should end with:

		jar: BUILD SUCCESSFUL


Now, LightZone is built and can be set up to run inside Eclipse.

# Setup to add LightZone to the run menu
If any part of this fails, check the error message and the console tab, and see the Troubleshooting section.

- Project Explorer: in src, in package com.lightcrafts.app, find Application.java
- right-click, Run As: Java Application

  (startup will fail with several errors, but this at least creates an eclipse run configuration)

  Click OK at each error message. Eventually you will see "LightZone has encountered an internal error." Choose "Exit without saving files"

-  edit the run configuration

	- Arguments tab: Working directory: Other: file system: under source tree: lightcrafts/products
	- Apply, Run
	- "LightZone failed to start last time... Would you like to try resetting your LightZone settings?"
		- choose Reset
	- "Are you sure?"
		- choose Reset

# Setup is Complete
At this point, you can now run and develop LightZone.

## Testing your Build
- Run LightZone, from the Eclipse run menu or from the command line with Ant.
- Navigate to a folder with some JPEGs, TIFFs or RAWs. Try all 3 if you have them; they are parsed with different libraries.
- Make sure you can see the thumbnails at the bottom of the window.
- Right-click an image thumbnail and choose Apply Style (any style) to test Batch Processing.
- Double-click an image thumbnail and try some Tools and Styles on it.


## Troubleshooting

If you get a popup error, or something doesn't work as expected, note the error and also check the console in Eclipse.
If you get a build error, you can get more details by adding `-debug` to the build arguments (External Tool: main tab) in Eclipse.

Some specific errors:

# Execute failed: java.io.IOException: Cannot run program "/Applications/eclipse-3.6/plugins/org.apache.ant_1.7.1.v20100518-1145/bin/antRun": error=13, Permission denied
If this appears during build, you may need to go to the antRun directory under /Applications/eclipse-3.6/... and run:

		sudo chmod +x *

# class file has wrong version 51.0, should be 50.0
If this appears, then some of the project was built with java 1.7, some with java 1.6.

- Find which one is the default JVM version for your machine, and set that (see "Eclipse initial setup" above).
- Then, run the "clean" external tool and rebuild the project.

# "java.lang.UnsatisfiedLinkError" ending with "mach-o, but wrong architecture"
  or:

# "Couldn't link with native library: DCRaw: libDCRaw.jnilib: mach-o, but wrong architecture":
Probably some code was compiled as 64-bit but you're running the app in a 32-bit JVM.

- Use the "`file`" command on the library:

		libDCRaw.jnilib: Mach-O dynamically linked shared library x86_64

- x86_64 means 64-bit (i386 means 32-bit); if this is the case, Add `-d64` as a VM argument in the Eclipse Run Configuration and re-run.


# "java.lang.UnsatisfiedLinkError" or "Link not satisfied": LCJPEG

- go to the lightzone/products folder and run: `otool -L libLCJPEG.jnilib`
- make sure each other library file listed there actually exists there
- use the "file" command to verify each one's architecture and bitness (32 or 64) matches the project settings and system default JDK

## Questions/Comments?
This quick guide, and the OSX 64-bit build updates, were done by Jeremy D Monin <jdmonin@nand.net>.
