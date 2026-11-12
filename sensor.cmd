@echo off
cd /d "%~dp0"
mvn -q compile exec:java@sensor
