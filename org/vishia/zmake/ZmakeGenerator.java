package org.vishia.zmake;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.Report;
import org.vishia.zmake.ZmakeGenScript.Zbnf_genContent;

/**This class generates the output make file. */
public class ZmakeGenerator
{

	final Writer outAnt;
	
	final Report console;
	
	final private ZmakeUserScript.UserScript userScript;
	
	final private ZmakeGenScript mainGenScript;
	
	/**used for{@link #getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}.
	 * It is filled from the content of the script variable "currDir".
	 * 
	 */
	private String sCurrDir;
	
	/**That are all lists which are named in a generate control script. 
	 * The lists have names and are sorted in here.
	 */
	Map<String, List<CharSequence>> addToListTexts = new TreeMap<String, List<CharSequence>>();

  Map<String, String> scriptVariables = new TreeMap<String, String>();
	
	/**This List will be filled with all generated targets in its order in the users generation file,
	 * to determine the 'depends' in the main target.
	 */
	//final private List<String> indexAllAntTargets = new LinkedList<String>();
	
	public ZmakeGenerator(File fileAnt, ZmakeUserScript.UserScript userScript, ZmakeGenScript antGenCtrl, Report console) throws IOException
	{
	  this.outAnt = new FileWriter(fileAnt);
		this.console = console;
		this.userScript = userScript;
		this.mainGenScript = antGenCtrl;
	}


	
	/**Generates the output file which is invoked for make.
	 * @throws IOException
	 */
	void gen_ZmakeOutput() throws IOException
	{
		//complete script-level-variables:
		StringBuilder uBuffer = new StringBuilder();
		Gen_Content genVariable = new Gen_Content(null);
		
		for(Zbnf_genContent scriptVariableScript: mainGenScript.zbnfZmakeGenCtrl.listScriptVariables){
			uBuffer.setLength(0);
			genVariable.gen_ContentWithScript(uBuffer, null, null, scriptVariableScript, null, null, null);
			scriptVariables.put(scriptVariableScript.name, uBuffer.toString());
		}
		//the variable (?=currDir?) may exist. Get it:
		sCurrDir = scriptVariables.get("currDir");
		if(sCurrDir == null){
			sCurrDir = "";
		}
		
		//Generate the (?:file?)-part.
		Gen_Content genFile = new Gen_Content(null);
		if(mainGenScript.zbnf_genFile == null){
			final String sHint = "You must have a part in the ZmakeGen-script for the whole file output\n"
				+ "Syntax: (?:file?) ...some outputs...(?/file?)\n";
			throw new IllegalArgumentException(sHint);	
		}
		uBuffer.setLength(0);
		genFile.gen_Content(uBuffer, outAnt, null, mainGenScript.zbnf_genFile, null, null);
		outAnt.append(uBuffer);
		outAnt.close();
	}
	
	
	
	
	void genUserTargets(Writer out) throws IOException
	{
		StringBuilder uBuffer = new StringBuilder();
		for(ZmakeUserScript.UserTarget inpTarget: userScript.targets){
			uBuffer.setLength(0);
			//Note: target in Zmake maybe more as one target in ANT. It depends from the kind of target.
			ZmakeGenScript.Zbnf_genContent antZmakeTarget = mainGenScript.searchZmakeTaget(inpTarget.translator);
			if(antZmakeTarget == null){
				console.writeError("Zmake - unknown target; " + inpTarget.translator);
			} else {
				Gen_Content genZmakeTarget = new Gen_Content(null);
				genZmakeTarget.gen_Content(uBuffer, out, inpTarget, antZmakeTarget, null, null);
			}
			out.append(uBuffer);
		}
		
	}
	
	
	
	
	
	
	/**
	 * @author Hartmut
	 *
	 */
	final class Gen_Content
	{
		final Gen_Content parent;
		
		Map<String, CharSequence> localVariables = new TreeMap<String, CharSequence>();
		
		
		
		
		public Gen_Content(Gen_Content parent)
		{ this.parent = parent;
		  if(parent !=null){
			  this.localVariables.putAll(parent.localVariables);
		  }
		}


		/**Generates the content controlled with the zmakeCtrl-script into the buffer.
		 * @param uBuffer
		 * @param userTarget
		 * @param container
		 * @param input
		 * @param srcPathP
		 * @return
		 * @throws IOException
		 */
		boolean gen_Content(StringBuilder uBuffer
			, Writer out
			,	ZmakeUserScript.UserTarget userTarget
			, ZmakeGenScript.Zbnf_genContent container
			, ZmakeUserScript.UserFilepath input
			, ZmakeUserScript.UserFilepath srcPathP
			) 
		throws IOException
		{ boolean bOk = true;
			if(container.isContentForInput){
				for(ZmakeUserScript.UserInput inputIntern : userTarget.inputs){
					if(inputIntern.inputFile !=null){
						Gen_Content contentData = new Gen_Content(this);	
						contentData.gen_ContentWithScript(uBuffer, out, userTarget, container, inputIntern.inputFile
							, userTarget.srcpath, null);
					} else {
						//it is a input-set
						genContentForInputset(uBuffer, userTarget, container, inputIntern);
						stop();
					}
				}
			} else {
				Gen_Content contentData = new Gen_Content(this);	
				contentData.gen_ContentWithScript(uBuffer, out, userTarget, container, input, srcPathP, null);
			}
			return bOk;
		}
		
		
		/**Generates the content for a input-fileset. It is called inside 
		 * {@link #gen_Content(StringBuilder, org.vishia.zmake.ZmakeUserScript.UserTarget, Zbnf_genContent, org.vishia.zmake.ZmakeUserScript.UserFilepath, org.vishia.zmake.ZmakeUserScript.UserFilepath)}
		 * if a input-set is found in the input list. Note, that more as one input-set or input-files
		 * can be found in the input-list. This method is called in a for...loop for all inputs
		 * of the input-list.
		 * @param uBuffer The buffer for output
		 * @param userTarget The part of the user.zmake input, the < target> which forces the translation 
		 *                   of this output part. It contains the inputSetElement as one element 
		 *                   of its < input>. It is used here because it may contain a < ?srcpath>
		 * @param genScript The script for generation this output part.
		 * @param inputIntern The element of the users input in the script
		 * @throws IOException
		 */
		private void genContentForInputset(StringBuilder uBuffer
			,	ZmakeUserScript.UserTarget userTarget
			, ZmakeGenScript.Zbnf_genContent genScript
			, ZmakeUserScript.UserInput inputIntern
			) throws IOException
		{
			ZmakeUserScript.UserVariable inputsetVariable = userScript.allVariables.get(inputIntern.inputSet.name);
      final ZmakeUserScript.UserFilepath srcPath =
      	userTarget.srcpath !=null ? userTarget.srcpath
      	: inputsetVariable.fileset.srcpath;
			for(ZmakeUserScript.UserFilepath file: inputsetVariable.fileset.filesOfFileset){
      	Gen_Content contentData = new Gen_Content(this);	
				contentData.gen_ContentWithScript(uBuffer, null, userTarget, genScript, file, srcPath, null);
			}
			
		}


		/**Generates
		 * @param uBuffer      If out isn't given, the whole output is expected in the buffer. Used for internal values (variableValue)
		 * @param out          If given, then it writes to the file.
		 * @param userTarget   The users target. It may be the (?:file?)
		 * @param contentScript The zmakeCtrl-script-part
		 * @param input        The input of the users target.
		 * @param srcPath      A given srcPath of users target
		 * @param listElement  
		 * @throws IOException
		 */
		private void gen_ContentWithScript(
			StringBuilder uBuffer
		, Writer out	
		,	ZmakeUserScript.UserTarget userTarget
		, ZmakeGenScript.Zbnf_genContent contentScript
		, ZmakeUserScript.UserFilepath input
		, ZmakeUserScript.UserFilepath srcPath
		, CharSequence listElement
		) 
		throws IOException
		{ String sNameTarget;
		
		  //Fill all local variable, which are defined in this script.
			//store its values in the local Gen_Content-instance.
			for(Zbnf_genContent variableScript: contentScript.localVariables){
				StringBuilder uBufferVariable = new StringBuilder();
				Gen_Content genVariable = new Gen_Content(this);
				genVariable.gen_Content(uBufferVariable, null, userTarget, variableScript, input, srcPath);
				localVariables.put(variableScript.name, uBufferVariable);
			}
		
			//Generate the result for all output lists to fill and complete it.
			for(Zbnf_genContent listContainer: contentScript.addToList){
				StringBuilder uBufferLocal = new StringBuilder();
				Gen_Content contentData = new Gen_Content(this);	
			  contentData.gen_Content(uBufferLocal, out, userTarget, listContainer, input, srcPath); 
			  //save it
			  List<CharSequence> list = addToListTexts.get(listContainer.name);
				if(list == null){
					list = new LinkedList<CharSequence>();
					addToListTexts.put(listContainer.name, list);
				}
				list.add(uBufferLocal);
			}
		
			//Generate direct requested output. It is especially on inner content-scripts.
			for(ZmakeGenScript.Zbnf_ScriptElement contentElement: contentScript.content){
			  switch(contentElement.whatisit){
			  case 't': uBuffer.append(contentElement.text); break;
			  case 'i': {
			  	CharSequence text = getPartsFromFilepath(input, srcPath, contentElement.text);
			  	uBuffer.append(text); 
			  } break;
			  case 'o': { 
			  	CharSequence text = getPartsFromFilepath(userTarget.output, null, contentElement.text);
			  	uBuffer.append(text); 
			  } break;
			  case 'e': {
			  	uBuffer.append(listElement); 
				} break;
			  case 'v': { 
			  	if(contentElement.text.equals("target")){
			  		//generates all targets, only advisable in the (?:file?)
			  		out.append(uBuffer);   //flush content before.
			  		uBuffer.setLength(0);  //fill new
			  		genUserTargets(out);
			  	} else {
			  	 	CharSequence text = getTextofVariable(userTarget, contentElement.text, this);
				  	uBuffer.append(text); 
				 }
			  } break;
			  case 'I': {  //generation (?:forInput?) <genContent?forInputContent> (\?/forInput\?) 
			  	Gen_Content contentData = new Gen_Content(this);	
				  contentData.gen_Content(uBuffer, out, userTarget, contentElement.subContent, input, srcPath); 
			  } break;
			  case 'L': { //generation (\?:forList : <$?@name>\?) <genContent?> (\?/forList\?)
			  	String sListName = contentElement.subContent.name;
			  	List<CharSequence>  list = addToListTexts.get(sListName);
			  	if(list == null) {
			  		uBuffer.append("ERROR: list \"" + sListName + "\" not found :ERROR");
			  	} else {
			  		for(CharSequence listText: list){
			  			gen_ContentWithScript(uBuffer, out, userTarget, contentElement.subContent, input, null, listText);
			  		}
			  	}
			  } break;
			  default: 
			  	uBuffer.append("ERROR: unknown type '" + contentElement.whatisit + "' :ERROR");
			  }//switch
			  
			}
			if(out !=null){
				out.append(uBuffer);
				uBuffer.setLength(0);
			}
		}
		
	}		
	

  void addToList()
  {
  	/*
		String name = val.name;
		List<Zbnf_genContent> list = contentLists.get(name);  //from global lists
		if(list == null){
			list = new LinkedList<Zbnf_genContent>();
			contentLists.put(name, list);
		}
		list.add(val);
		*/
  }
	
	
	/**Builds a String with given parts from a given ZmakeString-format and user data.
	 * @param string The ZmakeString-format in the script to build the ANT-XML-file.
	 * @param userTarget The users Zmake-Target-data-
	 * @param input null or information about input-file if target per input.
	 * @return String
	 */
	private String buildString(ZmakeAntGenCtrl.ZmakeString string, ZmakeUserScript.UserTarget userTarget
		, ZmakeUserScript.UserFilepath input)
	{
		StringBuilder uBuffer = new StringBuilder(64);
		for(ZmakeAntGenCtrl.ZmakeStringElement element: string.elements){
			if(element.isVariable){
				if(element.text.startsWith("input.")){
					CharSequence part = getPartsFromFilepath(input, null, element.text.substring(6));
					uBuffer.append(part);
				} else if(element.text.startsWith("output.")){
					CharSequence part = getPartsFromFilepath(userTarget.output, null, element.text.substring(7));
					uBuffer.append(part);
				} else {
					ZmakeUserScript.UserVariable variable = userScript.allVariables.get(element.text);
					if(variable.string !=null){
			      stop();
					}
				}
			} else {
				uBuffer.append(element.text);
			}
		}
		return uBuffer.toString();
	}
	
	
	/**Gets parts from a file
	 * <ul>
	 * <li>absFile: The absolute file path: If a source path is given, it is used instead a base  path.
	 *   If a relative path is given, the {@link #sCurrDir}-content is added before the relative path.
	 * <li>localFile: The local file path with all parts. It is supplied as relative path.
	 *   It is used normally in composition with another maybe absolute directory path.   
	 * </li>
	 * @param file
	 * @param generalPath
	 * @param part
	 * @return
	 */
	public CharSequence getPartsFromFilepath(ZmakeUserScript.UserFilepath file
		, ZmakeUserScript.UserFilepath generalPath, String part)
	{ if(part.equals("name")){ return file.file; }
		else if(part.equals("file")){ return file.path + file.file+ file.ext; }
		else if(part.equals("absFile")){
			StringBuilder uRet = new StringBuilder();
			if(generalPath !=null){
				if(generalPath.drive !=null){ uRet.append(generalPath.drive); }
				if(!generalPath.absPath){ 
					uRet.append(sCurrDir); 
				}
				if(generalPath.pathbase !=null){ uRet.append(generalPath.pathbase).append('/'); }
				uRet.append(generalPath.path);  //ends with /
			} else {
				if(file.drive !=null){ uRet.append(file.drive); }
				if(!file.absPath){ uRet.append(sCurrDir); }
				if(file.pathbase !=null){ uRet.append(file.pathbase).append('/'); }
			}
			uRet.append(file.path);
			uRet.append(file.file);
			if(file.someFiles){ uRet.append('*'); }
			if(file.wildcardExt){ uRet.append(".*"); }
			uRet.append(file.ext);
			return uRet;
		}
		else if(part.equals("absDir")){
			StringBuilder uRet = new StringBuilder();
			if(file.drive !=null){ uRet.append(file.drive); }
			if(!file.absPath){ uRet.append(sCurrDir); }
			if(file.pathbase !=null){ uRet.append(file.pathbase).append('/'); }
			uRet.append(file.path);
			return uRet;
		}
		else if(part.equals("localPathName")){ return file.path + file.file; }
		else if(part.equals("localFile")){ return file.path + file.file + file.ext; }
		else if(part.equals("nameExt")){ return file.file + file.ext; }
		else if(part.equals("ext")){ return file.ext; }
		else return("fault-pathrequest(" + part + ")");
	}
	
	
  /**Get the content of a named variable.
   * 
   * @param userTarget The users data. Not used here.
   * @param name Name of the variable
   * @param contentData The local context where variable may be defined.
   * @return
   */
	private CharSequence getTextofVariable(ZmakeUserScript.UserTarget userTarget, String name, Gen_Content contentData)
	{ 
		CharSequence text = contentData.localVariables.get(name);
		if(text == null){ 
		  List<CharSequence> list = addToListTexts.get(name);
		  if(list !=null){
		  	StringBuilder uText = new StringBuilder();
		  	for(CharSequence text1: list){
		  		uText.append(text1);
		  	}
		  	text = uText;
		  } else {
		  	text = scriptVariables.get(name);
		  }
		}
		if(text == null)
			stop();
			//throw new IllegalArgumentException("unknown variable: " + name); 
		return text;
	}
	
	
  
	
	
	
	
	private boolean addFileset(String name, String srcpath, List<String> container)
	{ boolean bOk = true;
		//search the fileset:
		ZmakeUserScript.UserVariable variable = userScript.allVariables.get(name);
		if(variable.fileset == null){
			console.writeError("fileset not found; " + name);
			bOk = false;
		} else {
			final String srcPathFile;
			if(srcpath != null){ srcPathFile = srcpath;
			} else if(variable.fileset.srcpath != null){ 
				                   srcPathFile = variable.fileset.srcpath.pathbase; 
			} else {             srcPathFile = "sCurrDir";
			}
			for(ZmakeUserScript.UserFilepath file: variable.fileset.filesOfFileset){
				String sFilePath = srcPathFile + file.path + file.file + file.ext;
				container.add(sFilePath);
			}
			//container
		}
		return bOk;
	}
	
	
	
	void stop(){}
}
