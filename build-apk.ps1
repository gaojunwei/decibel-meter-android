$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "Install JDK 17 and set JAVA_HOME/PATH." }
if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) { throw "Install Gradle 8.9 and add it to PATH. See docs/BUILD.md." }
& gradle --no-daemon :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "APK build failed: $LASTEXITCODE" }
Write-Host "APK: app/build/outputs/apk/debug/app-debug.apk"
