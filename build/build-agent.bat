@echo off
set JAVA_HOME=D:\SteamLibrary\steamapps\common\Rusted Warfare\jvm64
set PATH=%JAVA_HOME%\bin;%PATH%
set SRC_DIR=..\src
set OUT_JAR=..\rw-live-final.jar

:: Clean old classes
if exist *.class del /Q *.class

:: Compile with Java 13 (must match game's JVM version)
javac -encoding UTF-8 -d . %SRC_DIR%\RWLiveFinal.java
if errorlevel 1 (
    echo Compile failed!
    pause
    exit /b 1
)

:: Package JAR (include ALL .class files, including inner classes)
jar cvmf MANIFEST.MF %OUT_JAR% *.class
if errorlevel 1 (
    echo JAR packaging failed!
    pause
    exit /b 1
)

echo Build successful: %OUT_JAR%
pause
