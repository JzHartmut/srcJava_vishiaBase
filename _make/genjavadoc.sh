

export DSTDIR="../../"
export DST="docuSrcJava_vishiaBase"
export DST_priv="docuSrcJava_vishiaBase_priv"

export SRC="-subpackages org.vishia"
export SRCPATH=".."

echo set linkpath
export LINKPATH=""
#export LINKPATH="$LINKPATH -link ../docuSrcJava_Zbnf"


../../srcJava_vishiaBase/_make/+genjavadocbase.sh

