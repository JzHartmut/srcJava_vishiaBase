package org.vishia.zmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.Report;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.xmlSimple.XmlException;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;
import org.vishia.zbnf.ZbnfXmlOutput;

/**This class contains control data and sub-routines to generate the ANT-file for all Zmake-targets.
 * 
 * @author Hartmut Schorrig
 *
 */
public class ZmakeAntGenCtrl
{

	private final Report console;
	
	/**Mirror of the content of the ant-genctrl-configuration file. Filled from ZBNF-ParseResult*/
	private ZmakeGenCtrl zmakeGenCtrl = new ZmakeGenCtrl();
  
	
	public ZmakeAntGenCtrl(Report console)
	{ this.console = console;
	}

	
	boolean parseAntGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl) 
	throws FileNotFoundException, IOException
	  , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
	{ boolean bOk;
		ZbnfParser parserGenCtrl = new ZbnfParser(console);
    parserGenCtrl.setSyntax(fileZbnf4GenCtrl);
    bOk = parserGenCtrl.parse(new StringPartFromFileLines(fileGenCtrl));
    if(!bOk){
    	String sError = parserGenCtrl.getSyntaxErrorReport();
    	throw new ParseException(sError,0);
    }
    //ZbnfParseResultItem parseResultGenCtrl = parserGenCtrl.getFirstParseResult();
    ZbnfXmlOutput xmlOutputGenCtrl = new ZbnfXmlOutput();
    xmlOutputGenCtrl.write(parserGenCtrl, fileGenCtrl.getAbsoluteFile()+".xml");  //only for test
    //write into Java classes:
    ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput(console);
    parserGenCtrl2Java.setContent(zmakeGenCtrl.getClass(), zmakeGenCtrl, parserGenCtrl.getFirstParseResult());
    
    return bOk;
	}
	
	
	/**Searches the Zmake-target by name (binary search. TreeMap.get(name).
	 * @param name The name of given < ?translator> in the end-users script.
	 * @return null if the Zmake-target is not found.
	 */
	ZmakeTarget searchZmakeTaget(String name){ return zmakeGenCtrl.zmakeTargets.get(name); }
	
	
	
	/**Class for ZBNF parse result Element of String 
	 */
	public final static class ZmakeStringElement
	{
		final boolean isVariable;
		public String text;
		public ZmakeStringElement(boolean isVariable){ this.isVariable = isVariable; }
		public ZmakeStringElement(String text, boolean isVariable){ this.text = text; this.isVariable = isVariable; }
		@Override public String toString(){ return (isVariable?"$$(":"") + text + (isVariable?")":""); } 
	}
	
	
	
	/**Class for ZBNF parse result 
	 * <pre>
string::=
{ $$(<*)?variable>)   ##a part from any variable, forex from the input files, may be some files or fileset
| \e                  ##end of text
| <*|$$|\e?text>      ##constant parts of text.
} \e.
	 * </pre>
	 */
  public static class ZmakeString
  {
  	List<ZmakeStringElement> elements = new LinkedList<ZmakeStringElement>();
 
  	public ZmakeStringElement new_text(){ return new ZmakeStringElement(false); }
  	
  	public ZmakeStringElement new_variable(){ return new ZmakeStringElement(true); }
  	
  	public void add_text(ZmakeStringElement val){ elements.add(val); }

  	public void add_variable(ZmakeStringElement val){ elements.add(val); }

  	public void set_text(String val){ elements.add(new ZmakeStringElement(val, false)); }

  	public void set_variable(String val){ elements.add(new ZmakeStringElement(val, true)); }

 }
	
	
  public static final class ZmakeArgString extends ZmakeString
  {
  	/**mode: i=forInput .=else */
    char mode;
    
    ZmakeArgString(char mode){ this.mode = mode; }
  }
  

  
  /**Class for ZBNF parse result 
	 * <pre>
exec ::=
{ dir = [<?dir> <*\s\n;?!string> | <""?!string>];
| executable = [<?exectuable> <*\s\n;?!string>];
| failonerror=[<?failonerror> true|false] ;
| args \{ { [<?arg> <""?!string>|!] | forinput ( [<?argForinput> <""?!string>|!] )} \}
}.
	 * </pre>
	 */
  public final static class AntExec
  {
  	ZmakeString dir;
  	
  	public String executable;
  	
  	public String failonerror;
  	
  	public ZmakeString new_dir(){ return dir = new ZmakeString(); }
  	
  	public void set_dir(String val){}
  	
  	public void set_dir(ZmakeString val){}
  	
  	List<ZmakeArgString> args = new LinkedList<ZmakeArgString>();
 
  	public ZmakeArgString new_arg(){ return new ZmakeArgString('.'); }
  	
  	public ZmakeArgString new_argForinput(){ return new ZmakeArgString('i'); }
  	
   	public void add_arg(ZmakeArgString val){ args.add(val); }

   	public void add_arg(String val){ } //args.add(new ZmakeArgString(val, 'i')); }

  	public void add_argForinput(ZmakeArgString val){ args.add(val); }
  }
	
	
  
  final static class AntAction
  { final AntExec exec;
  
    final String nameRoutine;
  	
    AntAction(String nameAction){
    	this.nameRoutine = nameAction;
    	if(nameAction.equals("exec")){
    		exec = new AntExec();
    	} else {
    		exec = null;
    	}
    		
    }
  }
  
  
  
  /**Class for ZBNF parse result 
	 * <pre>
depends::=
[ forinput ( [<?forinputFile> <*\s\n,;?!string> | <""?!string>] )
| [<?file> <*\s\n,;\?!string> | <""?!string>]                             ##string contains build prescript for file name
].
	 * </pre>
	 */
  public final static class ZmakeDepends
  {
  	public ZmakeArgString string;
  	//public ZmakeString forinputFile;
  	
  	public ZmakeString new_file(){ return string = new ZmakeArgString('.'); }
   	//public void set_file(String val){  }
   	public void set_file(ZmakeString val){  }
    
   	public ZmakeString new_forinputFile(){ return string = new ZmakeArgString('i'); }
   	public void set_forinputFile(ZmakeString val){}
  	
  }
	
	
  
  
  
  
	
	/**Class for ZBNF parse result 
	 * <pre>
AntTarget::= genAntTarget \{
{ name = [<?name> <* ;?!string>|xxx] ;
| depends =   { <depends> ? , }  ;       ##more as one {depends,...} or only one depends
| exec \{ <exec> \}                                    ##exec written in { } to build a block
} \}.
	 * </pre>
	 */
	public final static class AntTarget
	{
		ZmakeString nameTarget;
		
		final boolean forInput;
		
		List<ZmakeDepends> depends = new LinkedList<ZmakeDepends>();
		
		List<AntAction> actions = new LinkedList<AntAction>();
		
		AntTarget(boolean forInput){ this.forInput = forInput; }

		public ZmakeString new_name(){ return nameTarget = new ZmakeString(); }
		
		public void set_name(ZmakeString val){}
		
		public String get_name(ZmakeUserScript.UserTarget inpTarget)
		{
			return "";
		}
		
		public void set_name(String val){}
		
		public ZmakeDepends new_depends(){ return new ZmakeDepends(); }
		
		public void add_depends(ZmakeDepends val){ depends.add(val); }
		
		public AntExec new_exec(){ 
			AntAction action = new AntAction("exec"); actions.add(action); 
			return action.exec; 
		}
		
		public void add_exec(AntExec val){ }
		
		
	}
	
	/**Class for ZBNF parse result 
	 * <pre>
ZmakeTarget::= target ( <$?@name> ) \{
{ AntTargetForinput < AntTarget?forinputAntTarget>
| AntTarget < AntTarget>
} \}.                                    ##one Zmake-target can generate more as one ANT-target.
	 * </pre>
	 */
	public final static class ZmakeTarget
	{
		private boolean bhasForInputTargets = false;
		
		public String name;
		List<AntTarget> antTargets = new LinkedList<AntTarget>();
		
		/**From ZBNF: < AntTarget> */ 
		public AntTarget new_AntTarget(){ return new AntTarget(false); }
		
		/**From ZBNF: < AntTarget?forinputAntTarget> */ 
		public AntTarget new_forinputAntTarget(){ bhasForInputTargets = true; return new AntTarget(true); } //true signed forinput
		
		/**From ZBNF: < AntTarget> */ 
		public void add_AntTarget(AntTarget val){ antTargets.add(val); } 
		
		/**From ZBNF: < AntTarget?forinputAntTarget> */ 
		public void add_forinputAntTarget(AntTarget val){ antTargets.add(val); }
		
		/**Query whether any forInput-ANT-Target is contained, then a superior target is necessary
		 * because more as one ANT-Targets are created from this.
		 * @return
		 */
		public boolean hasForInputAntTargets(){ return bhasForInputTargets; }
	}
	
	/**Main class for ZBNF parse result 
	 * <pre>
	 * ZmakeGenctrl::= { <target> } \e.
	 * </pre>
	 */
	public final static class ZmakeGenCtrl
	{

		private Map<String, ZmakeTarget> zmakeTargets = new TreeMap<String, ZmakeTarget>();
		
		public ZmakeTarget new_ZmakeTarget(){ return new ZmakeTarget(); }
		
		public void add_ZmakeTarget(ZmakeTarget val){ zmakeTargets.put(val.name, val); }
		
	}
	
}
