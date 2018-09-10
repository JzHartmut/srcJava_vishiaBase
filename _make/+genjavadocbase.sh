#/bin/bash

#$JAVA_JDK/bin/javadoc --help >javadoc.help

if test -z "$JAVA_JDK";  then
  if test -d /usr/share/JDK; then export JAVA_JDK="/usr/share/JDK"; fi
  if test -d /d/Programs/JAVA/jdk1.8.0_92; then export JAVA_JDK="/d/Programs/JAVA/jdk1.8.0_92"; fi
fi
echo JAVA_JDK=$JAVA_JDK
echo genJavadoc: $DSTDIR$DST
rm -f -r $DST/*
rm -f -r $DST_priv/*

#mkdir $DSTDIR$DST
#mkdir $DSTDIR$DST_priv

echo generate docu: $SRC

echo javadoc -d $DSTDIR$DST -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH
$JAVA_JDK/bin/javadoc -d $DSTDIR$DST -protected -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH $SRC 1>$DSTDIR$DST/javadoc.rpt 2>$DSTDIR$DST/javadoc.err

echo javadoc -d $DSTDIR$DST_priv -private -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH
$JAVA_JDK/bin/javadoc -d $DSTDIR$DST_priv -private -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH $SRC 1>$DSTDIR$DST_priv/javadoc.rpt 2>$DSTDIR$DST_priv/javadoc.err

mkdir $DSTDIR$DST/img
cp -r ../img $DSTDIR$DST

mkdir $DSTDIR$DST_priv/img
cp -r ../img $DSTDIR$DST_priv

cp ../../srcJava_vishiaBase/_make/stylesheet_javadoc.css $DSTDIR$DST/stylesheet.css
cp ../../srcJava_vishiaBase/_make/stylesheet_javadoc.css $DSTDIR$DST_priv/stylesheet.css

echo successfull generated $DST
