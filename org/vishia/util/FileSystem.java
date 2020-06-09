package org.vishia.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;



/*Test with Jbat: call Jbat with this java file with its full path:
D:/vishia/Java/srcJava_vishiaBase/org/vishia/util/FileSystem.java
==JZcmd==
currdir = scriptdir;
Obj found = java org.vishia.util.FileSystem.searchInParent(File: ".", "_make/xgenjavadoc.bat", "ximg"); 
==endJZcmd==
*/





/**This class supports some functions of file system access as enhancement of the class java.io.File
 * independently of other classes of vishia packages, only based on Java standard.
 * Some methods helps a simple using of functionality for standard cases.
 * <br><br>
 * Note that the Java-7-Version supplies particular adequate functionalities in its java.nio.file package.
 * This functionality is not used here. This package was created before Java-7 was established. 
 * <br><br>
 * <b>Exception philosophy</b>:
 * The java.io.file classes use the exception concept explicitly. Thats right if an exception is not the normal case.
 * But it is less practicable if the throwing of the exception is an answer of an expected situation 
 * which should be though tested in any case.
 * <br><br>
 * Some methods of this class doesn't throw an exception if the success can be checked with a simple comparison
 * and it should be inquired in usual cases. For example {@link #getDirectory(File)} returns the directory or null 
 * if the input file is not existing. It does not throw a FileNotFoundException. The result is expected, the user
 * can do a null-check easily.
 * 
 */
public class FileSystem extends FileFunctions
{



}
