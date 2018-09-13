@echo off
set DSTDIR=D:\vishia\Java\
if exist %DSTDIR% goto :okdstdir
set DSTDIR=..\..\
:okdstdir
echo %DSTDIR%
set DST=docuSrcJava_Zbnf
set DST_priv=docuSrcJavaPriv_Zbnf




set SRC=-subpackages org.vishia
set SRCPATH=..;../../srcJava_Zbnf
set CLASSPATH=xxxxx
set LINKPATH=
set CLASSPATH=xxxxx
::set LINKPATH=-link ../docuSrcJava_vishiaBase

..\..\srcJava_Zbnf\_make\+genjavadocbase.bat
