package org.vishia.zcmd;

import javax.script.ScriptException;

import org.vishia.jztxtcmd.JZtxtcmd;

/**This class is the old form of JZtcmd for compatibility. See {@link JZtxtcmd}
 * @author Hartmut Schorrig
 *
 */
@Deprecated public class JZcmd extends JZtxtcmd
{

  public JZcmd()  throws ScriptException {
    super(null);
  }
  
  
  public static void main(String [] sArgs){ JZtxtcmd.main(sArgs); }
  
  public static int smain(String[] sArgs) throws ScriptException { return JZtxtcmd.smain(sArgs); }
  
  
  
  
}
