call bzr_call.bat V:\Bzr\srcJava_Zbnf 
::NOTE: putty/pageant.exe runs. 
D:\Progs\putty\PAGEANT.EXE V:\vishia\buero\keys\privateSSHkeyFromPuttyGen.bin
pause
call bzr launchpad-login hartmut-schorrig
pause
call bzr push lp:~hartmut-schorrig/zbnf/srcJava.zbnf
pause
