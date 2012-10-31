
export DST="../../docuSrcJava_Zbnf"
export DST_priv="../../docuSrcJava_Zbnf_priv"


export SRC="-subpackages org.vishia.zbnf:org.vishia.textGenerator:org.vishia.zmake:org.vishia.ant:org.vishia.header2Reflection"
export SRC="$SRC  ../org/vishia/byteData/*.java"


export SRCPATH="..:../../srcJava_vishiaBase"

export LINKPATH="-link ../docuSrcJava_vishiaBase"

../../srcJava_vishiaBase/_make/+genjavadocbase.sh
