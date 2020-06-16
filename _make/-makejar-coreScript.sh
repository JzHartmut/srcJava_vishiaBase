#shell script to generate jar file
#it can be run under Windows using MinGW: sh.exe - thisScript.sh
#MinGW is part of git, it should be known for Gcc compile too.

##Both variables should be corrected for any new version, 
##if is used for gradle build and for shell build!

#determine out file names from VERSION
export JARFILE=$TMPJAVAC/result/$DEPLOY-$VERSION.jar
export MD5FILE=$TMPJAVAC/result/$DEPLOY-$VERSION.jar.MD5.txt
export SRCZIPFILE=$TMPJAVAC/result/$DEPLOY-$VERSION-source.zip

if test "$TIME" = ""; then export TIME="$VERSION+00:00"; fi

echo compile java and generate jar with binary-compatible content. 
echo JAVAC_HOME = $JAVAC_HOME
echo DEPLOY = $DEPLOY  - output file names
echo TIME = $TIME  - determine timestamp in jar
echo SRC_ALL = $SRC_ALL  - gather all *.java there
echo SRC_ALL2 = $SRC_ALL2  - gather all *.java there
echo FILE1SRC = $FILE1SRC  - argument files for javac
echo RESOURCEFILES = $RESOURCEFILES  - additional files in jar
echo SRCPATH = $SRCPATH  - search path sources for javac
echo CLASSPATH = $CLASSPATH
echo JAR_zipjar = $JAR_zipjar  - jar file for jar generation
echo TMPJAVAC =  $TMPJAVAC  - temporary files while compilation
echo JARFILE = $JARFILE  - generated jar
echo MD5FILE = $MD5FILE  - generated MD5 text file

if test "$JAVAC_HOME" = ""; then
  echo you must set JAVAC_HOME in your system to the installed JDK
  exit 5
fi
# clean the build dir because maybe old faulty content:
if test -d $TMPJAVAC; then rm -f -r -d $TMPJAVAC; fi
mkdir -p $TMPJAVAC/binjar
mkdir $TMPJAVAC/result
echo
echo Output to: $TMPJAVAC/result
echo ===============================================================

##Automatic build a zip file if SRC_ALL and maybe additionally SRC_ALL2 is given.
##SRC_ALL refers to the java package path root directory,
##but the source.zip should contain the parent folder which is srcJava_xyz/org/... 
export SRCZIP=""
if ! test "$SRC_ALL" = ""; then
  echo source-set all files = $SRC_ALL
  find $SRC_ALL -name "*.java" > $TMPJAVAC/sources.txt
  export FILE1SRC=@$TMPJAVAC/sources.txt
  export SRCZIP=$SRC_ALL/..:**/*  ## with the srcJava_... dir
fi  
if ! test "$SRC_ALL2" = ""; then
  echo source-set all files = $SRC_ALL2
  find $SRC_ALL2 -name "*.java" >> $TMPJAVAC/sources.txt
  export FILE1SRC=@$TMPJAVAC/sources.txt
  export SRCZIP="$SRCZIP $SRC_ALL2/..:**/*"
fi  
echo compile javac
pwd
echo $JAVAC_HOME/bin/javac -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
$JAVAC_HOME/bin/javac -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 

echo build jar
##do not use: $JAVAC_HOME/bin/jar -n0cvfM $JARFILE -C $TMPJAVAC/binjar . > $TMPJAVAC/jar.txt
echo java -cp $JAR_zipjar org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIME  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
java -cp $JAR_zipjar org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIME  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
if ! test "$MD5FILE" = ""; then echo output MD5 checksum
  md5sum -b $JARFILE > $MD5FILE
fi  
echo ok $JARFILE

if test ! "$SRCZIP" = ""; then
  pwd
  echo java -cp $JAR_zipjar org.vishia.zip.Zip -o:$SRCZIPFILE -sort $SRCZIP
  java -cp $JAR_zipjar org.vishia.zip.Zip -o:$SRCZIPFILE -sort $SRCZIP
fi  
  

