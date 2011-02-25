REM Call java-compilation and jar with given input environment. This following commands are the same for all java-compilations.
echo on
if exist %TMP_JAVAC% rmdir /S /Q %TMP_JAVAC%
mkdir %TMP_JAVAC%
mkdir %TMP_JAVAC%\bin
%JAVA_HOME%\bin\javac.exe -deprecation -d %TMP_JAVAC%/bin -sourcepath %SRCPATH_JAVAC% -classpath %CLASSPATH_JAVAC% %INPUT_JAVAC% 1>>%TMP_JAVAC%\javac_ok.txt 2>%TMP_JAVAC%\error.txt
echo off
if errorlevel 1 goto :error
echo copiling successfull, generate jar:

set ENTRYDIR=%CD%
cd %TMP_JAVAC%\bin
echo jar -c
%JAVA_HOME%\bin\jar.exe -cvfm %ENTRYDIR%/%OUTPUTFILE_JAVAC% %ENTRYDIR%/%MANIFEST_JAVAC% *  >>../error.txt
if errorlevel 1 goto :error
cd %ENTRYDIR%

pause
goto :ende

:error
  type %TMP_JAVAC%\error.txt
  pause
  goto :ende

:ende
