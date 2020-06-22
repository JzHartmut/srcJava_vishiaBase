echo script +makejar_vishiaBase.sh
echo script $0

#it compiles the srcJava_vishisBase to vishisMinisys.jar and vishiaBase.jar, see $DEPLOY

#$BUILD is the main build output directory. 
#possible to give $BUILD from outside. On argumentless call determine in tmp.
if test "$BUILD" = ""; then export BUILD="/tmp/BuildJava_vishiaBase"; fi

#No further classpath necessary. 
export CLASSPATH=xx

# srcpath located from this workingdir as currdir for shell execution:
export SRCPATH=..

##TODO: on next reproducible mismatch use Timestamp +00:00. 
##      If it is done for all, remove TIME to the core script.
##      =>Lesser complexity 


#build minisys:
#==============
#Do not change the version on repeated build, and check the checksum and content of jar.
#If it is equal, it is a reproduces build. The $VERSION is important 
#  because it determines the timestamp and hence the checksum in the jar file. 
#Set the version newly here to the current date if the sources are changed in jar and checksum.
export VERSION="2020-06-22"

#to store temporary class files:
export TMPJAVAC=$BUILD/vishiaMiniSys/

#use the built before jar to generate jar. Hint: binjar is used in the core script.
export JAR_zipjar=$TMPJAVAC/binjar


# located from this workingdir as currdir for shell execution:
export MANIFEST=minisys.manifest
#determine the sources:
export SRC_ALL=""
export SRC_ALL2=""
export FILE1SRC="@minisys.files" #files to compile contained in this file

if ! test -d $BUILD/deploy; then mkdir $BUILD/deploy; fi
export DEPLOY="$BUILD/deploy/vishiaMinisys-$VERSION"

#now run the common script:
./-makejar-coreScript.sh

#use the successfull built minisys for jar 
export JAR_zipjar=$DEPLOY.jar





#Build the whole vishiaBase
#==========================
#Note: See comment on VERSION above, same procedure!
#Hint: This is the whole jar, 
export VERSION="2020-06-22"
export TMPJAVAC=$BUILD/vishiaBase

#Output files
export DEPLOY="$BUILD/deploy/vishiaBase-$VERSION"

#use the built before jar to generate jar

export MANIFEST=vishiaBase.manifest
export SRC_ALL=".."
export FILE1SRC=""

#now run the common script:
./-makejar-coreScript.sh





