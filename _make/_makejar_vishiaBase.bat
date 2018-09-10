echo off
REM generating a jar file which contains all re-useable classes of the vishiaBase-component.
REM examples are not compiled.

REM The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
set TMP_JAVAC=..\..\..\tmp_javac

REM Output jar-file with path and filename relative from current dir:
set JAR_JAVAC=vishiaBase.jar
set OUTDIR_JAVAC=..\..\exe

REM Manifest-file for jar building relativ path from current dir:
set MANIFEST_JAVAC=vishiaBase.manifest

REM Input for javac, only choice of primary sources, relativ path from current (make)-directory:
set INPUT_JAVAC=
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/bridgeC/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/byteData/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/byteData/reflection_Jc/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/cmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/mainCmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/util/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/msgDispatch/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/stateMachine/*.java
::set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/xml/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/xmlSimple/*.java


REM Sets the CLASSPATH variable for compilation (used jar-libraries). do not leaf empty also it aren't needed:
set CLASSPATH_JAVAC=nothing

REM Sets the src-path for further necessary sources:
set SRCPATH_JAVAC=..

call .\+javacjarbase.bat
