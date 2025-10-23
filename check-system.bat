@echo off
echo ========================================
echo    AdaptixBot System Check
echo ========================================
echo.

echo Checking Java version...
java -version
if %errorlevel% neq 0 (
    echo ✗ Java not found. Please install Java 17+
    pause
    exit /b 1
)
echo ✓ Java found
echo.

echo Checking Gradle...
.\gradlew.bat --version
if %errorlevel% neq 0 (
    echo ✗ Gradle not found
    pause
    exit /b 1
)
echo ✓ Gradle found
echo.

echo Checking configuration files...
if not exist "src\main\resources\app.properties" (
    echo ✗ app.properties not found
    pause
    exit /b 1
)
echo ✓ app.properties found
echo.

if not exist "src\main\resources\hibernate.cfg.xml" (
    echo ✗ hibernate.cfg.xml not found
    pause
    exit /b 1
)
echo ✓ hibernate.cfg.xml found
echo.

echo Creating necessary directories...
if not exist "upload" mkdir upload
if not exist "reviews" mkdir reviews
echo ✓ Directories created
echo.

echo Compiling project...
.\gradlew.bat compileJava --no-daemon
if %errorlevel% neq 0 (
    echo ✗ Compilation failed
    pause
    exit /b 1
)
echo ✓ Compilation successful
echo.

echo ========================================
echo System check completed!
echo ========================================
echo.
echo System is ready to run AdaptixBot
echo.
echo To start the bot: start-bot.bat
echo.
pause
