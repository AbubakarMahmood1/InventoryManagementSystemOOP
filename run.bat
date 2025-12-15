@echo off
setlocal

REM Set JavaFX SDK location
set JAVAFX_SDK=C:\JavaFX\javafx-sdk-22.0.2

REM Set the module path
set MODULE_PATH=%JAVAFX_SDK%\lib

REM Run the jar with JavaFX modules
java --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml -jar target/warehouse-management-1.0-SNAPSHOT.jar

pause
