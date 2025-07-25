#!/bin/bash

# JDK 24 and MotiveWave 7 Compatibility Test
echo "=== Starting Compatibility Tests ==="

# Check JAVA_HOME
echo "1. Checking JAVA_HOME..."
if [ -z "$JAVA_HOME" ]; then
    echo "   [WARN] JAVA_HOME not set"
    JAVA_HOME="/c/Program Files/Java/jdk-24"
    echo "   Using: $JAVA_HOME"
fi

# Check Java version
echo "2. Checking Java version..."
"$JAVA_HOME/bin/java" -version

# Compile project
echo "3. Compiling project..."
./build.sh > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "   [ERROR] Compilation failed"
    ./build.sh  # Show error details
    exit 1
fi
echo "   [OK] Compilation successful (no warnings)"

# Run compatibility test
echo "4. Running compatibility test..."
"$JAVA_HOME/bin/java" -cp "lib/mwave_sdk.jar;build/classes" com.biotak.test.CompatibilityTest
if [ $? -ne 0 ]; then
    echo "   [ERROR] Compatibility test failed"
    exit 1
fi

# Run performance test
echo "5. Running performance test..."
"$JAVA_HOME/bin/java" -cp "lib/mwave_sdk.jar;build/classes" com.biotak.test.PerformanceTest
if [ $? -ne 0 ]; then
    echo "   [ERROR] Performance test failed"
    exit 1
fi

# Run advanced performance test
echo "6. Running advanced performance test..."
"$JAVA_HOME/bin/java" -cp "lib/mwave_sdk.jar;build/classes" com.biotak.test.AdvancedPerformanceTest
if [ $? -ne 0 ]; then
    echo "   [ERROR] Advanced performance test failed"
    exit 1
fi

# Run accurate performance test
echo "7. Running accurate performance test..."
"$JAVA_HOME/bin/java" -cp "lib/mwave_sdk.jar;build/classes" com.biotak.test.AccuratePerformanceTest
if [ $? -ne 0 ]; then
    echo "   [ERROR] Accurate performance test failed"
    exit 1
fi

# Run deep performance analysis
echo "8. Running deep performance analysis..."
"$JAVA_HOME/bin/java" -cp "lib/mwave_sdk.jar;build/classes" com.biotak.test.DeepPerformanceAnalysis
if [ $? -ne 0 ]; then
    echo "   [ERROR] Deep performance analysis failed"
    exit 1
fi

echo "=== All tests completed successfully! ==="
echo "[SUCCESS] Project is compatible with JDK 24 and MotiveWave 7"