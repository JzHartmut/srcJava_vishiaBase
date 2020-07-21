#This script outputs a may be full path call of javac
#to set in environment. export JAVAC="$($(dirname $0)/JAVAC_CMD.sh)"
#It sets the JAVAC variable to the text which is stdout of this file.
#Some possible locations were tested.
#If no proper location is found this program outputs "javac"
#Then javac should be found in the systems PATH, it is the standard installation of JAVA_JDK.
#With a here detected abbreviating javac call another JDK can be used than the standard one.
#This enables a reproducible build with a selected tool 
#
#You should switch the order of JAVAC proposals to select the desired too.
#You should add more JAVAC proposals if necessary.
#
if test "$OS" = "Windows_NT"; then
  JAVAC=c:/Programs/Java/jdk1.8.0_241/bin/javac.exe
  if test -f $JAVAC; then echo $JAVAC; exit; fi
  JAVAC="c:/Program Files/Java/jdk1.8.0_241/bin/javac.exe"
  if test -f $JAVAC; then echo $JAVAC; exit; fi
  #should be found in path
  echo javac
else
  JAVAC="/usr/share/JDK/jdk1.8.0_241/bin/javac"
  if test -f $JAVAC; then echo $JAVAC; exit; fi
  echo javac
fi