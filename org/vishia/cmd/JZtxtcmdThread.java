package org.vishia.cmd;

import org.vishia.util.DataAccess;
import org.vishia.util.MessageQueue;
import org.vishia.util.StringFormatter;

/**This class is uses as Data base for all threads in a JZcmd execution environment.
 * One instance is aggregated in {@link JZtxtcmdExecuter#scriptThread}. Any user created thread (in a script)
 * creates an instance of this. 
 * @author Hartmut Schorrig
 *
 */
public class JZtxtcmdThread extends JZtxtcmdThreadData implements Runnable
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-04-24 Hartmut chg: {@link #sendcmd(String, Object)} etc. uses 2 arguments, internally {@link MsgItem} is stored.
   * <li>2014-04-24 Hartmut created from 2 inner classes of {@link JZtxtcmdExecuter}: JZcmdThread and ThreadData.
   *   Both class are joined.  
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-04-30";

  /**Item inside the message queues.
   * @author hartmut
   *
   */
  public static class MsgItem {
    public final String cmd;
    public final Object data;
    
    public MsgItem(String cmd, Object data){ this.cmd = cmd; this.data = data; }
  }
  
  /**Used only for starting the thread. */
  private JZtxtcmdExecuter.ExecuteLevel executeLevel;

  /**Used only for starting the thread. */
  private JZtxtcmdScript.ThreadBlock statement;
  


  /**State of thread execution. i-init, r-run, y-finished.*/
  char state = 'i';

  
  /**A queue for input and a queue for output data. Use {@link #sendcmd(Object)} etc.*/
  private MessageQueue<MsgItem> msg1, cmd1;
  
  
  public JZtxtcmdThread()
  {
  }

  protected void startThread(String name, JZtxtcmdExecuter.ExecuteLevel startLevel
      , JZtxtcmdScript.ThreadBlock statementArg) {
    this.executeLevel = startLevel;
    this.statement = statementArg;
    Thread threadmng = new Thread(this, name);
    threadmng.start();  

  }
  
  
  @Override public void run(){ 
    state = 'r';
    executeLevel.runThread(executeLevel, statement, this); 
    state = 'y';
    executeLevel.finishThread(this);
  }

  
  protected void clear(){
    state = 'i';
  }
  
  
  /**State of thread execution. 
   * <ul>
   * <li>i: init
   * <li>r: runs
   * <li>y: finished
   * </ul>
   */
  public char state(){ return state; }
  
  
  /**Sends a command from another thread to this thread.
   * @param cmd the string given command.
   * @param data Any data.
   */
  public void sendcmd(String cmd, Object data){
    if(cmd1 == null){ cmd1 = new MessageQueue<MsgItem>(); }
    cmd1.put(new MsgItem(cmd, data));
  }
  
  /**Waits for a command which can be sent by another thread using {@link #sendcmd(String, Object)}
   * @param timeout milliseconds for wait, use 0 for wait forever.
   * @return instance with the String given command and an Object.
   */
  public MsgItem awaitcmd(int timeout){
    if(cmd1 == null){ cmd1 = new MessageQueue<MsgItem>(); }
    return cmd1.await(timeout);
  }
  
  public void sendmsg(String cmd, Object data){
    if(msg1 == null){ msg1 = new MessageQueue<MsgItem>(); }
    msg1.put(new MsgItem(cmd, data));
  }
  
  public Object awaitmsg(int timeout){
    if(msg1 == null){ msg1 = new MessageQueue<MsgItem>(); }
    return msg1.await(timeout);
  }
  
  /**Waits for finishing the thread.
   * @param time timeout for waiting
   * @return true if the thread is finished, false on timeout.
   */
  public boolean join(int time){
    synchronized(this){
      if(state =='r'){
        try {
          wait(time);
        } catch (InterruptedException e) { }
      }
    }
    return state == 'y';
  }
  
}

  

