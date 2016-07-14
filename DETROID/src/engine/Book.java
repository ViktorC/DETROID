package engine;

import java.nio.channels.*;
import java.nio.file.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.Random;

import engine.Bitboard.Square;

/**
 * A class for reading and selecting moves from PolyGlot opening books. It allows for using an alternative book once out of the main book.
 * 
 * @author Viktor
 *
 */
class Book implements AutoCloseable {
	
	/**
	 * A simple container class for Polyglot book entries. Only stores relevant information, i.e. move and weight.
	 * 
	 * @author Viktor
	 *
	 */
	private class Entry {
		
		short move;
		short weight;
		
		Entry(short move, short weight) {
			this.move = move;
			this.weight = weight;
		}
	}
	/**
	 * An enumeration type for mathematical models used in the process of selecting one from all the available opening moves for a position.
	 * RANDOM is actually pseudo-random.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum SelectionModel {
		
		RANDOM,
		STOCHASTIC,
		DETERMINISTIC;
		
	}
	
	/**
	 * An own opening book compiled using SCID 4.62, PGN-Extract 17-21 and Polyglot 1.4w. Located in the root folder of the project by the name
	 * "default.bin".
	 */
	public final static String DEFAULT_BOOK_FILE_PATH = "default.bin";
	// Polyglot entry size in bytes: U64 hash + U16 move + U16 weight + U32 learning
	private final static byte ENTRY_SIZE = 8 + 2 + 2 + 4;
	
	private SeekableByteChannel bookStream;
	private Book secondaryBook;
	private ZobristKeyGenerator gen;
	
	private Book(String filePath) throws IOException {
		gen = ZobristKeyGenerator.getInstance();
		bookStream = Files.newByteChannel(Paths.get(filePath), StandardOpenOption.READ);
	}
	/**
	 * It instantiates and returns a Book object on the default opening book or null if the default book file can not be accessed.
	 * 
	 * @return
	 */
	public static Book getInstance() {
		try {
			return new Book(DEFAULT_BOOK_FILE_PATH);
		}
		catch (IOException e) {
			System.out.println("Default book file missing: " + DEFAULT_BOOK_FILE_PATH);
			return null;
		}
	}
	/**
	 * It instantiates and returns a Book object on the opening book file specified by filePath or null if the file can not be accessed.
	 * 
	 * @param filePath
	 * @return
	 */
	public static Book getInstance(String filePath) {
		try {
			return new Book(filePath);
		}
		catch (IOException e) {
			System.out.println("File not found: " + filePath);
			return null;
		}
	}
	/**
	 * It instantiates and returns a Book object on the opening book files specified by filePath and secondaryBookFilePath (as an alternative
	 * book for when out of the main book) or null if any of the files can not be accessed.
	 * 
	 * @param filePath
	 * @return
	 */
	public static Book getInstance(String filePath, String secondaryBookFilePath) {
		Book book;
		try {
			book = new Book(filePath);
		}
		catch (IOException e1) {
			System.out.println("File not found: " + filePath);
			return null;
		}
		try {
			book.secondaryBook = new Book(secondaryBookFilePath);
		}
		catch (IOException e2) {
			System.out.println("File not found: " + secondaryBookFilePath);
			try {
				book.bookStream.close();
			} catch (IOException e3) { }
			return null;
		}
		return book;
	}
	/**
	 * Tries and sets/changes the main book to the file specified by the path.
	 * 
	 * @param filePath
	 * @return Whether the book could be successfully set to the file.
	 */
	public boolean setMainBookPath(String filePath) {
		if (bookStream != null) {
			try {
				bookStream.close();
			} catch (IOException e1) {
				System.out.println("Could not close stream: " + bookStream.toString());
			}
		}
		try {
			bookStream = Files.newByteChannel(Paths.get(filePath), StandardOpenOption.READ);
			return true;
		}
		catch (IOException e2) {
			System.out.println("Could not change to file path: " + filePath);
			return false;
		}
	}
	/**
	 * Tries and sets/changes the secondary book to the file specified by the path.
	 * 
	 * @param filePath
	 * @return Whether the book could be successfully set to the file.
	 */
	public boolean setSecondaryBookPath(String filePath) {
		if (secondaryBook == null) {
			try {
				secondaryBook = new Book(filePath);
			} catch (IOException e) {
				System.out.println("File not found: " + filePath);
				return false;
			}
		}
		return secondaryBook.setMainBookPath(filePath);
	}
	/**
	 * Returns a list of all the entries stored in the book whose Book instance it is called on. It uses a binary search algorithm to search through
	 * the entries.
	 * 
	 * @param p The position for which the entries are to be returned.
	 * @return
	 * @throws NullPointerException If the book stream is not initialized.
	 */
	private ArrayList<Entry> getRelevantEntries(Position p) throws NullPointerException {
		long low, mid, hi, temp = -1;
		long readerPos, currKey, key = gen.getPolyglotHashKey(p);
		ArrayList<Entry> entries = new ArrayList<>();
		ByteBuffer buff = ByteBuffer.allocateDirect(ENTRY_SIZE);
		try {
			// A simple binary search on the position hash values.
			low = 0;
			hi = bookStream.size();
			while ((mid = (((hi + low)/2)/ENTRY_SIZE)*ENTRY_SIZE) != temp) {
				bookStream.position(mid);
				bookStream.read(buff);
				buff.clear();
				currKey = buff.getLong();
				/* If our reader head falls onto the right entry, run it in both directions until it encounters entries with a different hash code
				 * to collect all the relevant moves for p. */
				if (currKey == key) {
					readerPos = mid;
					do {
						entries.add(new Entry(buff.getShort(), buff.getShort()));
						bookStream.position((readerPos -= ENTRY_SIZE));
						buff.clear();
						bookStream.read(buff);
						buff.clear();
					}
					while (buff.getLong() == key && readerPos >= 0);
					readerPos = mid + ENTRY_SIZE;
					bookStream.position((readerPos));
					buff.clear();
					while (bookStream.read(buff) != -1) {
						buff.clear();
						if (buff.getLong() != key) break;
						entries.add(new Entry(buff.getShort(), buff.getShort()));
						bookStream.position((readerPos += ENTRY_SIZE));
						buff.clear();
					}
					return entries;
				}
				/* If not, we compare p's hash code to the hash code of the entry the reader head fell on. We have to consider that PolyGlot books
				 * use unsigned 64 bit integers for hashing. */
				else {
					if ((currKey + Long.MIN_VALUE) > (key + Long.MIN_VALUE))
						hi = mid;
					else
						low = mid;
				}
				buff.clear();
				temp = mid;
			}
			/* No matching entries have been found; we are out of book. If this method is called on ALTERNATIVE_BOOK and its useDefaultBook is set
			 * to true, we search the DEFAULT_BOOK, too. */
			return secondaryBook != null ? secondaryBook.getRelevantEntries(p) : null;
		}
		// Some IO error has occured.
		catch (IOException e) {
			return null;
		}
	}
	/**
	 * Parses a short in which moves are encoded in PolyGlot books and creates a Pure Algebraic Coordinate Notation string from it.
	 * 
	 * @param polyglotMove
	 * @return
	 * @throws IllegalArgumentException
	 */
	private static String polyglotMoveToPACN(Position pos, short polyglotMove) throws IllegalArgumentException {
		String toFile, toRank, fromFile, fromRank, promPiece, pacn;
		toFile = "" + (char)((polyglotMove & 7) + 'a');
		toRank = "" + (int)(((polyglotMove >>> 3) & 7) + 1);
		fromFile = "" + (char)(((polyglotMove >>> 6) & 7) + 'a');
		fromRank = "" + (int)(((polyglotMove >>> 9) & 7) + 1);
		pacn = fromFile + fromRank + toFile + toRank;
		if (pacn.equals("e1h1")) {
			if (pos.whiteKing == Square.E1.bit)
				return "e1g1";
		}
		else if (pacn.equals("e1a1")) {
			if (pos.whiteKing == Square.E1.bit)
				return "e1c1";
		}
		else if (pacn.equals("e8h8")) {
			if (pos.blackKing == Square.E8.bit)
				return "e8g8";
		}
		else if (pacn.equals("e8a8")) {
			if (pos.blackKing == Square.E8.bit)
				return "e8c8";
		}
		switch (polyglotMove >>> 12) {
			case 0: promPiece = "";
			break;
			case 1: promPiece = "=n";
			break;
			case 2: promPiece = "=b";
			break;
			case 3: promPiece = "=r";
			break;
			case 4: promPiece = "=q";
			break;
			default: throw new IllegalArgumentException();
		}
		return pacn + promPiece;
	}
	/**
	 * Picks and returns an opening move for the Position from all the relevant entries found in the PolyGlot book based on the specified
	 * mathematical model for selection. If there have been no relevant entries found, it returns null.
	 * 
	 * @param p The position for which an opening move is sought.
	 * @param selection An enumeration of type {@link #Book.SelectionModel SelectionModel} that specifies the mathematical model to be applied when
	 * selecting the move.
	 * @return An opening move.
	 */
	public Move getMove(Position p, SelectionModel selection) {
		short max;
		double totalWeight, randomDouble, weightSum;
		Entry e;
		ArrayList<Entry> relEntries = getRelevantEntries(p);
		if (relEntries == null)
			return null;
		switch (selection) {
			case RANDOM: {
				Random rand = new Random(System.currentTimeMillis());
				e = relEntries.get(rand.nextInt(relEntries.size()));
			}
			break;
			case STOCHASTIC: {
				totalWeight = 0;
				for (Entry ent : relEntries)
					totalWeight += ent.weight;
				Random rand = new Random(System.currentTimeMillis());
				randomDouble = rand.nextDouble();
				weightSum = 0;
				e = null;
				for (Entry ent : relEntries) {
					weightSum += ((double)ent.weight/totalWeight);
					if (weightSum >= randomDouble) {
						e = ent;
						break;
					}
				}
			}
			break;
			case DETERMINISTIC: {
				max = -1;
				e = null;
				for (Entry ent : relEntries) {
					if (ent.weight > max) {
						max = ent.weight;
						e = ent;
					}
				}
			}
			break;
			default: return null;
		}
		try {
			return p.parsePACN(polyglotMoveToPACN(p, e.move));
		} catch (ChessParseException | NullPointerException | IllegalArgumentException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			return null;
		}
	}
	@Override
	public void close() throws Exception {
		bookStream.close();
		if (secondaryBook != null)
			secondaryBook.close();
	}
}
