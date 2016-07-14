package engine;

import java.text.ParseException;

/**
 * An exception for when a piece of chess notation text such as FEN, PGN, or SAN can not be parsed due to violations of the notation standards.
 * 
 * @author Viktor
 *
 */
class ChessParseException extends ParseException {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -2375327691687152573L;

	public ChessParseException() {
		this("");
	}
	public ChessParseException(String arg) {
		super(arg, -1);
	}
	public ChessParseException(String arg0, int arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}
	public ChessParseException(Exception e) {
		this(e.getMessage());
	}
}
