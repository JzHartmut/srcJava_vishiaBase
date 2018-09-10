#!/bin/bash
##Task: Compilation javac and call jar

if test -z "$INPUT_JAVAC"; then
  echo == javacjar.sh ==
  echo script to organize javac compilation and build the jar.
  echo The environment variables should be export as Input for this script:
  echo ---------------------------------------------------------------------------------------------
  echo JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation
  echo   if not set, it searches the JDK in some directories and set it so proper as well.
  echo TMP_JAVAC: Directory for all class files and logs: The directory will be cleaned and created
  echo INPUT_JAVAC: All primary input java files to compile separated with space
  echo CLASSPATH_JAVAC: PATH where compiled classes are found, relativ from current dir or absolute
  echo SRCPATH_JAVAC: PATH where sources are found, relativ from current dir or absolute
  echo OUTDIR_JAVAC: Path where the generated jar file will be stored. It may be a relative path.
  echo   It will be created if not found.
  echo JAR_JAVAC: file.jar
  echo MANIFEST_JAVAC: Path/file.manifest relative from currect dir for manifest file

  exit 1
fi

##--------------------------------------------------------------------------------------------
## Environment variables set from zbnfjax:

## The java-copiler may be located at a user-specified position.
## Set the environment variable JAVA_HOME, where bin/javac will be found.
if test -z "$JAVA_JDK";  then
  if test -d /usr/share/JDK; then export JAVA_JDK="/usr/share/JDK"; fi
  if test -d /d/Progs/JAVA/jdk1.6.0_21; then export JAVA_JDK="/d/Progs/JAVA/jdk1.6.0_21"; fi
fi
echo JAVA_JDK=$JAVA_JDK
#set PATH="$JAVA_JDK_HOME\bin:$PATH"


if test ! $OUTDIR_JAVAC; then mkdir $OUTDIR_JAVAC; fi

## Delete and create the tmp_javac new. It should be empty because all content will be stored in the jar-file.
if test -d $TMP_JAVAC; then rm -r $TMP_JAVAC; fi
mkdir -p $TMP_JAVAC/bin

## Delete the result jar-file
rm -f $OUTDIR_JAVAC/$JAR_JAVAC
rm -f $OUTDIR_JAVAC/$JAR_JAVAC.compile.log
rm -f $OUTDIR_JAVAC/$JAR_JAVAC.compile_error.log

echo === javac -sourcepath $SRCPATH_JAVAC -classpath $CLASSPATH_JAVAC $INPUT_JAVAC

$JAVA_JDK/bin/javac -deprecation -d $TMP_JAVAC/bin -sourcepath $SRCPATH_JAVAC -classpath $CLASSPATH_JAVAC $INPUT_JAVAC 1>>$TMP_JAVAC/javac_ok.txt 2>$TMP_JAVAC/javac_error.txt
if test $? -ge 1; then
  echo ===Compiler error
  cat $TMP_JAVAC/javac_ok.txt $TMP_JAVAC/javac_error.txt
  cat $TMP_JAVAC/javac_ok.txt $TMP_JAVAC/javac_error.txt >$OUTDIR_JAVAC/$JAR_JAVAC.compile_error.log
  exit 1
fi

## The jar works only correct, if the current directory contains the classfile tree:
## Store the actual directory to submit restoring on end
ENTRYDIR=$PWD
cd $TMP_JAVAC/bin
echo === SUCCESS compiling, generate jar: $ENTRYDIR/$OUTDIR_JAVAC/$JAR_JAVAC

$JAVA_JDK/bin/jar -cvfm $ENTRYDIR/$OUTDIR_JAVAC/$JAR_JAVAC $ENTRYDIR/$MANIFEST_JAVAC *  1>../jar_ok.txt 2>../jar_error.txt

cat ../jar_ok.txt ../jar_error.txt >$ENTRYDIR/$OUTDIR_JAVAC/$JAR_JAVAC.compile.log

if test $? -ge 1; then
  echo === ERROR jar
  cat ../jar_ok.txt ../jar_error.txt
  cd $ENTRYDIR
  exit 1
fi

## Restore current dir: $ENTRYDIR
cd $ENTRYDIR
echo === SUCCESS making $JAR_JAVAC in $OUTDIR_JAVAC.

