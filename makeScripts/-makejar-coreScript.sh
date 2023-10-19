#shell script to generate jar file
#it can be run under Windows using MinGW: sh.exe - thisScript.sh
#MinGW is part of git, it should be known for Gcc compile too.

#$BUILD_TMP is the build output directory. 
#possible to give $BUILD_TMP from outside. On argumentless call determine in build.
#the build should be a temporary directory for build outputs, beside the src of the src-tree
#It can be removed too, then always create newly
echo
echo ====== -makejar-coreScript.sh =====================================================
echo  ... execute  $0
echo  ... compile java and generate jar with binary-compatible content. 
echo PWD=$PWD
if test "$BUILD_TMP" = ""; then                                                      ## check whether a build exists:
  if test -d build; then export BUILD_TMP="build"                ## beside the components sources
  elif test -d ../build; then export BUILD_TMP="../build"        ## beside the src dir of all components
  else
    export BUILD_TMP="/tmp/BuildJava/$DSTNAME"
    if ! test -d $BUILD_TMP; then mkdir -p $BUILD_TMP; fi
  fi
fi  

# clean the build dir because maybe old faulty content: 
# note there was a problem detected on link folders (JUNCTION) in Windows file system, 
#      mkdir is related to the original directory, not to the linked folder using ../../../path
# therefore change temporary to the $BUILD_TMP to use mkdir
export PWDD="$PWD"
cd $BUILD_TMP
if test -d javac_$DSTNAME; then rm -f -r -d javac_$DSTNAME; fi
mkdir -p javac_$DSTNAME/binjar   
mkdir javac_$DSTNAME/result
if ! test -d deploy; then mkdir deploy; fi;
cd $PWDD
export TMPJAVAC="$BUILD_TMP/javac_$DSTNAME"


## The VERSIONSTAMP can come form calling script, elsewhere it is set with the current date.
## This determines the names of the results, but not the content and not the MD5 check sum.
## See $TIMEinJAR_VISHIABASE in next block.
if test "$VERSIONSTAMP" = ""; then export VERSIONSTAMP="$(date -I)"; fi   ## writes current date
if test "$TIMEinJAR" = ""; then export TIMEinJAR="$VERSIONSTAMP+00:00";  fi

#determine out file names from VERSIONSTAMP
if test "$JARFILE" = ""; then export JARFILE="$BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar"; fi
if test "$MD5FILE" = ""; then export MD5FILE="$BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar.MD5.txt"; fi
if test "$SRCZIPFILE" = ""; then export SRCZIPFILE="$DSTNAME-$VERSIONSTAMP-source.zip"; fi #The SRCZIPFILE name will be written in MD5 file also for vishiaMiniSys.


##specific condition, use the yet compiled class files to zip:
if test "$DSTNAME" = "vishiaBase"; then export Classpath_vishiaBase=$TMPJAVAC/binjar;
elif test "$Classpath_vishiaBase" = ""; then
  if test -f tools/vishiaBase.jar; then export Classpath_vishiaBase="tools/vishiaBase.jar"
  elif test -f jars/vishiaBase.jar; then export Classpath_vishiaBase="jars/vishiaBase.jar"
#  elif test -f ../jars/vishiaBase.jar; then export Classpath_vishiaBase="../jars/vishiaBase.jar"
  elif test -f ../tools/vishiaBase.jar; then export Classpath_vishiaBase="../tools/vishiaBase.jar"                 
  elif test -f ../../Java/tools/vishiaBase.jar; then export Classpath_vishiaBase="../../Java/tools/vishiaBase.jar" ##necessary for compilation from other directory
  else echo ERROR vishiaBase.jar not able to found.; exit 5
  fi
fi

if test "$OS" = "Windows_NT"; then export sepPath=";"; else export sepPath=":"; fi

if test "$SRCPATH" == ""; then
  if ! test "$SRC_ALL2" = ""; then export SRCPATH="$SRC_ALL$sepPath$SRC_ALL2"
  else export SRCPATH="$SRC_ALL"
  fi
fi  

##determine the destination directory for the ready to use jar file after build.
##It is either tools, or jars, or ../tools
if ! test "$DSTJARDIR" = ""; then echo DSTJARDIR is set by calling script.
elif test -d tools; then export DSTJARDIR="tools"
elif test -d jars; then export DSTJARDIR="jars"
elif test -d ../tools; then export DSTJARDIR="../tools"
else mkdir jars; export DSTJARDIR="jars"
fi


echo pwd=$(PWD)
echo VERSIONSTAMP = $VERSIONSTAMP  ## determine suffix of output file names
echo TIMEinJAR = $TIMEinJAR  ## determine timestamp in jar
echo DSTNAME = $DSTNAME  ## output file names
echo BUILD_TMP = $BUILD_TMP  ## root for all temporary outputs
echo SRC_ALL = $SRC_ALL      ## gather all *.java there
echo SRC_ALL2 = $SRC_ALL2  ## gather all *.java there - optional
echo SRCPATH = $SRCPATH  - search path sources for javac
echo FILE1SRC = $FILE1SRC  ## alternatively: argument files for javac
echo RESOURCEFILES = $RESOURCEFILES  ## additional files in jar
echo CLASSPATH = $CLASSPATH - search path jars for javac
echo Classpath_vishiaBase = $Classpath_vishiaBase  - jar file for jar/zip generation
echo TMPJAVAC =  $TMPJAVAC  - temporary files while compilation
echo JARFILE = $JARFILE  - generated jar    
echo MD5FILE = $MD5FILE  - generated MD5 text file
echo SRCZIPFILE = $SRCZIPFILE - generated sozrce.zip file
echo DSTJARDIR = $DSTJARDIR

if test "$JAVAC_HOME" = ""; then
  export JAVAC_HOME="$($(dirname $0)/JAVAC_HOME.sh)"
fi  
echo JAVAC_HOME = $JAVAC_HOME
##regards an empty JAVAC_HOME, then javac should be able as command in the path:
if test "$JAVAC_HOME" = ""; then export JAVAC="javac"; else export JAVAC="$JAVAC_HOME/bin/javac"; fi
#xx echo JAVAC = $JAVAC                                                                                       
#xx echo Output to: $JARFILE
#xx echo ====== gen src.zip ================================================================

##Automatic build a zip file if SRC_ALL and maybe additionally SRC_ALL2 is given.
##SRC_ALL refers to the java package path root directory,
##but the source.zip should contain the parent folder which is srcJava_xyz/org/... 
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

#xx echo compile javac
#xx echo pwd=$(pwd)
##echo ls /tmp
##ls /tmp
echo
echo ====== javac ================================================
echo $JAVAC -encoding UTF-8 -d $TMPJAVAC/binjar $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
$JAVAC -encoding UTF-8 -d $TMPJAVAC/binjar $CLASSPATH -sourcepath $SRCPATH $FILE1SRC 
if test ! $? = 0; then
  echo ERROR javac --?????????????????????????????????????????--
  if test -f $JARFILE; then rm $JARFILE; fi    ##prevent usage of an older version here.  
  exit 5
fi  
#xx echo build jar
##do not use: $JAVAC_HOME/bin/jar -n0cvfM $JARFILE -C $TMPJAVAC/binjar . > $TMPJAVAC/jar.txt
#xx echo pwd=$(pwd)
echo
echo ====== zip-jar ================================================
echo java -cp $Classpath_vishiaBase org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIMEinJAR  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
java -cp $Classpath_vishiaBase org.vishia.zip.Zip -o:$JARFILE -manifest:$MANIFEST -sort -time:$TIMEinJAR  $TMPJAVAC/binjar:**/*.class $RESOURCEFILES
if ! test "$MD5FILE" = ""; then echo output MD5 checksum
  echo
  echo ====== output MD5 checksum ==================================
  echo md5sum -b $JARFILE
  md5sum -b $JARFILE > $MD5FILE
  echo "  srcFiles: $SRCZIPFILE" >> $MD5FILE
  cat $MD5FILE
fi  

if test -v SRCADD_ZIP; then  ## do not zip sources if $SRCADD_ZIP is non existing, but build zip if it is empty.
  echo
  echo ====== zip-sources ================================================
  pwd
  SRCZIP=""
  if ! test "$SRC_ALL" = ""; then
    SRCZIP=".:$SRC_ALL/**/*"  ## with the srcJava_... dir
  fi                                                                                        
  if ! test "$SRC_ALL2" = ""; then 
    SRCZIP="$SRCZIP .:$SRC_ALL2/**/*"                         
  fi  
  SRCZIP="$SRCZIP $SRCADD_ZIP"      ## note: adds only a space if $SRCADD_ZIP is empty                   
  ## $SRCZIP is a list of wildcard path arguments (separated with space) to add in zip
  echo java -cp $Classpath_vishiaBase org.vishia.zip.Zip -o:$BUILD_TMP/deploy/$SRCZIPFILE -sort $SRCZIP
  java -cp $Classpath_vishiaBase org.vishia.zip.Zip -o:$BUILD_TMP/deploy/$SRCZIPFILE -sort $SRCZIP
  if test -f $BUILD_TMP/deploy/$SRCZIPFILE; then echo ok $BUILD_TMP/deploy/$SRCZIPFILE; else echo ERROR src.zip $BUILD_TMP/deploy/$SRCZIPFILE; fi
fi  

## ====== copy to  ================================================
if test -f $JARFILE; then echo ok $JARFILE; else echo ERROR $JARFILE; fi

#xx echo ====== copy to DSTJARDIR=$DSTJARDIR ==============================================================
##REM: It should be assumed that the file is correct. 
##REM It replaces the given in $DSTJARDIR to support more tests with the new jar without more effort.
##REM Only the jar file is copied. The rest is done by the deploy script.
##echo cp $BUILD_TMP/deploy/* $DSTJARDIR
##cp $BUILD_TMP/deploy/* $DSTJARDIR
#xx echo "current dir =$PWD"
echo
echo ====== copy to $DSTJARDIR ================================================
echo "cp $JARFILE $DSTJARDIR/$DSTNAME.jar"
cp $JARFILE $DSTJARDIR/$DSTNAME.jar          ## copy jar without timestamp
echo "cp $JARFILE $DSTJARDIR"
cp $JARFILE $DSTJARDIR                       ## copy jar with timestamp, the original
echo "cp $MD5FILE $DSTJARDIR"
cp $MD5FILE $DSTJARDIR                       ## copy md5 
echo "cp $BUILD_TMP/deploy/$SRCZIPFILE $DSTJARDIR"
cp $BUILD_TMP/deploy/$SRCZIPFILE $DSTJARDIR  ## copy zipped sources
#xx echo === $DSTJARDIR content:
#xx ls -all $DSTJARDIR

#xx echo                                 
#xx echo ====== deploy script ==============================================================
#xx echo DEPLOYSCRIPT=$DEPLOYSCRIPT


