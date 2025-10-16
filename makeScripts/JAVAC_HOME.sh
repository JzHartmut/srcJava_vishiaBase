#This script outputs the root directoriy of Java_Jdk.../bin to invoke java or javadoc
#to set in environment. export JAVAC_HOME="$($(dirname $0)/JAVAC_HOME.sh)"
#It sets the JAVAC_HOME variable to the text which is stdout of this file.
#Some possible locations were tested.
#If no proper location is found this program outputs "" it mains $JAVAC_HOME is set to empty.
#Then javac should be found in the systems PATH, it is the standard installation of JAVA_JDK.
#With a here detected abbreviating javac call another JDK can be used than the standard one.
#This enables a reproducible build with a selected tool 
#
#You should switch the order of JAVAC_HOME proposals to select the desired too.
#You should add more JAVAC_HOME proposals if necessary.
#
if test "$OS" = "Windows_NT"; then
  JAVAC_HOME=c:/Programs/Java/jdk1.8.0_241
  if test -d $JAVAC_HOME; then echo $JAVAC_HOME/; exit; fi
  JAVAC_HOME="c:/Program Files/Java/jdk1.8.0_241"
  if test -f $JAVAC_HOME; then echo $JAVAC_HOME/; exit; fi
  #should be found in path
  echo ""
else
  JAVAC_HOME="/usr/Java/JDK8_86zulu/"
  ##JAVAC_HOME="/usr/share/JDK/jdk1.8.0_241"
  if test -f $JAVAC_HOME; then echo $JAVAC_HOME/; exit; fi
  echo ""
fi