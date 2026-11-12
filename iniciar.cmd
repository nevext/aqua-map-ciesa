@echo off
cd /d "%~dp0"
echo.
echo   AquaMap - iniciando
echo.

call :painel_no_ar
if not errorlevel 1 (
  echo   O sistema ja esta ligado. Abrindo o painel no navegador...
  start "" http://localhost:8080
  exit /b 0
)

echo   Ligando o sistema (janela "AquaMap - Sistema")...
start "AquaMap - Sistema" cmd /k "mvn -q compile exec:java@sistema"
echo.
echo   O painel abre no navegador quando o sistema ficar pronto.
echo   Para enviar coletas, use a aba "Sensor" do painel.
echo   Para encerrar: feche a janela do sistema (ou rode parar.cmd).
echo.
exit /b 0

:painel_no_ar
powershell -NoProfile -Command "try { Invoke-WebRequest http://localhost:8080 -UseBasicParsing -TimeoutSec 1 | Out-Null; exit 0 } catch { exit 1 }"
exit /b %errorlevel%
