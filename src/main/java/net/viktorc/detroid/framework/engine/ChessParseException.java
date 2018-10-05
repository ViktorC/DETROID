package net.viktorc.detroid.framework.engine;

import java.text.ParseException;

/**
 * An exception for when a piece of chess notation text such as FEN, PGN, or SAN can not be parsed due to violations of the notation standards.
 * 
 * @author Viktor
 *
 */
public class ChessParseException extends ParseException {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -2375327691687152573L;

	/**
	 * @param desc A description of the error.
	 */
	public ChessParseException(String desc) {
		super(desc, -1);
	}
	/**
	 * @param e The underlying exception.
	 */
	public ChessParseException(Exception e) {
		this(e.getMessage());
	}
	
}
