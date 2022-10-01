package org.vishia.util;


/**Supports special handling of Outputs and assertions especially in debug phase. 
 * The application can create any special {@link CheckVs} class which extends this class
 * and override the both methods {@link #assertion(boolean)} and {@link #assertion(boolean, String)}.
 * The special instance can be set for the whole application calling {@link #setAssertionInstance(CheckVs)}.
 * It is also be possible to use a special assertion instance.
 * <br><br>
 * To check any assertion one can use the static methods 
 * <pre>
 * CheckVs.check(condition);
 * CheckVs.check(condition, msg);
 * </pre>
 * Then either this class (self instantiating) or the application wide CheckVs object is used.
 * <br>
 * The other possibility is, use a special modul-wide CheckVs object:
 * <pre>
 * CheckVs assert = new MyAssert();
 * ...
 * assert.assertion(condition);
 * assert.assertion(condition, msg);
 * </pre>
 * @author Hartmut Schorrig
 * @deprecated use {@link ExcUtil}
 *
 */
@Deprecated public class CheckVs extends ExcUtil {

 
}
