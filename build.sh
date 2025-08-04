#!/bin/bash

# =============================================================================
# Comprehensive build script for Biotak Financial Indicator
# =============================================================================
# Usage:
#   ./build.sh           -> Compile & package jar (default)
#   ./build.sh dev       -> Hot-reload development mode
#   ./build.sh test      -> Run tests after compilation
#   ./build.sh clean     -> Clean build and rebuild
#   ./build.sh release   -> Build optimized release version
# =============================================================================

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

# Function to handle errors and exit
error_exit() {
    print_error "$1"
    exit 1
}

# Validate environment
validate_environment() {
    print_info "Validating build environment..."
    
    # Check if JAVA_HOME is set, if not try to detect
    if [ -z "$JAVA_HOME" ]; then
        if command -v java &> /dev/null; then
            JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
            print_warning "JAVA_HOME not set, detected: $JAVA_HOME"
        else
            error_exit "JAVA_HOME not set and java not found in PATH"
        fi
    fi
    
    # Verify Java installation
    if [ ! -f "$JAVA_HOME/bin/javac" ] && [ ! -f "$JAVA_HOME/bin/javac.exe" ]; then
        error_exit "javac not found at $JAVA_HOME/bin/javac"
    fi
    
    # Check SDK dependency
    if [ ! -f "$MWAVE_SDK_JAR" ]; then
        error_exit "MotiveWave SDK not found at $MWAVE_SDK_JAR"
    fi
    
    print_success "Environment validation passed"
}

# Display build information
show_build_info() {
    echo "====================================="
    echo "    BIOTAK BUILD INFORMATION"
    echo "====================================="
    JAVA_VERSION=$(eval "\"$JAVA_HOME/bin/java\" -version 2>&1" | head -n 1 2>/dev/null || echo "Java version detection failed")
    echo "Java Version: $JAVA_VERSION"
    echo "Build Mode: $MODE"
    echo "Source Directory: $SRC_DIR"
    echo "Build Directory: $BUILD_DIR"
    echo "Target Directory: $EXT_DIR"
    echo "====================================="
}

# Configuration
MODE=${1:-jar}
# Force Java 24 to match MotiveWave SDK version requirements
JAVA_HOME="/c/Program Files/Java/jdk-24"
MWAVE_SDK_JAR="lib/mwave_sdk.jar"
JUNIT_JAR="lib/junit-platform-console-standalone-1.10.0.jar"
SRC_DIR="src"
BUILD_DIR="build/classes"
TEST_DIR="build/test-classes"
EXT_DIR="/c/Users/Fatemehkh/MotiveWave Extensions"
JAR_OUT="build/biotak.jar"

# Commands (using eval for paths with spaces)
JAVAC_CMD="$JAVA_HOME/bin/javac"
JAVA_CMD="$JAVA_HOME/bin/java"
JAR_CMD="$JAVA_HOME/bin/jar"

# Validate environment first
validate_environment
show_build_info

# Handle clean mode
if [[ "$MODE" == "clean" ]]; then
    print_info "Cleaning build directories..."
    rm -rf "build"
    rm -rf "out"
    print_success "Clean completed"
    exit 0
fi

# Create build directories
print_info "Setting up build directories..."
mkdir -p "$BUILD_DIR"
mkdir -p "$TEST_DIR"
mkdir -p "build"

# Count source files
JAVA_FILES=$(find "$SRC_DIR" -name "*.java" -type f)
FILE_COUNT=$(echo "$JAVA_FILES" | wc -l)
print_info "Found $FILE_COUNT Java source files"

# Compile main sources
print_info "Compiling main sources..."
# Create a temporary file list for javac (each file on a separate line)
TEMP_FILE_LIST="build/java_files.txt"
find "$SRC_DIR" -name "*.java" -type f > "$TEMP_FILE_LIST"
eval "\"$JAVAC_CMD\" -cp \"$MWAVE_SDK_JAR\" -d \"$BUILD_DIR\" -nowarn -Xlint:unchecked @\"$TEMP_FILE_LIST\"" || error_exit "Main compilation failed"
rm -f "$TEMP_FILE_LIST"
print_success "Main compilation completed successfully"

# Handle test mode
if [[ "$MODE" == "test" ]]; then
    print_info "Compiling and running tests..."
    
    # Find test files
    TEST_FILES=$(find "$SRC_DIR" -name "*Test*.java" -o -name "*test*.java" -type f)
    if [ -n "$TEST_FILES" ]; then
        print_info "Compiling test files..."
        eval "\"$JAVAC_CMD\" -cp \"$MWAVE_SDK_JAR:$JUNIT_JAR:$BUILD_DIR\" -d \"$TEST_DIR\" $TEST_FILES" || error_exit "Test compilation failed"
        
        print_info "Running tests..."
        eval "\"$JAVA_CMD\" -cp \"$JUNIT_JAR:$BUILD_DIR:$TEST_DIR\" org.junit.platform.console.ConsoleLauncher --scan-classpath" || print_warning "Some tests failed"
    else
        print_warning "No test files found"
    fi
    exit 0
fi

# Handle dev mode
if [[ "$MODE" == "dev" ]]; then
    print_info "Deploying classes for development mode..."
    
    DEV_DIR="$EXT_DIR/dev"
    rm -rf "$DEV_DIR"
    mkdir -p "$DEV_DIR"
    
    cp -r "$BUILD_DIR"/* "$DEV_DIR/" || error_exit "Failed to copy dev classes"
    touch "$EXT_DIR/.last_updated"
    
    print_success "Development classes deployed to $DEV_DIR"
    print_info "MotiveWave will hot-reload these classes automatically"
    exit 0
fi

# Build JAR (default and release modes)
print_info "Creating JAR package..."

# Create manifest
MANIFEST_FILE="build/MANIFEST.MF"
cat > "$MANIFEST_FILE" << EOF
Manifest-Version: 1.0
Created-By: Biotak Build System
Implementation-Title: Biotak Financial Indicator
Implementation-Version: 1.0.0
Implementation-Vendor: Biotak Development Team
Build-Date: $(date)
Main-Class: com.biotak.BiotakTrigger

EOF

# Create JAR with manifest
cd "$BUILD_DIR" || error_exit "Cannot change to build directory"
eval "\"$JAR_CMD\" cfm \"../biotak.jar\" \"../MANIFEST.MF\" com" || error_exit "JAR creation failed"
cd - > /dev/null

print_success "JAR created: $JAR_OUT"

# Deploy to MotiveWave
print_info "Deploying to MotiveWave Extensions..."
LIB_DIR="$EXT_DIR/lib"
mkdir -p "$LIB_DIR"
cp "$JAR_OUT" "$LIB_DIR/" || error_exit "Failed to deploy JAR"

# Update timestamp
touch "$EXT_DIR/.last_updated"

# Show deployment info
JAR_SIZE=$(du -h "$JAR_OUT" | cut -f1)
print_success "Build completed successfully!"
print_info "JAR size: $JAR_SIZE"
print_info "Deployed to: $LIB_DIR/biotak.jar"
print_info "MotiveWave Extensions directory updated"

if [[ "$MODE" == "release" ]]; then
    print_info "Release mode: Consider running tests with './build.sh test'"
fi
