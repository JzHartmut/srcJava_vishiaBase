
##--------------------------------------------------------------------------------------------
## Environment variables set from zbnfjax:
## JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation
## The java-copiler may be located at a user-specified position.
## Set the environment variable JAVA_HOME, where bin/javac will be found.
if test "$JAVA_JDK" = "";  then export JAVA_JDK="/usr/share/JDK"; fi


export DST="../../docuSrcJava_Zbnf"
export DST_priv="../../docuSrcJava_Zbnf_priv"


export SRC="-subpackages org.vishia.zbnf:org.vishia.zmake:org.vishia.ant:org.vishia.header2Reflection"
export SRC="$SRC  ../org/vishia/byteData/*.java"


export SRCPATH="..:../../srcJava_vishiaBase"

export LINKPATH="-link ../docuSrcJava_vishiaBase"

../../srcJava_vishiaBase/_make/+genjavadocbase.sh
