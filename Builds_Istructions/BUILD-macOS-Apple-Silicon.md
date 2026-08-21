# Building LightZone on Apple Silicon macOS

This document describes how to build LightZone directly from source on an Apple Silicon Mac (`arm64`). The build uses Gradle and the Gradle Wrapper included in the repository; Apache Ant is not required.

## Requirements

Install the Xcode Command Line Tools, Homebrew, and the required dependencies:

```sh
xcode-select --install
brew install openjdk@21 jpeg-turbo lensfun libomp libraw libtiff little-cms2 pkg-config
```

LightZone is built and tested with Java 21. Confirm that Homebrew is using the Apple Silicon prefix:

```sh
brew --prefix
uname -m
```

The expected architecture is `arm64`, and the usual Homebrew prefix is `/opt/homebrew`.

## Configure Java

Set `JAVA_HOME` to the Homebrew Java 21 installation:

```sh
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

To make this configuration permanent for Zsh:

```sh
cat >> ~/.zshrc <<'EOF'
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
EOF

source ~/.zshrc
```

Verify the Java installation:

```sh
java -version
javac -version
./gradlew --version
```

All commands should report Java 21. If `java_home` cannot find Java, use the explicit `JAVA_HOME` value above rather than `/usr/libexec/java_home`.

## Build

Run the build from the repository root, where `gradlew` and `settings.gradle.kts` are located:

```sh
cd /path/to/LightZone
chmod +x gradlew

./gradlew clean
./gradlew build -x test
./gradlew jar
```

A successful build ends with:

```text
BUILD SUCCESSFUL
```

The native JNI libraries are compiled as part of the Gradle build. Some compiler and linker warnings may be emitted; they do not necessarily indicate a failed build. The final `BUILD SUCCESSFUL` status is the relevant result.

## Run the application

To run the application directly from Gradle:

```sh
./gradlew run
```

## Create a macOS package

To create a macOS application package and DMG installer:

```sh
./gradlew jpackage
```

The generated files are placed below the Gradle `build/jpackage` directory.

## Troubleshooting

### Java/Kotlin JVM-target mismatch

If Gradle reports a mismatch between Java and Kotlin JVM targets, make sure the build is using Java 21:

```sh
java -version
./gradlew --version
```

Both commands should report Java 21.

### Native compilation failures

If the build fails during native compilation, verify that the dependencies are installed for the same Homebrew architecture:

```sh
brew list jpeg-turbo lensfun libomp libraw libtiff little-cms2 pkg-config
brew --prefix
```

The prefix should be `/opt/homebrew` on Apple Silicon.

### Detailed diagnostics

For more detailed Gradle diagnostics, run:

```sh
./gradlew build -x test --stacktrace --warning-mode all
```

The complete macOS development notes are available in [`macosx/BUILD-macosx.md`](macosx/BUILD-macosx.md).