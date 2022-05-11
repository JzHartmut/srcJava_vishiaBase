package org.vishia.util;

/**This interface is used to mark functionality to set the original source information.
 * It is used especially for {@link org.vishia.zbnf.ZbnfJavaOutput} to determine which String was present as source,
 * for example on numeric conversion.
 * 
 * @author Hartmut Schorrig, LPGL license or second license
 *
 */
public interface SetSrcInfo_ifc {

  void setSrcInfo(String src);
}
