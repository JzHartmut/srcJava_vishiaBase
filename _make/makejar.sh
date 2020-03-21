#shell script to generate jar file
#it can be run under Windows using MinGW: sh.exe - thisScript.sh
#MinGW is part of git, it should be known for Gcc compile too.

##Both variables should be corrected for any new version, 
##if is used for gradle build and for shell build!
if test "$VERSION" == ""; then export VERSION="2020-03-20"; fi
export TIME="2020-03-20+06:11"

#determine out file names from VERSION
export JARFILE=$DEPLOY$VERSION.jar
export MD5FILE=$DEPLOY$VERSION.jar.MD5.txt

echo compile java and generate jar with binary-compatible content. 
echo JAVAC_HOME = $JAVAC_HOME
echo VERSION = $VERSION  - determine only output file names
echo TIME = $TIME  - determine timestamp in jar
echo SRC_ALL = $SRC_ALL  - gather all *.java there
echo SRC_ALL2 = $SRC_ALL2  - gather all *.java there
echo FILE1SRC = $FILE1SRC  - argument files for javac
echo RESOURCEFILES = $RESOURCEFILES  - additional files in jar
echo SRCPATH = $SRCPATH  - search path sources for javac
echo CLASSPATH = $CLASSPATH
echo JAR_vishiaBASE = $JAR_vishiaBase  - jar file for jar generation
echo TMPJAVAC =  $TMPJAVAC  - temporary files while compilation
echo JARFILE = $JARFILE  - generated jar
echo MD5FILE = $MD5FILE  - generated MD5 text file

if test "$JAVAC_HOME" = ""; then
  echo you must set JAVAC_HOME in your system to the installed JDK
  exit 5
fi
# clean the binjar because maybe old faulty content:
if test -d $TMPJAVAC/binjar; then rm -f -r -d $TMPJAVAC/binjar; fi
mkdir -p $TMPJAVAC/binjar

if ! test "$SRC_ALL" = ""; then
  echo source-set all files = $SRC_ALL
  find $SRC_ALL -name "*.java" > $TMPJAVAC/sources.txt
  export FILE1SRC=@$TMPJAVAC/sources.txt
fi  
if ! test "$SRC_ALL2" = ""; then
  echo source-set all files = $SRC_ALL2
  find $SRC_ALL2 -name "*.java" >> $TMPJAVAC/sources.txt
  export FILE1SRC=@$TMPJAVAC/sources.txt
fi  
echo compile javac
echo $JAVAC_HOME/bin/javac -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
$JAVAC_HOME/bin/javac -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 

echo build jar
echo java -cp $JAR_vishiaBase org.vishia.util.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIME  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
java -cp $JAR_vishiaBase org.vishia.util.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIME  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
##$JAVAC_HOME/bin/jar -n0cvfM $JARFILE -C $TMPJAVAC/binjar . > $TMPJAVAC/jar.txt
if ! test "$MD5FILE" = ""; then echo output MD5 checksum
  md5sum -b $JARFILE > $MD5FILE
fi  
echo ok $JARFILE


