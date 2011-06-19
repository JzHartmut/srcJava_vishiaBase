package org.vishia.mainCmd;

import java.io.FileNotFoundException;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.msgDispatch.LogMessage;

public class ReportWrapperLog implements Report
{

  final LogMessage log;
  
  
  
  public ReportWrapperLog(LogMessage log)
  { this.log = log;
  }

  @Override
  public void flushReport()
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public int getExitErrorLevel()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public LogMessage getLogMessageOutputConsole()
  {
    return log;
  }

  @Override
  public LogMessage getLogMessageOutputFile()
  {
    return log;
  }

  @Override
  public int getReportLevel()
  {
    return 0;
  }

  @Override
  public int getReportLevelFromIdent(int ident)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void openReportfile(String sFileReport, boolean bAppendReport)
    throws FileNotFoundException
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void report(int nLevel, String string)
  {
    log.sendMsg(nLevel, string);
    
  }

  @Override
  public void report(String sText, Exception exception)
  {
    log.sendMsg(0, sText + exception.getMessage());
    
  }

  @Override
  public void reportln(int nLevel, int nLeftMargin, String string)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void reportln(int nLevel, String string)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setExitErrorLevel(int level)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setReportLevelToIdent(int ident, int nLevelActive)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeError(String sError)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeError(String sError, Exception exception)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeInfo(String sInfo)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeInfoln(String sInfo)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeWarning(String sError)
  {
    // TODO Auto-generated method stub
    
  }
}
