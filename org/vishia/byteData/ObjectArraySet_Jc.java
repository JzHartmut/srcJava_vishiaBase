package org.vishia.byteData;

import java.text.ParseException;
import java.util.Iterator;
import java.util.Vector;


public class ObjectArraySet_Jc extends ObjectArray_Jc
{
  
  private Vector<ObjectArray_Jc> objectArrays;

  public ObjectArraySet_Jc(ByteDataAccess parent, int idxChildInParent)
  { assignAtIndex(idxChildInParent, parent);
  }
    
  
    public ObjectArraySet_Jc()
    {
        objectArrays = new Vector<ObjectArray_Jc>();
        myReflection_Jc = null; //nur bei DataObject Arrays eingestellt
   }
     
      public Vector<ObjectArray_Jc> getObjectArrays()
      { return objectArrays;
      }

      
      public String report()
      { String sRet = "";
        Iterator<ObjectArray_Jc> i = objectArrays.iterator();
        while (i.hasNext())
        { 
          ObjectArray_Jc arr = (ObjectArray_Jc) i.next();
          String sName = "ObjectArray_Jc ownAdress: 0x" + Integer.toHexString(arr.getOwnAdress()) + " reflectionclass: 0x" + Integer.toHexString(arr.getReflectionClass())
                          + " length " + arr.getLength_ArrayJc() + " sizeEl " + arr.getSizeofElement();
          
          sRet += sName;
          sRet += '\n';
        }
        return sRet;      
      }
      
      public String toString(){ return report(); }
    
      
      /**Initializes the instance with the given image from a target system.
       * The imagedata were analyzed. The {@link objectArrays} were setted.
       * 
       * @param imageData The byte image of Reflection.
       * @throws ParseException If any abnormal data content is inside. 
       * @throws AccessException
       */
      public void setImageData(byte[] imageData)
      throws ParseException
      { 
        super.assignData(imageData, imageData.length);
        //  super.assignFromParentsChild();
         //counts the number of classes
          //Schleife über alle Elemente:
          /** Test the image. If any problem with the offset, an exception is thrown.
           * The idxChild after last child have to hit exactly the idxEnd.
           * NOTE: The offset is always positiv, because it is masked.
           */
        objectArrays.removeAllElements(); 
        //objectarrays einlesen
        
        while ( idxBegin < idxEnd )
        {
          if(getReflectionClass() != 0)//ObjectArray Class
          {
              ObjectArray_Jc newItem = new ObjectArray_Jc();
              objectArrays.add(newItem );
              newItem.assignData(data, data.length, idxBegin);
           }

              int lengthCurrentElement = specifyLengthElement();
              if (lengthCurrentElement == 0) 
                throw new ParseException("Position: " + idxBegin, idxBegin);
                 
              assignData(data, data.length, idxBegin + lengthCurrentElement);
         }
      } //setImageData(byte[])

  
  
  
  
}
