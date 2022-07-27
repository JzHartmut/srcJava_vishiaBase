

# Deploy the result
if test ! -f $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP.jar; then   ##compilation not successfull
  echo "?????? compiling ERROR, abort ????????????????????????" 
  exit 255
else                                                       ##compilation not successfull
  ##
  ## copy the useable version to a existing tools directory:
  if test -d ../../../../../../../Java/tools; then ##beside cmpnJava... should be existing
    export CURRENT_JARS_PATH="../../../../../../../Java/tools" 
  else
    export CURRENT_JARS_PATH="../../jars" 
    if ! test -d $CURRENT_JARS_PATH; then mkdir $CURRENT_JARS_PATH; fi
  fi  
  if test -v CURRENT_JARS_PATH; then
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
    echo copy to the deploy directory if exists, also only the source may be changed
    if test -d ../../../../../../../Java/deploy; then
      echo $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP* ==> ../../../../../../../Java/deploy
      cp $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP* ../../../../../../../Java/deploy
    elif test -d ../../deploy; then
      echo $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP* ==> ../../deploy
      cp $BUILD_TMP/deploy/$DSTNAME-$VERSIONSTAMP* ../../deploy
    else
      echo deploy directory not found.
    fi  
    echo ======= success ==========================================================
  fi  
fi  



