package org.vishia.zcmd;

import javax.script.ScriptException;

import org.vishia.jztcmd.JZtcmd;

/**This class is the old form of JZtcmd for compatibility. See {@link JZtcmd}
 * @author Hartmut Schorrig
 *
 */
@Deprecated public class JZcmd extends JZtcmd
{

  public JZcmd()  throws ScriptException {
    super(null);
  }
  
  
  public static void main(String [] sArgs){ JZtcmd.main(sArgs); }
  
  public static int smain(String[] sArgs) throws ScriptException { return JZtcmd.smain(sArgs); }
  
  
  
  
}
