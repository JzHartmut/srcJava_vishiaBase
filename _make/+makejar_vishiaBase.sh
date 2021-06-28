echo script +makejar_vishiaBase.sh
echo script $0

#it compiles the srcJava_vishisBase to vishisMinisys.jar and vishiaBase.jar, see $DEPLOY

#build minisys:
#==============
#Do not change the version on repeated build, and check the checksum and content of jar.
#If it is equal, it is a reproduces build. The $VERSION is important 
#  because it determines the timestamp and hence the checksum in the jar file. 

#Set the version newly here to the current date if the sources are changed in jar and checksum.
#If the relevant sources are not change in functionality, may be changed in comment, 
#  it is not necessary the change this VERSION because the generated content is the same.
#The next line is the version for vishiaMiniSys:
export VERSION_MINISYS="2021-06-28"
export VERSION_VISHIABASE="2021-06-28"

#The SRCZIPFILE name will be written in MD5 file also for vishiaMiniSys.
#It should have anytime the stamp of the newest file, independing of the VERSION
export SRCZIPFILE="vishiaBase-$VERSION_VISHIABASE-source.zip"

#No further classpath necessary. 
#The CLASSPATH is used for reference jars for compilation which should be present on running too.
export CLASSPATH=xx

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

#$BUILD is the main build output directory. 
#possible to give $BUILD from outside. On argumentless call determine in tmp.
if test "$BUILD" = ""; then export BUILD="/tmp/BuildJava_vishiaBase"; fi

export VERSION=$VERSION_MINISYS

#to store temporary class files:
export TMPJAVAC=$BUILD/vishiaMiniSys/

#use the built before jar to generate jar. Hint: binjar is used in the core script.
export JAR_zipjar=$TMPJAVAC/binjar

#DEPLOY is the directory where the results are written.
if ! test -d $BUILD/deploy; then mkdir --parent $BUILD/deploy; fi
export DEPLOY="$BUILD/deploy/vishiaMinisys"

#now run the common script:
chmod 777 makejar.sh
./-makejar-coreScript.sh

#use the successfull built minisys for jar 
export JAR_zipjar=$DEPLOY-$VERSION.jar





#Build the whole vishiaBase
#==========================
#Note: See comment on VERSION above, same procedure!
#Hint: This is the whole jar, 
export VERSION=$VERSION_VISHIABASE
export TMPJAVAC=$BUILD/vishiaBase

#Output files
export DEPLOY="$BUILD/deploy/vishiaBase"

#use the built before jar to generate jar

export MANIFEST=vishiaBase.manifest
export SRC_ALL=".."
if test -d ../../srcJava_vishiaRun; then export SRC_ALL2="../../srcJava_vishiaRun"
else export SRC_ALL2="../../../../../../cmpnJava_vishiaRun/src/main/java/srcJava_vishiaRun"
fi
export FILE1SRC=""

#now run the common script:
./-makejar-coreScript.sh

if test -f $DEPLOY-$VERSION.jar -a -d D:/vishia/Java/libStd; then
  cp $DEPLOY-$VERSION.jar D:/vishia/Java/libStd/vishiaBase.jar
  ls -l D:/vishia/Java/libStd
fi  





