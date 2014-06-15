@echo off
jzcmd %0
exit /B

==JZcmd== 

include $ZBNFJAX_HOME/zmake/javac.jzcmd;

currdir = scriptdir;
               
## Note: get all files to compile
##because the last version of zbnf.jar itself is present while compiling 
##and should not resolve dependencies.


Fileset src = ( 
  ../../srcJava_vishiaBase:org/vishia/bridgeC/*.java
, ../../srcJava_vishiaBase:org/vishia/byteData/*.java
, ../../srcJava_vishiaBase:org/vishia/byteData/reflection_Jc/*.java
, ../../srcJava_vishiaBase:org/vishia/cmd/*.java
, ../../srcJava_vishiaBase:org/vishia/event/*.java
, ../../srcJava_vishiaBase:org/vishia/fileLocalAccessor/*.java
, ../../srcJava_vishiaBase:org/vishia/fileRemote/*.java
, ../../srcJava_vishiaBase:org/vishia/mainCmd/*.java
, ../../srcJava_vishiaBase:org/vishia/msgDispatch/*.java
, ../../srcJava_vishiaBase:org/vishia/stateMachine/*.java
, ../../srcJava_vishiaBase:org/vishia/util/*.java
, ../../srcJava_vishiaBase:org/vishia/xmlSimple/*.java
, ..:org/vishia/byteData/*.java
, ..:org/vishia/checkDeps_C/*.java
, ..:org/vishia/header2Reflection/*.java
, ..:org/vishia/sclConversions/*.java
, ..:org/vishia/stateMGen/*.java
, ..:org/vishia/zbnf/*.java
, ..:org/vishia/zcmd/*.java
);


Filepath dst=../../exe/zbnf.jar;

Map cc;  ##compileCondition

##String cc.TMP_JAVAC=<:><&$TMP>/tmp_javac<.>;
String cc.TMP_JAVAC=<:>D:/TMP/tmp_javac<.>;

REM Manifest-file for jar building relativ path from current dir:
String cc.MANIFEST_JAVAC="zbnf.manifest";


main()
{
  zmake &dst := javacjar( &src, cc=cc);
  //copy to running jar don't do it?
  //copy src=&dst dst=<:><&$ZBNFJAX_HOME>/zbnf.jar<.>; 
}
