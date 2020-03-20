#shell script to generate jar file
#it can be run under Windows using MinGW: sh.exe - thisScript.sh
#MinGW is part of git, it should be known for Gcc compile too.

##This should be correct for any new version, used for gradle build and for shell build!
if test "$VERSION" == ""; then export VERSION="2020-03-20"; fi
if test "$TIME" == ""; then export TIME="2020-03-20+06:11"; fi

export JARFILE=$DEPLOY$VERSION.jar
export MD5FILE=$DEPLOY$VERSION.jar.MD5.txt

echo compile java and generate jar with binary-compatible content. 
#echo Java-Tools at $JAVAC_HOME
#echo time stamp and version = $VERSION
#echo primary sources to compile in file $PRIMARYSRCFILE
#echo sourcepath: $SRCPATH
#echo classpath : $CLASSPATH
#echo temparary files at $TMPJAVAC
#echo jar-file to generate = $JARFILE
#echo MD5-file to generate = $MD5FILE
#it is better to read:
echo JAVAC_HOME = $JAVAC_HOME
echo VERSION = $VERSION
echo TIME = $TIME
echo FILE1SRC = $FILE1SRC
echo SRCPATH = $SRCPATH
echo CLASSPATH = $CLASSPATH
echo VishiaBASE = $VISHIABASE
echo TMPJAVAC =  $TMPJAVAC
echo JARFILE = $JARFILE
echo MD5FILE = $MD5FILE

if test "$JAVAC_HOME" = ""; then
  echo you must set JAVAC_HOME in your system to the installed JDK
  exit 5
fi
# clean the binjar because maybe old faulty content:
if test -d $TMPJAVAC/binjar; then rm -f -r -d $TMPJAVAC/binjar; fi
mkdir -p $TMPJAVAC/binjar

if ! test "$SRC_ALL" = ""; then
  echo gather all sources, at $SRC_ALL
  find $SRC_ALL -name "*.java" > $TMPJAVAC/sources.txt
  export FILE1SRC=@$TMPJAVAC/sources.txt
fi  
echo compile javac
$JAVAC_HOME/bin/javac -encoding UTF-8 -d $TMPJAVAC/binjar -cp $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
##mkdir $TMPJAVAC/binjar/META-INF
##Note: create the manifest file manually, not with jar, because of time stamp
##cp $MANIFEST $TMPJAVAC/binjar/META-INF/MANIFEST.MF  
##echo touch timestams to $VERSION
##find $TMPJAVAC/binjar -exec touch -d $VERSION {} \;
echo build jar
##Hint: Use the new generated class files itself, it contains the necessary code:
java -cp $VISHIABASE org.vishia.util.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIME $TMPJAVAC/binjar:**/*.class
##$JAVAC_HOME/bin/jar -n0cvfM $JARFILE -C $TMPJAVAC/binjar . > $TMPJAVAC/jar.txt
if ! test "$MD5FILE" = ""; then echo output MD5 checksum
  md5sum -b $JARFILE > $MD5FILE
fi  
echo ok $JARFILE


