call bzr_call.bat V:\Bzr\srcJava_Zbnf exit
::NOTE: putty/pageant.exe runs. 
echo NOTE: SSH-key should be started
pause
call bzr launchpad-login hartmut-schorrig
pause
call bzr push lp:~hartmut-schorrig/zbnf/srcJava.zbnf
pause
