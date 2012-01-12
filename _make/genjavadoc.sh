
##--------------------------------------------------------------------------------------------
## Environment variables set from zbnfjax:
## JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation
## The java-copiler may be located at a user-specified position.
## Set the environment variable JAVA_HOME, where bin/javac will be found.
if test "$JAVA_JDK" = "";  then export JAVA_JDK="/usr/share/JDK"; fi


export DST="../../javadocZbnf"
export DST_priv="../../javadocZbnf_priv"

rm -f -r $DST
rm -f -r $DST_priv

mkdir $DST
mkdir $DST_priv

export SRC=""
export SRC="$SRC  ../org/vishia/ant/*.java"
export SRC="$SRC  ../org/vishia/bridgeC/*.java"
export SRC="$SRC  ../org/vishia/byteData/*.java"
export SRC="$SRC  ../org/vishia/byteData/reflection_Jc/*.java"
export SRC="$SRC  ../org/vishia/cmd/*.java"
export SRC="$SRC  ../org/vishia/header2Reflection/*.java"
export SRC="$SRC  ../org/vishia/mainCmd/*.java"
export SRC="$SRC  ../org/vishia/msgDispatch/*.java"
export SRC="$SRC  ../org/vishia/util/*.java"
export SRC="$SRC  ../org/vishia/xml/*.java"
export SRC="$SRC  ../org/vishia/xmlSimple/*.java"
export SRC="$SRC  ../org/vishia/zbnf/*.java"
export SRC="$SRC  ../org/vishia/zmake/*.java"

echo generate docu: $SRC

$JAVA_JDK/bin/javadoc -d $DST -linksource -notimestamp $SRC   1>$DST/javadoc.rpt 2>$DST/javadoc.err
$JAVA_JDK/bin/javadoc -d $DST_priv -private -linksource -notimestamp $SRC   1>$DST_priv/javadoc.rpt 2>$DST_priv/javadoc.err

mkdir $DST/img
cp -r ../img $DST

mkdir $DST_priv/img
cp -r ../img $DST_priv

cp stylesheet_javadoc.css $DST/stylesheet.css
cp stylesheet_javadoc.css $DST_priv/stylesheet.css

echo successfull generated $DST
