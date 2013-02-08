echo off
REM generating a jar file which contains all re-useable classes of the ZBNF-component 
REM inclusive its used classes from the component vishiaBase.
REM Note that the srcJava_vishiaBase component should be present in the correct version in parallel path of this component. 
REM examples of ZBNF are not included.

REM The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
set TMP_JAVAC=..\..\..\tmp_javac

REM Output dir and jar-file with path and filename relative from current dir:
REM The output dir is exe usually but zbnfjax if this file is compiled in the ZBNF download preparation.
set OUTDIR_JAVAC=..\..\exe
if exist ..\..\zbnfjax set OUTDIR_JAVAC=..\..\zbnfjax
set JAR_JAVAC=zbnf.jar


REM Manifest-file for jar building relativ path from current dir:
set MANIFEST_JAVAC=zbnf.manifest

REM Input for javac, only choice of primary sources, relativ path from current (make)-directory:
set INPUT_JAVAC=
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/bridgeC/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/byteData/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/byteData/reflection_Jc/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/cmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/mainCmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/util/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/msgDispatch/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/stateMachine/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/xml/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/xmlSimple/*.java

set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/byteData/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zbnf/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/checkDeps_C/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/stateMGen/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zTextGen/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zmake/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/header2Reflection/CmdHeader2Reflection.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/sclConversions/*.java


REM Sets the CLASSPATH variable for compilation (used jar-libraries). do not leaf empty also it aren't needed:
set CLASSPATH_JAVAC=nothing

REM Sets the src-path for further necessary sources:
set SRCPATH_JAVAC=..;../../srcJava_vishiaBase

..\..\srcJava_vishiaBase\_make\+javacjarbase.bat
