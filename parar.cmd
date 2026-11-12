@echo off
cd /d "%~dp0"
echo.
echo   Encerrando AquaMap (portas 8080 e 5050)...
echo.
for /f "tokens=5" %%p in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":8080 "') do (
  echo   Processo %%p — porta 8080
  taskkill /PID %%p /F >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":5050 "') do (
  echo   Processo %%p — porta 5050
  taskkill /PID %%p /F >nul 2>&1
)
echo.
echo   Pronto. Agora: .\sistema.cmd
echo.
