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
import org.vishia.util.FileSystem;
import org.vishia.zmake.ZmakeGenScript.Zbnf_genContent;
import org.vishia.zmake.ZmakeUserScript.UserFilepath;

/**This class generates the output make file. */
public class ZmakeGenerator
{

  /**Changes
   * <ul>
   * <li>2012-05-25 Hartmut bugfix: {@link #getPartsFromFilepath(UserFilepath, UserFilepath, String)}: 
   *   'localFile': It returns now path/*.ext or path/*.* if the input is parsed 
   *   with 'someFiles' or 'wildCardExt'. It is important if '<:expandFiles>' is requested in the genScript.zmake.
   * <li>2011-08-13: Gen_Content#genContentForInputset(...) now regards < ?expandFiles>.
   *   If this tag is found in a < :forInput>...< .forInput> content generation prescript,
   *   then wildcards in the file name are expanded to the execution of the content prescript
   *   for all files which are found matching to the wildcard filename. The {@link FileSystem#addFilesWithBasePath(String, List)}
   *   is used to evaluate the files. The user scripts can be more shorten now if all files in a directory
   *   should be taken.
   * </ul>
   */
  public final static int version = 0x20110813;

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
				Map<String,ZmakeUserScript.UserFilepath> threadVariables = new TreeMap<String,ZmakeUserScript.UserFilepath>();
				threadVariables.put("output", inpTarget.output);
				genZmakeTarget.gen_Content(uBuffer, out, inpTarget, antZmakeTarget, threadVariables, null);
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
		 * @param out
		 * @param userTarget
		 * @param container
		 * @param threadVariable Variable which are valid in the processing thread of conversion.
		 * @param srcPathP
		 * @return
		 * @throws IOException
		 */
		boolean gen_Content(StringBuilder uBuffer
			, Writer out
			,	ZmakeUserScript.UserTarget userTarget
			, ZmakeGenScript.Zbnf_genContent container
			, Map<String,ZmakeUserScript.UserFilepath> threadVariable
			, ZmakeUserScript.UserFilepath srcPathP
			) 
		throws IOException
		{ boolean bOk = true;
			if(container.isContentForInput){
				for(ZmakeUserScript.UserInput inputIntern : userTarget.inputs){
					if(inputIntern.inputFile !=null){
						Gen_Content contentData = new Gen_Content(this);	
						Map<String,ZmakeUserScript.UserFilepath> forElementsNested = new TreeMap<String,ZmakeUserScript.UserFilepath>();
						if(threadVariable !=null){ forElementsNested.putAll(threadVariable); }
						forElementsNested.put("input", inputIntern.inputFile);
						contentData.gen_ContentWithScript(uBuffer, out, userTarget, container, forElementsNested
							, userTarget.srcpath, null);
					} else {
						//it is a input-set
						genContentForInputset(uBuffer, userTarget, container, inputIntern);
						stop();
					}
				}
			} else {
				Gen_Content contentData = new Gen_Content(this);	
				contentData.gen_ContentWithScript(uBuffer, out, userTarget, container, threadVariable, srcPathP, null);
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
			ZmakeUserScript.ScriptVariable inputsetVariable = userScript.allVariables.get(inputIntern.inputSet.name);
      final ZmakeUserScript.UserFilepath srcPath =
      	userTarget.srcpath !=null ? userTarget.srcpath
      	: inputsetVariable.fileset.srcpath;
			for(ZmakeUserScript.UserFilepath file: inputsetVariable.fileset.filesOfFileset){
      	Gen_Content contentData = new Gen_Content(this);	
      	Map<String,ZmakeUserScript.UserFilepath> forElementsNested = new TreeMap<String,ZmakeUserScript.UserFilepath>();
				//if(forElements !=null){ forElementsNested.putAll(forElements); }
				if(file.someFiles && genScript.expandFiles){ //input contains wildcards, it should be expanded:
				  List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
				  final String basePath = getPartsFromFilepath(file, null, "absBasePath").toString();
				  final String filePath1 = getPartsFromFilepath(file, null, "file").toString();
				  final String filePath = getPartsFromFilepath(file, null, "localFile").toString();
          final String sPathSearch = basePath + ":" + filePath;
				  FileSystem.addFilesWithBasePath(sPathSearch, listFiles);
          for(FileSystem.FileAndBasePath file1: listFiles){
            ZmakeUserScript.UserFilepath file2 = new ZmakeUserScript.UserFilepath();
            if(file1.basePath !=null ){
              int posEnd = file1.basePath.length() -1;  //last char is path separator, file2.pathbase without separator!
              if(file.absPath){
                file2.pathbase = file1.basePath.substring(0, posEnd);
              } else {
                //The file1.basePath contains the currDir, remove it.
                file2.pathbase = file1.basePath.substring(sCurrDir.length(), posEnd);
              }
            }
            int posName = file1.localPath.lastIndexOf('/') +1;  //if not found, set to 0
            int posExt = file1.localPath.lastIndexOf('.');
            final String sPath = file1.localPath.substring(0, posName);  //"" if only name
            final String sName;
            final String sExt;
            if(posExt < 0){ sExt = ""; sName = file1.localPath.substring(posName); }
            else { sExt = file1.localPath.substring(posExt); sName = file1.localPath.substring(posName, posExt); }
            file2.path = sPath;
            file2.file = sName;
            file2.ext = sExt;
            forElementsNested.put("input", file2);  //named input
            contentData.gen_ContentWithScript(uBuffer, null, userTarget, genScript, forElementsNested, srcPath, null);
          }
				} else {
      	  forElementsNested.put("input", file);
          contentData.gen_ContentWithScript(uBuffer, null, userTarget, genScript, forElementsNested, srcPath, null);
				}
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
		, Map<String,ZmakeUserScript.UserFilepath> forElements
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
				genVariable.gen_Content(uBufferVariable, null, userTarget, variableScript, forElements, srcPath);
				localVariables.put(variableScript.name, uBufferVariable);
			}
		
			//Generate the result for all output lists to fill and complete it.
			for(Zbnf_genContent listContainer: contentScript.addToList){
				StringBuilder uBufferLocal = new StringBuilder();
				Gen_Content contentData = new Gen_Content(this);	
			  contentData.gen_Content(uBufferLocal, out, userTarget, listContainer, forElements, srcPath); 
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
			  case 't': { 
			  	int posLine = 0;
			  	int posEnd;
			  	do{
			  		posEnd = contentElement.text.indexOf('\n', posLine);
			      if(posEnd >= 0){ 
			      	uBuffer.append(contentElement.text.substring(posLine, posEnd));		
				  		uBuffer.append("\r\n");
				  	  posLine = posEnd +1;  //after \n 
					  } else {
			  			uBuffer.append(contentElement.text.substring(posLine));		
					  }
			      
			    } while(posEnd >=0);  //output all lines.
			  } break;
			  case 'i': {
			  	ZmakeUserScript.UserFilepath input = forElements.get("input");  //all for elements, use the input
			  	CharSequence text = getPartsFromFilepath(input, srcPath, contentElement.text);
			  	uBuffer.append(text); 
			  } break;
			  case 'o': { 
			  	CharSequence text = getPartsFromFilepath(userTarget.output, null, contentElement.text);
			  	uBuffer.append(text); 
			  } break;
			  case 'e': {
			  	final CharSequence text;
		  		if(contentElement.text !=null && contentElement.text.equals("target")){
			  		//generates all targets, only advisable in the (?:file?)
			  		out.append(uBuffer);   //flush content before.
			  		uBuffer.setLength(0);  //fill new
			  		genUserTargets(out);
			    } else if(contentElement.name !=null) {
			  		ZmakeUserScript.UserFilepath file;
			  		if(forElements !=null && (file = forElements.get(contentElement.name)) !=null){
			  		  text = getPartsFromFilepath(file, null, contentElement.elementPart);
				  		uBuffer.append(text);
			  		} else {
				  	 	text = getTextofVariable(userTarget, contentElement.name, this);
					  	uBuffer.append(text); 
			  		}
		  		} else if(contentElement.text !=null){
			  	 	text = getTextofVariable(userTarget, contentElement.text, this);
				  	uBuffer.append(text); 
			  	} else {
			    	uBuffer.append(listElement);
			  	}
				} break;
			  case 'f': {
			  	if(contentElement.text.equals("nextNr")){
			  		String val = "" + nextNr.toString();
			  		uBuffer.append(nextNr);
			  	}
			 } break;
			  case 'v': {
			  	//TODO: delete it later
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
			  	//TODO delete later
			  	Gen_Content contentData = new Gen_Content(this);	
				  contentData.gen_Content(uBuffer, out, userTarget, contentElement.subContent, forElements, srcPath); 
			  } break;
			  case 'L': { //generation (\?:forList : <$?@name>\?) <genContent?> (\?/forList\?)
			  	String sListName = contentElement.subContent.name;
			  	List<CharSequence>  list = addToListTexts.get(sListName);
			  	if(list == null) {
			  		uBuffer.append("ERROR: list \"" + sListName + "\" not found :ERROR");
			  	} else {
			  		for(CharSequence listText: list){
			  			gen_ContentWithScript(uBuffer, out, userTarget, contentElement.subContent, forElements, null, listText);
			  		}
			  	}
			  } break;
			  case 'V': { //generation (?:for:<$?@name>?) <genContent?> (?/for?)
			  	String sVariable = contentElement.subContent.name;
			  	if(sVariable.equals("input")){ //generation (?:forInput?) <genContent?forInputContent> (\?/forInput\?) 
			  		Gen_Content contentData = new Gen_Content(this);	
					  contentData.gen_Content(uBuffer, out, userTarget, contentElement.subContent, forElements, srcPath); 
				  } else {
					  if(sVariable.equals("includePath"))
				  		stop();
				  		//first test whether a param of the target is requested:
				  	final ZmakeUserScript.UserParam param;
				  	final ZmakeUserScript.ScriptVariable scriptVariable;
				  	final String sRefName; //The name which is used in <*sRefName:element>
				  	if( userTarget.params != null 
				  		&& (param = userTarget.params.get(sVariable)) !=null
				  		) { //param with the given name is found:
				  		if(param.value !=null){
				  			//a simple string in parameter, use (?name?) instead!
				  			scriptVariable = null;
				  			sRefName = null;
				  		} else {
				        assert(param.variable !=null);
				        scriptVariable = userScript.allVariables.get(param.variable);
				        sRefName = param.name;
				        if(scriptVariable == null){
				        	throw new IllegalArgumentException("TODO not a scriptvariable: " + param.variable);
				        }
				  		}
				  	} else {
				  		scriptVariable = userScript.allVariables.get(sVariable);
				  		sRefName = sVariable;
				  	}
		        if(scriptVariable !=null){
			        if(scriptVariable.fileset !=null){
			        	for(UserFilepath filesetElement : scriptVariable.fileset.filesOfFileset){
			        		//generate the content in (?:for:variable?)...CONTENT...(?/for?)
			        		Gen_Content contentData = new Gen_Content(this);	
									Map<String,ZmakeUserScript.UserFilepath> forElementsNested = new TreeMap<String,ZmakeUserScript.UserFilepath>();
									if(forElements !=null){ forElementsNested.putAll(forElements); }
									forElementsNested.put(sRefName, filesetElement);  //select the elements of for with the name
									contentData.gen_ContentWithScript(uBuffer, out
			        			, userTarget   //access to all user script data.
			        			, contentElement.subContent //for ...CONTENT...
			        			, forElementsNested  //It contains the fileset-element in the users script.
			        			, null         //a srcPath of the target isn't valid here.
			        			, listElement  //can access it
			        			);
			        	}
			        }
		        } else {
		          stop();
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
          ZmakeUserScript.ScriptVariable variable = userScript.allVariables.get(element.text);
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
   * <li>file: The local file path with all parts. It is supplied as relative path.
   *   It is used normally in composition with another maybe absolute directory path.
   * <li>absFile: The absolute file path: If a source path is given, it is used instead a base  path.
   *   If a relative path is given, the {@link #sCurrDir}-content is added before the relative path.
   * <li>localFile: The local file path with all parts. It is supplied as relative path.
   *   It is used normally in composition with another maybe absolute directory path.   
   * <li>localPath: The local path only without the file. It is supplied as relative path.
   *   It is used normally in composition with another maybe absolute directory path.   
   * </li>
   * @param file
   * @param generalPath
   * @param part
   * @return
   */
  public CharSequence getPartsFromFilepath(ZmakeUserScript.UserFilepath file
    , ZmakeUserScript.UserFilepath generalPath, String part)
  { if(file.path.contains("inspectorAccessor"))
      stop();
  
    if(part.equals("file")){ 
      StringBuilder uRet = new StringBuilder();
      if(generalPath !=null){
        if(generalPath.drive !=null){ uRet.append(generalPath.drive); }
        if(generalPath.pathbase !=null && file.pathbase.length() >0){ uRet.append(generalPath.pathbase).append('/'); }
        uRet.append(generalPath.path);  //ends with /
      } else {
        if(file.drive !=null){ uRet.append(file.drive); }
        if(file.pathbase !=null && file.pathbase.length() >0){ uRet.append(file.pathbase).append('/'); }
      }
      uRet.append(file.path);
      uRet.append(file.file);
      if(file.someFiles){ uRet.append('*'); }
      if(file.wildcardExt){ uRet.append(".*"); }
      uRet.append(file.ext);
      return uRet;
    }
    else if(part.equals("absFile")){
      StringBuilder uRet = new StringBuilder();
      if(generalPath !=null){
        if(generalPath.drive !=null){ uRet.append(generalPath.drive); }
        if(!generalPath.absPath){ 
          uRet.append(sCurrDir); 
        }
        if(generalPath.pathbase !=null && file.pathbase.length() >0){ uRet.append(generalPath.pathbase).append('/'); }
        uRet.append(generalPath.path);  //ends with /
      } else {
        if(file.drive !=null){ uRet.append(file.drive); }
        if(!file.absPath){ uRet.append(sCurrDir); }
        if(file.pathbase !=null && file.pathbase.length() >0){ uRet.append(file.pathbase).append('/'); }
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
      if(file.pathbase !=null && file.pathbase.length() >0){ uRet.append(file.pathbase).append('/'); }
      uRet.append(file.path);
      return uRet;
    }
    else if(part.equals("localPathName")){ return file.path + file.file; }
    else if(part.equals("localFile")){ 
      StringBuilder uRet = new StringBuilder();
      uRet.append(file.path);
      uRet.append(file.file);
      if(file.someFiles){ uRet.append('*'); }
      if(file.wildcardExt){ uRet.append(".*"); }
      uRet.append(file.ext);
      return uRet;
    }
    else if(part.equals("localDir")){ return file.path; }
    else if(part.equals("localPath")){ return file.path; }  //deprecated
    else if(part.equals("name")){ return file.file; }
    else if(part.equals("nameExt")){ return file.file + file.ext; }
    else if(part.equals("ext")){ return file.ext; }
    else if(part.equals("basePath")){
      StringBuilder uRet = new StringBuilder();
      if(file.drive !=null){ uRet.append(file.drive).append(':'); }
      if(file.pathbase !=null){ uRet.append(file.pathbase); }
      return uRet;
    }
    else if(part.equals("absBasePath")){ 
      StringBuilder uRet = new StringBuilder();
      if(file.drive !=null){ uRet.append(file.drive); }
      if(!file.absPath){ uRet.append(sCurrDir); }
      if(file.pathbase !=null){ uRet.append(file.pathbase); }
      return uRet;
    }
    else return("...ERROR Zmake: fault-pathRequest(" + part + ")...");
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
    if(name.equals("compileOptions"))
      stop();
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
        text = scriptVariables.get(name);  //variables defined in the gen-script (not user script)
      }
    }
    if(text == null 
      && userTarget !=null     //prevent recursively call from getTextOfParam(...) 
      && userTarget.params !=null
      ){
      ZmakeUserScript.UserParam param = userTarget.params.get(name);
      if(param !=null){
        text = getTextOfParam(param, contentData);
      }
    }
    if(text == null){
      ZmakeUserScript.ScriptVariable variable = userScript.allVariables.get(name);
      if(variable !=null){
        if(variable.string !=null){
          text = variable.string.getText();
        }
      }
    }
    if(text == null)
      text="...ERROR: unknown:" + name + "...";
      stop();
      //throw new IllegalArgumentException("unknown variable: " + name); 
    return text;
  }
  
  
  
  
  
  
  private CharSequence getTextOfParam(ZmakeUserScript.UserParam param, Gen_Content contentData)
  {
    if(param.value !=null){
      return param.value;
    } else {
      String nameVariable = param.variable;
      //Note: prevent recursion: userTarget = null. Don't get content of a variable in usertarget, especially another param.
      return getTextofVariable(null, nameVariable, contentData);
    }
  }
  
  
  
  
  
  private boolean addFileset(String name, String srcpath, List<String> container)
  { boolean bOk = true;
    //search the fileset:
    ZmakeUserScript.ScriptVariable variable = userScript.allVariables.get(name);
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
  
  /**Small class instance to build a next number. 
   * Note: It is anonymous to encapsulate the current number value. 
   * The only one access method is Object.toString(). It returns a countered number.
   */
  private final Object nextNr = new Object(){
    int nr = 0;
    @Override
    public String toString(){
      return "" + ++nr;
    }
  };
  
  void stop(){}
}
