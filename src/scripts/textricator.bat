@echo off
set batpath=%~dp0
java -cp "%batpath%lib/*" io.mfj.textricator.cli.TextricatorCli %*