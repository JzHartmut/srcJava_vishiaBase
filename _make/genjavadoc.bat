@echo off
set DSTDIR=D:\vishia\Java\
if exist %DSTDIR% goto :okdstdir
set DSTDIR=..\..\
:okdstdir
echo %DSTDIR%
set DST=docuSrcJava_Zbnf
set DST_priv=docuSrcJavaPriv_Zbnf

set SRC=
::set SRC=%SRC% org.vishia.byteData.ByteDataSymbolicAccessReadConfig
set SRC=%SRC% -subpackages org.vishia.checkDeps_C
set SRC=%SRC% -subpackages org.vishia.sclConversions
set SRC=%SRC% -subpackages org.vishia.stateMGen
set SRC=%SRC% -subpackages org.vishia.zbnf
set SRC=%SRC% -subpackages org.vishia.zcmd
set SRC=%SRC% -subpackages org.vishia.jztxtcmd
set SRC=%SRC% -subpackages org.vishia.zbnf
set SRC=%SRC% -subpackages org.vishia.header2Reflection
::set SRC=%SRC% -subpackages org.vishia.ant
::set SRC=%SRC% -subpackages org.vishia.checkDeps_C
::set SRC=%SRC% -subpackages org.vishia.sclConversions
::set SRC=%SRC% -subpackages org.vishia.stateMGen
::set SRC=%SRC% -subpackages org.vishia.zbnf
::set SRC=%SRC% -subpackages org.vishia.zmake
::set SRC=%SRC% -subpackages org.vishia.zcmd
::set SRC=%SRC% -subpackages org.vishia.header2Reflection 
::set SRC=%SRC% ../org/vishia/byteData/*.java

set SRCPATH=..;..\..\srcJava_vishiaBase
::set SRCPATH=..

set CLASSPATH=xxxxx
set LINKPATH=-link ../docuSrcJava_vishiaBase

..\..\srcJava_vishiaBase\_make\+genjavadocbase.bat
