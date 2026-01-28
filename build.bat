@echo off
REM ============================================
REM RiSC-16 Graphical Simulator - Build Script
REM Uses your current Java version
REM ============================================

setlocal enabledelayedexpansion

echo.
echo ========================================
echo Building RiSC-16 Graphical Simulator
echo ========================================
echo.

REM Check Java version
echo Checking Java version...
java -version 2>&1 | findstr /C:"version" 
javac -version 2>&1
echo.

REM Step 1: Clean previous build
echo [1/6] Cleaning previous build...
if exist bin rmdir /s /q bin
mkdir bin
if exist risc16-simulator.jar del risc16-simulator.jar
if exist MANIFEST.MF del MANIFEST.MF
echo       Done!
echo.

REM Step 2: Create MANIFEST.MF file
echo [2/6] Creating manifest file...
(
echo Manifest-Version: 1.0
echo Main-Class: gui.Simulator
echo Created-By: RiSC-16 Simulator Build
echo.
) > MANIFEST.MF
echo       Done!
echo.

REM Step 3: Compile all Java files
echo [3/6] Compiling Java source files...
echo       Using your current Java version

REM Find all .java files and compile them
dir /s /b src\*.java > sources.txt

javac -encoding UTF-8 -d bin -cp "lib\*" @sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo ERROR: Compilation failed!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo.
    del sources.txt
    pause
    exit /b 1
)

del sources.txt
echo       Compiled successfully!
echo.

REM Step 4: Copy resources to bin directory
echo [4/6] Copying resource files...
if exist src\gui\resources (
    if not exist bin\gui\resources mkdir bin\gui\resources
    xcopy /E /I /Y /Q src\gui\resources bin\gui\resources > nul
    echo       Resources copied successfully!
) else (
    echo       Warning: No resources folder found
)
echo.

REM Step 5: Extract and package JUnit dependencies
echo [5/6] Packaging dependencies...
if exist lib (
    echo       Extracting JUnit and Hamcrest...
    
    REM Create temp directory for extraction
    if not exist temp_extract mkdir temp_extract
    cd temp_extract
    
    REM Extract each JAR
    for %%j in (..\lib\*.jar) do (
        echo       - Extracting %%~nxj...
        jar xf "%%j"
    )
    
    REM Copy extracted class files to bin (exclude META-INF to avoid conflicts)
    if exist org xcopy /E /I /Y /Q org ..\bin\org > nul 2>&1
    if exist junit xcopy /E /I /Y /Q junit ..\bin\junit > nul 2>&1
    if exist org\hamcrest xcopy /E /I /Y /Q org\hamcrest ..\bin\org\hamcrest > nul 2>&1
    
    cd ..
    
    REM Clean up temp directory
    rmdir /s /q temp_extract
    
    echo       Dependencies packaged!
) else (
    echo       No lib folder found - skipping dependencies
)
echo.

REM Step 6: Create JAR file
echo [6/6] Creating JAR file...
cd bin
jar cfm ..\risc16-simulator.jar ..\MANIFEST.MF .
cd ..

if %ERRORLEVEL% EQU 0 (
    echo       Done!
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo JAR file created: risc16-simulator.jar
    
    REM Get file size
    for %%A in (risc16-simulator.jar) do set size=%%~zA
    set /a sizekb=!size!/1024
    echo File size: !sizekb! KB
    echo.
    echo To run your simulator:
    echo   java -jar risc16-simulator.jar
    echo.
    echo Or simply double-click the JAR file.
    echo.
    
) else (
    echo.
    echo ========================================
    echo ERROR: JAR creation failed!
    echo ========================================
    echo.
    pause
    exit /b 1
)

REM Clean up
if exist MANIFEST.MF del MANIFEST.MF

echo Build completed successfully!
echo Press any key to exit...
pause > nul