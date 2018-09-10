echo %0
::pause
REM may use a different JDK/JRE for Javadoc. Therefore a batch setJAVA_Javadoc.bat may found in the PATH in users space.
REM template see TODO
echo on
if "" == "%JAVA_JDK%" call setJAVA_Javadoc.bat
if "" == "%JAVA_JDK%" call ..\..\srcJava_vishiaBase\_make\+findJAVA_Javadoc.bat
echo off
pause
if "" == "%DST_priv%" set DST_priv=%DST%_priv
echo JAVA_JDK=%JAVA_JDK% 
pause

javadoc --help >javadoc.help
javadoc -help >>javadoc.help

pause

echo off
echo +genJavadocbase.bat: %DSTDIR%%DST%

echo +genjavadocbase.bat: JAVA_JDK=%JAVA_JDK%
if  "" == "%DST_priv%" goto :error

::goto :zip
if exist %DSTDIR%%DST% rmdir /Q /S %DSTDIR%%DST% >NUL
if exist %DSTDIR%%DST_priv% rmdir /Q /S %DSTDIR%%DST_priv% >NUL

if not exist %DSTDIR%%DST% mkdir %DSTDIR%%DST%
if not exist %DSTDIR%%DST_priv% mkdir %DSTDIR%%DST_priv%

echo javadoc -d %DSTDIR%%DST% -linksource -notimestamp -nodeprecated %LINKPATH% -classpath %CLASSPATH% -sourcepath %SRCPATH%
echo on
%JAVA_JDK%\bin\javadoc -Xdoclint:none -d %DSTDIR%%DST% -protected -linksource -notimestamp -nodeprecated %LINKPATH% -classpath %CLASSPATH% -sourcepath %SRCPATH% %SRC% 1>%DSTDIR%%DST%\javadoc.rpt 2>%DSTDIR%%DST%\javadoc.err
if errorlevel 1 goto :error
echo off
copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DSTDIR%%DST%\stylesheet.css >NUL

echo javadoc -Xdoclint:none -d %DSTDIR%%DST_priv% -private -linksource -notimestamp %LINKPATH% %CLASSPATH% -sourcepath %SRCPATH%
%JAVA_JDK%\bin\javadoc -d %DSTDIR%%DST_priv% -private -linksource -notimestamp %LINKPATH% -classpath %CLASSPATH% -sourcepath %SRCPATH% %SRC% 1>%DSTDIR%%DST_priv%\javadoc.rpt 2>%DSTDIR%%DST_priv%\javadoc.err
if errorlevel 1 goto :error
copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DSTDIR%%DST_priv%\stylesheet.css >NUL

if "%NOPAUSE%"=="" pause

if not exist ..\img goto :noImg
  echo copy %DSTDIR%%DST%\img
	if not exist %DSTDIR%%DST%\img mkdir %DSTDIR%%DST%\img
	copy ..\img\* %DSTDIR%%DST%\img\* >NUL
	if not exist %DSTDIR%%DST_priv%\img mkdir %DSTDIR%%DST_priv%\img
	copy ..\img\* %DSTDIR%%DST_priv%\img\* >NUL
:noImg


:zip
if "%DSTDIR%" == "" goto :nozip
::pause
::echo on
set PWD1=%CD%
cd %DSTDIR%
if exist %DST%.zip del %DST%.zip >NUL
echo zip %CD%\%DST%.zip
pkzipc.exe -add -Directories %DST%.zip %DST%\* %DST_priv%\*
cd %PWD1%
echo off
if errorlevel 1 goto :error
:nozip

echo successfull generated %DSTDIR%%DST%
if "%NOPAUSE%"=="" pause
goto :ende

:error
echo ===ERROR===
notepad.exe %DSTDIR%%DST%\javadoc.err
pause
:ende



