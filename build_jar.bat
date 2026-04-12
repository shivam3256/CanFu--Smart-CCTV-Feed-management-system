@echo off
setlocal enabledelayedexpansion
cd /D "C:\Users\kumar\OneDrive\Desktop\CamFu\target"

REM Remove old JAR
if exist CamFu.jar del CamFu.jar

REM Create new JAR with manifest and classes
jar cvfm CamFu.jar MANIFEST.MF -C classes .

echo JAR creation complete
java -jar CamFu.jar
