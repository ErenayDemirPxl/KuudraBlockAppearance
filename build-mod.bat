@echo off
call "%~dp0gradlew.bat" build
if errorlevel 1 (
  echo.
  echo BUILD FAILED. Copy the error above and send it to ChatGPT.
  pause
  exit /b 1
)
echo.
echo BUILD SUCCESSFUL.
echo Your mod JAR should be in: %~dp0build\libs
pause
