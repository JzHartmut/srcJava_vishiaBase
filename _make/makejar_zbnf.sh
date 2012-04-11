#!/bin/bash
##This script compiles Java files to build the zbnfjax/zbnf.jar. This file contains all files to run the ZBNF parser and all zbnfjax capabilities.
##It contains that sources from the srcJava_vishiaBase component which were need to run this.
##It is not the compilation of all capabilities of vishiaBase.


## The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
export TMP_JAVAC="../../../tmp_javac"

## Output jar-file with path and filename relative from current dir:
export OUTDIR_JAVAC="../../exe"
export JAR_JAVAC="zbnf.jar"

## Manifest-file for jar building relativ path from current dir:
export MANIFEST_JAVAC="zbnf.manifest"

## Input for javac, only choice of primary sources, relativ path from current (make)-directory:
INPUT_JAVAC=""
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/zbnf/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/zmake/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/header2Reflection/CmdHeader2Reflection.java"
export INPUT_JAVAC

## Sets the CLASSPATH variable for compilation (used jar-libraries). do not leaf empty also it aren't needed:
export CLASSPATH_JAVAC="nothing"

## Sets the src-path for further necessary sources:
export SRCPATH_JAVAC="..:../../srcJava_vishiaBase"

## Call java-compilation and jar within zbnfjax with given input environment:
#zbnfjax javacjar
../../srcJava_vishiaBase/_make/+javacjarbase.sh

