package org.vishia.zmake;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ZmakeUserScript
{
	
	
	/**ZBNF-result for
	 * <pre>
prepFilePath::=<$NoWhiteSpaces><! *?>
[<!.?@drive>:]               ## only 1 char with followed : is drive
[ [/|\\]<?@absPath>]         ## starting with / is absolute path
[<*:?@pathbase>[?:=]:]       ## all until : is pathbase
[ <*|**?@path>               ## all until ** is path
| <toLastCharIncl:/\\?@path> ## or all until last \\ or / is path
|]                           ## or no path is given.
[ **<?@allTree>[/\\] ]       ## ** is found, than files in subtree
[ <**?@file>                 ## all until * is the file (begin)
  *<?@someFiles>             ## * detect: set someFiles
| <toLastChar:.?@file>       ## or all until dot is the file
|]                           ## or no file is given.
[\.*<?@wildcardExt>]         ## .* is wildcard-extension
[ <* \e?@ext> ]              ## the rest is the extension
.
	 * </pre>
	 */
	public static class UserFilepath
	{
		public String drive;
		/**From Zbnf: [ [/|\\]<?@absPath>]. Set if the path starts with '/' or '\' maybe after drive letter. */
		public boolean absPath;
		
		/**Path-part before a ':'. */
		public String pathbase;
		
		/**Localpath after ':' or the whole path. */
		public String path = "";
		public String file = "";
		public String ext = "";
		
		boolean allTree, someFiles, wildcardExt;
		public void set_someFiles(){ someFiles = true; }
		public void set_wildcardExt(){ wildcardExt = true; }
		public void set_allTree(){ allTree = true; }
		
		String getPath(){ return file; }
		
		public String toString()
		{ return path + file + ext;
		}
	}
	
	
	
	
	/**A < fileset> in the ZmakeStd.zbnf:
	 * <pre>
fileset::= 
{ srcpath = <""?!prepSrcpath>
| srcext = <""?srcext>
| <file>
? , | + 
}.

	 * </pre>
	 * @author Hartmut
	 *
	 */
	public static class UserFileset
	{
		/**From ZBNF: */
		UserFilepath srcpath;
		public String srcext;
		List<UserFilepath> filesOfFileset = new LinkedList<UserFilepath>();
		public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(); }
		public void set_srcpath(UserFilepath val){  }
		
		/**From ZBNF: < file>. */
		public UserFilepath new_file(){ return new UserFilepath(); }
		/**From ZBNF: < file>. */
		public void add_file(UserFilepath val){ filesOfFileset.add(val); }
		
		public String toString(){ 
			StringBuilder u = new StringBuilder();
			if(srcpath !=null) u.append("srcpath="+srcpath+", ");
			u.append(filesOfFileset);
			return u.toString();
		}
	}
	
	
	private final static class UserStringContent
	{
		private final char type;
		private String text;
		private UserStringContent(char type){ this.type = type; }
		private UserStringContent(char type, String text){ this.type = type; this.text = text; }
	}
	
	
	/**A < string> in the ZmakeStd.zbnf is
	 * string::= { <""?literal> | <forInputString> | @<$?inputField> | $<$?contentOfVariable> ? + }.
   *
	 */
	public final static class UserString
	{
		private List<UserStringContent> content = new LinkedList<UserStringContent>();
		
		/**Set From ZBNF: */
		public UserStringContent new_literal(){ return new UserStringContent('\"'); }
		
		/**Set From ZBNF: */
		public void add_literal(UserStringContent val){ content.add(val); }
		
		public void set_literal(String val){ 
			UserStringContent el = new UserStringContent('\"', val);
			content.add(el);
		}
		
		public CharSequence getText()
		{
			StringBuilder u = new StringBuilder();
			for(UserStringContent el: content){
				switch(el.type){
				case '"': u.append(el.text);
				}
			}
			return u;
		}

	}
	
	/**A < variable> in the ZmakeStd.zbnf is
	 * variable::=<$?@name> = [ fileset ( <fileset> ) | <string> | { <execCmd> } ].
	 *
	 */
	public static class ScriptVariable
	{ public String name;
	  public UserFileset fileset;
	  public UserString string;
	  public UserString new_string(){ return string = new UserString(); }
	  public void set_string(UserString val){} //left empty, is set already.
	}
	
	public static class UserParam
	{ public String name;
	  public String value;
	  public String variable;
	}
	
	public static class UserInputSet
	{
		public String name;
		public UserFilepath srcpath;
	}
	
	
	/**Describes 1 input item, maybe a file, maybe a inputset. */
	static class UserInput
	{ final UserInputSet inputSet;
		final UserFilepath inputFile;
		UserInput(UserInputSet inputSet){ this.inputSet = inputSet; this.inputFile = null; }
		UserInput(UserFilepath inputFile){ this.inputSet = null; this.inputFile = inputFile; }
	}
	
	
	/**
	 * <pre>
	 * target::=  [:<$?@target>:] 
			[ <specials?do>  ##action without dst file. 
			|
			  ##<*|\ |\r|\n|:=?!prepOutputfile> :=  
			  <output> :=
			  [ for ( <input?> ) <routine?doForAll> 
			  | <routine?>
			  ##| exec <exec?>
			  ##| <$?@translator> ( <input?> )
			  ]
			].

input::=
{ \$<inputSet>
| <""?!prepInputfile>
| \{ <target> \}
| srcpath = <""?!prepSrcpath>
| srcext = <""?srcext>
##| target = <""?@target>
##| task = <""?@target>
| <param>
| <*\ \r\n,)?!prepInputfile>
? , | +
}.

   * </pre>
	 * @author Hartmut
	 *
	 */
	public final static class UserTarget
	{
		public UserFilepath output;
		
		public String name;
		
		public String translator;
		
		UserFilepath srcpath;
		
		public String srcext;
		
		public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(); }
		
		public void set_srcpath(UserFilepath value){  }
		
		List<UserInput> inputs = new LinkedList<UserInput>();
		
		Map<String,UserParam> params;
		
		/**From ZBNF < inputSet>*/
		public UserInputSet new_inputSet()
		{ UserInput userInput = new UserInput(new UserInputSet());
		  inputs.add(userInput);
			return userInput.inputSet; 
		}
		
		public void add_inputSet(UserInputSet value){  }
		
		
		/**From ZBNF < param>. */
		public UserParam new_param(){ return new UserParam();}
		
		/**From ZBNF < param>. */
		public void add_param(UserParam val){ 
		  if(params ==null){ params = new TreeMap<String,UserParam>(); }
		  params.put(val.name, val); 
		}
		
		/**From ZBNF < ?!prepInputfile> < ?input>*/
		public UserFilepath new_input()
		{ UserInput userInput = new UserInput(new UserFilepath());
		  inputs.add(userInput);
			return userInput.inputFile; 
		}

		public void add_input(UserFilepath val){}

	
	}
	
	
	
	/**The main class of the data of the users script for zmake.
	 * Filled from {@link org.vishia.zbnf.ZbnfJavaOutput}.
	 *
	 */
	public static class UserScript
	{
		Map<String, ScriptVariable> allVariables = new TreeMap<String, ScriptVariable>();
		
		List<UserTarget> targets = new LinkedList<UserTarget>();
		
		/**From ZBNF: < variable> */
		public ScriptVariable new_variable(){ return new ScriptVariable(); }
		
		public void add_variable(ScriptVariable  value){ allVariables.put(value.name, value); }
		
		public UserTarget new_target(){ return new UserTarget(); }
		
		public void add_target(UserTarget value){ targets.add(value); }
		
	}

}
