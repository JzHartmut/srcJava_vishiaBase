export DSTDIR=$TMP/_Javadoc/
mkdir $DSTDIR
#export DSTDIR=D:/vishia/Java/

if ! test -d $DSTDIR; then export DSTDIR=../../; fi
echo %DSTDIR%
export DST=docuSrcJava_vishiaBase
export DST_priv=docuSrcJavaPriv_vishiaBase

export SRC="-subpackages org.vishia"
export SRCPATH=..
export CLASSPATH=xxxxx
export LINKPATH=
export CLASSPATH=xxxxx
##export LINKPATH=-link ../docuSrcJava_vishiaBase

../../../../../cmpnJava_vishiaBase/src/main/java/_make/-genjavadocbase.sh


