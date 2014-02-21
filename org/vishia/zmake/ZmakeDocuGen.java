package org.vishia.zmake;

import java.util.ArrayList;
import java.util.List;


import org.vishia.cmd.ZGenExecuter;
import org.vishia.zmake.ZmakeUserScript.ZbnfUserFilepath;

/**This class extends a ZmakeUserScript.UserScript to hold the data from a docuGen description file for generating
 * the execution script and the XSLT control script for documentation generation. It holds the data to use in zTextGen
 * with capabilities of zmake (especially file handling).
 * 
 * @author Hartmut Schorrig
 *
 */
public class ZmakeDocuGen extends ZmakeUserScript.UserScript
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-03-09 Hartmut created: Up to now the docuGen was realized with XSLT and ANT. This class is the approach
   *   to use Zmake generation instead. The data which were hold in XML files for XSLT translation are hold in Java data now. 
   * </ul>
   * <b>Copyright/Copyleft</b>:
    * For this source the LGPL Lesser General Public License,
    * published by the Free Software Foundation is valid.
    * It means:
    * <ol>
    * <li> You can use this source without any restriction for any desired purpose.
    * <li> You can redistribute copies of this source to everybody.
    * <li> Every user of this source, also the user of redistribute copies
    *    with or without payment, must accept this license for further using.
    * <li> But the LPGL ist not appropriate for a whole software product,
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
   static final public int version = 20130310;

  
  
  String inputFile;
  
  UserFilepath hrefCtrl;
  
  List<ImportXsl> importsXsl = new ArrayList<ImportXsl>();
  
  List<Document> documents = new ArrayList<Document>();
  
  
  public HyperlinkAssociations hyperlinkAssociations = new HyperlinkAssociations();
  
  public void set_file(String val){ inputFile = val; }
  
  
  public ZmakeUserScript.ZbnfUserFilepath new_hrefCtrl(){ return new ZmakeUserScript.ZbnfUserFilepath(this); }
  
  public void add_hrefCtrl(ZbnfUserFilepath val){ hrefCtrl = val.filepath; }
  
  public void set_hrefCtrl(String val){  }
  
  public ImportXsl new_import(){ return new ImportXsl(); }
  
  public void add_import(ImportXsl val){ importsXsl.add(val); }
  
  
  public Document new_document(){ return new Document(); }
  
  public void add_document(Document val){ documents.add(val); }
  
  
  
  public ZmakeDocuGen(ZGenExecuter jbatExecuter){
    super();
  }
 
  public class ImportXsl
  {
    public String dir;
    public String href;
    public String type;
  }
  
  
  public class InputDocument
  {
    public String inputfile;
  }
  
  
  public class ContentDocument
  {
    public String select;
    
    final String type;
    
    ContentDocument(String val){ type = val; }
  }
  
  
  public class Document
  {
    public String ident;
    
    public String title;
    
    public String cssHtml;
    
    
    UserFilepath outHtml; 
    
    
    public ZmakeUserScript.ZbnfUserFilepath new_outHtml(){ return new ZmakeUserScript.ZbnfUserFilepath(ZmakeDocuGen.this); }
    
    public void set_outHtml(ZmakeUserScript.ZbnfUserFilepath val){ outHtml =val.filepath; }
    
    
    List<InputDocument> inputs = new ArrayList<InputDocument>();
    
    List<ContentDocument> content = new ArrayList<ContentDocument>();
    
    
    public InputDocument new_input(){ return new InputDocument(); }
    
    public void add_input(InputDocument val){ inputs.add(val); }
    
    
    public ContentDocument new_topictree(){ return new ContentDocument("topictree"); }
    
    public void add_topictree(ContentDocument val){ content.add(val); }
    
    public ContentDocument new_topic(){ return new ContentDocument("topic"); }
    
    public void add_topic(ContentDocument val){ content.add(val); }
    
    
    
    
  }

  
  public static class HyperlinkAssociations
  {
    public List<HyperlinkAssociation> association = new ArrayList<HyperlinkAssociation>();
    
    
  }
  
  public static class HyperlinkAssociation
  {
    public String dst;
    
    public String href;
    
  }
}
