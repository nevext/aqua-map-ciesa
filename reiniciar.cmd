@echo off
cd /d "%~dp0"
echo.
echo   AquaMap - comecar do zero
echo.
echo   Isto MOVE as coletas, as planilhas (CSV e XLSX) e os agendamentos
echo   atuais para a pasta data\backup\ (numa subpasta com data e hora).
echo   Nada e apagado. O mapa de comunidades (data\comunidades.csv) e mantido.
echo.
set /p resposta=  Continuar? (S/N):
if /i not "%resposta%"=="S" (
  echo.
  echo   Cancelado. Nada foi alterado.
  echo.
  exit /b 1
)

echo.
echo   Encerrando o sistema, se estiver ligado...
call "%~dp0parar.cmd" >nul 2>&1

echo   Arquivando os dados...
call mvn -q compile exec:java@reiniciar
if errorlevel 1 (
  echo.
  echo   Nao foi possivel arquivar os dados. Nada mais foi feito.
  echo.
  exit /b 1
)

echo.
echo   Ligando o sistema de novo...
call "%~dp0iniciar.cmd"
exit /b 0
