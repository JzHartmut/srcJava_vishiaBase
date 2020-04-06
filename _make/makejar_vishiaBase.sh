#this file is the user-adapt-able frame for makejar_vishiaBase.sh
#edit some settings if there are different form default.

#export JAVAC_HOME=c:/Programs/Java/jdk1.8.0_211
export JAVAC_HOME=c:/Programs/Java/jdk1.8.0_241
#export JAVAC_HOME=/usr/share/JDK/jdk1.8.0_241

export CLASSPATH=xx
# located from this workingdir as currdir for shell execution:
export SRCPATH=..




##build zipjar:
export VERSIONZIPJAR=version  //"2020-04-04"
export VERSION="$VERSIONZIPJAR"   #generate exact this version   
export TIME="2020-04-04+02:03"    #check generated content!
export TMPJAVAC=/tmp/javac_vishiaZipJar/build/javac
export DEPLOY=../vishiaZipJar-

#use the built before jar to generate jar
export JAR_zipjar=$TMPJAVAC/binjar

# located from this workingdir as currdir for shell execution:
export MANIFEST=zipjar.manifest
export FILE1SRC=../org/vishia/zip/Zip.java
export SRC_ALL=""

#now run the common script:
chmod 777 makejar.sh
./makejar.sh




##build minisys:
export VERSION="2020-03-24"    ##use inside makejar.sh!
export TIME="2020-03-24+02:02"
export TMPJAVAC=/tmp/javac_vishiaMiniSys/build/javac
export DEPLOY=../vishiaMinisys-

#use the built before jar to generate jar
export JAR_zipjar=../vishiaZipJar-$VERSIONZIPJAR.jar

# located from this workingdir as currdir for shell execution:
export MANIFEST=minisys.manifest
# FILE1SRC=../org/vishia/jztxtcmd/JZtxtcmd.java
export SRC_ALL=""
export FILE1SRC="@minisys.files"
#export FILE1SRC=../org/vishia/minisys/GetWebfile.java

#now run the common script:
./makejar.sh




##Build the whole vishiaBase
export VERSION="2020-04-04"    ##use inside makejar.sh!
export TIME="2020-04-04+02:03"
export TMPJAVAC=/tmp/javac_vishiaBase/build/javac

#Output files
export DEPLOY=../vishiaBase-

#use the built before jar to generate jar
export JAR_zipjar=../vishiaZipJar-$VERSIONZIPJAR.jar

export MANIFEST=vishiaBase.manifest
export SRC_ALL=..

#now run the common script:
./makejar.sh





