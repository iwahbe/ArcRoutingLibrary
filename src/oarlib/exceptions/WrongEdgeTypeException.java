package oarlib.exceptions;

/**
 * Exception that should be thrown whenever a routine is called (usually for a Mixed Graph) that expects
 * a different type of edge.
 * 
 * @author oliverlum
 *
 */
public class WrongEdgeTypeException extends Exception{
	public WrongEdgeTypeException() { super(); }
	public WrongEdgeTypeException(String message) { super(message); }
	public WrongEdgeTypeException(String message, Throwable cause) {super(message,cause);}
	public WrongEdgeTypeException(Throwable cause){super(cause);}
}