package org.vishia.zcmd;

import javax.script.ScriptException;

import org.vishia.jzTc.JzTc;

/**This class is the old form of JZtcmd for compatibility. See {@link JzTc}
 * @author Hartmut Schorrig
 *
 */
@Deprecated public class JZcmd extends JzTc
{

  public JZcmd()  throws ScriptException {
    super(null);
  }
  
  
  public static void main(String [] sArgs){ JzTc.main(sArgs); }
  
  public static int smain(String[] sArgs) throws ScriptException { return JzTc.smain(sArgs); }
  
  
  
  
}
