## may use a different JDK/JRE for Javadoc. Therefore a batch setJAVA_Javadoc.bat may found in the PATH in users space.
echo on                                                                                  
echo JAVAC_HOME=$JAVAC_HOME
echo clean $DSTDIR$DST
echo clean $DSTDIR$DST_priv  
if test -d $DSTDIR$DST_priv; then rm -f -r  $DSTDIR$DST_priv ; fi
if test -d $DSTDIR$DST; then rm -f -r  $DSTDIR$DST ; fi
                                                                                                                                             
if ! test -d $DSTDIR$DST; then mkdir $DSTDIR$DST ; fi
if ! test -d $DSTDIR$DST_priv ; then mkdir $DSTDIR$DST_priv ; fi

## -nodeprecated 
export ARGS="-Xdoclint:none -d $DSTDIR$DST -private -notimestamp $LINKPATH -classpath $CLASSPATH -sourcepath $SRCPATH $SRC"
echo javadoc $ARGS
$JAVAC_HOME/bin/javadoc.exe $ARGS 1> $DSTDIR$DST/javadoc.rpt 2> $DSTDIR$DST/javadoc.err                                                       

###if errorlevel 1 (
###  type %DSTDIR%%DST%\javadoc.err
###  notepad.exe %DSTDIR%%DST%\javadoc.err                                                           
###  pause                                                                                                                                      
###)
###echo off        
#cp ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DSTDIR%%DST%\stylesheet.css >NUL
###


export ARGS="-Xdoclint:none -d $DSTDIR$DST_priv -private -linksource -notimestamp $LINKPATH -classpath $CLASSPATH -sourcepath $SRCPATH $SRC"
echo javadoc $ARGS
$JAVAC_HOME/bin/javadoc.exe $ARGS 1> $DSTDIR$DST_priv/javadoc.rpt 2> $DSTDIR$DST_priv/javadoc.err

###echo on                 
###%JAVA_JDK%\bin\javadoc -Xdoclint:none -d %DSTDIR%%DST_priv% -private -linksource -notimestamp %LINKPATH% -classpath %CLASSPATH% -sourcepath %SRCPATH% %SRC% 1>%DSTDIR%%DST_priv%\javadoc.rpt 2>%DSTDIR%%DST_priv%\javadoc.err
###::%JAVA_JDK%\bin\javadoc -d %DSTDIR%%DST_priv% -private -notimestamp %LINKPATH% -classpath %CLASSPATH% -sourcepath %SRCPATH% %SRC% 1>%DSTDIR%%DST_priv%\javadoc.rpt 2>%DSTDIR%%DST_priv%\javadoc.err
###@echo off
###if errorlevel 1 (
###  type %DSTDIR%%DST_priv%\javadoc.err                                                                             
###  notepad.exe %DSTDIR%%DST_priv%\javadoc.err
###  pause
###)
###copy ..\..\srcJava_Zbnf\_make\stylesheet_javadoc.css %DSTDIR%%DST_priv%\stylesheet.css >NUL
###
###

if test -d ../img; then
###  echo copy %DSTDIR%%DST%\img
	if test ! -d $DSTDIR$DST/img; then mkdir $DSTDIR$DST/img; fi;
  cp ../img/* $DSTDIR$DST/img 
	if test ! -d "$DSTDIR$DST"_priv/img; then mkdir "$DSTDIR$DST"_priv/img; fi;
  cp ../img/* "$DSTDIR$DST"_priv/img 
fi

#zip
###if test -f  $DSTDIR$DST.zip; then rm $DSTDIR$DST.zip; fi
###echo zip $DSTDIR$DST.zip
###export CDD="$CD"
###cd $DSTDIR
###pwd
###cmd /C pkzipc -Add -Directories $DST.zip $DST\* "$DST"_priv\*
###cd $CDD



###if "%DSTDIR%" == "" goto :nozip
###::pause
###echo on
###set PWD1=%CD%
###cd %DSTDIR%
###if exist %DST%.zip del %DST%.zip >NUL
###echo zip %CD%\%DST%.zip
###pkzipc.exe -add -Directories %DST%.zip %DST%\* %DST_priv%\*
###pause
###cd %PWD1%
###echo off
###if errorlevel 1 goto :error
###:nozip
###
###echo successfull generated %DSTDIR%%DST%
###if "%NOPAUSE%"=="" pause
###goto :ende
###
###:error
###echo ===ERROR===
###::call edit.bat %DSTDIR%%DST%\javadoc.err
###notepad.exe %DSTDIR%%DST%\javadoc.err
###pause
###:ende
###


