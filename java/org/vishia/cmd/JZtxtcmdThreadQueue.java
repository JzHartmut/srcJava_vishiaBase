package org.vishia.cmd;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.cmd.JZtxtcmdScript.Subroutine;
import org.vishia.util.DataAccess;
import org.vishia.util.DataAccess.Variable;


/**This class builds a queue to run sub routines in an separated Thread of JZtxtcmd execution.
 * It is similar working in {@link CmdQueue} but only for sub routines with the stable Scriptlevel as base for execution.
 * It is similar working in {@link JZtxtcmdThread} but it has a queue and waits for execution of new sub routines.
 * <br>
 * This class is used as aggregate in {@link org.vishia.gral.ifc.GralActionJztc} to run sub routines of JZtxtcmd as action from widgets.
 * @author Hartmut Schorrig
 *
 */
public class JZtxtcmdThreadQueue extends JZtxtcmdThreadData implements Runnable
{
  /**The version, history and license.
   * <ul>
   * <li>2023-08-14 subroutine able to call with arguments. More capabilities. 
   *   Used for {@link org.vishia.gral.cfg.GuiCfg} in srcJava_vishiaGui component. 
   * <li>2018-09-17 created 
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
   * 
   */
  public final static String version = "2023-08-14";

  
  class SubArg {
    JZtxtcmdScript.Subroutine sub;
    List<DataAccess.Variable<Object>> args;
    
    public SubArg(Subroutine sub, List<Variable<Object>> args) {
      this.sub = sub;
      this.args = args;
    }
    
  }
  
  
  ConcurrentLinkedQueue<SubArg> queue = new ConcurrentLinkedQueue<SubArg>();
  
  /**Used for execution all queued subroutines. */
  private JZtxtcmdExecuter.ExecuteLevel executeLevel;

  
  boolean bRun = true;
  
  
  /**It is invoked from GralActionJztc if used with GUI. It is for Callback sub routines.
   * The constructor starts the Thread and waits for actions.
   * @param name of the thread
   * @param jzTc to build an execute level.
   */
  public JZtxtcmdThreadQueue(String name, JZtxtcmdExecuter jzTcExec) {
    //An own executeLevel based on the scriptLevel, independent of the calling level
    this.executeLevel = jzTcExec.newExecuteLevel(this);
        
        //(startLevel.jzcmdMain, startLevel.jzClass, startLevel.jzcmdMain.scriptLevel, this, startLevel.localVariables);
    Thread threadmng = new Thread(this, name);
    threadmng.start();  

  }

  
  /**Add a subroutine to the queue.
   * @param sub The sub routine from the JZtxtcmdScript
   * @param actual named arguments, can be null.
   */
  public void add(JZtxtcmdScript.Subroutine sub, List<DataAccess.Variable<Object>> args) {
    queue.offer(new SubArg(sub, args));
  }
  
  /**Only internally as thread run. It executes all sub routines in the queue. 
   * @see java.lang.Runnable#run()
   */
  @Override public void run()
  {
    while(bRun) {
      SubArg sub = this.queue.poll();
      if(sub == null) { 
        try{ Thread.sleep(100);} catch(InterruptedException exc) {}
      } else {
        this.executeLevel.exec_Subroutine(sub.sub, sub.args, this.executeLevel.jzcmdMain.textline, 0);
      }
    }
    
  }

}
