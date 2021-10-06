package org.vishia.util;

import org.vishia.event.EventTimerThread;

/**Deprecated older class instead TimeOrderMng
 * @author Hartmut Schorrig
 * @deprecated use {@link EventTimerThread}
 */
@Deprecated public class OrderListExecuter extends EventTimerThread
{

  public OrderListExecuter(String name)
  {
    super(name);
  }
  
}
