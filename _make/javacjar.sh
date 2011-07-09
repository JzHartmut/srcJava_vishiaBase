#!/bin/bash
##Task: Compilation javac and call jar inside the zbnfJax shell

##--------------------------------------------------------------------------------------------
## Environment variables set from zbnfjax:
## JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation

##---------------------------------------------------------------------------------------------
## Environment variables should be set as Input before calling zbnfjax or this script:
## TMP_JAVAC: Directory for all class files and logs: The directory will be cleaned and created
## INPUT_JAVAC: All primary input java files to compile separated with space
## CLASSPATH_JAVAC: PATH where compiled classes are found, relativ from current dir or absolute
## SRCPATH_JAVAC: PATH where sources are found, relativ from current dir or absolute
## OUTPUTFILE_JAVAC: Path/file.jar relative from current diretory for the jar-file
## MANIFEST_JAVAC: Path/file.manifest relative from currect dir for manifest file

## Delete and create the tmp_javac new. It should be empty because all content will be stored in the jar-file.
if test -d $TMP_JAVAC; then rm -r $TMP_JAVAC; fi
mkdir -p $TMP_JAVAC/bin

## Delete the result jar-file
rm -f $OUTPUTFILE_JAVAC
rm -f $OUTPUTFILE_JAVAC.ERROR.txt

echo === javac -sourcepath $SRCPATH_JAVAC -classpath $CLASSPATH_JAVAC $INPUT_JAVAC

$JAVA_JDK/bin/javac -deprecation -d $TMP_JAVAC/bin -sourcepath $SRCPATH_JAVAC -classpath $CLASSPATH_JAVAC $INPUT_JAVAC 1>>$TMP_JAVAC/javac_ok.txt 2>$TMP_JAVAC/javac_error.txt
if test $? -ge 1; then
  echo ===Compiler error
  cat $TMP_JAVAC/javac_ok.txt $TMP_JAVAC/javac_error.txt
  cat $TMP_JAVAC/javac_ok.txt $TMP_JAVAC/javac_error.txt >$OUTPUTFILE_JAVAC.ERROR.txt
  exit 1
fi

## The jar works only correct, if the current directory contains the classfile tree:
## Store the actual directory to submit restoring on end
ENTRYDIR=$PWD
cd $TMP_JAVAC/bin
echo === SUCCESS compiling, generate jar: $ENTRYDIR/$OUTPUTFILE_JAVAC

$JAVA_JDK/bin/jar -cvfm $ENTRYDIR/$OUTPUTFILE_JAVAC $ENTRYDIR/$MANIFEST_JAVAC *  1>../jar_ok.txt 2>../jar_error.txt

if test $? -ge 1; then
  echo === ERROR jar
  cat ../jar_ok.txt ../jar_error.txt
  cat ../jar_ok.txt ../jar_error.txt >$ENTRYDIR/$OUTPUTFILE_JAVAC.ERROR.txt
  cd $ENTRYDIR
  exit 1
fi

## Restore current dir: $ENTRYDIR
cd $ENTRYDIR
echo === SUCCESS making $OUTPUTFILE_JAVAC.

