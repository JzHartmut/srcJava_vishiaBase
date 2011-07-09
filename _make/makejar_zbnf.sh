#!/bin/bash

## The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
export TMP_JAVAC="../../../tmp_javac"

## Output jar-file with path and filename relative from current dir:
export OUTPUTFILE_JAVAC="../../zbnfjax/zbnf.jar"

## Manifest-file for jar building relativ path from current dir:
export MANIFEST_JAVAC="zbnf.manifest"

## Input for javac, only choice of primary sources, relativ path from current (make)-directory:
INPUT_JAVAC=""
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/zbnf/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/zmake/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/util/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/msgDispatch/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/header2Reflection/CmdHeader2Reflection.java"
export INPUT_JAVAC

## Sets the CLASSPATH variable for compilation (used jar-libraries). do not leaf empty also it aren't needed:
export CLASSPATH_JAVAC="nothing"

## Sets the src-path for further necessary sources:
export SRCPATH_JAVAC=".."

## Call java-compilation and jar within zbnfjax with given input environment:
#zbnfjax javacjar
javacjar.sh

