export DSTDIR=$TMP/_Javadoc/
mkdir --parents $DSTDIR
#export DSTDIR=D:/vishia/Java/

export DST=docuSrcJava_vishiaBase
##export DST_priv=docuSrcJavaPriv_vishiaBase

export SRC="-subpackages org.vishia"
export SRCPATH=..
export CLASSPATH=xxxxx
export LINKPATH=
##export LINKPATH=-link ../docuSrcJava_vishiaBase

if test -d ../../srcJava_vishiaBase; then export vishiaBase="../../srcJava_vishiaBase"
else export vishiaBase="../../../../../../cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase"
fi

$vishiaBase/_make/-genjavadocbase.sh


