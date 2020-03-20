#this file is the user-adapt-able frame for makejar_vishiaBase.sh
#edit some settings if there are different form default.

export TMPJAVAC=/tmp/javac_vishiaBase/build/javac
#export JAVAC_HOME=c:/Programs/Java/jdk1.8.0_211
#export JAVAC_HOME=c:/Programs/Java/jdk1.8.0_241
export JAVAC_HOME=/usr/share/JDK/jdk1.8.0_241

#It determines only the file names, the TIME is inside makejar.sh
export VERSION="2020-03-20"

#use the yet compiled class to generate jar
export VISHIABASE=$TMPJAVAC/binjar
# SWT for Windows-64
##export CLASSPATH=../../../libs/org.eclipse.swt.win32.win32.x86_64_3.110.0.v20190305-0602.jar;../../../../cmpnJava_vishiaBase/libs/vishiaBase-2020-03-14.jar

export CLASSPATH=xx
# located from this workingdir as currdir for shell execution:
export SRCPATH=..
export MANIFEST=vishiaBase.manifest
# FILE1SRC=../org/vishia/jztxtcmd/JZtxtcmd.java
export SRC_ALL=..

#Output files
export DEPLOY=../vishiaBase-

chmod 777 makejar.sh
#now run the common script:
./makejar.sh




##Then build minisys:
export TMPJAVAC=/tmp/javac_vishiaMiniSys/build/javac
export DEPLOY=../vishiaMinisys-

#use the built before jar to generate jar
export VISHIABASE=../vishiaBase-$VERSION.jar

# located from this workingdir as currdir for shell execution:
export MANIFEST=minisys.manifest
# FILE1SRC=../org/vishia/jztxtcmd/JZtxtcmd.java
export SRC_ALL=""
export FILE1SRC=@minisys.files


#now run the common script:
./makejar.sh

