echo =========================================================================
echo execute  $0
echo " ... generates the vishiaBase.jar from srcJava_vishiaBase core sources"

#it compiles the srcJava_vishisBase to vishisMinisys.jar and vishiaBase.jar

#Set the version newly here to the current date if the sources are changed in jar and checksum.
#If the relevant sources are not change in functionality, may be changed in comment, 
#  it is not necessary the change this VERSIONSTAMP because the generated content is the same.
#The next line is the version for vishiaMiniSys. Change it only if the content of generated MiniSys.jar is changed.
export VERSIONSTAMP="2022-05-26"
export VERSION_MINISYS="2022-05-26"

## The VERSIONSTAMP can come form calling script, elsewhere it is set with the current date.
## This determines the names of the results, but not the content and not the MD5 check sum.
## See $TIMEinJAR_VISHIABASE in next block.
if test "$VERSIONSTAMP" = ""; then export VERSIONSTAMP=$(date -I); fi   ## writes current date
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
export TIMEinJAR_MINISYS="2022-05-23+00:00"
export TIMEinJAR_VISHIABASE="2022-05-23+00:00"
##Note: The next is worse because it prevents reproducible results:
##export TIMEinJAR_VISHIABASE="$VERSIONSTAMP+00:00"   

#The SRCZIPFILE name will be written in MD5 file also for vishiaMiniSys.
#It should have anytime the stamp of the newest file, independing of the VERSIONSTAMP
export SRCZIPFILE="vishiaBase-$VERSION_VISHIABASE-source.zip"

export MAKEBASEDIR="."

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
export RESOURCEFILES="..:**/*.zbnf ..:**/*.txt"

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

#now run the common script for MiniSys:
chmod 777 $MAKEBASEDIR/-makejar-coreScript.sh
chmod 777 $MAKEBASEDIR/-deployJar.sh
$MAKEBASEDIR/-makejar-coreScript.sh


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

#now run the common script for vishiaBase:
$MAKEBASEDIR/-makejar-coreScript.sh

#deploy:
export JAR_vishiaBase=$BUILD_TMP/deploy/vishiaBase-$VERSIONSTAMP.jar
$MAKEBASEDIR/-deployJar.sh

#deploy also the minisys:
export VERSIONSTAMP="$VERSION_MINISYS"
export TMPJAVAC=$BUILD_TMP/vishiaMiniSys/
export DSTNAME="vishiaMinisys"
$MAKEBASEDIR/-deployJar.sh




