@echo off
set DSTDIR=..\..\
set DST=docuSrcJava_Zbnf
set DST_priv=docuSrcJava_Zbnf_priv

set SRC=-subpackages org.vishia.zbnf
::set SRC=%SRC% -subpackages org.vishia.ant
set SRC=%SRC% -subpackages org.vishia.checkDeps_C
set SRC=%SRC% -subpackages org.vishia.sclConversions
set SRC=%SRC% -subpackages org.vishia.stateMGen
set SRC=%SRC% -subpackages org.vishia.zbnf
set SRC=%SRC% -subpackages org.vishia.zmake
set SRC=%SRC% -subpackages org.vishia.zgen
set SRC=%SRC% -subpackages org.vishia.header2Reflection 
set SRC=%SRC% ../org/vishia/byteData/*.java

set SRCPATH=..;..\..\srcJava_vishiaBase

set LINKPATH=-link ../docuSrcJava_vishiaBase

..\..\srcJava_vishiaBase\_make\+genjavadocbase.bat
