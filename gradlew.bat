@echo off
setlocal
set "GRADLE_VERSION=9.7.0"
set "ROOT=%~dp0"
set "DIST_DIR=%ROOT%.gradle-dist"
set "GRADLE_HOME=%DIST_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP=%DIST_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found in PATH.
  echo Install JDK 25, reopen Command Prompt, then try again.
  exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%~v"
echo Using Java: %JAVA_VER%

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Gradle %GRADLE_VERSION% is not installed for this project. Downloading it now...
  if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
  if errorlevel 1 (
    echo ERROR: Failed to download Gradle.
    exit /b 1
  )
  echo Extracting Gradle...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP%' -DestinationPath '%DIST_DIR%' -Force"
  if errorlevel 1 (
    echo ERROR: Failed to extract Gradle.
    exit /b 1
  )
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
