package org.vishia.util;

/**This interface is used to mark functionality to get a type to use with an instance.
 * It is used especially for {@link org.vishia.zbnf.ZbnfJavaOutput} 
 * <br><br>
 * This interface is part of the component srcJava_vishiaBase because it is used in user classes 
 * which are independent from the component srcJava_Zbnf. The interface is recognized in the {@link org.vishia.zbnf.ZbnfJavaOutput}
 * but the user's code should not regard that.
 * 
 * @author Hartmut Schorrig, LPGL license or second license
 *
 */
public interface GetTypeToUse
{
  Class<?> getTypeToUse();  
}
