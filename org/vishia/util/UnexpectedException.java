package org.vishia.util;

/**Use this exception type if an exception is not expected. Use it before you let the case empty!
 * But use this exception only for dedicated exceptions! For example:
 * <pre>
 * //this routine does nothing with a file because it is null. But the routine can throw an IOException, but only if the file is used.
 * //because the file is null, it will never throw it. Only this IOException is catched because the calling routine should not throw that.  
 * void exampleRoutine() throws AnyException{
 *   File myFile = null;
 *   try{ callAnything(parameters, myFile); } } catch(IOException){ throw new UnexpectedException(exc); }
 *   ...
 * }
 * </pre>
 * @author Hartmut Schorrig
 *
 */
public class UnexpectedException extends RuntimeException{

  private static final long serialVersionUID = -7395481201677194625L;

  public UnexpectedException(Exception exc){
    super("This exception is not expected. It seems to be a software failure. ", exc);
  }
}
