#!/bin/bash

# RiSC-16 Simulator Test Runner
# This script compiles and runs JUnit tests for the assembler

# Configuration
JUNIT_VERSION="4.13.2"
HAMCREST_VERSION="2.2"
LIB_DIR="lib"
SRC_DIR="src"
TEST_DIR="test"
BUILD_DIR="build"
TEST_BUILD_DIR="build/test"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "  RiSC-16 Assembler Test Runner"
echo "========================================"
echo ""

# Create directories if they don't exist
mkdir -p "$LIB_DIR"
mkdir -p "$BUILD_DIR"
mkdir -p "$TEST_BUILD_DIR"

# Download JUnit and Hamcrest if not present
if [ ! -f "$LIB_DIR/junit-$JUNIT_VERSION.jar" ]; then
    echo -e "${YELLOW}Downloading JUnit $JUNIT_VERSION...${NC}"
    curl -sL "https://repo1.maven.org/maven2/junit/junit/$JUNIT_VERSION/junit-$JUNIT_VERSION.jar" \
        -o "$LIB_DIR/junit-$JUNIT_VERSION.jar"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}JUnit downloaded successfully${NC}"
    else
        echo -e "${RED}Failed to download JUnit. Please download manually from:${NC}"
        echo "https://repo1.maven.org/maven2/junit/junit/$JUNIT_VERSION/junit-$JUNIT_VERSION.jar"
        exit 1
    fi
fi

if [ ! -f "$LIB_DIR/hamcrest-$HAMCREST_VERSION.jar" ]; then
    echo -e "${YELLOW}Downloading Hamcrest $HAMCREST_VERSION...${NC}"
    curl -sL "https://repo1.maven.org/maven2/org/hamcrest/hamcrest/$HAMCREST_VERSION/hamcrest-$HAMCREST_VERSION.jar" \
        -o "$LIB_DIR/hamcrest-$HAMCREST_VERSION.jar"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Hamcrest downloaded successfully${NC}"
    else
        echo -e "${RED}Failed to download Hamcrest. Please download manually from:${NC}"
        echo "https://repo1.maven.org/maven2/org/hamcrest/hamcrest/$HAMCREST_VERSION/hamcrest-$HAMCREST_VERSION.jar"
        exit 1
    fi
fi

# Build classpath
JUNIT_JAR="$LIB_DIR/junit-$JUNIT_VERSION.jar"
HAMCREST_JAR="$LIB_DIR/hamcrest-$HAMCREST_VERSION.jar"
CLASSPATH="$SRC_DIR:$TEST_DIR:$BUILD_DIR:$TEST_BUILD_DIR:$JUNIT_JAR:$HAMCREST_JAR"

echo ""
echo "Step 1: Compiling source code..."
echo "----------------------------------------"

# Compile main source files
find "$SRC_DIR" -name "*.java" > /tmp/sources.txt
javac -d "$BUILD_DIR" -source 8 -target 8 @/tmp/sources.txt 2>&1

if [ $? -ne 0 ]; then
    echo -e "${RED}Source compilation failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Source compilation successful${NC}"

echo ""
echo "Step 2: Compiling test code..."
echo "----------------------------------------"

# Compile test files
find "$TEST_DIR" -name "*.java" > /tmp/test_sources.txt
javac -d "$TEST_BUILD_DIR" -source 8 -target 8 -cp "$CLASSPATH" @/tmp/test_sources.txt 2>&1

if [ $? -ne 0 ]; then
    echo -e "${RED}Test compilation failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Test compilation successful${NC}"

echo ""
echo "Step 3: Running tests..."
echo "----------------------------------------"

# Find all test classes
TEST_CLASSES=$(find "$TEST_BUILD_DIR" -name "*Test.class" | \
    sed "s|$TEST_BUILD_DIR/||" | \
    sed 's|/|.|g' | \
    sed 's|.class||')

# Run tests
java -cp "$SRC_DIR:$BUILD_DIR:$TEST_BUILD_DIR:$JUNIT_JAR:$HAMCREST_JAR" \
    org.junit.runner.JUnitCore $TEST_CLASSES

TEST_RESULT=$?

echo ""
echo "========================================"
if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
else
    echo -e "${RED}Some tests failed.${NC}"
fi
echo "========================================"

# Cleanup
rm -f /tmp/sources.txt /tmp/test_sources.txt

exit $TEST_RESULT
