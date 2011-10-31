@echo on
set DST=..\..\javadocZBNF
if exist %DST% rmdir /S /Q %DST%

if not exist %DST% mkdir %DST%
if not exist %DST%_priv mkdir %DST%_priv
call setZBNFJAX_HOME.bat

set SRC=
set SRC=%SRC% ../org/vishia/ant/*.java
set SRC=%SRC% ../org/vishia/bridgeC/*.java
set SRC=%SRC% ../org/vishia/byteData/*.java
set SRC=%SRC% ../org/vishia/byteData/reflection_Jc/*.java
set SRC=%SRC% ../org/vishia/cmd/*.java
set SRC=%SRC% ../org/vishia/header2Reflection/*.java
set SRC=%SRC% ../org/vishia/mainCmd/*.java
set SRC=%SRC% ../org/vishia/msgDispatch/*.java
set SRC=%SRC% ../org/vishia/util/*.java
set SRC=%SRC% ../org/vishia/xml/*.java
set SRC=%SRC% ../org/vishia/xmlSimple/*.java
set SRC=%SRC% ../org/vishia/zbnf/*.java
set SRC=%SRC% ../org/vishia/zmake/*.java

echo generate docu: %SRC%
echo on

%JAVA_HOME%\bin\javadoc.exe -d %DST% -linksource -notimestamp %SRC%  1>%DST%\javadoc.rpt 2>%DST%\javadoc.err
%JAVA_HOME%\bin\javadoc.exe -d %DST%_priv -private -linksource -notimestamp %SRC%  1>%DST%_priv\javadoc.rpt 2>%DST%_priv\javadoc.err
copy stylesheet_javadoc.css %DST%\stylesheet.css
copy stylesheet_javadoc.css %DST%_priv\stylesheet.css
if "NOPAUSE" == "" pause

