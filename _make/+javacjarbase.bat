::echo off
REM Task: Compilation javac and call jar
if "%INPUT_JAVAC%" =="" (
  echo == +javacjar.bat ==
  echo script to organize javac compilation and build the jar.
  echo The environment variables should be exported as Input for this script:
  echo ---------------------------------------------------------------------------------------------
  echo JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation
  echo   if not set, setJAVA_JDK.bat is called. It should be found in the system's PATH.
  echo TMP_JAVAC: Directory for all class files and logs: The directory will be cleaned and created
  echo INPUT_JAVAC: All primary input java files to compile separated with space
  echo CLASSPATH_JAVAC: PATH where compiled classes are found, relativ from current dir or absolute
  echo SRCPATH_JAVAC: PATH where sources are found, relativ from current dir or absolute
  echo OUTDIR_JAVAC: Path where the generated jar file will be stored. It may be a relative path.
  echo   It will be created if not found.
  echo JAR_JAVAC: file.jar
  echo MANIFEST_JAVAC: Path/file.manifest relative from currect dir for manifest file

  exit 1
)


if "%SRCvishiaBase%"=="" set SRCvishiaBase=..

REM --------------------------------------------------------------------------------------------
REM  Environment variables set from zbnfjax:

REM  The java-copiler may be located at a user-specified position.
REM  Set the environment variable JAVA_JDK, where bin/javac will be found.
REM  The java-copiler may be located on a user-specified position.
REM  Set the environment variable JAVA_JDK, where bin/javac will be found.
REM  The setJAVA_JDK.bat may be existing in the user's path.
::if "" == "%JAVA_JDK%" 

if not "%JAVA_JDK%" == "" goto :JavaOK
REM call a batch which sets the environment variable JAVA_JDK and expands the path to a proper JAVA-JDK
echo on

::The bat for JDK environment setting should be found in the path (C:\batch etc.) 
call setJAVA_JDK.bat
::faulty: call %SRCvishiaBase%\_make\setJAVA_JDK.bat
echo off
if exist %JAVA_JDK% goto :JavaOK

REM if setJAVA_JDK.bat is not found or its content is faulty.
echo %JAVA_JDK% not found. 
echo read the srcJava_vishiaBase/_make/readme_javac.txt
pause
exit /B

:JavaOk

if exist %JAVA_JDK%\bin\javac.exe goto JavaJdkOk
echo JAVA_JDK is set but it is not ok, bin/javac is not found there:
echo read the srcJava_vishiaBase/_make/readme_javac.txt
echo JAVA_JDK=%JAVA_JDK% 
pause
exit /B

:JavaJdkOk



if not exist %OUTDIR_JAVAC% mkdir %OUTDIR_JAVAC%

REM  Delete and create the tmp_javac new. It should be empty because all content will be stored in the jar-file.
if exist %TMP_JAVAC% rmdir /S/Q %TMP_JAVAC%
mkdir %TMP_JAVAC%\bin

REM  Delete the result jar-file
del /F/Q %OUTDIR_JAVAC%\%JAR_JAVAC%
del /F/Q %OUTDIR_JAVAC%\%JAR_JAVAC%.jar.log
del /F/Q %OUTDIR_JAVAC%\%JAR_JAVAC%.compile.log
del /F/Q %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log

echo === javac -d %TMP_JAVAC%\bin -sourcepath %SRCPATH_JAVAC% -classpath %CLASSPATH_JAVAC% %INPUT_JAVAC%

%JAVA_JDK%\bin\javac -deprecation -d %TMP_JAVAC%\bin -sourcepath %SRCPATH_JAVAC% -classpath %CLASSPATH_JAVAC% %INPUT_JAVAC% 1>>%TMP_JAVAC%\javac_ok.txt 2>%TMP_JAVAC%\javac_error.txt
if ERRORLEVEL 1 (
  echo ===Compiler error
  type %TMP_JAVAC%\javac_ok.txt 
	type %TMP_JAVAC%\javac_error.txt
  type %TMP_JAVAC%\javac_ok.txt >%OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
  type %TMP_JAVAC%\javac_error.txt >%OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
  echo see %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
	call edit.bat %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
	exit /B 1
)

REM  The jar works only correct, if the current directory contains the classfile tree:
REM  Store the actual directory to submit restoring on end
set ENTRYDIR=%CD%
echo ENTRYDIR=%ENTRYDIR%
:: pause
echo on
if not "%COPYJAR%" == "" copy %COPYJAR% 
if not "%COPYJAR1%" == "" copy %COPYJAR1% 
if not "%COPYJAR2%" == "" copy %COPYJAR2% 
if not "%COPYJAR3%" == "" copy %COPYJAR3% 
::cd /D %TMP_JAVAC%\bin
echo === SUCCESS compiling, generate jar: %ENTRYDIR%\%OUTDIR_JAVAC%\%JAR_JAVAC%
echo TMP_JAVAC=%CD%

echo on
%JAVA_JDK%\bin\jar -cvfm %OUTDIR_JAVAC%\%JAR_JAVAC% %MANIFEST_JAVAC% -C %TMP_JAVAC%\bin . 1>%TMP_JAVAC%\%JAR_JAVAC%.jar.log 2>%TMP_JAVAC%\jar_error.txt
echo off
REM join jar_error.txt and compile_error.log to one file to view it after this script.
type %TMP_JAVAC%\jar_error.txt >%TMP_JAVAC%.compile_error.log

if errorlevel 1 (
  echo === ERROR jar
  echo see %TMP_JAVAC%.compile_error.log
	call edit.bat %TMP_JAVAC%.compile_error.log
  cd /D %ENTRYDIR%
  exit /B 1
)

REM  Restore current dir: %ENTRYDIR%
::cd /D %ENTRYDIR%
echo === SUCCESS making %JAR_JAVAC% in %OUTDIR_JAVAC%.

if "%NOPAUSE%" == "" pause

