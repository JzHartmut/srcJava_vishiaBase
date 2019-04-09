REM This file sets environment variables for the compilation.
REM adapt and copy it! It should be able to found in the PATH.

::set JAVA_JDK=c:\Programs\Java\JDK8\jdk1.8.0_201
set JAVA_JDK=c:\Programs\Java\jdk1.8.0_181
::set JAVA_JDK=D:\Programs\JAVA\jdk1.7.0_65
set JAVA_JRE=D:\Programs\JAVA\jre
::call setJAVA_JDK6.bat
set PATH=%JAVA_JDK%\bin;%JAVA_JRE%\bin;%PATH%
echo JAVA_JDK=%JAVA_JDK%

