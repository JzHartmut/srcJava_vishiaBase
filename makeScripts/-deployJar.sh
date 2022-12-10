echo ====== deploy ==================================================================

# Deploy the result
if test ! -f $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar; then   ##compilation not successfull
  echo "?????? compiling ERROR, abort ????????????????????????" 
  exit 255
else                                                       ##compilation not successfull
  ##
  ## copy the useable version to a existing tools or jar directory:
  if test -d tools; then    ##beside src/... should be existing, for compilations from working tree
    export CURRENT_JARS_PATH="tools" 
  else
    export CURRENT_JARS_PATH="jars"       ##This is for compilation from zip file
    if ! test -d $CURRENT_JARS_PATH; then mkdir $CURRENT_JARS_PATH; fi
  fi  
  echo CURRENT_JARS_PATH = $CURRENT_JARS_PATH
  if test -v CURRENT_JARS_PATH; then
    ##
    ##The next script, especially corrBom.jzTc, checks the existing files, checks the MD5 and copies only if necessary 
    echo test and correct the bom file: JZtxtcmd corrBom.jzTc $CURRENT_JARS_PATH $BUILD_TMP/deploy vishiaBase $VERSIONSTAMP
    echo java -cp $JAR_vishiaBase org.vishia.jztxtcmd.JZtxtcmd $MAKEBASEDIR/corrBom.jzTc $CURRENT_JARS_PATH $BUILD_TMP/deploy $DSTNAME $VERSIONSTAMP
    java -cp $JAR_vishiaBase org.vishia.jztxtcmd.JZtxtcmd $MAKEBASEDIR/corrBom.jzTc $CURRENT_JARS_PATH $BUILD_TMP/deploy $DSTNAME $VERSIONSTAMP
    echo ========================================================================== $?
    if test ! -f $CURRENT_JARS_PATH/$DSTNAME.new.jar; then
      echo "BOM not changed, unchanged MD5 (or error while copying to $CURRENT_JARS_PATH/$DSTNAME*_old.jar"
    else
      ## Hint: copy in the JZtxtcmd script is not possible for vishiaBase.jar itself because it is in use, locked.
      ## Hence: copy always in the shell script.
      echo $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar ==> $CURRENT_JARS_PATH/$DSTNAME.jar
      mv $CURRENT_JARS_PATH/$DSTNAME.new.jar $CURRENT_JARS_PATH/$DSTNAME.jar    
      ls -l $CURRENT_JARS_PATH
      ##
    fi  
    ##
    ##The next script copies to the maybe given deploy directory.
    if test -d ../../Java/deploy; then export DEPLOYDIR="../../Java/deploy";
    elif test -d deploy; then export DEPLOYDIR="deploy";
    else echo deploy directory not given, create it.
    fi  
    if test -v DEPLOYDIR; then
      echo == Deploy:
      echo copy to $DEPLOYDIR
      cp $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP* $DEPLOYDIR
      ls $DEPLOYDIR/$DSTNAME-$VERSIONSTAMP*
    fi  
    echo ======= success ==========================================================
  fi  
fi  



