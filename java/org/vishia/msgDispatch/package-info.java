/**
 * This package contains classes to organize handling with an message system.
 * The component srcJava_vishiaBase contains the interface for {@link LogMessage} and a simple
 * outputter only. Therefore it is possible to invoke message outputs in any library which depends
 * on the srcJava_vishiaBase component. The component srcJava_vishiaRun contains a message dispatcher
 * and especially a file outputter in the same package. That component supplies the runtime system
 * to handle with the messages. It is possible to use another message handling instead. That other
 * handling should implement the {@link LogMessage} - interface.   
 */
package org.vishia.msgDispatch;
