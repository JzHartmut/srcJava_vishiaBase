## intern script for genJavadoc
## Note: the $DSTDIR can be set outside also to D:/vishia.
## $DST should contain for ex. Java/srcJava_vishiaBase to create it parallel to other main components.
## set $JAVAC_HOME maybe outside to a version for proper javadoc.

if test -z "$DSTDIR"; then   ## only if DSTDIR is not set:
  export DSTDIR=/tmp/RAMd/_Javadoc
fi
if ! test -d $DSTDIR; then mkdir $DSTDIR; fi
if ! test -d $DSTDIR; then export DSTDIR="../../"; fi
echo DSTDIR = $DSTDIR

if test -z "$JAVAC_HOME"; then
  echo "calls: $(dirname $0)/JAVAC_HOME.sh"
  . $(dirname $0)/JAVAC_HOME.sh
  ##export JAVAC_HOME="$($(dirname $0)/JAVAC_HOME.sh)"
fi
echo JAVAC_HOME = $JAVAC_HOME
echo clean $DSTDIR/$DST
if test -d $DSTDIR/$DST; then rm -f -r  $DSTDIR/$DST ; fi
if ! test -d $DSTDIR/$DST; then mkdir --parents $DSTDIR/$DST ; fi


echo output to $DSTDIR/$DST
##NOTE: unfortunately there is no possible to output private elements but do not output private routines.
##      View to private elements documents which things contains the class, it is important.
##      View to private methods is sometimes very unnecessary.
## -nodeprecated
echo currdir=$PWD
export ARGS="-Xdoclint:none -d $DSTDIR/$DST -encoding UTF-8 -private -notimestamp $LINKPATH -classpath $CLASSPATH -sourcepath $SRCPATH $SRC"
echo $JAVAC_HOME/bin/javadoc $ARGS
"$JAVAC_HOME/bin/javadoc" $ARGS 1> $DSTDIR/$DST/javadoc.rpt 2> $DSTDIR/$DST/javadoc.err
if test -d ../img; then
	if test ! -d $DSTDIR/$DST/img; then mkdir $DSTDIR/$DST/img; fi;
  cp ../img/* $DSTDIR/$DST/img
fi


if ! test -z "$DST_priv"; then
  echo clean $DSTDIR/$DST_priv
  if test -d $DSTDIR/$DST_priv; then rm -f -r  $DSTDIR/$DST_priv ; fi
  if ! test -d $DSTDIR/$DST_priv ; then mkdir --parents $DSTDIR/$DST_priv ; fi
  export ARGS="-Xdoclint:none -d $DSTDIR/$DST_priv -encoding UTF-8 -private -linksource -notimestamp $LINKPATH -classpath $CLASSPATH -sourcepath $SRCPATH $SRC"
  echo javadoc $ARGS
  "$JAVAC_HOME"/bin/javadoc $ARGS 1> $DSTDIR/$DST_priv/javadoc.rpt 2> $DSTDIR/$DST_priv/javadoc.err
  if test -d ../img; then
  	if test ! -d "$DSTDIR/$DST"_priv/img; then mkdir "$DSTDIR/$DST"_priv/img; fi;
    cp ../img/* "$DSTDIR/$DST"_priv/img
  fi
  echo see also $DSTDIR/$DST_priv
fi

echo The javadoc was produced in $DSTDIR/$DST
echo you should copy or compare and copy the doc to the destination
echo and/or zip the doc, shipment and unzip it on the destination.
echo it should be manually done.


