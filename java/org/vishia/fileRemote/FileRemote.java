package org.vishia.fileRemote;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventConsumerAwait;
import org.vishia.event.EventSource;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventTimerThread_ifc;
import org.vishia.event.EventWithDst;
import org.vishia.event.Payload;
import org.vishia.fileLocalAccessor.FileAccessorLocalJava7;
import org.vishia.util.Debugutil;
import org.vishia.util.ExcUtil;
import org.vishia.util.FileFunctions;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.MarkMask_ifc;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;
import org.vishia.util.TreeNodeNamed_ifc;


/**This class stores the name, some attributes of one File and also a List of all children 
 * if the file is a directory.
 * This class inherites from javaio.File to provide all capabilities of {@link File} 
 * but it does not immediately or never use the implementation of File. All operations are overridden.
 * The implementation is part of the aggregated {@link #device} which may use the capabilities of java.io.File
 * or also java.nio.files.  
 * <br>The file may be localized on any maybe remote device or it may be a normal file in the operation system's file tree.
 * The information about the file (date, size, attributes) are stored locally in the instance of this class,
 * hence they are accessible in a fast way without operation system access. 
 * But therefore they can be correct or not: A file on its data storage can be changed or removed. 
 * The data of this class should be refreshed if it seems to be necessary.
 * <br> 
 * Such an refresh operation can be done if there is enough time, no traffic on system level. 
 * Then the information about the file may be actual and fast accessible if an application needs one of them.
 * <br><br>
 * In comparison to the standard {@link java.io.File}, there the information are gotten from the operation system 
 * in any case, the access is correct but can be slow especially the file is located anywhere in network 
 * or in a specific device.
 * <br><br>
 * A remote file should be accessed by {@link FileRemoteAccessor} implementations referenced with {@link #device}. 
 * It may be any information on an embedded hardware, not only in a standard network. 
 * For example the access to zip archives are implemented by {@link FileAccessZip}. 
 * A file can be present only with one line in a text document which contains path, size, date.
 * In this case the content is not available, but the properties of the file. See {@link org.vishia.util.FileList}.
 * <br>
 * This class stores the following information (overview)
 * <ul>
 * <li>path, timestamp, length, flags for read/write etc. like known for file systems.
 * <li>parent and children of this file in its file tree context.
 * <li>time of last refresh: {@link #timeRefresh} and {@link #timeChildren}. With this information
 *   an application can decide whether the file should be {@link #refreshProperties(CallbackEvent)}
 *   or whether its propertees seems to be actual valid.   
 * <li>Aggregation to its {@link FileRemoteAccessor}: {@link #device} to refresh.
 * <li>Aggregation to any data which are necessary to access the physical file for the 
 *   FileRemoteAccessor implementation. This is an instance of {@link java.io.File} for local files.
 * <li>Aggregation to its {@link FileCluster}: {@link #itsCluster}. 
 *   The cluster assures that only one FileRemote instance exists for one physical file inside an application.
 *   In this case some additional properties can be stored to the FileRemote instance like selected etc.
 *   which are seen application-width.
 * <li>Information about a mark state. There are some bits for simple mark or for mark that it is
 *   equal, new etc. in any comparison. This bits can be used by applications.  
 * </ul>
 * <br>
 * <b>The cluster of files, {@link #clusterOfApplication}:</b><br>
 * The {@link FileCluster} assures that any file has only exact one FileRemote instance as presentation. 
 * On the one hand this prevents too many access to the file system. 
 * If the instance is refreshed a short time ago then it should not be refreshed for the same physical file
 * in another instance.
 * <br>
 * The other aspect is: If a file is marked, it is marked for the whole application.
 * Marking ({@link #setMarked(int)} is not supported by a file system itself but it is supported here.
 * The {@link #itsCluster} for a file (usual the {@link #clusterOfApplication}) is independent 
 * of the {@link #device()} which is the reference to the file system implementor.
 * <br><br>
 * <b>How to create instances of this class?</b><br>
 * The constructor of this class is protected, hence only internally accessible.
 * To create a FileRemote instance you can use
 * <ul><li>{@link #fromFile(File)} if you have already given a java.io.File instance.
 * <li>{@link #get(String)} with relative or absolute given path.
 * <li>{@link #getDir(CharSequence)} explicitely get or create a directory
 * <li>{@link #getFile(CharSequence, CharSequence)} in opposite to get(String) with dispersed directory path and name
 * <li>{@link #child(CharSequence)} from a given FileRemote as directory
 * <li>{@link #child(CharSequence, int, int, long, long, long)} to create a child with partially known properties
 * <li>{@link #subdir(CharSequence)} to get or create definitely a directory inside a given FileRemote directory. 
 * </ul>
 * All these operations do not access the real file system. Instead they search in the {@link #clusterOfApplication}
 * whether the file is already known and returns it, or they create and register the adequate FileRemote instance
 * in the cluster. 
 * The FileRemote instance should/can be synchronized with the real file instance by calling {@link #refreshProperties(CallbackEvent)}
 * or for directory instances with {@link #refreshPropertiesAndChildren()}
 * 
 * from the local file system you can call
 * {@link #fromFile(File)}. This delegates working to {@link #fromFile(FileCluster, File)}
 * with the static given {@link #clusterOfApplication} which looks whether the FileRemote instance 
 * is existing already, or registeres it in the file cluster. 
 * The implementor of the file system depends on the given path.
 * <br>
 * You can create a child of an existing file by given FileRemote directory instance
 * using {@link #child(CharSequence)}, {@link #subdir(CharSequence)}
 * or {@link #child(CharSequence, int, int, long, long, long)}
 * whereby the child can be a deeper one, the path is a relative path from the given parent. 
 * This FileRemote instance describes a possible existing file or maybe a non existing one, 
 * adequate like creation of instance  of {@link java.io.File}. 
 * Note that on {@link #refreshPropertiesAndChildren(CallbackEvent)} this instance will be removed
 * from the children list if it is not existing.
 * <br>
 * You can get any FileRemote instance with any absolute path calling {@link FileCluster#getFile(CharSequence)}.
 * This instance will be created if it is not existing, or it will be get from an existing instance
 * of this RemoteFile inside the cluster. The instance won't be deleted if the physical file is not existing.
 * For example you can create <pre>
 *   FileRemote testFile = theFileCluster.getFile("X:/MyPath/file.ext");
 * </pre>  
 * This instance is existing independent of the existence of such a physical file. But <pre>
 *   if(testFile.exists()){....
 * </pre>
 * may return false.   
 * <br><br>
 * This class inherits from {@link java.io.File} in the determination as interface. The advantage is:
 * <ul>
 * <li>The class File defines a proper interface to deal with files which can be used for the remote files concept too. 
 *   No extra definition of access methods is need.
 * <li>Any reference to an instance of this class can be store as reference of type java.io.File. The user can deal with it
 *   without knowledge about the FileRemote concept in a part of its software. Only the refreshing
 *   should be regulated in the proper part of the application. 
 * </ul>
 * <br>
 * <br>
 * <b>Concepts of callback</b>: <br>
 * A callback is an instance of the {@link FileRemoteWalkerCallback} which is given either by the application 
 * or given for specific commands in the implementation in the device. 
 * It is used to do actions with any (selected) file or directory.
 * 
 * <br>
 * This class offers some methods to deal with directory trees:
 * <ul>
 * <li>{@link #refreshAndMark(int, boolean, String, int, int, FileRemoteWalkerCallback)} marks files for copy, move or delete.
 * <li>{@link #refreshAndCompare(FileRemote, int, String, int, FileRemoteProgressEvData)} compares two trees of files.
 * <li>{@link #copyChecked(String, String, int, CallbackEvent)} copies some or all files in a tree.
 * <li>{@link #deleteChecked(CallbackEvent, int)} deletes some or all files in a tree.
 * </ul> 
 * This operations may need more seconds, sometimes till minutes if there are a lot of files in a remote device with network access.
 * To show the progress two startegies are used:
 * <ul>
 * <li>{@link FileRemoteWalkerCallback}: It is a callback interface with methods for each file and each started and finished directory.
 * <li>{@link FileRemoteProgressEvData}: It is an instance which is filled with data. The time order is handled by a timer
 *   which activates it for example in a cycle for 200 milliseconds. The user extends this base class with the showing method. 
 *   In a time range of 200 milliseconds there can be executed some more files, for example 1000 if the file system is local. 
 *   Not any file forces to be showed. Therewith calculation time is saved.  
 * </ul>
 * The user can provide one of that, both or no instance for showing. 
 * Handling of a event driven communication need one event per file. It is too much effort if the file system is fast.
 * That is tested in the past but not supported furthermore.
 * <br>
 * <br>
 * <b>Concepts of feedback with events</b>:<br>
 * There are three concepts generally in software how to deal with feedback:
 * <ul><li>a) offer a return value which contains feedback information
 * <li>b) throw an exception if any stuff is wrong, if all is ok, then no further feedback is given. All is ok.
 * <li>c) send an event
 * </ul>
 * All three possibilities are usual in Java programming and especially for file handling:
 * <ul><li>a) is used for example for {@link File#mkdir()} or {@link File#delete()}. 
 *   This operations are the first in the Java of the 1990th presuming a simple file system. 
 * <li>b) Exception handling becomes very familiar for Java programming, used overall. For example {@link java.io.FileReader#read(char[])}
 *   does not return a negative number on reading error which may be possible, instead throws.
 *   An FileReader.open() which may return an information does not exist, instead the constructor is used for open, 
 *   which throws an exception if the file does not exist.
 *   This means effort for exception handling though the situation of a non existing file is expectable.
 *   Exception handling should be intrinsically only done on real faulty situations.
 * <li>c) Event handling is a little bit unusual in Java programming, else for graphic applications.
 * </ul>
 * Because the operations of this class are frequently used also in graphic applications, the event handling should be the first choice.
 * But some operations compatible to {@link File} needs a). Exception handling is done only on really exceptions. 
 * <br><br>
 * The second question is: Execute the file handling in an extra thread.
 * <br><br>
 * The solution is the following:
 * <ul><li>All operations can be called via a given back or progress event instance. 
 *   The type of the back event is {@link EventWithDst} with {@link FileRemoteProgressEvData} as data type.
 *   The back or progress event can be used also to get information about the progress of a longer operation, 
 *   for example copy long files. But it is also used for the success or error information. 
 * <li>Operations which can be executed in the same thread returns a String information if something is wrong but expectable.
 *   exception throwing is used only on really exceptions or it comes from the implementation level.
 *   The return String is null if all is ok. 
 * <li>On access to a really remote file system using the same thread is not possible because communication is necessary.
 *   Hence general using the back event is recommended if the file system may be remote.
 *   The return value can only present whether or not the communication was initiated.
 * <li>The decision using the same thread (the operation may be need a longer time for example on access via network)
 *   or using an extra (given, prepared) thread if it is possible to do the action in the same thread
 *   depends on the offering the back event. If it is given, the action is executed in an extra thread.
 * <li>But for some test cases or application specific the back event can be given as information destination,
 *   and nevertheless the same thread should be used.
 *   This decision is set as information in the      
 * <li>The back event can be absent. Then detail information about the progress are not possible. 
 *   If the    
 * </ul>     
 * 
 * @author Hartmut Schorrig
 *
 */
public class FileRemote extends File implements MarkMask_ifc, TreeNodeNamed_ifc
{
  /**interface to build an instance, which decides about the instance for physical access to files
   * in knowledge of the path. The implementing instance should be present as singleton on instantiation of this class.
   */
  public interface FileRemoteAccessorSelector
  {
    /**The structure of path, usual a start string should be decide which access instance should be used.
     * For example "C:path" accesses the local disk but "$Device:path" should access an embedded device.
     * @param sPath
     * @return
     */
    @SuppressWarnings("resource")
    FileRemoteAccessor selectFileRemoteAccessor(CharSequence sPath);
  }

  private static final long serialVersionUID = -5568304770699633308L;

  /**Version, history and license.
   * <ul>
   * <li>2025-12-18 {@link #moveTo(FileRemote, EventWithDst)} 
   * <li>2024-02-12 The {@link #cmdRemote(org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, String, int, int, int, FileRemoteCmdEventData, EventWithDst)}
   *   has now beside the String selectFilter the int bMaskSel. This CAN be used (is not yet) for selection via bits (TODO test may be run),
   *   but the importance yet used is the bit {@link FileMark#ignoreSymbolicLinks}. This was the reason of change. 
   *   Other select possibilities may work because there are regarded by the {@link FileAccessorLocalJava7}. Test it!
   * <li>2023-07-24 the CallbackWait was no more used, removed (cleanup), the concept is adequate {@link FileRemoteProgressEventConsumer}. 
   * <li>2023-07-19 move and create {@link FileRemoteCmdEventData} from the inner class CmdEventData. 
   *   That causes using {@link FileRemoteCmdEventData#setCmdWalkRemote(FileRemote, org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, String, int, int)}
   *   etc. instead immediately writing to its variables. Its better to obvious the software goal!
   * <li>2023-07-19 rename {@link #walkRemote(boolean, FileRemote, int, String, int, EventWithDst)} from 'walker' 
   * <li>2023-07-18 rename {@link Cmd#walkRefresh} from Cmd.walkSelectMark, the name is more concise. 
   * <li>2023-07-18 new {@link #walkLocal(Cmd, FileRemote, int, int, String, int, int, FileRemoteWalkerCallback, FileRemoteCmdEventData, int, EventWithDst)}
   *   it uses the moved and renewed {@link FileRemoteWalker}. 
   * <li>2023-07-17 remove some old stuff for delete, cleanup
   * <li>2023-07-17 {@link FileRemoteCmdEventData} has now private members and getter. It should be used consequently as payload for a remote access.
   *   Yet it is obfuscated, it is necessary to know the FileRemote instance while working with. But this is not possible if it is really remote.
   * <li>2023-04-06 Hartmut chg: {@link #copyTo(FileRemote, EventWithDst)} and {@link #moveTo(String, FileRemote, FileRemoteProgressEvData)}
   *   now via device.cmd(), the new approach. Used in The.file.Commander  
   * <li>2023-04-02 Hartmut chg: {@link #getPathChars()} now returns dir path ends with slash. 
   *   This is in opposite to {@link #getPath()}, unchanged, defined by {@link File#getPath()}.
   * <li>2023-03-15 Hartmut chg: refactoring {@link #get(String)}, {@link #getFile(CharSequence, CharSequence)}, {@link #getDir(String)}
   * <li>2023-02-13 Hartmut chg: {@link #refreshAndMark(int, int, int, String, long, FileRemoteWalkerCallback, FileRemoteProgressEvData)}
   *   the depths should be unique as first or second argument, todo change the other operations adequate 
   * <li>2023-02-13 Hartmut new: {@link #getDirFileDst(String, FileRemote, String[])} usable for String given dir and mask
   * <li>2023-01-02 Hartmut chg: {@link #fromFile(File)} and {@link #child(CharSequence, int, int, long, long, long)}:
   *   Generally on creating a FileRemote it should not has any access to the file system, because it is remote.
   *   This is now consequently realized. Till now only the PC file system was used, 
   *   hence one access was usual not obviously. But on access to a non existing network drive, sometimes trouble occur.
   *   For The.file.Commander the rule is: Do not access to resources in the graphic thread which are not guaranteed existing.
   *   The graphic thread should never be delayed. This is now true (should be true).   
   * <li>2015-09-09 Hartmut bug: It marks too much directories with {@link FileMark#selectSomeInDir}, fix: algorithm. 
   * <li>2015-07-04 Hartmut chg: Values of flags {@link #mTested}, {@link #mShouldRefresh}, {@link #mRefreshChildPending}
   *   {@link #mThreadIsRunning} to the highest digit of int, to recognize for manual viewing. That flags should not used in applications.
   *   Remove of flags for comparison, the bits are used and defined inside {@link FileMark} for a longer time.  
   * <li>2015-05-30 Hartmut new: {@link #copyDirTreeTo(FileRemote, int, String, int, FileRemoteProgressEvData)} 
   * <li>2015-05-25 Hartmut new: {@link #clusterOfApplication}: There is a singleton, the {@link FileCluster}
   *   is not necessary as argument for {@link #fromFile(File)} especially to work more simple to use FileRemote
   *   capabilities for File operations.
   * <li>2014-12-20 Hartmut new: {@link #refreshPropertiesAndChildren(CallbackFile)} used in Fcmd with an Thread on demand, 
   *   see {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7#walkFileTree(FileRemote, boolean, boolean, String, int, int, FileRemoteWalkerCallback)}
   *   and {@link FileRemoteAccessor.FileWalkerThread}.
   * <li>2014-07-30 Hartmut new: calling {@link FileAccessorLocalJava6#selectLocalFileAlways} in {@link #getAccessorSelector()}
   *   for compatibility with Java6. There the existence of java.nio.file.Files is checked, and the File access
   *   in Java7 is used if available. So this class runs with Java6 too, which is necessary in some established environments.
   * <li>2013-10-06 Hartmut new: {@link #getChildren(ChildrenEvent)} with aborting a last request.
   * <li>2013-09-15 Hartmut new: {@link #getChildren(ChildrenEvent)} should work proper with
   *   {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}.
   *   The event should be called back on 300 ms with the gathered files. If the access needs more time,
   *   it should be able to have more events to present a part of files, or to abort it.
   *   The concept is tested in java-6 yet.  
   * <li>2013-09-15 Hartmut new: Ideas from Java-7 java.nio.Files in the concept of FileRemote: 
   *   The FileRemote is an instance independent of the file system and stores the file's data. 
   *   If Java-7 is present, it should be used. But the Java-6 will be held compatible.
   * <li>2013-08-09 Hartmut chg: The {@link MarkMask_ifc} is renamed, all methods {@link #setMarked(int)}
   *   etc. are named 'marked' instead of 'selected'. 
   *   It is a problem of wording: The instances are only marked, not yet 'selected'. See application
   *   of this interface in {@link org.vishia.gral.widget.GralFileSelector}: A selected line is the current one
   *   or some marked lines.
   * <li>2013-08-09 Hartmut new: implements {@link MarkMask_ifc} for the {@link #cmprResult}.
   * <li>2013-07-29 Hartmut chg: {@link #moveTo(String, FileRemote, CallbackEvent)} now have a parameter with file names. 
   * <li>2013-05-24 Hartmut chg: The root will be designated with "/" as {@link #sFile}
   * <li>2013-05-24 Hartmut new {@link #cmprResult}, {@link #setMarked(int)}
   * <li>2013-05-05 Hartmut new {@link #isTested()}
   * <li>2013-05-05 Hartmut new {@link #mkdir()}, {@link #mkdirs()}, {@link #mkdir(boolean, CallbackEvent)}, 
   *   {@link #createNewFile()}. 
   *   TODO some operation uses still the super implementation of File.
   *   But the super class File should only be used as interface.
   * <li>2013-05-05 Hartmut chg: {@link #child(CharSequence)}  accepts a path to a sub child.
   *   New {@link #isChild(CharSequence)}, {@link #isParent(CharSequence)}.  
   * <li>2013-05-04 Hartmut redesigned ctor and order of elements. sDir does not end with "/" up to now.
   * <li>2013-04-30 Hartmut new: {@link #resetMarkedRecurs(int, int[])}
   * <li>2013-04-29 Hartmut chg: {@link #fromFile(FileCluster, File)} has the FileCluster parameter yet, necessary for the concept.
   *   This method is not necessary as far as possible because most of Files are a FileRemote instance yet by reference (Fcmd app)
   * <li>2013-04-28 Hartmut chg: re-engineering check and copy
   * <li>2013-04-26 Hartmut chg: The constructors of this class should be package private, instead the {@link FileCluster#getFile(String, String)}
   *   and {@link #child(String)} should be used to get an instance of this class. The instances which refers the same file
   *   on the file system are existing only one time in the application respectively in the {@link FileRemoteAccessor}. 
   *   In this case additional information can be set to files such as 'selected' or a comparison result. It is in progression.  
   * <li>2013-04-21 Hartmut new: The {@link #flags} contains bits for {@link #mChecked} to mark files as checked
   *   with {@link #check(FileRemote, CallbackEvent)}. With that the check state is able to view.
   * <li>2013-04-21 Hartmut new: The {@link #flags} contains bits for {@link #mCmpContent} etc. for comparison to view.  
   * <li>2013-04-21 Hartmut new: {@link #copyChecked(CallbackEvent, int)} in cohesion with changes 
   *   in {@link org.vishia.fileLocalAccessor.Copy_FileLocalAcc}.
   * <li>2013-04-12 Hartmut chg: Dedicated attributes for {@link CallbackCmd#successCode} etc.
   * <li>2013-04-07 Hartmut adapt: Event<?,?> with 2 generic parameter
   * <li>2013-04-07 Hartmut chg: {@link CallbackEvent} contains all the methods to do something with currently copying files,
   *   for example {@link CallbackEvent#copyOverwriteFile(int)} etc.
   * <li>2013-03-31 Hartmut chg: Event<Type>
   * <li>2012-11-16 Hartmut chg: Usage of {@link FileRemoteCmdEventData#filesrc} and filedst and {@link CallbackEvent#filedst} and dst
   *   instead {@link EventCmdtypeWithBackEvent#getRefData()}.
   * <li>2012-11-11 Hartmut chg: The flag bit {@link #mDirectory} should be set always, especially also though {@link #mTested} is false.
   *   That should be assured by the {@link FileRemoteAccessor} implementation.
   * <li>2012-10-01 Hartmut chg: {@link #children} is now of super type File, not FileRemote. Nevertheless FileRemote objects
   *   are stored there. Experience is possible to store a File object returned from File.listFiles without wrapping, and
   *   replace that with a FileRemote object if necessary. listFiles() returns a File[] like its super method.
   * <li>2012-10-01 Hartmut new: {@link #isTested()}  
   * <li>2012-08-11 Hartmut new: method {@link #openInputStream(long)}. An application may need that, for example to create
   *   a {@link java.io.Reader} with the input stream. Some implementations, especially a local file and a {@link java.util.zip.ZipFile}
   *   supports that. An {@link java.io.InputStream} may force a blocking if data are not available yet for file in a remote device
   *   but that may be accepted. 
   * <li> 2012-08-11 Hartmut new: {@link #listFiles(FileFilter)} now implemented here. 
   * <li>2012-08-05 Hartmut chg: The super class File needs the correct path. So it is able to use for a local file nevertheless.
   *   What is with oFile if it is a FileRemote? should refer this? See change from 2012-01-01.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-28 Hartmut chg: Concept of remote files enhanced with respect to {@link FileAccessZip}.
   *   <ul>
   *   <li>New references {@link #parent} and {@link #children}. They are filled calling {@link #refreshPropertiesAndChildren(CallbackEvent)}.
   *   <li>More separation of java.io.File accesses. In the past only the local files were supported really.
   *   <li>new interface {@link FileRemoteAccessorSelector} and {@link #setAccessorSelector(FileRemoteAccessorSelector)}.
   *     The user can have any algorithm to select a {@link FileRemoteAccessor} depending on the
   *     path of the file. A prefix String may determine how the file is to access. If that routine
   *     is not called, the {@link FileAccessorLocalJava7#selectLocalFileAlways}.
   *   <li>{@link #FileRemote(FileRemoteAccessor, FileRemote, String, String, long, long, int, Object)}
   *     has the parent as parameter. The parameter oFileP is stored now. It is any data to access the file object.
   *   <li>The constructor had access the file if length=-1 was given. But that is not the convention.
   *     An access may need execution and waiting time for a remote communication. The constructor
   *     should never wait. Instead the methods:
   *   <li>{@link #refreshProperties(CallbackEvent)} and {@link #refreshPropertiesAndChildren(CallbackEvent)}
   *     have to be called if the properties of the real file on the local system (java.io.File)
   *     or any remote system are need. That routines envisages the continuation of working
   *     with a callback event are invocation mechanism. For example if the file properties
   *     should be shown in a graphic application, the building of the graphic can't stop and wait 
   *     for more as some 100 milliseconds. It is better to clear a table and continue working in graphic. 
   *     If the properties are gotten from the remote system then the table will be filled.
   *     That may be invoked from another thread, the communication thread for the remote device
   *     or by an event mechanism (see {@link FileRemote.CallbackEvent} respectively {@link org.vishia.event.EventCmdtypeWithBackEvent}.
   *   <li>The routine {@link #fromFile(File)} reads are properties of a local file if one is given.
   *     In that case the {@link #refreshProperties(CallbackEvent)} need not be invoked additionally.
   *   <li>{@link #openRead(long)} and {@link #openWrite(long)} accepts a non-given device.
   *     They select it calling {@link FileRemoteAccessorSelector#selectFileRemoteAccessor(String)}
   *   <li>All get methods {@link #length}, {@link #lastModified()}, {@link #isDirectory()} etc.
   *     now returns only the stored values. It may necessary to invoke {@link #refreshProperties(CallbackEvent)}
   *     in the application before they are called to get the correct values. The refreshing
   *     can't be called in that getter routines because they should not wait for communication.
   *     In the case of local files that access may be shorten in time, but it isn't known
   *     whether it is a local file. The user algorithm should work with remote files too if they are
   *     tested locally only. Therefore a different strategy to access properties are not proper to use.
   *   <li>{@link #getParentFile()} now uses the {@link #parent} reference. If it is null,
   *     a new FileRemote instance for the parent is created, but without access to the file,
   *     only with knowledge of the path string. Because the {@link #FileRemote(FileRemoteAccessor, FileRemote, String, String, long, long, int, Object)}
   *     will be gotten the parent of it too, all parent instances will be set recursively then.
   *   <li>{@link #listFiles()} now returns the {@link #children} only. If the user has not called
   *     {@link #refreshPropertiesAndChildren(CallbackEvent)}, it is empty.           
   *   </ul>
   * <li>2012-07-21 Hartmut new: {@link #delete(String, boolean, EventCmdtypeWithBackEvent)} with given mask. TODO: It should done in 
   *   {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7} in an extra thread.
   * <li>2012-03-10 Hartmut new: {@link #chgProps(String, int, int, long, CallbackEvent)}, {@link #countAllFileLength(CallbackEvent)}.
   *   Enhancements.
   * <li>2012-02-02 Hartmut chg: Now the {@link #sFile} (renamed from name) is empty if this describes
   *   an directory and it is known that it is an directory. The ctor is adapted therefore.
   *   {@link #getParent()} is changed. Some assertions are set.
   * <li>2012-02-02 Hartmut chg: Handling of relative paths: It is detected in ctor. TODO relative paths are not tested well. 
   * <li>2012-01-14 Hartmut chg: The toplevel directory contains only one slash in the {@link #sDir}
   *   and an empty name in {@link #key}. 
   * <li>2012-01-14 Hartmut new: {@link #getParentFile()} now implemented here.  
   * <li>2012-01-14 Hartmut new: {@link #fromFile(File)} to convert from a normal File instance.
   * <li>2012-01-06 Hartmut new: Some functionality for {@link #_setProperties(long, long, int, Object)}
   *   and symbolic linked paths.
   * <li>2012-01-01 Hartmut new: {@link #oFile}. In the future the superclass File should be used only as interface.
   *   TODO: For any file access the oFile-instance should be used by {@link #device}.
   * <li>2012-01-01 Hartmut new: {@link #copyTo(FileRemote, EventCmdPingPongType)}
   * <li>2011-12-10 Hartmut creation: It is needed for {@link org.vishia.commander.Fcmd}, this tool
   *   should work with remote files with any protocol for example FTP. But firstly it is implemented and tested
   *   only for local files. The concept is: 
   *   <ul>
   *   <li>All relevant information about a file are stored locally in this instance if they are known.
   *   <li>If information are unknown, on construction 'unknown' is stored, no access occurs.
   *     But if they are requested, an access is done.
   *   <li>Any instance of the interface {@link FileRemoteAccessor} is responsible to execute the remote access.
   *   <li>The access to local files are done with this class directly.
   *   </ul>
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final String version = "2025-12-18";

  public final static int modeCopyReadOnlyMask = 0x00f
  , modeCopyReadOnlyNever = 0x1, modeCopyReadOnlyOverwrite = 0x3, modeCopyReadOnlyAks = 0;

  public final static int modeCopyExistMask = 0x0f0
  , modeCopyExistNewer = 0x10, modeCopyExistOlder = 0x20, modeCopyExistAll = 0x30, modeCopyExistSkip = 0x40, modeCopyExistAsk = 0;
  
  public final static int modeCopyCreateMask = 0xf00
  , modeCopyCreateNever = 0x200, modeCopyCreateYes = 0x300 , modeCopyCreateAsk = 0;
  
  
  /**Bits for the logging mode of {@link #cmprDirs(File, File, String, List)}.
   * 
   */
  public static final int modeCmprLogNotEqualFiles = 0x1, modeCmprLogMissing2File = 0x2
      , modeCmprLogMissing1File = 0x4, modeCmprLogComparedFiles = 0x8;
  
  /**Info about the file stored in {@link #flags} returned with {@link #getFlags()}. */
  public final static int  mExist =   1, mCanRead =  2, mCanWrite =  4, mHidden = 0x08;
  
  /**Info whether the File is a directory. This flag-bit should be present always independent of the {@link #mTested} flag bit. */
  public final static int  mDirectory = 0x10, mFile =     0x20;
  
  /**Info whether it is an executable (executable flag on unix)*/
  public final static int  mExecute =     0x40, mExecuteAny =     0x80;
 
  /**Type of given path. */
  public final static int  mRelativePath = 0x100, mAbsPath = 0x200;
  
  /**A symbolic link in unix. */
  public final static int  mSymLinkedPath = 0x400;
  
  /**Group and any read and write permissions. */
  public final static int  mCanReadGrp =  0x0800, mCanWriteGrp = 0x1000, mExecuteGrp =  0x2000
  , mCanReadAny =  0x4000, mCanWriteAny = 0x8000;

  
  
  /**Set if it is a root directory. The {@link #mDirectory} is set too. */
  public final static int mRoot = 0x00100000;
  
  /**Flags as result of an comparison: the other file does not exist, or exists and has another length or time stamp */
  //public final static int mCmpTimeLen = 0x03000000;
  
  /**Flags as result of an comparison: the other file exist but its content is different. See */
  //public final static int mCmpContent = 0x0C000000;
  

  public final static int  mShouldRefresh = 0x10000000;
  
  /**Set if a thread runs to get file properties. */
  public final static int mThreadIsRunning =0x20000000;

  /**Set if the file is removed yet because a refresh is pending. Note that the FileRemote instance should be kept for marking etc.
   */
  public final static int  mRefreshChildPending = 0x40000000;
  
  /**Set if the file is tested physically. If this bit is not set, all other flags are not representable and the file
   * may be only any path without respect to an existing file.
   */
  public final static int  mTested =        0x80000000;
  
  /**Instance of the application-width selector for {@link FileRemoteAccessor}.
   */
  private static FileRemoteAccessorSelector accessorSelector;
  
  /**Counter, any instance has an ident number. */
  private static int ctIdent = 0;
  

  
  /**A indent number, Primarily for debug and test. */
  private final int _ident;
  
  /**This cluster is used if a specific cluster should not be used. It is the standard cluster.
   * With null as argument for the FileCluster, this cluster is used.
   */
  public static final FileCluster clusterOfApplication = new FileCluster();
  
  /**Any FileRemote instance should be member of a FileCluster. Files in the cluster can be located on several devices.
   * But they are selected commonly.
   * */
  public final FileCluster itsCluster;
  
  /**The device which manages the physical files. For the local file system the 
   * {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7} is used. */
  protected FileRemoteAccessor device;
  
  /**A mark and count instance for this file. It is null if it is not necessary. */
  protected FileMark mark;
  
  /**The last time where the file was synchronized with its physical properties. */
  public long timeRefresh, timeChildren;
  
  /**The directory path of the file. It does not end with "/"
   * The directory path is absolute and normalized. It doesn't contain any "/./" 
   * or "/../"-parts. 
   * <br>
   * This absolute path can contain a start string for a remote device designation respectively the drive designation for MS-Windows.
   * <br> 
   * If the instance is the root, this element contains an empty String in UNIX systems, the "C:" drive in Windows
   * or a special remote designation before the first slash in the path.
   */  
  protected String sDir;
  
  /**The name with extension of the file or directory name. 
   * If this instance is the root, this element contains "/".
   * Note: It is not final because the name may be varied in upper or lower case on Windows file system.
   * It is possible that first the file is defined in other case writing, then corrected on refresh. 
   */
  protected String sFile;
  
  /**The unique path to the file or directory entry. If the file is symbolic linked (on UNIX systems),
   * this field contains the non-linked direct path. But the {@link #sDir} contains the linked path. 
   */
  protected String sCanonicalPath;
  
  /**Timestamp of the file. */
  protected long date, dateCreation, dateLastAccess;
  
  /**Length of the file. */
  protected long length;
  
  /**Some flag bits. See constants {@link #mExist} etc.*/
  protected int flags;

  /**The parent instance, it is the directory where the file is member of. This reference may be null 
   * if the parent instance is not used up to now. If it is filled, it is persistent.
   */
  FileRemote parent;
  
  /**The content of a directory. It contains all files, proper for return {@link #listFiles()} without filter. 
   * The content is valid at the time of calling {@link #refreshPropertiesAndChildren(boolean, EventWithDst)} or its adequate.
   * It is possible that the content of the physical directory is changed meanwhile.
   * If this field should be returned without null, especially on {@link #listFiles()} and the file is a directory, 
   * the {@link #refreshPropertiesAndChildren(boolean, EventWithDst)
   * */
  private Map<String,FileRemote> children;
  
  /**This is the internal file object. It is handled by the device only. */
  Object oFile;
  
  
  /**can be null or set with the valid path.
   * It is the concept using java.nio.file.
   * Note: This element hides the File#path of the super class which is final and private.
   * It should not confuse the user.
   */
  protected Path path;
  
  
  /**Constructs an instance. The constructor is protected because only special methods
   * constructs an instance in knowledge of existing instances in {@link FileCluster}.
   * <br><br>
   * This invocation does not force any access to the file system. The parameter may be given
   * by a complete communication or file access before construction of this. 
   * Then they are given as parameter for this constructor.
   * <br><br>
   * The parameter of the file (properties, length, date) can be given as 'undefined' 
   * using the 0 as value. Then the quest {@link #exists()} returns false. This instance
   * describes a File object only, it does not access to the file system.
   * The properties of the real file inclusively the length and date can be gotten 
   * from the file system calling {@link #refreshProperties(CallbackEvent)}. This operation may be
   * invoked in another thread (depending on the device) and may be need some operation time.
   * 
   *  
   * @param cluster null, then use {@link #clusterOfApplication}, elsewhere the special cluster where the file is member of. 
   *   It can be null if the parent is given, then the cluster of the parent is used. 
   *   It have to be matching to the parent. It do not be null if a parent is not given. 
   *   A cluster can be created in an application invoking the constructor {@link FileCluster#FileCluster()}.
   * @param device The device which organizes the access to the file system. It may be null, then the device 
   *   will be gotten from the parent or from the sDirP.
   * @param parent The parent file if known or null. If it is null, the sDirP have to be given with the complete absolute path.
   *   If parent is given, this file will be added to the parent as child.
   * @param sPath The path to the directory. If the parent file is given, either it have to be match to or it should be null.
   *   The standard path separator is the slash "/". 
   *   A backslash will be converted to slash internally, it isn't distinct from the slash.
   *   If this parameter ends with an slash or backslash and the name is null or empty, this is designated 
   *   as an directory descriptor. {@link #mDirectory} will be set in {@link #flags}.
   * @param length The length of the file. Maybe 0 if unknown. 
   * @param date Timestamp of the file. Maybe 0 if unknown.
   * @param flags Properties of the file. Maybe 0 if unknown.
   * @param oFileP an system file Object, may be null.
   * @param OnlySpecialCall
   */
  /*
   *    * If the length parameter is given or it is 0, 

   */
  protected FileRemote(final FileCluster cluster, final FileRemoteAccessor device
      , final FileRemote parent
      , final CharSequence sPath //, CharSequence sName
      , final long length, final long dateLastModified, long dateCreation, long dateLastAccess, final int flags
      , Object oFileP, boolean OnlySpecialCall) {
    //super("??"); //sDirP + (sName ==null ? "" : ("/" + sName)));  //it is correct if it is a local file. 
    super(parent == null ? sPath.toString() : parent.getPath() + "/" + sPath);  //it is correct if it is a local file. 
//    if(StringFunctions.contains(sPath, "testCopyDirTree"))
//      Debugutil.stop();
    if(parent !=null){
      this.parent = parent;
      this.sDir = parent.getAbsolutePath();
      this.sFile = sPath.toString();
      if(cluster !=null && cluster != parent.itsCluster){ 
        throw new IllegalArgumentException("FileRemote.ctor - Mismatching cluster association; parent.itsCluster=" 
            + parent.itsCluster.toString() + ";  parameter cluster=" + cluster.toString() + ";");
      }
      this.itsCluster = parent.itsCluster;
    } else {
      this.parent = null;
      int posSep = StringFunctions.lastIndexOf(sPath, '/');
      if(posSep >=0){
        int zPath = sPath.length();
        this.sDir = posSep ==0 ? "/" : sPath.subSequence(0, posSep).toString();
        if((posSep == 0 && zPath ==1) || (posSep ==2 && sPath.charAt(1)== ':' && zPath == 3)){
          //it is the root  
          this.sFile = "/";
        } else {
          this.sFile = sPath.subSequence(posSep+1, sPath.length()).toString();
          //sFile maybe "" if sPath ends with "/"
        }
      } else {
        this.sDir = "";  //it is a local file
        this.sFile = sPath.toString();
      }
      if(cluster == null) this.itsCluster = clusterOfApplication;
      else this.itsCluster = cluster;
    }
    //super("?");  //NOTE: use the superclass File only as interface. Don't use it as local file instance.
    this._ident = ++ctIdent;
    this.device = device;
    this.flags = flags;
    ExcUtil.check(this.sDir !=null);
    //Assert.check(sName.contains("/"));
    //Assert.check(this.sDir.length() == 0 || this.sDir.endsWith("/"));
    //TODO Assert.check(!this.sDir.endsWith("//"));
    ExcUtil.check(length >=0);
    this.oFile = oFileP;
    this.length = length;
    this.date = dateLastModified;
    this.dateCreation = dateCreation;
    this.dateLastAccess = dateLastAccess;
    final String sPath1 = this.sDir + (sFile !=null ? "/"  + this.sFile : "");  //maybe overwrite from setSymbolicLinkedPath
    this.sCanonicalPath = sPath1;
    this.path = Paths.get(sPath1);
    ///
    if(this.device == null){
      this.device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(parent !=null){
      parent.putNewChild(this);
    }
    //System.out.println("FileRemote - ctor; " + _ident + "; " + sCanonicalPath);
  }
  
  
  public Path path() { return this.path; }
  
  public File file() { return this.oFile instanceof File ? (File)this.oFile : null; } 
  
  
  
  /**Gets or creates a child with the given name. 
   * If the child is not existing, it is create as a new FileRemote instance which is not tested.
   * The created instances is not registered in the {@link #itsCluster} because it is assumed that it is a file, not a directory.
   * But all created sub directories of a given local path are registered.
   * @param sPathChild Name or relative pathname of the child.
   *   It can be contain "/" or "\", if more as one level of child should be created. If it has a '/' on end it is created as a sub directory
   *   and registered in its {@link #itsCluster}.
   * @return The child, not null.
   */
  public FileRemote child(CharSequence sPathChild){
    return child(sPathChild, 0, 0, 0,0, 0);
  }
  
  /**Gets or creates a child with the given name which is a sub directory of this. 
   * If the directory is not existing, it is create as a new FileRemote instance which is not tested.
   * The created instances is registered in the {@link #itsCluster}.
   * @param sPathChild Name or relative pathname of the child.
   *   It can be contain "/" or "\", if more as one level of child should be created. It may have a '/' on end or not.
   * @return The child, not null.
   */
  public FileRemote subdir(CharSequence sPathChild){
    return child(sPathChild, mDirectory, 0, 0,0, 0);
  }
  
  /**Returns the instance of FileRemote which is the child of this. If the child does not exists
   * it is created and added as child. That is independent of the existence of the file on the file system.
   * A non existing child is possible, it may be created on file system later.
   * <br><br>
   * All created sub directories are registered in {@link #itsCluster}
   * <br>
   * TODO: test if the path mets a file but the path is not on its end. Then it may be a IllegalArgumentException. 
   * @param sName Name of the child or a local path from this to a deeper sub child. If the name ends with a slash
   *   or backslash, the returned instance will be created as sub directory.
   * @return The child instance. 
   */
  private FileRemote child(CharSequence sPathChild, int flags, int length
      , long dateLastModified, long dateCreation, long dateLastAccess 
      ) {                      //TODO: Test with path/path
    CharSequence pathchild1 = FileSystem.normalizePath(sPathChild);
    StringPart pathchild;
    int posSep1;
    if(( posSep1 = StringFunctions.indexOf(pathchild1, '/', 0))>=0){
      //NOTE: pathchild1 maybe an instanceof StringPartBase too, but it acts as a CharSequence! 
      pathchild = new StringPart(pathchild1, 0, pathchild1.length());
      pathchild1 = pathchild.lento('/');  //Start with this part of pathchild.
    } else {
      pathchild = null; //only one child level.
    }
    FileRemote dir1 = this;  //the parent dir of child
    FileRemote child;
    boolean bCont = true;
    int flagNewFile = 0; //FileRemote.mDirectory;
    StringBuilder uPath = new StringBuilder(100);
    boolean bWindows = this.sDir.length()>=2 && sDir.charAt(1) == ':';
    do{
      uPath.setLength(0);
      dir1.setPathTo(uPath).append('/').append(pathchild1);
      final CharSequence pathchildKey = bWindows ? pathchild1.toString().toUpperCase(): pathchild1;
      child = dir1.children == null ? null : dir1.children.get(pathchildKey.toString()); //search whether the child is known already.
      if(child == null) {
        if( pathchild !=null){  //a child given with a pathchild/name
          //maybe the child directory is registered already, take it.
          child = itsCluster.getFile(uPath, null, false);  //try to get the child from the cluster.
        } 
        else {             
          child = new FileRemote(itsCluster, device, dir1, pathchild1, length
                , dateLastModified,dateCreation,dateLastAccess, flags, null, true);
          dir1.putNewChild(child);                       
        }
      } else {
        if(StringFunctions.compare(child.sFile, pathchild1)!=0) {
          child.sFile = pathchild1.toString();   // correct the name writing with correct upper/lower case.
        }
      }
      if(pathchild !=null){
        dir1 = child;
        pathchild1 = pathchild.fromEnd().seek(1).lento('/');
        if(!pathchild.found()){
          //no slash on end:
          flagNewFile = FileRemote.mFile;
          pathchild.len0end();   //also referred from pathchild1
          if(pathchild.length() ==0){
            bCont = false;
          }
          pathchild = null;   //ends.
        } 
      } else {
        bCont = false;  //set in the next loop after pathchild = null.
      }
    } while(bCont);
    return child;
  }
  
 
  public FileRemote getChild(CharSequence name){
    boolean bWindows = this.sDir.length() >=2 && this.sDir.charAt(1) == ':';
    String key = bWindows ? name.toString().toUpperCase() : name.toString();
    return this.children == null ? null : this.children.get(key);
  }
  
  
  /**Gets the Index of the children sorted by name.
   * @return
   */
  public Map<String,FileRemote> children() { return children; }
  
 
  /**Cleans the children list. The children should be refreshed from the file system. */
  public void cleanChildren() { children = null; flags |= mShouldRefresh; }
  
  //private Map<String, FileRemote> createChildrenList(){ return new TreeMap<String, FileRemote>(); } 
  
  /**Method to create a children list. Firstly a java.util.TreeMap was used for that. But a {@link IndexMultiTable} is better,
   * because the sorted list is better able to view in debugger.
   * @return An instance of Map to store children FileRemote sorted by its filename.
   */
  //public static Map<String, FileRemote> createChildrenList(){ return new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString); } 
  public static Map<String, FileRemote> createChildrenList(){ return new TreeMap<String, FileRemote>(); } 

 
  public static boolean setAccessorSelector(FileRemoteAccessorSelector accessorSelectorP){
    boolean wasSetAlready = accessorSelector !=null;
    accessorSelector = accessorSelectorP;
    return wasSetAlready;
  }
  
  
  public static FileRemoteAccessorSelector getAccessorSelector(){
    if(accessorSelector == null){
      //accessorSelector = FileAccessorLocalJava6.selectLocalFileAlways;
      accessorSelector = FileAccessorLocalJava7.selectLocalFileAlways;
    }
    return accessorSelector;
  }
  
  
  /**Returns a FileRemote instance from a standard java.io.File instance.
   * If src is instanceof FileRemote already, it returns src.
   * Elsewhere it builds a new instance of FileRemote which inherits from File,
   * it is a new instance of File too.
   * @param src Any File or FileRemote instance.
   * @return src if it is instance of FileRemote or a new Instance within the {@link #clusterOfApplication}.
   */
  public static FileRemote fromFile(File src){ 
    if(src instanceof FileRemote) return (FileRemote)src; 
    else return get(src.getAbsolutePath());              // use only the given path
  }
  
  
  
  
  
  public static FileRemote get(FileCluster cluster, String filePath ) {
    FileRemoteAccessor device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(filePath);
    CharSequence sfPath = null;
    if(device !=null) {
      sfPath = device.completeFilePath(filePath);
    }
    CharSequence[] dirName = FileFunctions.separateDirName(sfPath);
    return cluster.getFile(dirName[0], dirName[1]);
  }  
    

  
  /**Gets an existing FileRemote instance or creates a new one.
   * @param filePath if ends with "/" then gets or creates a directory.
   * @return 
   */
  public static FileRemote get(String filePath ) {
    return get(clusterOfApplication, filePath);
  }
  
  /**Returns the instance which is associated to the given directory.
   * @param cluster the cluster where the dir should be searched
   * @param dirPath The directory path where the file is located, given absolute.
   * @return A existing or new instance.
   * @apiNote usual the {@link #clusterOfApplication} should be used. For that use {@link #getDir(CharSequence)}.
   */
  public static FileRemote getDir(FileCluster cluster, CharSequence dirPath){ 
    FileRemoteAccessor device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dirPath);
    CharSequence sfPath = null;
    if(device !=null) {
      sfPath = device.completeFilePath(dirPath);
    }
    return cluster.getFile(sfPath, null);
  }  
  
  /**Returns the instance which is associated to the given directory.
   * @param dirPath The directory path where the file is located, given absolute.
   * @return A existing or new instance.
   */
  public static FileRemote getDir(CharSequence dirPath ) {
    return getDir(clusterOfApplication, dirPath);
  }
  
 
  /**Returns the instance which is associated to the given directory and the file in this directory.
   * @param dirPath The directory path where the file is located,
   * @param name The name of the file.
   * @return A existing or new instance.
   */
  public static FileRemote getFile(FileCluster cluster, CharSequence dirPath, CharSequence name){ 
    FileRemoteAccessor device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dirPath);
    CharSequence sfPath = null;
    if(device !=null) {
      sfPath = device.completeFilePath(dirPath);
    }
    return cluster.getFile(sfPath, name);
  }  

  /**Returns the instance which is associated to the given directory and name.
   * @param dir The directory path where the file is located, given absolute.
   * @param name The name of the file.
   * @return A existing or new instance.
   */
  public static FileRemote getFile(CharSequence dir, CharSequence name){ 
    return getFile(clusterOfApplication, dir, name); 
  }  
 
  
  
  
  
  
  public FileRemoteAccessor device() { return device; }
  
  
  
  
  
  void putNewChild(FileRemote child){
    if(children == null){
      //children = new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString);  //TreeMap<String, FileRemote>();
      children = createChildrenList();  
    }
    if(child.parent != this){
      if(child.parent != null) { 
        throw new IllegalStateException("faulty parent-child"); 
      }
      child.parent = this;
    }
    final boolean bWindows = this.sDir.length() >=2 && this.sDir.charAt(1) == ':';
    String key = bWindows ? child.sFile.toUpperCase() : child.sFile;
    this.children.put(key, child);  //it may replace the same child with itself. But search is necessary.
    child.flags &= ~mRefreshChildPending;
  }
  
  
  public void setShouldRefresh(){
    flags |= mShouldRefresh;
  }
  
  
  public void setDirShouldRefresh(){
    if(parent !=null) parent.setShouldRefresh();
  }
  
  
  public boolean shouldRefresh(){ return (flags & mShouldRefresh )!=0; }
  
  /**Marks the file with the given bits in mask.
   * @param mask
   * @return number of Bytes (file length)
   */
  public long setMarked(int mask){
    if(mark == null){
      mark = new FileMark(this);
    }
    mark.setMarked(mask, this);
    return length();
  }
  
  /**Resets the mark bits of this file.
   * @param mask Mask to reset bits.
   * @return number of Bytes (file length)
   */
  public long resetMarked(int mask){
    if(mark != null){
      mark.setNonMarked(mask, this);
      return length();
    }
    else return 0;
  }
  
  /**Resets the mark bits of this file and all children ({@link #children()})  which are referred in the FileRemote instance.
   * This method does not access the files on the file system. It works only with the FileRemote instances.
   * 
   * @param bit mask Mask to reset mark bits.
   * @param nrofFiles Output reference, will be incremented for all files which's mark are reseted.
   *   Maybe null, then unused.
   * @return Sum of all bytes (file length) of the reseted files.
   */
  public long resetMarkedRecurs(int mask, int[] nrofFiles){
    return resetMarkedRecurs(mask, nrofFiles, 0);  
  }
  
  
  
  /**Recursively called method for {@link #resetMarkedRecurs(int, int[])}
   * @param mask
   * @param nrofFiles
   * @param recursion
   * @return
   */
  private long resetMarkedRecurs(int mask, int[] nrofFiles, int recursion){
    long bytes = length();
    if(nrofFiles !=null){ nrofFiles[0] +=1; }
    //if(!isDirectory() && mark !=null){
    if(mark !=null){
      mark.setNonMarked(mask, this);
    }
    if(recursion > 1000) throw new RuntimeException("FileRemote - resetMarkedRecurs,too many recursion");
    if(children !=null){
      for(Map.Entry<String, FileRemote> item: children.entrySet()){
        FileRemote child = item.getValue();
        //if(child.isMarked(mask)){
          bytes += child.resetMarkedRecurs(mask, nrofFiles, recursion +1);
        //}
      }
    }
    return bytes;
  }
  
  
  /**Gets or creates a {@link FileMark} for this file.
   * @return
   */
  public FileMark getCreateMark () { 
    if(this.mark == null) { this.mark = new FileMark(this); } 
    return mark;
  }
  
  
  /**Gets the {@link FileMark} for this file or null if not marked in any kind.
   * @return
   */
  public FileMark mark () { return this.mark; }
  
  
  /**Returns the mark of a {@link #mark} or 0 if it is not present.
   * @see org.vishia.util.MarkMask_ifc#getMark()
   */
  @Override public int getMark() { 
//    if(sFile.equals("ReleaseNotes.topic"))
//      Debugutil.stop();
    return this.mark == null ? 0 : this.mark.getMark();
  }


  /**resets a marker bit in the existing {@link #mark} or does nothing if the bit is not present.
   * @see org.vishia.util.MarkMask_ifc#setNonMarked(int, java.lang.Object)
   */
  @Override public int setNonMarked(int mask, Object data)
  { if(mark == null) return 0;
    else return mark.setNonMarkedRecursively(mask, data, false);
  }


  /**resets a marker bit in the existing {@link #mark} or does nothing if the bit is not present.
   * @see org.vishia.util.MarkMask_ifc#setNonMarked(int, java.lang.Object)
   */
  public int setNonMarkedRecursively(int mask, Object data)
  { if(mark == null) return 0;
    else return mark.setNonMarkedRecursively(mask, data, true);
  }




  /**marks a bit in the {@link #mark}, creates it if it is not existing yet.
   * @see org.vishia.util.MarkMask_ifc#setMarked(int, java.lang.Object)
   */
  @Override public int setMarked(int mask, Object data)
  { if(sFile.equals("ReleaseNotes.topic"))
      Debugutil.stop();
    if(mark == null){ mark = new  FileMark(this); }
    return mark.setMarked(mask, data);
  }
  

  
  public boolean isMarked(int mask){ return mark !=null && (mark.getMark() & mask) !=0; }
  
  
  /**Sets the properties to this.
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param length
   * @param date
   * @param flags
   * @param oFileP reference to a file Object, for example java.io.File for local files.
   */
  public void _setProperties(final long length
      , final long date, long dateCreation, long dateAccess
      , final int flags
      , Object oFileP) {
    this.length = length;
    this.date = date;
    this.dateCreation = dateCreation;
    this.dateLastAccess = dateAccess;
    this.flags = flags;
    this.oFile = oFileP;
  }
  
  
  /**Gets the properties of the file from the physical file.
   * @param callback if null gets the properties immediately in this thread.
   * 
   */
  public void refreshProperties(EventWithDst<FileRemoteProgressEvData,?> evBack){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    device.refreshFileProperties(this, evBack);
  }
  
  
  /**This is the basic operation to force execution with this file or dir
   * either on another device or in another thread, depending on its {@link #device()}.
   * It uses the {@link FileRemoteAccessor#cmd(boolean, FileRemoteCmdEventData, EventWithDst)} for access, it's only a wrapper.
   * But the {@link #device()} is clarified before.
   * @param cmd one of the admissible commands for this argument set, all commands which should be executable in a remote device.
   * @param dstdir if necessary a destination, null if cmd does not need a destination
   * @param selectFilter to select files and dirs per name.
   * @param cycleProgress 0 = progressEv for any file action, else time in ms for progress feedback
   * @param depthWalk 0 any deepness, 1 = one child level
   * @param cmdDataArg If null then create temporary instance internally, else reuse it
   * @param progressEv event for back information to inform about progress and success. 
   *    null then execution is done in the own thread if possible in the own device (local file system).
   *    If it should be executed really on a remote device, then null = no feedback.
   *    Else use of this event instance for feedback and done.
   *    Use {@link EventConsumerAwait#awaitExecution(long, boolean)} for done information.
   *    The {@link EventConsumer} is the instance used as destination for {@link EventWithDst#EventWithDst(String, EventSource, EventConsumer, org.vishia.event.EventThread_ifc, Payload)}. 
   *    The instances for the progressEv and its consumer can be created and used persistently.
   *    Note that {@link EventWithDst#cleanData()} should be invoked before call this operation.
   */
  public void cmdRemote ( FileRemoteCmdEventData.Cmd cmd, FileRemote dstdir, String selectFilter, int bMaskSel, int cycleProgress, int depthWalk
      , FileRemoteCmdEventData cmdDataArg, EventWithDst<FileRemoteProgressEvData,?> progressEv) {
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData cmdData = cmdDataArg == null ? new FileRemoteCmdEventData() : cmdDataArg.clean();
    cmdData.setCmdWalkRemote(this, cmd, dstdir, selectFilter, bMaskSel, cycleProgress, depthWalk);
    this.device.cmd(progressEv ==null, cmdData, progressEv);
  }
  
  
  /**Walks local only through the FileRemote instances without touch the underlying files on a device.
   * It may be that the FileRemote tree is updated (synchronized with the File system) or not. 
   * It deals with the current state.
   * For the selected files and dirs the callback is executed. The callback can deal especially with the underlying file system. 
   * @param dstdir a possible destination FileRemote instance, maybe used in callback.
   * @param markSet this bits are set or reset to all selected files. Note: {@link FileMark#resetMark} determines set or reset.
   * @param markSetDir this bits are set or reset to all selected directories. Note: {@link FileMark#resetMark} determines set or reset.
   * @param selectFilter given file filter due to {@link org.vishia.util.FilepathFilterM}
   * @param selectMask mask for selection dir or files. Note: it is possible to reset exact this bits with markSet and markSetDir
   * @param depthWalk 0: walk through full deepness, 1: only the first level after this src directory.
   * @param callback execute the callback for any selected dir or file.
   * @param cmdDataArg if not null then this instance is reused as data for walking. 
   * @param cycleProgress <0 then do not use an extra thread. 1: event for any selected file and dir for feedback. 
   *   >1: time in ms for feedback in a suitable time. 
   * @param progressEv if not null then this event is processed for any file or dir or after cycleProcess ms.
   *   For any file and dir the number of files and the sum of bytes are count. 
   */
  public void walkLocal ( FileRemote dstdir, int markSet, int markSetDir, String selectFilter, int selectMask, int depthWalk
      , FileRemoteWalkerCallback callback
      , FileRemoteCmdEventData cmdDataArg, int cycleProgress, EventWithDst<FileRemoteProgressEvData,?> progressEv) {
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData cmdData = cmdDataArg == null ? new FileRemoteCmdEventData() : cmdDataArg;
    cmdData.setCmdWalkLocal(this, FileRemoteCmdEventData.Cmd.noCmd, dstdir, markSet, markSetDir, selectFilter, selectMask, depthWalk, callback, cycleProgress);
    if(progressEv == null || cycleProgress <0) {
      FileRemoteWalker.walkFileTree(cmdData, progressEv);
    } else {
      FileRemoteWalker.walkFileTreeThread(cmdData, progressEv);
    }
  }
  
  
  /**walk file tree with specified callback adequate the concept which is implemented in {@link FileAccessorLocalJava7}
   * or maybe other file access for embedded control.
   * All arguments are set to an instance of {@link FileRemoteCmdEventData}, 
   * with them {@link #device} {@link FileRemoteAccessor#cmd(boolean, FileRemoteCmdEventData, EventWithDst)} is called. 
   * @param bWait true then executes the walker in this thread, false then use an extra thread.
   * @param dirDst can be used by the callback
   * @param depth depth to walk,
   * @param markSet Bits to mark files while walking through
   * @param markSetDir  Bits to mark directories while walking through
   * @param selectFilter Wildcard mask to select source files. Maybe empty or null, then all files are used.
   * @param selectMark Bits to select from mark maybe manually set before or via other algorithm
   *        It can use some specific bits: {@link FileMark#orWithSelectString}, {@link FileMark#ignoreSymbolicLinks}
   * @param callback null possible. A callback operation set for each file and dir. This defines what to do with the files.
   * @param cycleProgress cycle for progress in ms, 0 means progress for any file.
   * @param evProgress for progress. If bWait is false and evBack is null, no answer is given. 
   */
  public void walkRemote ( boolean bWait, FileRemote dirDst, int depth, String selectFilter, int bMaskSel
    , int cycleProgress, EventWithDst<FileRemoteProgressEvData,?> evProgress) { 
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdWalkRemote(this, FileRemoteCmdEventData.Cmd.walkTest, dirDst, selectFilter, bMaskSel, cycleProgress, depth);
    this.device.cmd(bWait, co, evProgress);
  }


  /**Gets the properties of the file from the physical file immediately in this thread.*/
  public void refreshProperties ( ) {
    refreshProperties(null);
  }
  
  
  
  /**Refreshes the properties of this file and gets all children in an extra thread with user-callback for any file.
   * In an on demand created thread the routine {@link FileRemoteAccessor#refreshPropertiesAndChildren(FileRemote, boolean, FileFilter, CallbackFile)}
   * will be called with the given CallbackFile.
   * @param callback maybe null if the callback is not used.
   * @param bWait true then waits for success. On return the walk through is finished and all callback routines are invoked already.
   *   false then this method may return immediately. The callback routines are not invoked. The walk is done in another thread.
   *   Note: Whether or not another thread is used for communication it is not defined with them. It is possible to start another thread
   *   and wait for success, for example if communication with a remote device is necessary. 
   */
  public void refreshPropertiesAndChildren(boolean bWait, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdWalkRemote(this, FileRemoteCmdEventData.Cmd.walkRefresh, null, null, 0, 0, 1);
    this.device.cmd(bWait, co, evBack);
    //device.walkFileTree(this, bWait, true, 0, 0, null, 0,  1,  callback, evBack, false);  //should work in an extra thread.
  }
  
  
  
  /**Refreshes a file tree and mark some files. This routine creates another thread usually, which accesses the file system
   * and invokes the callback routines. A longer access time does not influence this thread. 
   * The result is given only if the {@link FileRemoteWalkerCallback#finished(FileRemote, org.vishia.util.SortedTreeWalkerCallback.Counters)}
   * will be invoked.
   * @param depth deepness to entry in the directory tree. Use 0 if all levels should enter.
   * @param setMark bits to set in the {@link #mark()} {@link FileMark#selectMask} for the selected files
   *   If {@link FileMark#resetMark} is set, the bits given in this field will be set to 0.
   * @param setMarkDir bits to set in the {@link #mark()} for the directories containing selected files
   *   If {@link FileMark#resetMark} is set, the bits given in this field will be set to 0.
   * @param sMaskSelection file selection mask due to {@link org.vishia.util.FilepathFilterM} 
   *   for files using the device {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7}
   *   or any other proper select string for other devices.
   * @param markSelection Bits to select with mark bits. 
   *   If {@link FileMark#orWithSelectString} is set, this is a OR relation, elsewhere AND with the select string.
   *   OR relation means, a file is selected either with the sMaskSelection or with one of this bits.
   *   AND relation means, one of this bits should be set, and the sMaskSelection should be matching.  
   * @param callbackUser a user instance which will be informed on start, any file, any directory and the finish.
   * @param progressCopyDirTreeWithCallback instance for callback.
   */
  //tag::refreshAndMark[]
  public void refreshAndMark ( boolean bWait, int depth, int setMark, int setMarkDir, String sMaskSelection, int markSelection
      , FileRemoteWalkerCallback callbackUser, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(callbackUser !=null)
      Debugutil.stop();
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdWalkLocal(this, FileRemoteCmdEventData.Cmd.walkRefresh, null, setMark, setMarkDir, sMaskSelection, markSelection, depth, null, 0);
    this.device.cmd(bWait, co, evBack);
//    boolean bWait = (evBack ==null);
//    this.device.walkFileTree(this,  bWait, true, setMark, setMarkDir
//          , sMaskSelection, markSelection,  depth,  callbackUser, evBack, false);  //should work in an extra thread.
  }
  //end::refreshAndMark[]
  
  
  
  
  
  
  
  /**Refreshes a file tree and search in  some or all files. This routine creates another thread usually which accesses the file system
   * and invokes the callback routines. A longer access time does not influence this thread. 
   * The result is given only if the {@link FileRemoteWalkerCallback#finished(FileRemote, org.vishia.util.SortedTreeWalkerCallback.Counters)}
   * will be invoked.
   * @param depth at least 1 for enter in the first directory. Use 0 if all levels should entered.
   * @param mask a mask to select directory and files
   * @param bits to select files by its mark, 0 then select all (ignore mark)
   * @param callbackUser maybe null, a user instance which will be informed on start, any file, any directory and the finish.
   * @param timeOrderProgress maybe null, if given then this callback is informed on any file or directory.
   */
  public void refreshAndSearch(int depth, String mask, int mark, byte[] search, FileRemoteWalkerCallback callbackUser, EventWithDst<FileRemoteProgressEvData,?> evBack) { //FileRemote.CallbackEvent evCallback) { ////
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    //TODO implement with cmd
  }
  
  
  
  
  /**Refreshes a file tree and compare some or all files. This routine creates another thread usually which accesses the file system
   * and invokes the callback routines. A longer access time does not influence this thread. 
   * The result is given only if the {@link FileRemoteWalkerCallback#finished(FileRemote, org.vishia.util.SortedTreeWalkerCallback.Counters)}
   * will be invoked.
   * @param depth at least 1 for enter in the first directory. Use 0 if all levels should entered.
   * @param sMask a mask to select directory and files
   * @param bMaskSel to select files by its mark, 0 then select all (ignore mark)
   * @param callbackUser maybe null, a user instance which will be informed on start, any file, any directory and the finish.
   * @param timeOrderProgress maybe null, if given then this callback is informed on any file or directory.
   */
  public void cmprDirTreeTo(boolean bWait, FileRemote dir2, String sMask, int bMaskSel, int modeCmpOper, EventWithDst<FileRemoteProgressEvData,?> evBack) { 
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(this.getAbsolutePath());
    }
    if(dir2.device == null){
      dir2.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dir2.getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    //co.callback = new FileRemoteCallbackCmp(this, dir2, null, evBack);  //evCallback);
    co.setCmdWalkRemote(this, FileRemoteCmdEventData.Cmd.walkCompare, dir2, sMask, bMaskSel, 100, 0);
    co.modeCmpOper = modeCmpOper;
    this.device.cmd(bWait, co, evBack);
  }
  
  
  
  public void copyDirTreeTo(boolean bWait, FileRemote dirDst, int depth, String sFilterSel, int bMaskSel
      , EventWithDst<FileRemoteProgressEvData,?> evBack) { 
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdWalkRemote(this, FileRemoteCmdEventData.Cmd.walkCopyDirTree, dirDst, sFilterSel, bMaskSel, 100, depth);
//    co.markSet = setMark;
//    co.markSetDir = setMarkDir;
//    co.selectMask = (int)mark;
    this.device.cmd(bWait, co, evBack);
  }
  
  
  public void copyDirTreeTo(boolean bWait, FileRemote dirDst, int depth, int markSet, int markSetDir
      , String selectFilter, int selectMask
      , EventWithDst<FileRemoteProgressEvData,?> evBack) { 
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdWalkLocal(this, FileRemoteCmdEventData.Cmd.walkCopyDirTree, dirDst, markSet, markSetDir, selectFilter, selectMask, depth, null, 100);
//    co.setCmdWalkRemote(this, FileRemoteCmdEventData.Cmd.walkCopyDirTree, dirDst, mask, 100, depth);
//    co.markSet = setMark;
//    co.markSetDir = setMarkDir;
//    co.selectMask = (int)mark;
    this.device.cmd(bWait, co, evBack);
  }
  
  
  
  public void deleteFilesDirTree(boolean bWait, int depth, String sFilterSel, int bMaskSel
      , EventWithDst<FileRemoteProgressEvData,?> evBack) { 
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdWalkRemote(this, FileRemoteCmdEventData.Cmd.walkDelete, null, sFilterSel, bMaskSel, 100, depth);
    this.device.cmd(bWait, co, evBack);
  }
  
  
  
  /**Starts the execution of copy in another thread.
   * See {@link FileRemote#copyChecked(String, String, int, org.vishia.fileRemote.FileRemote.CallbackEvent)}.
   * Hint: Set breakpoint to {@link #execCmd(org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}
   * to stop in the execution thread.
   */
  public String copyTo(FileRemote dst, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdChgFileRemote(this, FileRemoteCmdEventData.Cmd.copyFile, dst, null, 0);
    boolean bWait = (evBack ==null);
    String sError = this.device.cmd(bWait, co, evBack);
    return sError;
    //this.device.copyFile(this, dst, evBack);    
  }



  
  /**Starts the execution of copy in another thread.
   * See {@link FileRemote#copyChecked(String, String, int, org.vishia.fileRemote.FileRemote.CallbackEvent)}.
   * Hint: Set breakpoint to {@link FileAccessorLocalJava7#execCmd(org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}
   * to stop in the execution thread.
   */
  public String moveTo(FileRemote dst, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdChgFileRemote(this, FileRemoteCmdEventData.Cmd.moveFile, dst, null, 0);
    boolean bWait = (evBack ==null);
    String sError = this.device.cmd(bWait, co, evBack);
    return sError;
  }



  
  public String renameTo(FileRemote dst, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    if(this.device == null){
      this.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdChgFileRemote(this, FileRemoteCmdEventData.Cmd.moveFile, dst, null, 0);
    return this.device.cmd(evBack ==null, co, evBack);
  }
  
  
  /**renames or moves this file to the given path in dst. 
   * This is the alternative implementation against {@link File#renameTo(File)} which is not used.  
   * The File dst should be either a FileRemote instance, or it is converted to it for temporary usage.
   */
  @Override public boolean renameTo(File dst) {
    return renameTo(fromFile(dst), null) ==null;    
  }
  
  
  /**Sets this as a symbolic linked file or dir with the given path. 
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param pathP The path where the file is organized.
   */
  public void setSymbolicLinkedPath(String pathP){
    flags |= mSymLinkedPath;
    this.sCanonicalPath = pathP;
  }
  
  
  /**Sets this as a non-symbolic linked file or dir with the given path. 
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param pathP The absolute canonical path where the file is organized.
   */
  public void setCanonicalAbsPath(String pathP){
    flags |= mAbsPath;
    flags &= ~mSymLinkedPath;
    this.sCanonicalPath = pathP;
  }
  
  
  /**Check whether two files are at the same device. It means it can be copied, compared etc. remotely. 
   * @param other The other file to check
   * @return true if they are at the same device. The same device is given if the comparison
   *   of the {@link FileRemoteAccessor} instances of both files using {@link #device}.equals(other.device) 
   *   returns true.
   */
  public boolean sameDevice(FileRemote other){ return device.equals(other.device); }
  
  
  /**Opens a read access to this file.
   * If the file is remote, this method should return immediately with a prepared channel functionality (depending from implementation).
   * A communication with the remote device will be initiated to get the first bytes parallel in an extra thread.
   * If the first access will be done, that bytes will be returned without waiting.
   * If a non-blocking mode is used for the device, a {@link java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)}
   * invocation returns 0 if there are no bytes available in this moment. An polling invocation later may transfer that bytes.
   * In this kind a non blocking mode is possible.
   * 
   * @param passPhrase a pass phrase if the access is secured.
   * @return The channel to access.
   */
  public ReadableByteChannel openRead(long passPhrase){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.openRead(this, passPhrase);
  }
  

  /**Opens a read access to this file.
   * If the file is remote, this method should return immediately with a prepared stream functionality (depending from implementation).
   * A communication with the remote device will be initiated to get the first bytes parallel in an extra thread.
   * If the first access will be done, that bytes will be returned without waiting. 
   * But if the data are not supplied in this time, the InputStream.read() method blocks until data are available
   * or the end of file or any error is detected. That is the contract for a InputStream.
   * 
   * 
   * @param passPhrase a pass phrase if the access is secured.
   * @return The byte input stream to access.
   */
  public InputStream openInputStream(long passPhrase){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.openInputStream(this, passPhrase);
    
  }
  
  
  
  
  /**Opens a write access to this file.
   * If the file is remote, this method should return immediately with a prepared stream functionality (depending from implementation).
   * A communication with the remote device will be initiated to write.
   * 
   * 
   * @param passPhrase a pass phrase if the access is secured.
   * @return The byte input stream to access.
   */
  public OutputStream openOutputStream(long passPhrase){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.openOutputStream(this, passPhrase);
    
  }
  
  
  
  
  /**Opens a write access to this file.
   * @param passPhrase a pass phrase if the access is secured.
   * @return The channel to access.
   */
  public WritableByteChannel openWrite(long passPhrase){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.openWrite(this, passPhrase);
  }
  
  
  @Override public long length(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return length; 
  }

  
  @Override public long lastModified(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return date; 
  }
  
  
  /**Returns the creation time of the file if the file system supports it. 
   * Elsewhere returns 0.
   */
  public long creationTime(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return dateCreation; 
  }
  
  
  /**Returns the last access time of the file if the file system supports it. 
   * Elsewhere returns 0.
   */
  public long lastAccessTime(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return dateLastAccess; 
  }
  
  
  @Override public boolean setLastModified(long time) {
    this.date = time;
    if(oFile !=null){
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      return device.setLastModified(this, time);
    } else {
      return super.setLastModified(time);
    }
  }

  
  public Object oFile(){ return oFile; }
  
  public void setFileObject(Object oFile){ this.oFile = oFile; }
  
  public int getFlags ( ){ return flags; }
  
  
  @Override public String getName(){ return sFile; }
  
  /**Returns the parent path, it is the directory path. 
   * It is define by:
   * @see java.io.File#getParent()
   * @return path without ending slash.
   */
  @Override public String getParent(){ 
    File parentFile = getParentFile();  //
    if(parentFile == null){ return sDir; }
    else { return parentFile.getAbsolutePath(); }
    //int posSlash = sDir.indexOf('/');  //the first slash
    //if only one slash is present, return it. Elsewhere don't return the terminating slash.
    //String sParent = zDir > posSlash+1 ? sDir.substring(0, zDir-1): sDir;
    //return sParent; 
  }
  
  /**Gets the path of the file. For this class the path should be esteemed as canonical,
   * it is considered on constructor. 
   * The difference between this routine and {@link #getCanonicalPath()} is:
   * The canonical path under Unix-Systems (linux) is the linked path. 
   * The return path of this routine is the path without dissolving symbolic links.
   * @return path never ending with "/", but canonical, slash as separator. 
   */
  @Override public String getPath(){ 
    CharSequence csPath = getPathChars();
    int zcs = csPath.length();
    if(csPath.charAt(zcs-1) != '/' || zcs ==1 ||  zcs==3 && csPath.charAt(1) == ':') {
      return csPath.toString();     // do not remove '/' on end if it is a root path         
    } else {
      return csPath.subSequence(0, zcs-1).toString();      // remove the '/' on end to make it compatible with File.getPath()
    }
  } 
  
  /**Returns the same as {@link #getPath()} for files, but ends with '/' if this is a directory.
   * It is very sensible and useful to dedicate a path to an existing directory with ending '/'. 
   * It does not build a String from a new StringBuilder.
   * @return The path to the file.
   */
  public CharSequence getPathChars(){
    int zFile = sFile == null ? 0 : sFile.length();
    if(zFile > 0){ 
      int zDir = sDir == null? 0: sDir.length();
      StringBuilder ret = new StringBuilder(zDir + 1 + zFile);
      if(zDir >0){
        ret.append(sDir);
//        if(sDir.charAt(zDir-1) != '/' //does not end with "/"
//          && sFile.charAt(0) !='/'    //root path has "/" in sFile
//        ) { 
//          ret.append('/'); 
//        }
      }
//      if(zFile==1 && sFile.charAt(0) == '/') {
//        // it is a root dir, do nothing
//      } else {
        if(zDir ==0 || this.sDir.charAt(zDir-1) != '/' ) {     // special case, sDir == "" or null, and 
          ret.append('/');
        }
        if(zFile==1 && sFile.charAt(0) == '/') {
          // it is a root dir, do nothing
        } else {
          ret.append(sFile);
          if(this.isDirectory()){
            ret.append('/');
          }
        }
//      }
      return ret;
    } else {
      return this.sDir;
    }    
  }

  
  public CharSequence getDirChars() {
    return sDir;
  }
  
  
  
  /**Fills the path to the given StringBuilder and returns the StringBuilder to concatenate.
   * This method helps to build a path with this FileRemote and maybe other String elements with low effort of memory allocation and copying. 
   * @return ret itself cleaned and filled with the path to the file.
   */
  public StringBuilder setPathTo(StringBuilder ret){
    int zFile = sFile == null ? 0 : sFile.length();
    if(zFile > 0){ 
      int zDir = sDir == null? 0: sDir.length();
      if(zDir >0){
        ret.append(sDir);
        if(sDir.charAt(zDir-1) != '/' //does not end with "/"
          && sFile.charAt(0) !='/'    //root path has "/" in sFile
        ) { 
          ret.append('/'); 
        }
      }
      ret.append(sFile);
    } else {
      ret.append(sDir);
    }
    return ret;
  }
  
  @Override public String getCanonicalPath(){ return sCanonicalPath; }
  
  
  
  /**Checks whether the given path describes a child (any deepness) of this file and returns the normalized child string.
   * @param path Any path may contain /../
   * @return null if path does not describe a child of this.
   *   The normalized child path from this directory path if it is a child.
   */
  public CharSequence isChild(CharSequence path){
    if(sFile !=null) return null; //a non-directory file does not have children.
    CharSequence path1 = FileSystem.normalizePath(path);
    int zDir = sDir.length();
    int zPath = path1.length();
    if(zPath > zDir && StringFunctions.startsWith(path1, sDir) && path1.charAt(zDir)== '/'){
      return path1.subSequence(zDir+1, zPath);
    }
    else return null;
  }
  
  
  
  /**Checks whether the given path describes a parent (any deepness) of this file and returns the normalized child string.
   * @param path Any path may contain /../
   * @return null if path does not describe a parent of this.
   *   The normalized parent path  if it is a parent.
   */
  public CharSequence isParent(CharSequence path){
    CharSequence path1 = FileSystem.normalizePath(path);
    CharSequence paththis = getPathChars();
    int zThis = paththis.length();
    int zPath = path1.length();
    if(zThis > zPath && StringFunctions.startsWith(paththis, path1) && paththis.charAt(zPath)== '/'){
      return path1;
    }
    else return null;
  }
  
  
  
  /**Gets the parent directory.
   * It creates a new instance of FileRemote with the path infos from {@link #sDir}.
   * 
   * @return null if this is the toplevel directory.
   */
  @Override public FileRemote getParentFile(){
    //final FileRemote parent;
    if(parent == null){
      String sParent;
      int zDir = sDir.length();
      int pDir;
      //Assert.check(sDir.charAt(zDir-1) == '/'); //Should end with slash
      if(sFile.equals("/")){
        sParent = null;  //root has no parent.
      }
      else if(sFile == null || sFile.length() == 0){
        //it is a directory, get its parent path
        if(zDir > 1){
          pDir = sDir.lastIndexOf('/', zDir-1);
          if(pDir == 0 || (pDir == 2 && sDir.charAt(1) == ':')){
            //the root. 
            pDir +=1; //return inclusive the /
          }
          if(pDir >0){
            sParent = sDir.substring(0, pDir);  //without ending slash
          } else {
            sParent = null;  //sDir is forex "D:/", or "/" it hasn't a parent.
          }
        } else {
          sParent = null;    //sDir has only one char, it may be a "/", it hasn't a parent.
        }
      } else { //a sFile is given, the sDir is the parent.
        if(zDir == 0 || (zDir == 2 && sDir.charAt(1) == ':')){
          //the root. 
          pDir = zDir; //return inclusive the /
          sParent = sDir.substring(0, pDir) + "/";
        } else {
          pDir = zDir; // -1;
          sParent = sDir.substring(0, pDir);
        }
      }
      if(sParent !=null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
        this.parent = itsCluster.getFile(sParent, null); //new FileRemote(device, null, sParent, null, 0, 0, 0, null); 
        if(this.parent.children == null){
          //at least this is the child of the parent. All other children are unknown yet. 
          this.parent.children = createChildrenList(); //new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString);  //TreeMap<String, FileRemote>();
          final boolean bWindows = this.sDir.charAt(1) == ':';
          String key = bWindows ? this.sFile.toUpperCase() : this.sFile;
          this.parent.children.put(key, this);
          this.parent.timeChildren = 0; //it may be more children. Not evaluated.
        }
      }
    }
    return this.parent;
    /*
    String sParent = getParent();
    if(sParent !=null){
      parent = new FileRemote(sParent);
    
    //if(sDir.indexOf('/') < sDir.length()-1 || name.length() > 0){
    //  parent = new FileRemote(sDir);
    } else {
      parent = null;
    }
    return parent;
    */
  }
  
  
  /**Returns true if the file was tested in the past. Returns false only if the file is created
   * and never refreshed.   */
  public boolean isTested(){ return (flags & mTested) == mTested; }
  
  
  /**Returns true if the last time of refreshing is newer than since.
   * @param since A milliseconds after 1970
   * @return true if may actual.
   */
  public boolean isTested(long since){
    if(timeRefresh < since) return false;
    if(isDirectory() && timeChildren < since) return false;
    return true;
  }
  
  
  
  
  /**Returns true if the file seems to be existing.
   * If the FileRemote instance was never refreshed with the physical file system,
   * the {@link #refreshProperties(CallbackEvent)} is called yet. But if the file was refreshed already,
   * the existing flag is returned only. To assure that the existing of the file is correct,
   * call {@link #refreshProperties(CallbackEvent)} before this call on a proper time.
   * Note that an invocation of {@link java.io.File#exists()} may have the same problem. The file
   * may exist in the time of this call, but it may not exist ever more in the future if the application
   * will deal with it. Usage of a file for opening a reader or writer without Exception is the only one
   * assurance whether the file exists really. Note that a deletion of an opened file will be prevent
   * from the operation system.
   * @see java.io.File#exists()
   */
  @Override public boolean exists(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mExist) !=0; 
  }
  
  
  
  
  /**TODO mFile is not set yet, use !isDirectory 2015-01-09
   * @see java.io.File#isFile()
   */
  @Override public boolean isFile(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mFile) !=0; 
  }
  
  
  
   /**TODO mFile is not set yet, use !isDirectory 2015-01-09
   * @see java.io.File#isFile()
   */
  @Override public boolean isHidden(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mHidden) !=0; 
  }
  
  
  
 
  /**Returns true if the FileRemote instance was created as directory
   * or if any {@link #refreshProperties(CallbackEvent)} call before this call
   * has detected that it is an directory.
   * @see java.io.File#isDirectory()
   */
  @Override public boolean isDirectory(){ 
    //NOTE: The mDirectory bit should be present any time. Don't refresh! 
    return (flags & mDirectory) !=0; 
  }
  
  public boolean isRoot() {
    return sFile.equals("/");
  }
  
  /**true if due to Java orientation to the file system the file is not read only.
   * it follows {@link File#canWrite()}
   */
  @Override public boolean canWrite(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mCanWrite) !=0; 
  }
  
  
  
  /**true if due to Java orientation to the file system the file is readable (visible).
   * it follows {@link File#canRead()}
   */
  @Override public boolean canRead ( ){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mCanRead) !=0; 
  }
  
  /**true if due to Java orientation to the file system the file is executable.
   * it follows {@link File#canExecute()}
   */
  @Override public boolean canExecute ( ){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mExecute) !=0; 
  }
  
  
  /**Return the flags of the file after tested with file system itself.
   * This operation may use more time if access to the file system is necessary.
   * @return the flags, see {@link #mCanRead} etc.
   */
  public int getFlagsTested ( ) {
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return flags; 
  }
  
  
  public boolean isSymbolicLink(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mSymLinkedPath) !=0; 
  }
  
  
  @Override public String getAbsolutePath(){
    String sAbsPath;
    sAbsPath = getPath(); //sDir + sFile;
    return sAbsPath;
  }
  
  
  
  /**This method overrides java.io.File.listFiles() but returns Objects from this class type.
   * @see java.io.File#listFiles()
   * If the children files are gotten from the maybe remote file system, this method returns immediately
   * with that result. But it may be out of date. The user can call {@link #refreshPropertiesAndChildren(CallbackEvent)}
   * to get the new situation.
   * <br><br>
   * If the children are not gotten up to now they are gotten yet. The method blocks until the information is gotten,
   * see {@link FileRemoteAccessor#refreshFilePropertiesAndChildren(FileRemote, EventCmdPingPongType)} with null as event parameter.
   */
  @Override public FileRemote[] listFiles(){
    if(children == null){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      refreshPropertiesAndChildren(true, null);
      //device.refreshFilePropertiesAndChildren(this, null);
    }
    if(children == null) { return null; }
    else {
      FileRemote[] aChildren = new FileRemote[children.size()];
      int ix = -1;
      for(Map.Entry<String, FileRemote> item: children.entrySet()){
        if(ix+1 >= aChildren.length) {
          System.err.println("Bug in IndexMultiTable");
        } else {
          aChildren[++ix] = item.getValue();
        }
      }
      return aChildren;
    }
  }
  
  
  
  @Override public File[] listFiles(FileFilter filter) {
    if(children == null){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      refreshPropertiesAndChildren(true, null);
      //device.refreshFilePropertiesAndChildren(this, null);
    }
    List<File> children = device.getChildren(this, filter);
    File[] aChildren = new File[children.size()];
    return children.toArray(aChildren);
  }
  
  
  
  
  
  
  
  
  
  @Override public boolean createNewFile() throws IOException {
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.createNewFile(this, null);
  }
  
  
  /**Deletes this file, correct the parent's children list, remove this file.
   * @return true if it is successfully deleted.
   */
  @Override public boolean delete(){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    boolean deleted = device.delete(this, null);
    if(deleted && parent !=null && parent.children !=null){
      parent.children.remove(this.sFile);
    }
    return deleted;
  }
  
  
  
  /**Deletes a file maybe in a remote device. This is a send-only routine without feedback,
   * because the calling thread should not be waiting for success.
   * The success is notified with invocation of the 
   * {@link EventCmdPingPongType#callback}.{@link EventConsumer#processEvent(EventCmdPingPongType)} method. 
   * @param backEvent The event for success. If null, delete in the same thread in the local file system. 
   */
  public void delete(EventWithDst<FileRemoteProgressEvData,?> evBack){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdChgFileRemote(this, FileRemoteCmdEventData.Cmd.delete, null, null, 0);
    this.device.cmd(evBack ==null, co, evBack);
  }
  
  
  
  
  
  
  
  
  
  


  
  
  

  /**Creates the directory named by this abstract pathname.
   * This routine waits for execution on the file device. If it is a remote device, it may be spend some more time.
   * See {@link #mkdir(boolean, CallbackEvent)}.
   * For local file system see {@link java.io.File#mkdir()}.
   * @return true if this operation was successfully. False if not.
   */
  @Override public boolean mkdir(){
    String sError = mkdir(false, null);
    return sError == null;
  }
  
  
  /**Creates the directory named by this abstract pathname , including any
   * necessary but nonexistent parent directories.  Note that if this
   * operation fails it may have succeeded in creating some of the necessary
   * parent directories.<br>
   * This routine waits for execution on the file device. If it is a remote device, it may be spend some more time.
   * See {@link #mkdir(boolean, CallbackEvent)}.
   * For local file system see {@link java.io.File#mkdir()}.
   * @return true if this operation was successfully. False if not.
   */
  @Override public boolean mkdirs(){
    mkdir(true, null);
    return true;
  }
  
  
  
  /**Creates the directory named by this abstract pathname
   * @param recursively Creates necessary but nonexistent parent directories.  Note that if this
   *   operation fails it may have succeeded in creating some of the necessary
   *   parent directories.
   * @param evback If given this routine does not wait. Instead the success will be sent with the given evback
   *  to the given destination routine given in its constructor {@link CallbackEvent#CallbackEvent(EventConsumer, EventTimerThread, EventSource)}.
   *  If not given this routine waits till execution, see {@link #mkdir()}
   * @return false if unsuccessfully, true if evback !=null or successfully. 
   */
  public String mkdir(boolean recursively, EventWithDst<FileRemoteProgressEvData, FileRemoteCmdEventData> evBack) {
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.cmdFile(this, FileRemoteCmdEventData.Cmd.mkDir, null, null, 0, evBack);
  }

  
  
  

  
  /**Checks a file maybe in a remote device maybe a directory. 
   * This is a send-only routine without immediate feedback, because the calling thread should not be waiting 
   * for success. The success is notified with invocation of the 
   * {@link EventCmdPingPongType#callback}.{@link EventConsumer#processEvent(EventCmdPingPongType)} method in the other execution thread
   * or communication receiving thread.
   * 
   * @param sFiles Maybe null or empty. If given, some file names separated with ':' or " : " should be used
   *   in this directory to check and select.
   * @param sWildcardSelection Maybe null or empty. If given, it is the select mask for files in directories.
   * @param evback The event instance for success. It can be contain a ready-to-use {@link FileRemoteCmdEventData}
   *   as its opponent. then that is used.
   */
  @Deprecated public void check(String sFiles, String sWildcardSelection, FileRemoteProgressEvData evback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
//    CmdEvent ev = device.prepareCmdEvent(500, evback);
//    
//    ev.filesrc =this;
//    ev.filedst = null;
//    ev.namesSrc = sFiles;
//    ev.maskSrc = sWildcardSelection;
//    //ev.data1 = 0;
//    ev.sendEvent(FileRemoteCmdEventData.Cmd.check);
  }

  
  
  /**Copies a file or directory tree to another file in the same device. The device is any file system maybe in a remote device.  
   * This is a send-only routine without immediate feedback, because the calling thread should not be waiting 
   * for success. The copy process is done in another thread.
   * <br><br>
   * A feedback over the progress, for quests or the success of the copy process is given with evaluating of callback events. 
   * An event instance should be provided from the caller (param evback). This event instance is used and re-used
   * for multiple callback events. If the event is occupied still from the last callback, a next status callback is suppressed.
   * Therewith a flooding with non-processed events is prevented. If a quest is necessary or the success should be announced,
   * that request should be waiting till the event is available with a significant timeout. 
   * <br><br>
   * The mode of operation, given in param mode, can be determine how several situations should be handled:
   * <ul>
   * <li>Overwrite write protected files: Bits {@link #modeCopyReadOnlyMask}
   *   <ul>
   *   <li>{@link #modeCopyReadOnlyAks}: Send a callback event with the file name, wait for answer 
   *   </ul>
   * </ul>  
   * <br><br>
   * This routine sends a {@link CmdEvent} with {@link FileRemoteCmdEventData.Cmd#copyChecked} to the destination. Before that the destination 
   * for the event is set with calling of {@link FileRemoteAccessor#prepareCmdEvent(CallbackEvent)}. 
   * That completes the given {@link CallbackEvent} with the necessary {@link CmdEvent} and its correct destination {@link EventConsumer}.
   * <br><br>
   * Some status messages and the success is notified from the other thread or remote device with invocation of the 
   * given {@link CallbackEvent}. After any status message was received the {@link CmdEvent} gotten as {@link EventMsg2#getOpponent()}
   * from the received {@link CallbackEvent} can be used to influence the copy process:
   * The commands of the callback are:
   * <ul>
   * <li>{@link CallbackCmd#done}: The last callback to designate the finish of succession.
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file exists and it is readonly. It can't be set writeable 
   *   though the {@link #modeCopyReadOnlyOverwrite} designation was sent in the mode argument. 
   * <li>{@link CallbackCmd#askDstOverwr}: The destination file exits. 
   *   Because {@link #modeCopyExistAsk} is given it is the request for asking whether it should be overridden. 
   * <li>{@link CallbackCmd#askDstReadonly}: The destination file exists and it is read only. 
   *   Because the {@link #modeCopyReadOnlyAks} is given it is the request for asking whether it should be overridden though.
   * <li>{@link CallbackCmd#askErrorDstCreate}: The destination file does not exists or it exists and it is writeable or set writeable.
   *   Nevertheless the creation or replacement of the file (open for write) fails. It is possible that the medium is read only
   *   or the user has no write access to the directory. Usual the copy of that file should be skipped sending {@link FileRemoteCmdEventData.Cmd#abortCopyFile}. 
   *   On the other hand the user can clarify what's happen and then send {@link FileRemoteCmdEventData.Cmd#overwr} to repeat it. 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * </ul>
   * {@link EventMsg2#callback}.{@link EventConsumer#processEvent(EventMsg2)} method. 
   *
   * @param dst The destination which is created as copy of this source. If this is a file the dst should describe a file which may not exits.
   *   If this is a directory dst should describe a directory which can exist or not where the children of this are stored in.
   * @param mode  
   * @param nameDst maybe null, elsewhere it should contain 1 wildcard to specify an abbreviating name.
   * @param evback The event for status messages and success.
   * @return The consumer of the event. The consumer can be ask about its state.
  @Deprecated public EventConsumer copyChecked(String pathDst, String nameModification, int mode, FileRemoteProgressEvent evback){
    CmdEvent ev = evback.getOpponent();
    if(ev.occupy(evSrc, device.states, null, false)) {
      ev.filesrc = this;
      ev.filedst = null;
      ev.nameDst = pathDst;
      ev.newName = nameModification;
      ev.modeCopyOper = mode;
      ev.sendEvent(FileRemoteCmdEventData.Cmd.copyChecked);
      return device.states;
    } else {
      throw new IllegalStateException("FileRemote.copyChecked - event is occupied.");
    }
  }
  
   */
  

  
  /**Copies all files which are checked before. 
   * <code>this</code> is the dir or file as root for copy to the given pathDst. 
   * The files to copy are marked in this directory or it is this file.
   * <br><br>
   * The copying process is interactive. It is possible to ask whether files should be override etc, the progress is shown.
   * For that the {@link FileRemoteProgressEvData} is used. This timeOrder should be created as overridden time order
   * in the applications space with the application specific {@link EventTimerThread_ifc} instance. Especially it can be used
   * in a graphical environment. See there to show a sequence diagram.
   * <br><br>
   * The 
   * 
   * @param pathDst String given destination for the copy
   * @param nameModification Modification for each name. null then no modification. TODO
   * @param mode One of the bits {@link FileRemote#modeCopyCreateYes} etc.
   * @param callbackUser Maybe null, elsewhere on every directory and file which is finished to copy a callback is invoked.
   * @param timeOrderProgress may be null, to show the progress of copy.
   */
  public void copyChecked(String pathDst, String nameModification, int mode, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvData timeOrderProgress)
  {
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    device.copyChecked(this, pathDst, nameModification, mode, callbackUser, timeOrderProgress);
  }

  
  
  
  /**Copies a file or directory tree to another file in the same device. The device is any file system maybe in a remote device.  
   * This is a send-only routine without immediate feedback, because the calling thread should not be waiting 
   * for success. The copy process is done in another thread.
   * <br><br>
   * A feedback over the progress, for quests or the success of the copy process is given with evaluating of callback events. 
   * An event instance should be provided from the caller (param evback). This event instance is used and re-used
   * for multiple callback events. If the event is occupied still from the last callback, a next status callback is suppressed.
   * Therewith a flooding with non-processed events is prevented. If a quest is necessary or the success should be announced,
   * that request should be waiting till the event is available with a significant timeout. 
   * <br><br>
   * The mode of operation, given in param mode, can be determine how several situations should be handled:
   * <ul>
   * <li>Overwrite write protected files: Bits {@link #modeCopyReadOnlyMask}
   *   <ul>
   *   <li>{@link #modeCopyReadOnlyAks}: Send a callback event with the file name, wait for answer 
   *   </ul>
   * </ul>  
   * <br><br>
   * This routine sends a {@link FileRemoteCmdEventData} with {@link FileRemoteCmdEventData.Cmd#copyChecked} to the destination. Before that the destination 
   * for the event is set with calling of {@link FileRemoteAccessor#prepareCmdEvent(CallbackEvent)}. 
   * That completes the given {@link CallbackEvent} with the necessary {@link FileRemoteCmdEventData} and its correct destination {@link EventConsumer}.
   * <br><br>
   * Some status messages and the success is notified from the other thread or remote device with invocation of the 
   * given {@link CallbackEvent}. After any status message was received the {@link FileRemoteCmdEventData} gotten as {@link EventCmdPingPongType#getOpponent()}
   * from the received {@link CallbackEvent} can be used to influence the copy process:
   * The commands of the callback are:
   * <ul>
   * <li>{@link CallbackCmd#done}: The last callback to designate the finish of succession.
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file exists and it is readonly. It can't be set writeable 
   *   though the {@link #modeCopyReadOnlyOverwrite} designation was sent in the mode argument. 
   * <li>{@link CallbackCmd#askDstOverwr}: The destination file exits. 
   *   Because {@link #modeCopyExistAsk} is given it is the request for asking whether it should be overridden. 
   * <li>{@link CallbackCmd#askDstReadonly}: The destination file exists and it is read only. 
   *   Because the {@link #modeCopyReadOnlyAks} is given it is the request for asking whether it should be overridden though.
   * <li>{@link CallbackCmd#askErrorDstCreate}: The destination file does not exists or it exists and it is writeable or set writeable.
   *   Nevertheless the creation or replacement of the file (open for write) fails. It is possible that the medium is read only
   *   or the user has no write access to the directory. Usual the copy of that file should be skipped sending {@link FileRemoteCmdEventData.Cmd#abortCopyFile}. 
   *   On the other hand the user can clarify what's happen and then send {@link FileRemoteCmdEventData.Cmd#overwr} to repeat it. 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * </ul>
   * {@link EventCmdPingPongType#callback}.{@link EventConsumer#processEvent(EventCmdPingPongType)} method. 
   * 
   * @param dst This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param evback The event for status messages and success.
   */
  public void copyTo(FileRemote dst, EventWithDst<FileRemoteProgressEvData,?> evBack, int mode){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(dst.device == null){
      dst.device = getAccessorSelector().selectFileRemoteAccessor(dst.getAbsolutePath());
    }
    
//    CmdEvent ev = device.prepareCmdEvent(500, evback);
//    
//    ev.filesrc =this;
//    ev.filedst = dst;
//    ev.modeCopyOper = mode;
//    ev.sendEvent(FileRemoteCmdEventData.Cmd.copyChecked);

  }
  
  
  
  public void XXXcopyTo(FileRemote dst, int mode){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(dst.device == null){
      dst.device = getAccessorSelector().selectFileRemoteAccessor(dst.getAbsolutePath());
    }
  
  }  
  
  
  
  /**Moves this file or some files in this directory to another file(s) maybe in a remote device.
   * If the devices are the same, it sends a commission only to the device. 
   * The action is done in the other device respectively in another thread for local files
   * in {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7#executerCommission}.
   * <br><br>
   * Depending on the file system the moving may be a copy with deleting the source. 
   * But if this and dst are at the same partition, then it is a lightweight operation. 
   * If this and dst are at different partitions, the operation file system will be copy it 
   * and then delete this. It means, this operation can be need some time for large files.
   * <br><br> 
   * If this and dst are at different devices, this routine copies and deletes.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link EventCmdPingPongType#callback}.{@link EventConsumer#processEvent(EventCmdPingPongType)} method. 
   * 
   * @param sFiles maybe null or "". Elsewhere it contains a list of files in this directory 
   *   which are to move. The files are separated with dots and can have white spaces before and after the dot
   *   for better readability. Write for example "myfile1.ext : myfile2.ext"
   *   If it is empty, this will be moved to dst.
   * @param dst The destination for move. If dst is a directory this file or directory tree 
   *   will be copied into. If dst is an existing file nothing will be done and an error message will be fed back. 
   *    If dst does not exist this will be stored as a new file or directory tree as dst.
   *    Note that this can be a file or a directory tree.
   * @param backEvent The event for success.
   */
  public void moveTo(String sFiles, FileRemote dst, FileRemoteProgressEvData evback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
//    CmdEvent ev = device.prepareCmdEvent(500, evback);
//    ev.filesrc = this;
//    ev.namesSrc = sFiles;
//    ev.filedst = dst;
//    ev.sendEvent(FileRemoteCmdEventData.Cmd.move);
  }
  
  
  
  
  
  
  
  /**Search in all files. 
   * <code>this</code> is the dir or file as root for copy to the given pathDst. 
   * The files to copy are marked in this directory or it is this file.
   * <br><br>
   * The copying process is interactive. It is possible to ask whether files should be override etc, the progress is shown.
   * For that the {@link FileRemoteProgressEvData} is used. This timeOrder should be created as overridden time order
   * in the applications space with the application specific {@link EventTimerThread_ifc} instance. Especially it can be used
   * in a graphical environment. See there to show a sequence diagram.
   * <br><br>
   * The 
   * 
   * @param search String given destination for the copy
   * @param nameModification Modification for each name. null then no modification. TODO
   * @param mode One of the bits {@link FileRemote#modeCopyCreateYes} etc.
   * @param callbackUser Maybe null, elsewhere on every directory and file which is finished to copy a callback is invoked.
   * @param timeOrderProgress may be null, to show the progress of copy.
   */
  public void search(byte[] search, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvData timeOrderProgress)
  {
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    device.search(this, search, callbackUser, timeOrderProgress);
  }



  /**Change the file properties maybe in a remote device.
   * A new name may be given as parameter. Some properties may be changed calling
   * {@link #setWritable(boolean)} etc. or they are given as parameter.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. 
   * The success is notified with invocation of the evback.
   * The command itself uses the {@link EventCmdtypeWithBackEvent#opponent}
   * which is type of {@link FileRemoteCmdEventData} to send the request.
   * 
   * @param newName A new name for the file. This parameter may be null, then the old name remain.
   * @param maskFlags mask which flags should be changed
   * @param newFlags value of the flag bits.
   * @param newDate if not 0 a new time stamp of file
   * @param evback The event for success, containing the cmd event as opponent.
   */
  public void chgProps ( boolean bWait, String newName, int maskFlags, int newFlags, long newDate, EventWithDst<FileRemoteProgressEvData,?> evBack){
    FileRemoteCmdEventData co = new FileRemoteCmdEventData();
    co.setCmdChgFileRemote(this, FileRemoteCmdEventData.Cmd.chgProps, null, newName, newDate);
    co.maskFlags = maskFlags;
    co.newFlags = newFlags;
    
    this.device.cmd(bWait, co, evBack);
  }
  
  
  
  /**Change the file properties maybe in a remote device.
   * A new name may be given as parameter. Some properties may be changed calling
   * {@link #setWritable(boolean)} etc. or they are given as parameter.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link EventCmdPingPongType#callback}.{@link EventConsumer#processEvent(EventCmdPingPongType)} method. 
   * 
   * @param newName A new name for the file. This parameter may be null, then the old name remain.
   * @param backEvent The event for success.
   */
  public void chgPropsRecursive(int maskFlags, int newFlags, long newDate, EventWithDst<FileRemoteProgressEvData,?> evBack){
//    CmdEvent ev = device.prepareCmdEvent(500, evback);
//    ev.filesrc = this;
//    ev.filedst = null;
//    ev.newName = null;
//    ev.maskFlags = maskFlags;
//    ev.newFlags = newFlags;
//    ev.newDate = newDate;
//    ev.sendEvent(FileRemoteCmdEventData.Cmd.chgPropsRecurs);
  }
  
  
  
  
  /**Count the sum of length of all files in this directory tree.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link EventCmdPingPongType#callback}.{@link EventConsumer#processEvent(EventCmdPingPongType)} method. 
   * 
   * @param newName A new name for the file. This parameter may be null, then the old name remain.
   * @param backEvent The event for success.
   */
  public void countAllFileLength(EventWithDst<FileRemoteProgressEvData,?> evBack){
//    CmdEvent ev = device.prepareCmdEvent(500, evback);
//    ev.filesrc = this;
//    ev.filedst = null;
//    ev.sendEvent(FileRemoteCmdEventData.Cmd.countLength);
  }
  
  
  /**It sends an abort event to the execution thread or to any remote device to abort any action with files.
   * It aborts {@link #copyTo(FileRemote, CallbackEvent, int)}, {@link #check(FileRemote, CallbackEvent)},
   * 
   */
  public void abortAction(){
//    device.abortAll();
//    CmdEvent ev = device.prepareCmdEvent(500, null);
//    ev.filesrc = this;
//    ev.filedst = null;
//    ev.sendEvent(FileRemoteCmdEventData.Cmd.abortAll);
  }
  
  
  /**Returns the state of the device statemachine, to detect whether it is active or ready.
   * @return a String for debugging and show.
   */
  public CharSequence getStateDevice ( ){ return (device == null) ? "no-device" : device.getStateInfo(); }
  
  public int ident(){ return _ident; }
  
  
  @Override public String toString ( ){ return super.toString(); } //sDir + sFile + " @" + ident; }
  
  
  
  

  
  
  
  /**Possibilities for comparison. */
  public enum Ecmp{ ends, starts, contains, equals, always};
  
  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
  };


  
  /**Payload of event object for all commands to a remote device or other thread for file operations. It should be used for implementations
   * of {@link FileRemoteAccessor}.
   */
  
  
  
  
  /**Type for callback notification for any action with remote files.
   * The callback type contains an opponent {@link FileRemoteCmdEventData} object which is not occupied initially
   * to use for forward notification of the action. But the application need not know anything about it,
   * the application should only concern with this object. 
   * See {@link CallbackEvent#CallbackEvent(Object, EventConsumer, EventTimerThread)}.
   */
  public static class CallbackEvent {
    public final EventWithDst<FileRemoteProgressEvData, FileRemoteCmdEventData> ev;
    public final FileRemoteProgressEvData progress;
    
    public CallbackEvent ( String name, EventConsumer dst, EventTimerThread thread, EventSource evSrcCmd) {
      assert(dst instanceof FileRemoteProgressEventConsumer);
      this.progress = new FileRemoteProgressEvData();
      this.ev = new EventWithDst<FileRemoteProgressEvData, FileRemoteCmdEventData>(name, null, (FileRemoteProgressEventConsumer)dst, thread, this.progress);
    }
  }
  
  
//  public static class CallbackEvent extends EventCmdtypeWithBackEvent<FileRemote.CallbackCmd, FileRemote.CmdEvent>
//  {
//    private static final long serialVersionUID = 1L;
//
//    private FileRemote filesrc, filedst;
//
//    /**Source of the forward event, the opponent of this. It is the instance which creates the event. */
//    private final EventSource evSrcCmd;
//    
//    /**callback data: the yet handled file. It is a character array because it should not need a new instance. */
//    public char[] fileName = new char[100];
//    
//    /**callback data: number of bytes in the yet handled file.  */
//    public long nrofBytesInFile;
//    
//    /**callback data: number of bytes for the command.  */
//    public long nrofBytesAll;
//    
//    /**callback data: number of files in the yet handled command.  */
//    public int nrofFiles;
//    
//    public int successCode;
//    
//    public String errorMsg;
//    
//    public int promilleCopiedFiles, promilleCopiedBytes;
//    
//    /**A order number for the handled file which can be used to send the proper forward command. */
//    //public int orderFile;
//    
//    /**Creates a non-occupied event. This event contains an EventSource which is used for the forward event.
//     * @param dst
//     * @param thread
//     * @param evSrcCmd
//     */
//    public CallbackEvent(String name, EventConsumer dst, EventTimerThread thread, EventSource evSrcCmd){ 
//      super(name, null, null, dst, thread, new CmdEvent(name + "-cmd")); 
//      this.evSrcCmd = evSrcCmd;
//    }
    
    
    
    
    
    /**Creates the object of a callback event inclusive the instance of the forward event (used internally).
     * @param refData The referenced data for callback, used in the dst routine.
     * @param dst The routine which should be invoked with this event object if the callback is forced.
     * @param thread The thread which stores the event in its queue, or null if the dst can be called
     *   in the transmitters thread.
     */
//    public CallbackEvent(String name, EventSource evSrcCallback, FileRemote filesrc, FileRemote fileDst
//        , EventConsumer dst, EventTimerThread thread, EventSource evSrcCmd){ 
//      super(name, null, null, dst, thread, new CmdEvent(name + "-cmd", evSrcCmd, filesrc, fileDst, null, null)); 
//      this.filesrc = filesrc;
//      this.filedst = fileDst;
//      this.evSrcCmd = evSrcCmd;
//    }
//    
//    public boolean occupy(EventSource source, FileRemote fileSrc, boolean expect){
//      boolean bOccupied = occupy(source, expect); 
//      if(bOccupied){
//         this.filesrc = fileSrc;
//       }
//      return bOccupied;
//    }
//    
//    public boolean occupy(EventSource source, int orderId, boolean expect){
//      boolean bOccupied = occupy(source, expect); 
//      if(bOccupied){
//         this.orderId = orderId;
//       }
//      return bOccupied;
//    }
//    
//    public void setFileSrc(FileRemote fileSrc){ this.filesrc = fileSrc; }
//
//    public void setFiles(FileRemote fileSrc, FileRemote fileDst){ 
//      this.filesrc = fileSrc;
//      this.filedst = fileDst;
//    }
//
//    
//    
//    @Override
//    public boolean sendEvent(CallbackCmd cmd){ return super.sendEvent(cmd); }
//    
//
//    @Override public CallbackCmd getCmd(){ return super.getCmd(); }
//    
//    @Override public CmdEvent getOpponent(){ return (CmdEvent)super.getOpponent(); }
//
//    public FileRemote getFileSrc(){ return filesrc; }
//
//    public FileRemote getFileDst(){ return filedst; }
//    
//    
    /**Skips or aborts the copying of the last file which's name was received by the callback event. The destination file is deleted. 
     * <ul>
     * <li>If the received callback designates that the copy process is waiting for an answer, this cmd skips exact that file. 
     * <li>If the callback event does only give a status information and the copy process is running, the current copied file is aborted.
     *   This feature can be used if the file is a large file which is copied to a slow destination (network). Sometimes especially
     *   large files are not useful to copy if there are large report files or generation results etc. but such files needs unnecessary time 
     *   to copy.
     * <li>If the last callback had given a status information, the user sends the abort file, the processing of the event needs some time
     *     and the event was received later while a next file is copied, that file is not aborted. For that reason the ident number
     *     of the callback message should be copied in this event.
     * </ul>  
     *   But if the abort event is go though. It is possible to abort
     *   a long-time-copying of a large file especially in network access. The user can see the percent of progress and send this
     *   aborting command for the current file. To 

     * @param associatedCallback
     * @param modeCopyOper
    public void copySkipFile(int modeCopyOper){
      FileRemote.CmdEvent evcmd = getOpponent();
      if(evcmd.occupy(evSrcCmd, true)){
        evcmd.setOrderId(orderId);
        evcmd.modeCopyOper = modeCopyOper;
        evcmd.sendEvent(FileRemoteCmdEventData.Cmd.abortCopyFile);
      }

    }
     */
    
    
    /**Designates that the requested file which's name was received by the callback event should be overwritten. 
     * @param associatedCallback
     * @param modeCopyOper
    public void copyOverwriteFile(int modeCopyOper){
      FileRemote.CmdEvent evcmd = getOpponent();
      if(evcmd.occupy(evSrcCmd, true)){
        evcmd.setOrderId(orderId);
        evcmd.modeCopyOper = modeCopyOper;
        evcmd.sendEvent(FileRemote.FileRemoteCmdEventData.Cmd.overwr);
      }

    }
     */
    
    

    /**Designates that the directory of the requested file which's name was received by the callback event 
     * should be skipped by the copy process. The current copying file is removed. The files which were copied
     * before this event was received are not removed. 
     * @param associatedCallback
     * @param modeCopyOper
     public void copySkipDir(int modeCopyOper){
      FileRemote.CmdEvent evcmd = getOpponent();
      if(evcmd.occupy(evSrcCmd, true)){
        evcmd.setOrderId(orderId);
        evcmd.modeCopyOper = modeCopyOper;
        evcmd.sendEvent(FileRemote.FileRemoteCmdEventData.Cmd.abortCopyDir);
      }

    }
    */
    
    

    /**Designates that the copy process which was forced forward with this callback should be stopped and aborted. 
     * @param associatedCallback
     * @param modeCopyOper
     * @return true if the forward event was sent.
    public boolean copyAbortAll(){
      FileRemote.CmdEvent ev = getOpponent();
      FileRemote fileSrc;
      FileRemoteAccessor device;
      if( ev !=null && (fileSrc = ev.filesrc) !=null && (device = fileSrc.device) !=null){
        if((ev = device.prepareCmdEvent(500, this)) !=null){
          return ev.sendEvent(FileRemoteCmdEventData.Cmd.abortAll);
        } 
        else {
          return false; //event occupying fails
        } 
      }
      else {
        return false; //event is not in use.
      }
      
    }
     */
    
    
//  }
  
  
  public static enum XXXCallbackCmd  {
    /**Ordinary value=0, same as {@link Event.Cmd#free}. */
    free ,
    /**Ordinary value=1, same as {@link Event.Cmd#reserve}. */
    reserve,  //first 2 ordinaries from Event.Cmd
    /**A simple done feedback*/
    done,
    /**The operation is executed, but not successfully. */
    nok,
    /**Feedback, the operation is not executed. */
    error,
    
    /**Deletion error.*/
    errorDelete,
    
    /**Done message for the {@link Cmd#check} event. The event contains the number of files and bytes.*/
    doneCheck,
    
    /**Status event with processed number of files and bytes and the currently processed file path. 
     * This is only an intermediate message. The event can be removed from queue if it isn't processed
     * and replaced by a new event with the same Event object. 
     */
    nrofFilesAndBytes,
    
    /**Status event with the currently processed directory path. 
     * This is only an intermediate message. The event can be removed from queue if it isn't processed
     * and replaced by a new event with the same Event object. 
     */
    copyDir,

    /**callback to ask what to do because the source file or directory is not able to open. */
    askErrorSrcOpen,
    
    /**callback to ask what to do because the destination file or directory is not able to create. */
    askErrorDstCreate,
    
    /**callback to ask what to do on a file which is existing but able to overwrite. */
    askDstOverwr,
    
    /**callback to ask what to do on a file which is existing and read only. */
    askDstReadonly,
    
    /**callback to ask that the file is not able to overwrite. The user can try it ones more or can press skip. */
    askDstNotAbletoOverwr,
    
    /**callback to ask what to do because an copy file part error is occurred. */
    askErrorCopy,
    
    acknAbortAll,
    
    acknAbortDir,
    
    acknAbortFile,
    
    /**Start the process, the first event, also to check the event. */
    start,
    
    last
  }

  
  
  
  
  /**This inner non static class is only intent to access from a FileRemoteAccessor to any file.
   * It is not intent to use by any application. 
   */
  public class InternalAccess{
    public int setFlagBit(int bit){ flags |= bit; return flags; }
    
    public int clrFlagBit(int bit){ flags &= ~bit; return flags; }
    
    public int clrFlagBitChildren(int bit, int recursion){
      int nrofFiles = 1;
      if(recursion > 1000) throw new IllegalArgumentException("too many recursion in directory tree");
      clrFlagBit(bit);
      if(children !=null){
        for(Map.Entry<String, FileRemote> child: children.entrySet()){
          if(child !=null){ nrofFiles += child.getValue().internalAccess().clrFlagBitChildren(bit, recursion +1); }
        }
      }
      return nrofFiles;
    }
    
    public int setFlagBits(int mask, int bits){ flags &= ~mask; flags |= bits; return flags; }

    
    public void setPath(Path path) {
      //FileRemote.this.path = path;
      String sPath = path.toString();
      File fileExists = path.toFile();
      //CharSequence snPath = ;
      CharSequence[] dirName = FileFunctions.separateDirName(FileFunctions.normalizePath(sPath));
      boolean bOkDirName = StringFunctions.compare(FileRemote.this.sDir, dirName[0]) == 0
                        && StringFunctions.compare(FileRemote.this.sFile, dirName[1]) == 0;
      if(!bOkDirName) {           // do only change if necessary
        FileRemote.this.oFile = path.toFile();
        FileRemote.this.path = path;
        FileRemote.this.sDir = dirName[0].toString();
        FileRemote.this.sFile = dirName[1] == null ? "/" : dirName[1].toString();
      }
    }
    
    /**The values are set if there are not ==-1
     * @param length
     * @param dateLastModified
     * @param dateCreation
     * @param dateLastAccess
     */
    public void setLengthAndDate(long length, long dateLastModified, long dateCreation, long dateLastAccess){
      if(dateLastModified !=-1) { FileRemote.this.date = dateLastModified; }
      if(dateLastAccess !=-1) { FileRemote.this.dateLastAccess = dateLastAccess; }
      if(dateCreation !=-1) { FileRemote.this.dateCreation = dateCreation; }
      if(length !=-1) { FileRemote.this.length = length; }
    }
    
    public int setOrClrFlagBit(int bit, boolean set){ if(set){ flags |= bit; } else { flags &= ~bit;} return flags; }
  
    
    public void setRefreshed(){ flags &= ~mShouldRefresh; timeRefresh = System.currentTimeMillis(); }
    
    /**Removes all children which are marked as {@link FileRemote#mRefreshChildPending},
     * removes the {@link FileRemote#mShouldRefresh} mark for this directory
     * and sets the {@link FileRemote#timeRefresh} and {@link FileRemote#timeChildren} to the current time then.
     */
    public void setChildrenRefreshed(){
      if(FileRemote.this.children !=null) {
        synchronized(FileRemote.this.children) {
          Iterator<Map.Entry<String, FileRemote>> iter = FileRemote.this.children.entrySet().iterator();
          while(iter.hasNext()) {
            Map.Entry<String, FileRemote> filentry = iter.next();
            FileRemote child = filentry.getValue();
            if(child == null) {
              Debugutil.stop();
            } else {
              if((child.flags & mRefreshChildPending)!=0) {
                //child file is not existing, remove it:
                iter.remove();
              }
            }//if
          }//while
        } //synchronized
      }      
      flags &= ~mShouldRefresh; timeRefresh = timeChildren = System.currentTimeMillis(); 
    }
    
    /**Creates a new children list or removes all children because there should be refreshed.
     * The children are not removed but only marked as {@link FileRemote#mRefreshChildPending} because the instances are necessary for refreshing
     * to get the same instance for the same child again. Note that some marker may be stored there, see {@link FileRemote#setMarked(int)} etc.
     * On entry a new child the instance will be unchanged, but the marker mRemoved is removed and the properties of the child are refreshed.
     * On finishing the refreshing of a directory all {@link FileRemote} child instances which's mRemoved is remain are removed then.
     */
    public void newChildren(){ 
      if(children == null){
        children = createChildrenList(); 
      } else {
        Iterator<Map.Entry<String, FileRemote>> iter = children.entrySet().iterator();
        while(iter.hasNext()) {
          FileRemote child = iter.next().getValue();
          if(child == null) {
            Debugutil.stop();
          } else {
            child.flags |= mRefreshChildPending;
          }
        }
      }
    }
    
    /**Creates a new file as child of this file. It does not add the child itself because it may be gathered
     * in an second container and then exchanged. Only for internal use.
     * @return The new child, contains this as parent, but not added to this.
     */
    public FileRemote newChild(final CharSequence sPath
        , final long length, final long dateLastModified, long dateCreation, long dateLastAccess, final int flags
        , Object oFileP){
      return new FileRemote(itsCluster, device, FileRemote.this, sPath, length
          , dateLastModified, dateCreation,FileRemote.this.dateLastAccess, flags, oFileP, true);
    }
    
    public void putNewChild(FileRemote child){ FileRemote.this.putNewChild(child); }

    /**Sets this FileRemote to the delete state because the underlying file was deleted. 
     * This FileRemote itself will be marked with 0 in all length, flags and date.
     * This FileRemote will be removed from the parent's children list.
     * */
    public boolean setDeleted() {
      setFlagBits(0xffffffff, 0); //clean all
      setLengthAndDate(0, 0,0,0);
      String key = FileRemote.this.sDir.length()>=3 && FileRemote.this.sDir.charAt(1)== ':' 
          ? sFile.toUpperCase() : sFile;                   // Windows: keys in upper case
      FileRemote fdel = FileRemote.this.parent.children.remove(key);
      return fdel == FileRemote.this;
    }
    
    
  }
  
  private final InternalAccess acc_ = new InternalAccess();
  
  /**This routine is only intent to access from a FileRemoteAccessor to any file.
   * It is not intent to use by any application. 
   */
  public InternalAccess internalAccess(){ return acc_; }

  
  
  
  
  
}  
  
  
  
