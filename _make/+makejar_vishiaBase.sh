#this file is the user-adapt-able frame for makejar_vishiaBase.sh
#edit some settings if there are different form default.

#export JAVAC_HOME=c:/Programs/Java/jdk1.8.0_211
export JAVAC_HOME=c:/Programs/Java/jdk1.8.0_241
#export JAVAC_HOME=/usr/share/JDK/jdk1.8.0_241

export CLASSPATH=xx
# located from this workingdir as currdir for shell execution:
export SRCPATH=..

##TODO: on next reproducible mismatch use Timestamp +00:00. 
##      If it is done for all, remove TIME to the core script.
##      =>Lesser complexity 


##build zipjar:
#Note: on changes firstly build with the same given date here.
#Then check the checksum. If it matches, this VERSION date is ok.
#For example if only a comment is changed in sources or nothing is changed,
#the repeated built should deliver a reproducible build.
#-----
#If the checksum and the content does not match, 
# then set the new date here from the last timestamp of the last commit or changed file.


if test "$BUILD" = ""; then export BUILD="/tmp/BuildJava_vishiaBase"; fi


##build minisys:
export VERSION="2020-06-16"
##Note: Use the same path as in TestJava_vishiaBase for link "build", see +mkLinkBuild.bat or +mkLinkBuild.sh
export TMPJAVAC=$BUILD/vishiaMiniSys/
export DEPLOY=vishiaMinisys
#use the built before jar to generate jar. Hint: binjar is used in the core script.
export JAR_zipjar=$TMPJAVAC/binjar


# located from this workingdir as currdir for shell execution:
export MANIFEST=minisys.manifest
export SRC_ALL=""
export FILE1SRC="@minisys.files"

#now run the common script:
./-makejar-coreScript.sh



#use the successfull builld minisys for jar 
export JAR_zipjar=$TMPJAVAC/result/vishiaMinisys-$VERSION.jar

##Build the whole vishiaBase
#Note: See comment on VERSION above, same procedure!
#Hint: This is the whole jar, changes are expectable.
export VERSION="2020-06-16"
export TMPJAVAC=$BUILD/vishiaBase

#Output files
export DEPLOY=vishiaBase

#use the built before jar to generate jar

export MANIFEST=vishiaBase.manifest
export SRC_ALL=".."

#now run the common script:
./-makejar-coreScript.sh





