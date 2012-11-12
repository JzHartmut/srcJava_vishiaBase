
set DSTDIR=..\..\
set DST=docuSrcJava_Zbnf
set DST_priv=docuSrcJava_Zbnf_priv

echo set SRC
set SRC=-subpackages org.vishia.zbnf
set SRC=%SRC% -subpackages org.vishia.textGenerator
set SRC=%SRC% -subpackages org.vishia.zmake
set SRC=%SRC% -subpackages org.vishia.ant
set SRC=%SRC% -subpackages org.vishia.header2Reflection 
set SRC=%SRC% ../org/vishia/byteData/*.java

set SRCPATH=..;..\..\srcJava_vishiaBase

echo set linkpath
set LINKPATH=-link ../docuSrcJava_vishiaBase

..\..\srcJava_vishiaBase\_make\+genjavadocbase.bat
