:loop
pause
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Base/FileRemote.adoc ../../../../html/Base
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Base/Events.adoc ../../../../html/Base
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat source+build/buildtestVishiaJava.adoc ../../../../html/source+build
call C:\Programs\Asciidoc\genAsciidoc2Html.bat source+build/src_Archive.adoc ../../../../html/source+build
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat Java_C/DataExchange_C.adoc ../../../../html/Java_C
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat RWTrans/RWTrans.adoc ../../../../html/RWTrans
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat RWTrans/XmlJzReader.adoc ../../../../html/RWTrans
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat RWTrans/ZbnfParser.adoc ../../../../html/RWTrans
::call C:\Programs\Asciidoc\genAsciidoc2Html.bat RWTrans/Calculator_RPN.adoc ../../../../html/RWTrans
echo done
goto :loop

