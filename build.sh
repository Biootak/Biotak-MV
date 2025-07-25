#!/bin/bash

# -----------------------------
# Simple build script for Biotak indicator
# Usage:
#   ./build.sh          -> Compile & package jar (normal deploy)
#   ./build.sh dev      -> Compile classes into Extensions/dev for hot-reload
# -----------------------------
 
# Set the paths (edit if your paths differ)
JAVA_HOME="/c/Program Files/Java/jdk-23"
MWAVE_SDK_JAR="lib/mwave_sdk.jar"
SRC_DIR="src"
BUILD_DIR="build/classes"
EXT_DIR="/c/Users/fatemeh/MotiveWave Extensions"

MODE=${1:-jar}   # default jar, dev when arg = dev

# Clean previous build
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Compile sources
echo "Compiling Java files..."
"$JAVA_HOME/bin/javac" -cp "$MWAVE_SDK_JAR" -d "$BUILD_DIR" -nowarn $(find "$SRC_DIR" -name "*.java") || { echo "Compilation failed"; exit 1; }

echo "Compilation successful."

if [[ "$MODE" == "dev" ]]; then
  echo "Deploying class files to $EXT_DIR/dev ..."
  rm -rf "$EXT_DIR/dev"
  mkdir -p "$EXT_DIR/dev"
  cp -r "$BUILD_DIR"/* "$EXT_DIR/dev/" || { echo "Copy failed"; exit 1; }
  touch "$EXT_DIR/.last_updated"
  echo "Build successful – dev classes copied."
  exit 0
fi

# Otherwise build jar
JAR_OUT="build/biotak.jar"
mkdir -p build
cd "$BUILD_DIR" || exit 1
"$JAVA_HOME/bin/jar" cf "../biotak.jar" com || { echo "Jar creation failed"; exit 1; }
cd ../../

mkdir -p "$EXT_DIR/lib"
cp "$JAR_OUT" "$EXT_DIR/lib/" || { echo "Copy jar failed"; exit 1; }

touch "$EXT_DIR/.last_updated"
echo "Build successful – jar deployed to MotiveWave Extensions/lib." 