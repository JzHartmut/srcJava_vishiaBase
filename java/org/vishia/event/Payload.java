package org.vishia.event;

public interface Payload {

  
  /**Common operation to clean the content. */
  Payload clean();
  
  /**Converts to dedicated bytes which presents the content.
   * The kind of serialization is programmed manually to regard specific conditions
   * and also deserialization in any other language, especially C for embedded control applications.
   * <br>
   * Note: You can also use Java-like serialization using also the {@link java.io.Serializable} interface.
   * This is another approach.
   * @return a dedicated byte stream presenting the content.
   */
  byte[] serialize();
  
  /**Opposite operation, reads the serialized content and restore the Java content.
   * @param data
   * @return false if there is any error (instead an exception).
   */
  boolean deserialize(byte[] data);
}
