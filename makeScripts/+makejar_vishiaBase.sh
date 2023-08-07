echo ====== start script ===============================================================
echo execute  $0
## Set the current dir 3 level before the script, it sees the src/srcDir/makeScripts
cd $(dirname $0)/../../..
echo currdir $PWD
export DSTNAME="vishiaBase"
echo " ... generates the $DSTNAME.jar from srcJava_$DSTNAME core sources"

#Do not change the version on repeated build, and check the checksum and content of jar.
#If it is equal, it is a reproduces build. The $VERSIONSTAMP is important 
#  because it determines the timestamp and hence the checksum in the jar file. 
export VERSIONSTAMP="2023-08-07"

## Determine a dedicated vishiaBase-yyyy-mm-dd.jar or deactivate it to use the current vishiaBase.jar:
export VERSION_VISHIABASE=$VERSIONSTAMP


#The next line is the version for vishiaMiniSys. Change it only if the content of generated MiniSys.jar is changed.
export VERSION_MINISYS="2023-08-07"

## The VERSIONSTAMP can come form calling script, elsewhere it is set with the current date.
## This determines the names of the results, but not the content and not the MD5 check sum.
## See $TIMEinJAR_VISHIABASE in next block.
if test "$VERSIONSTAMP" = ""; then export VERSIONSTAMP=$(date -I); fi   ## writes current date

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
export TIMEinJAR_MINISYS="$VERSION_MINISYS+00:00"
export TIMEinJAR_VISHIABASE=""   ##get from $VERSIONSTAMP
##Note: The next is worse because it prevents reproducible results:
##export TIMEinJAR_VISHIABASE="$VERSIONSTAMP+00:00"   

## This directory is the source directory for this component to create a jar
export SRCDIRNAME="src/srcJava_vishiaBase"  ##must proper to the own location

## This directory contains some basic scripts. Should be exists
export MAKEBASEDIR="src/srcJava_vishiaBase/makeScripts"     ##must proper in the own location

#The SRCZIPFILE name will be written in MD5 file also for vishiaMiniSys.
# It should have anytime the stamp of the newest file, independing of the VERSIONSTAMP
export SRCZIPFILE="$DSTNAME-$VERSIONSTAMP-source.zip"


#No further classpath necessary. 
#The CLASSPATH is used for reference jars for compilation which should be present on running too.
export CLASSPATH=xx


#use the built before jar to generate jar. This is a special solution for vishiaBase.jar compilation.
#It is the tool for zip and jar used inside the core script
export JAR_zipjar="__vishiaBase_CLASSfiles__"

export JAR_vishiaBase=""    ## set same as $JAR_zipjar

#Build the whole vishiaBase
#==========================
#Note: See comment on VERSIONSTAMP above, same procedure!
#Hint: This is the whole jar, 
export VERSIONSTAMP=$VERSION_VISHIABASE
export TIMEinJAR="$TIMEinJAR_VISHIABASE"


export MANIFEST=$SRCDIRNAME/makeScripts/$DSTNAME.manifest

##This selects the files to compile
export SRC_MAKE="$SRCDIRNAME/makeScripts" 
export SRC_ALL="$SRCDIRNAME/java"
unset SRC_ALL2
unset FILE1SRC    ##left empty to compile all sources

##This is the path to find sources for javac, maybe more comprehensive as SRC_ALL
unset SRCPATH       ##set it with SRC_ALL;SRC_ALL2

# Resourcefiles for files in the jar
export RESOURCEFILES="$SRC_ALL:**/*.zbnf $SRC_ALL:**/*.txt $SRC_ALL:**/*.xml $SRC_ALL:**/*.png"


#now run the common script:
# The DEPLOYSCRIPT will be executed after generation in the coreScript if given and found.
export DEPLOYSCRIPT="$MAKEBASEDIR/-deployJar.sh"
echo DEPLOYSCRIPT=$DEPLOYSCRIPT

chmod 777 $MAKEBASEDIR/-makejar-coreScript.sh
chmod 777 $DEPLOYSCRIPT
$MAKEBASEDIR/-makejar-coreScript.sh

echo
echo
echo
echo
echo
echo


echo ====== build minisys: =============================================================
#==============
#determine the sources:
export DSTNAME="vishiaMinisys"
unset SRC_ALL   ##do not build a zio file
unset SRC_ALL2
export SRCPATH="$SRCDIRNAME/java"
export FILE1SRC="@$SRCDIRNAME/makeScripts/minisys.files" #files to compile contained in this file

# located from this workingdir as currdir for shell execution:
export MANIFEST=$SRCDIRNAME/makeScripts/minisys.manifest

export JAR_vishiaBase="tools/vishiaBase.jar"    ## set same as $JAR_zipjar

export VERSIONSTAMP="$VERSION_MINISYS"
export TIMEinJAR="$TIMEinJAR_MINISYS"
unset SRCZIPFILE=""         ## do not zip this sources

#now run the common script for MiniSys:
# The DEPLOYSCRIPT will be executed after generation in the coreScript.
export DEPLOYSCRIPT="$MAKEBASEDIR/-deployJar.sh"
$MAKEBASEDIR/-makejar-coreScript.sh

