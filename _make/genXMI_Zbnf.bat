if "%ZBNFJAX_HOME%" == "" call setZBNFJAX_HOME.bat -silent
if "%ZBNFJAX_HOME%" == "" set ZBNFJAX_HOME=%CD%\..\..\zbnfjax
if exist %ZBNFJAX_HOME%\zbnf.jar goto :okzbnfjax
set ZBNFJAX_HOME=D:\vishia\ZBNF\zbnfjax
if exist %ZBNFJAX_HOME%\zbnf.jar goto :okzbnfjax
set ZBNFJAX_HOME=D:\XML_Tools\zbnfjax
if exist %ZBNFJAX_HOME%\zbnf.jar goto :okzbnfjax
echo environment ZBNFJAX_HOME not found. Please contact your administrator
pause
exit /B
:okzbnfjax

REM the execution of JZcmd is done with the next java invocation, using this file as parameter:
::java -cp "%ZBNFJAX_HOME%\zbnf.jar" org.vishia.zcmd.JZcmd genXMI_Zbnf.bat
%JAX_EXE% org.vishia.zcmd.JZcmd genXMI_Zbnf.bat
exit /B

==JZcmd==

!checkJZcmd=<:><&$TMP>/tmpJZcmd_CHECK_<&scriptfile>.xml<.>;

currdir=<:><&scriptdir><.>;
include $ZBNFJAX_HOME/zmake/genXMI_exec.jzcmd;

Filepath xmldir = D:/tmp/tmpXml;


Fileset src =
( org/vishia/zbnf/*.java        
, ../srcJava_vishiaBase:org/vishia/util/*.java          
, ../srcJava_vishiaBase:org/vishia/xmlSimple/*.java          
);



main(){
  
  //zmake $xmldir/*.xml := parseJava2Xml(..&src);

  zmake ../../rhapsody_UML/srcJava_Zbnf.xmi := genXMI(..:&src);

}

