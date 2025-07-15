#!/bin/bash

# Set the paths
JAVA_HOME="C:/Program Files/Java/jdk-23"
MWAVE_SDK_JAR="lib/mwave_sdk.jar"
SRC_DIR="src"
BUILD_DIR="build"
EXTENSIONS_DIR="C:/Users/fatemeh/MotiveWave Extensions"

# Create build directory if it doesn't exist
mkdir -p $BUILD_DIR

# Compile the code
echo "Compiling Java files..."
"$JAVA_HOME/bin/javac" -cp "$MWAVE_SDK_JAR" -d $BUILD_DIR $(find $SRC_DIR -name "*.java")

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Create the JAR file
echo "Creating JAR file..."
cd $BUILD_DIR
"$JAVA_HOME/bin/jar" cf biotak.jar com

if [ $? -ne 0 ]; then
    echo "JAR creation failed!"
    exit 1
fi

# Copy the JAR to the MotiveWave Extensions directory
echo "Copying JAR to MotiveWave Extensions directory..."
cp biotak.jar "$EXTENSIONS_DIR"

if [ $? -ne 0 ]; then
    echo "Failed to copy JAR to Extensions directory!"
    exit 1
fi

echo "Build and deployment complete!" 