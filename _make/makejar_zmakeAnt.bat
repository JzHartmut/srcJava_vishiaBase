echo off
REM The java-copiler may be located at a user-specified position.
REM Set the environment variable JAVA_HOME, where bin/javac will be found.
if "%JAVA_HOME%" == "" set JAVA_HOME=D:\Progs\JAVA\jdk1.6.0_21
::set PATH=%JAVA_HOME%\bin;%PATH%

REM The ANT library is supplied with eclipse or it is able to download.
if "%ANT_HOME%" == "" set ANT_HOME=\Progs\XML_Tools\org.apache.ant_1.6.5

REM The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
set TMP_JAVAC=..\..\..\tmp_javac

REM Output jar-file with path and filename relative from current dir:
set OUTPUTFILE_JAVAC=..\..\zbnfjax\zmakeAnt.jar
if not exist ..\..\zbnfjax mkdir ..\..\zbnfjax

REM Manifest-file for jar building relativ path from current dir:
set MANIFEST_JAVAC=zmakeAnt.manifest

REM Input for javac, only choice of primary sources, relativ path from current (make)-directory:
set INPUT_JAVAC=
::set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zbnf/Zmake.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/ant/Zcopy.java

REM Sets the CLASSPATH variable for compilation (used jar-libraries). do not leaf empty also it aren't needed:
set CLASSPATH_JAVAC=../../zbnfjax/zbnf.jar;%ANT_HOME%\lib\ant.jar

REM Sets the src-path for further necessary sources:
set SRCPATH_JAVAC=..

call .\compileJava.bat
