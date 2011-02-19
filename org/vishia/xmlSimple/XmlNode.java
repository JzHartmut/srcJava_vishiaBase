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