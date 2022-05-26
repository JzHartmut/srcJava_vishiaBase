#shell script to generate jar file
#it can be run under Windows using MinGW: sh.exe - thisScript.sh
#MinGW is part of git, it should be known for Gcc compile too.

##Both variables should be corrected for any new version, 
##if is used for gradle build and for shell build!

#determine out file names from VERSIONSTAMP
export JARFILE=$BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar
export MD5FILE=$BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar.MD5.txt

if test "$TIMEinJAR" = ""; then export TIMEinJAR=$(date -I); TIMEinJAR="$TIMEinJAR+00:00"; fi

echo
echo ====== javac ================================================
echo execute  $0
echo  ... compile java and generate jar with binary-compatible content. 
echo DSTNAME = $DSTNAME  ## output file names
echo pwd=$(pwd)
echo BUILD_TMP = $BUILD_TMP  ## root for all temporary outputs
echo VERSIONSTAMP = $VERSIONSTAMP  ## determine suffix of output file names
echo TIMEinJAR = $TIMEinJAR  ## determine timestamp in jar
echo SRC_ALL = $SRC_ALL      ## gather all *.java there
echo SRC_ALL2 = $SRC_ALL2  ## gather all *.java there - optional
echo FILE1SRC = $FILE1SRC  ## alternatively: argument files for javac
echo RESOURCEFILES = $RESOURCEFILES  ## additional files in jar
echo SRCPATH = $SRCPATH  - search path sources for javac
echo CLASSPATH = $CLASSPATH - search path jars for javac
echo JAR_zipjar = $JAR_zipjar  - jar file for jar/zip generation
echo TMPJAVAC =  $TMPJAVAC  - temporary files while compilation
echo JARFILE = $JARFILE  - generated jar    
echo MD5FILE = $MD5FILE  - generated MD5 text file
echo SRCZIPFILE = $SRCZIPFILE - generated sozrce.zip file


if test "$JAVAC_HOME" = ""; then
  export JAVAC_HOME="$($(dirname $0)/JAVAC_HOME.sh)"
fi  
echo JAVAC_HOME = $JAVAC_HOME
##regards an empty JAVAC_HOME, then javac should be able as command in the path:
if test "$JAVAC_HOME" = ""; then export JAVAC="javac"; else export JAVAC="$JAVAC_HOME/bin/javac"; fi
echo JAVAC = $JAVAC                                                                                       
# clean the build dir because maybe old faulty content:
if test -d $TMPJAVAC; then rm -f -r -d $TMPJAVAC; fi
mkdir -p $TMPJAVAC/binjar   
mkdir $TMPJAVAC/result
echo                                             
echo Output to: $BUILD_TMP/deploy/$DSTNAME
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
echo pwd=$(pwd)
##echo ls /tmp
##ls /tmp
echo $JAVAC -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
###$JAVAC_HOME/bin/
$JAVAC -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
if test ! $? == 0; then
  echo ERROR javac --?????????????????????????????????????????--
  if test -f $JARFILE; then rm $JARFILE; fi    ##prevent usage of an older version here.  
  exit 5
fi  
echo build jar
##do not use: $JAVAC_HOME/bin/jar -n0cvfM $JARFILE -C $TMPJAVAC/binjar . > $TMPJAVAC/jar.txt
echo pwd=$(pwd)
echo java -cp $JAR_zipjar org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIMEinJAR  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
java -cp $JAR_zipjar org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIMEinJAR  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
if ! test "$MD5FILE" = ""; then echo output MD5 checksum
  md5sum -b $JARFILE > $MD5FILE
  echo "  srcFiles: $SRCZIPFILE" >> $MD5FILE
fi  
echo ok $JARFILE

if test ! "$SRCZIP" = ""; then  ##not produced if $SRC_ALL is empty instead $FILE1SRC is given from outside.
  pwd
  echo java -cp $JAR_zipjar org.vishia.zip.Zip -o:$BUILD_TMP/deploy/$SRCZIPFILE -sort $SRCZIP
  java -cp $JAR_zipjar org.vishia.zip.Zip -o:$BUILD_TMP/deploy/$SRCZIPFILE -sort $SRCZIP
fi  
  

