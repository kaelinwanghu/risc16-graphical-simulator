@echo off
REM ============================================
REM RiSC-16 Graphical Simulator - Build Script
REM Targets Java 8 for maximum compatibility
REM ============================================

echo.
echo ========================================
echo Building RiSC-16 Graphical Simulator
echo ========================================
echo.

REM Step 1: Clean previous build
echo [1/5] Cleaning previous build...
if exist bin rmdir /s /q bin
mkdir bin
if exist risc16-graphical-simulator.jar del risc16-graphical-simulator.jar
echo       Done!
echo.

REM Step 2: Create MANIFEST.MF file
echo [2/5] Creating manifest file...
(
echo Manifest-Version: 1.0
echo Main-Class: gui.Simulator
echo Created-By: RiSC-16 Simulator Build
echo.
) > MANIFEST.MF
echo       Done!
echo.

REM Step 3: Compile all Java files (targeting Java 8)
echo [3/5] Compiling Java source files...
echo       Target: Java 8 bytecode for maximum compatibility
javac -source 8 -target 8 -d bin -sourcepath src src\gui\Simulator.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo ERROR: Compilation failed!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo Make sure you have JDK 8 or higher installed.
    echo.
    pause
    exit /b 1
)
echo       Done!
echo.

REM Step 4: Copy resources to bin directory
echo [4/5] Copying resource files...
if exist src\gui\resources (
    if not exist bin\gui mkdir bin\gui
    xcopy /E /I /Y /Q src\gui\resources bin\gui\resources > nul
    echo       Resources copied successfully!
) else (
    echo       Warning: No resources folder found (this is OK if you don't have resources)
)
echo.

REM Step 5: Create JAR file
echo [5/5] Creating JAR file...
jar cvfm risc16-graphical-simulator.jar MANIFEST.MF -C bin . > jar_output.txt 2>&1

if %ERRORLEVEL% EQU 0 (
    echo       Done!
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo JAR file created: risc16-graphical-simulator.jar
    echo.
    echo To run your simulator:
    echo   java -jar risc16-graphical-simulator.jar
    echo.
    echo Or simply double-click the JAR file.
    echo.
    
    if exist jar_output.txt del jar_output.txt
    
) else (
    echo.
    echo ========================================
    echo ERROR: JAR creation failed!
    echo ========================================
    echo.
    type jar_output.txt
    if exist jar_output.txt del jar_output.txt
    echo.
    pause
    exit /b 1
)

echo Build completed successfully!
echo.
pause