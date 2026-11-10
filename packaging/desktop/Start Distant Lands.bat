@echo off
rem Double-click to start Distant Lands. Close this window or press Ctrl-C to stop.
setlocal
cd /d "%~dp0"
set "DLQ_LAUNCH=desktop"
call "bin\distantlands.bat" %*
set "status=%ERRORLEVEL%"
if not "%status%"=="0" (
  echo.
  echo Distant Lands stopped with an error ^(exit %status%^).
  pause
)
exit /b %status%
