echo =========================================================================
echo execute  $0
echo " ... generates the vishiaBase.jar from srcJava_vishiaBase core sources"

#it compiles the srcJava_vishisBase to vishisMinisys.jar and vishiaBase.jar

#Set the version newly here to the current date if the sources are changed in jar and checksum.
#If the relevant sources are not change in functionality, may be changed in comment, 
#  it is not necessary the change this VERSIONSTAMP because the generated content is the same.
#The next line is the version for vishiaMiniSys:

## The VERSIONSTAMP can come form calling script, elsewhere it is set with the current date.
## This determines the names of the results, but not the content and not the MD5 check sum.
if test "$VERSIONSTAMP" = ""; then export VERSIONSTAMP=$(date -I); fi   ## writes current date
export VERSION_MINISYS=$VERSIONSTAMP
export VERSION_VISHIABASE=$VERSIONSTAMP

## Determines the timestamp of the files in the jar. The timestamp determines also
## the MD5 check code. 
## Do not change the version on repeated build, and check the checksum and content of jar.
## If it is equal, it is a reproduces build. The $VERSIONSTAMP is important 
##  because it determines the timestamp and hence the checksum in the jar file. 
## Using another timestamp on equal result files forces another MD5.
## Hence let this unchanged in comparison to a pre-version 
## if it is assumed that the sources are unchanged.
## Only then a comparison of MD5 is possible. 
## The comparison byte by byte inside the jar (zip) file is always possible.
## Use this timestamp for file in jars, influences the MD5 check:
export TIMEinJAR_MINISYS="2021-07-01+00:00"
export TIMEinJAR_VISHIABASE="2021-07-09+00:00"   

#The SRCZIPFILE name will be written in MD5 file also for vishiaMiniSys.
#It should have anytime the stamp of the newest file, independing of the VERSIONSTAMP
export SRCZIPFILE="vishiaBase-$VERSION_VISHIABASE-source.zip"

#No further classpath necessary. 
#The CLASSPATH is used for reference jars for compilation which should be present on running too.
export CLASSPATH=xx

#build minisys:
#==============
#determine the sources:
export SRC_ALL=""
export SRC_ALL2=""
export FILE1SRC="@minisys.files" #files to compile contained in this file

# srcpath located from this workingdir as currdir for shell execution:
export SRCPATH=..

# Resourcefiles for files in the jar
export RESOURCEFILES=""

# located from this workingdir as currdir for shell execution:
export MANIFEST=minisys.manifest

#$BUILD_TMP is the main build output directory. 
#possible to give $BUILD_TMP from outside. On argumentless call determine in tmp.
if test "$BUILD_TMP" = ""; then export BUILD_TMP="/tmp/BuildJava_vishiaBase"; fi

export VERSIONSTAMP="$VERSION_MINISYS"
export TIMEinJAR="$TIMEinJAR_MINISYS"

#to store temporary class files:
export TMPJAVAC=$BUILD_TMP/vishiaMiniSys/

#use the built before jar to generate jar. Hint: binjar is used in the core script.
export JAR_zipjar=$TMPJAVAC/binjar

if ! test -d $BUILD_TMP/deploy; then mkdir --parent $BUILD_TMP/deploy; fi
export DSTNAME="vishiaMinisys"

#now run the common script:
chmod 777 "./-makejar-coreScript.sh"
./-makejar-coreScript.sh

# Deploy the result
if test ! -f $BUILD_TMP/deploy/vishiaMinisys-$VERSIONSTAMP.jar; then   ##compilation not successfull
  echo "?????? compiling ERROR, abort ????????????????????????" 
  exit 255
fi

#use the successfull built minisys for jar 
export JAR_zipjar=$BUILD_TMP/deploy/vishiaMinisys-$VERSIONSTAMP.jar




#Build the whole vishiaBase
#==========================
#Note: See comment on VERSIONSTAMP above, same procedure!
#Hint: This is the whole jar, 
export VERSIONSTAMP=$VERSION_VISHIABASE
export TIMEinJAR="$TIMEinJAR_VISHIABASE"
export TMPJAVAC=$BUILD_TMP/vishiaBase

#Output files
export DSTNAME="vishiaBase"

#use the built before jar to generate jar

export MANIFEST=vishiaBase.manifest
export SRC_ALL=".."
##if test -d ../../srcJava_vishiaRun; then export SRC_ALL2="../../srcJava_vishiaRun"
##else export SRC_ALL2="../../../../../../cmpnJava_vishiaRun/src/main/java/srcJava_vishiaRun"
##fi
export FILE1SRC=""

#now run the common script:
./-makejar-coreScript.sh


# Deploy the result
if test ! -f $BUILD_TMP/deploy/vishiaMinisys-$VERSIONSTAMP.jar; then   ##compilation not successfull
  echo "?????? compiling ERROR, abort ????????????????????????" 
  exit 255
else                                                       ##compilation not successfull
  ##
  ## copy the useable version to a existing libstd directory:
  if test -d ../../../../../../libstd; then ##beside cmpnJava... should be existing
    export CURRENT_JARS_PATH="../../../../../../libStd" 
  else
    export CURRENT_JARS_PATH="../../jars" 
    if ! test -d $CURRENT_JARS_PATH; then mkdir $CURRENT_JARS_PATH; fi
  fi  
  if test -v CURRENT_JARS_PATH; then
    echo test and correct the bom file: JZtxtcmd corrBom.jzTc $CURRENT_JARS_PATH $BUILD_TMP/deploy vishiaBase $VERSIONSTAMP
    java -cp $BUILD_TMP/deploy/vishiaBase-$VERSIONSTAMP.jar org.vishia.jztxtcmd.JZtxtcmd corrBom.jzTc $CURRENT_JARS_PATH $BUILD_TMP/deploy vishiaBase $VERSIONSTAMP
    echo ========================================================================== $?
    if test ! -f $CURRENT_JARS_PATH/vishiaBase*_old.jar; then
      echo "BOM not changed, unchanged MD5"
    else
      cp $BUILD_TMP/deploy/vishiaBase-$VERSIONSTAMP.jar $CURRENT_JARS_PATH/vishiaBase.jar    
      echo create BOM file $CURRENT_JARS_PATH/bomVishiaJava.new.txt
      ls -l $CURRENT_JARS_PATH
      ##
      ## copy to the deploy directory.
      if test -d ../../../../../../deploy; then
        cp $BUILD_TMP/deploy/vishiaBase-$VERSIONSTAMP* ../../../../../../deploy
      fi
      if test -d ../../deploy; then
        cp $BUILD_TMP/deploy/vishiaBase-$VERSIONSTAMP* ../../deploy
      fi  
    fi  
    echo ======= success ==========================================================
  fi  
fi  


# Deploy the result minisys, can be done only after compilation and deploy vishiaBase.jar
  ##
  ## copy the useable version to a existing libstd directory:
  if test -d ../../../../../../libstd; then ##beside cmpnJava... should be existing
    export CURRENT_JARS_PATH="../../../../../../libStd" 
  else
    export CURRENT_JARS_PATH="../../jars" 
    if ! test -d $CURRENT_JARS_PATH; then mkdir $CURRENT_JARS_PATH; fi
  fi  
  if test -v CURRENT_JARS_PATH; then
    echo test and correct the bom file: JZtxtcmd corrBom.jzTc $CURRENT_JARS_PATH $BUILD_TMP/deploy vishiaMinisys $VERSIONSTAMP
    java -cp $CURRENT_JARS_PATH/vishiaBase.jar org.vishia.jztxtcmd.JZtxtcmd corrBom.jzTc $CURRENT_JARS_PATH $BUILD_TMP/deploy vishiaMinisys $VERSIONSTAMP
    echo ========================================================================== $?
    if test ! -f $CURRENT_JARS_PATH/vishiaMinisys*old.jar; then
      echo "BOM not changed, unchanged MD5"
    else
      cp $BUILD_TMP/deploy/vishiaMinisys-$VERSIONSTAMP.jar $CURRENT_JARS_PATH/vishiaMinisys.jar    
      echo create BOM file $CURRENT_JARS_PATH/bomVishiaJava.new.txt
      ls -l $CURRENT_JARS_PATH
      ##
      ## copy to the deploy directory 
      if test -d ../../../../../../deploy; then
        cp $BUILD_TMP/deploy/vishiaMinisys-$VERSIONSTAMP* ../../../../../../deploy
      fi
      if test -d ../../deploy; then
        cp $BUILD_TMP/deploy/vishiaMinisys-$VERSIONSTAMP* ../../deploy
      fi  
    fi  
    echo ======= success ==========================================================
  fi  


