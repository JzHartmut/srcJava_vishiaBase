To translate Java source files the JDK (Java Developer Kit) is necessary. 
It should be installed or present in the file system by copying from a proper location. 
Note that a JDK is operation-system-depending, but it does not need to install in any case. 
If you have a well running JDK for the same operation system on another computer, 
it may run on another equal system by simple copying of the directory tree.

You can invoke the javac-Translator and all other exeutables of the JSK 
by enhancement of the PATH variable for the local compiling session.
Therefore a

 setJAVA_JDK.bat

sample file is included in this directory. 
You should copy this file on a location on your harddisk which is refer by the PATH.
Then adapt that file with your proper paths.

In that file the environment variable JAVA_JDK is set with the path to the JDK.
Then the System's PATH is enhanced with the necessary directories inside JDK, the bin-directories.
After them the javac Java compiler can be invoked with a simple command. 
This operation has not any effect on your system's settings.
It is only valid for the current session.

If the JDK is installed already and the PATH is enhanced to it as the start properties 
of your operation system, then the setJAVA_JDK.bat file may be contain only the setting of JAVA_JDK. 
That environment variable is checked in the compiling batches. 

If the JAVA_JDK environment variable is set on operation system level already, nothing should be done. All is okay. 

The PATH is that environment variable, which refers all executable files in the operation system to find out it.  

This principle of using the JAVA_JDK is valid for MS-Windows and any Unix/Linux version in the adequate manner.
But on Linux/Unix a called shell script has no effect on changing the environment, other than in MS-Windows.
Therefore for the Unix/Linux javac invocation the enhancement of the path should be done
in the file

 +javacjarbase.sh
 +genjavadocbase.sh
 
itself. You should edit that both files to adapt the directory path to JAVA_JDK. 
You can place that files in a directory which is refered in the PATH. Then the +javacjarbase.sh
and +genjavadocbase.sh are able to use for any Java compilation. 

To see how works a java compilation, look at

 _makejar_vishiaBase.bat (Windows)
 _makejar_vishiaBase.sh (Linux)
 
 