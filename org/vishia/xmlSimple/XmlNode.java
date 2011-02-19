/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2008-01-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
package org.vishia.xmlSimple;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;


public class XmlNode
{ String name;
  String namespaceKey;

  TreeMap<String, String> attributes;

  List<XmlContent> content;

  TreeMap<String, String> namespaces;

  
  
  XmlNode parent;
  
  public XmlNode(String name)
  { this.name = name;
  }
  
  public XmlNode(String name, String namespaceKey, String namespace)
  { this.name = name;
    this.namespaceKey = namespaceKey;
  }
  
  public void setAttribute(String name, String value)
  { if(attributes == null){ attributes = new TreeMap<String, String>(); }
    attributes.put(name, value);
  }
  
  public void addNamespaceDeclaration(String name, String value)
  { if(namespaces == null){ namespaces = new TreeMap<String, String>(); }
    namespaces.put(name, value);
  }
  
  public void addContent(String text)
  { if(content == null){ content = new LinkedList<XmlContent>(); }
    content.add(new XmlContent(text));
  }
  
  public void addContent(XmlNode node) 
  throws XmlException
  { if(content == null){ content = new LinkedList<XmlContent>(); }
    if(node.parent != null)
      throw new XmlException("node has always a parent");
    node.parent = this;
    content.add(new XmlContent(node));
  }
  
}


/*
class XmlAttribute
{ String name;
  String value;
}
*/


class XmlContent
{ String text;
  XmlNode xmlNode;
  
  XmlContent(String text){ this.text = text; }

  XmlContent(XmlNode xmlNode){ this.xmlNode = xmlNode; }

}