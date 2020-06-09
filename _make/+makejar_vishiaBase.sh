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
export VERSION="2020-06-09"
#Note: The $VERSIONZIPJAR is used for the further jar builds.
export VERSIONZIPJAR="$VERSION"   #generate exact this version   
export TIME="$VERSION+00:00"    #check generated content!
export TMPJAVAC=/tmp/javac_vishiaZipJar/build/javac
export DEPLOY=vishiaZipJar

#use the built before jar to generate jar
export JAR_zipjar=$TMPJAVAC/binjar

# located from this workingdir as currdir for shell execution:
export MANIFEST=zipjar.manifest
export FILE1SRC=../org/vishia/zip/Zip.java
export SRC_ALL=""

#now run the common script:
chmod 777 makejar.sh
./-makejar-coreScript.sh




##build minisys:
#Note: See comment on TIME above, same procedure!
export VERSION="2020-03-24"
export TIME="$VERSION+02:02"
export TMPJAVAC=/tmp/javac_vishiaMiniSys/build/javac
export DEPLOY=vishiaMinisys

#use the built before jar to generate jar
export JAR_zipjar=../build/vishiaZipJar-$VERSIONZIPJAR.jar

# located from this workingdir as currdir for shell execution:
export MANIFEST=minisys.manifest
# FILE1SRC=../org/vishia/jztxtcmd/JZtxtcmd.java
export SRC_ALL=""
export FILE1SRC="@minisys.files"
#export FILE1SRC=../org/vishia/minisys/GetWebfile.java

#now run the common script:
./-makejar-coreScript.sh




##Build the whole vishiaBase
#Note: See comment on VERSION above, same procedure!
#Hint: This is the whole jar, changes are expectable.
export VERSION="2020-06-09"
export TIME="$VERSION+00:00"
export TMPJAVAC=/tmp/javac_vishiaBase/build/javac

#Output files
export DEPLOY=vishiaBase

#use the built before jar to generate jar
export JAR_zipjar=../build/vishiaZipJar-$VERSIONZIPJAR.jar

export MANIFEST=vishiaBase.manifest
export SRC_ALL=..

#now run the common script:
./-makejar-coreScript.sh





