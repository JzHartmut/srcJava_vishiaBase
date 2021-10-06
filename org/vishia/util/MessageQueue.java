package org.vishia.util;

import java.util.concurrent.ConcurrentLinkedQueue;


public class MessageQueue<EntryType>
{

  final ConcurrentLinkedQueue<EntryType> queue = new ConcurrentLinkedQueue<EntryType>();
  

  private char state = 'r'; //ready
  
  public void put(EntryType data){
    queue.offer(data);
    if(state != 'r'){         //should be checked, not ready.
      synchronized(this){
        if(state == 'w'){     //only notify if it is waiting.
          notify();
        }
      }
    }
  }
  
  
  public EntryType await(int timeout){
    EntryType data = queue.poll();
    if(data == null){
      synchronized(this){
        try {
          state = 'c';  //should be checked waiting. Notify expected.
          //it is possible that data are put in the last time. Test it! 
          data = queue.poll();
          if(data == null){
            state = 'w';     //notify necessery.
            this.wait(timeout);  //only wait if queue is emtpy.
            data = queue.poll();  //should not be null on notifying.
          }
          state = 'r';  //has data or timeout. The queue is ready.
        } catch (InterruptedException e) { }
      }
    }
    return data;  //returns null on timeout.
  }


  
  public static void main(String[] args){
    MessageQueue<String> qu = new MessageQueue<String>();
    
    class T2 implements Runnable{ 
      MessageQueue<String> qu;
      public void run(){
        qu.put("test data");
    } }
    
    T2 t2 = new T2();
    t2.qu = qu;
    (new Thread(t2, "t2")).start();  //start the t2. 
    String data = qu.await(1000000);
    System.out.print(data);
    
  }
      
  
  

}
