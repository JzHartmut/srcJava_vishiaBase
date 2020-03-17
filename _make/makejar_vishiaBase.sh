#this file is the user-adapt-able frame for makejar_vishiaBase.sh
#edit some settings if there are different form default.

export TMPJAVAC=$TMP/javac_vishiaBase/build/javac
export JAVAC_HOME=c:/Programs/Java/jdk1.8.0_211

# SWT for Windows-64
##export CLASSPATH=../../../libs/org.eclipse.swt.win32.win32.x86_64_3.110.0.v20190305-0602.jar;../../../../cmpnJava_vishiaBase/libs/vishiaBase-2020-03-14.jar

export CLASSPATH=xx
export VERSION=2020-03-17
export JARFILE=../vishiaBase-$VERSION.jar.zip
export MD5FILE=../vishiaBase-$VERSION.jar.MD5

# located from this workingdir as currdir for shell execution:
export SRCPATH=..
export MANIFEST=MANIFEST.MF
# FILE1SRC=../org/vishia/jztxtcmd/JZtxtcmd.java
export SRC_ALL=..

#now run the common script:
./makejar.sh

