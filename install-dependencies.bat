@echo off
echo ========================================
echo    AdaptixBot Dependencies Installer
echo ========================================
echo.

echo Installing Java dependencies...
.\gradlew.bat build --no-daemon
echo.

echo Creating necessary directories...
if not exist "upload" mkdir upload
if not exist "reviews" mkdir reviews
echo ✓ Directories created
echo.

echo ========================================
echo Dependencies installed successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Configure database in hibernate.cfg.xml
echo 2. Configure bot token in app.properties
echo 3. Start PostgreSQL and Redis
echo 4. Run: run-bot.bat
echo.
pause
