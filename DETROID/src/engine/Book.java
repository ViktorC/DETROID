package engine;

import java.nio.channels.*;
import java.nio.file.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.Random;

import engine.Board.Square;

/**A class for reading and selecting moves from PolyGlot opening books. It allows for the possibility of using an alternative book, and furthermore,
 * to use the default book once out of the alternative book.
 * 
 * @author Viktor
 *
 */
public class Book {
	
	/**A simple container class for Polyglot book entries. Only stores relevant information, i.e. move and weight.
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
	/**An enumeration type for mathematical models used in the process of selecting one from all the available opening moves for a position. RANDOM
	 * is actually pseudo-random.
	 * 
	 * @author Viktor
	 *
	 */
	public static enum SelectionModel {
		
		RANDOM,
		STOCHASTIC,
		DETERMINISTIC;
		
	}
	
	// U64 hash + U16 move + U16 weight + U32 learning
	private final static byte ENTRY_SIZE = 8 + 2 + 2 + 4;
	// Will be an own book.
	private final static String DEFAULT_FILE_PATH = "book.bin";
	
	private static Book DEFAULT_BOOK;
	private static Book ALTERNATIVE_BOOK;
	
	private boolean useDefaultBook;
	private SeekableByteChannel bookStream;
	
	private Book(String filePath, boolean useDefaultBook) {
		try {
			bookStream = Files.newByteChannel(Paths.get(filePath), StandardOpenOption.READ);
			this.useDefaultBook = useDefaultBook;
		}
		catch (IOException e) {
			System.out.println("File not found: " + filePath);
		}
	}
	/**Returns an instance already created by invoking either this or the {@link #getInstance(String, boolean) getInstance(String, boolean)} method.
	 * If there is none, it instantiates and returns the default book.
	 * 
	 * @return
	 */
	public Book getInstance() {
		if (ALTERNATIVE_BOOK != null)
			return ALTERNATIVE_BOOK;
		if (DEFAULT_BOOK == null)
			DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH, false);
		return DEFAULT_BOOK;
	}
	/**Instantiates an alternative book and returns it. Only one Book instance can exist; two if an alternative book has been created by the
	 * invocation of this method with useDefaultBook set to true which results in the instantiation of the default book of the engine as well.
	 * If this method or {@link #getInstance() getInstance} has already been called before, it throws an
	 * {@link #java.lang.IllegalStateExcetion IllegalStateExcetion}.
	 * 
	 * @param filePath The path to the opening book file.
	 * @param useDefaultBook Whether to instantiate and use the default book in case there is no move found in the specified book for a position.
	 * @return
	 * @throws IllegalStateException
	 */
	public Book getInstance(String filePath, boolean useDefaultBook) throws IllegalStateException {
		if (ALTERNATIVE_BOOK != null || DEFAULT_BOOK != null)
			throw new IllegalStateException();
		ALTERNATIVE_BOOK = new Book(filePath, useDefaultBook);
		if (useDefaultBook)
			DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH, false);
		return ALTERNATIVE_BOOK;
	}
	/**Returns a list of all the entries stored in the book whose Book instance it is called on. It uses a binary search algorithm to search through
	 * the entries.
	 * 
	 * @param p The position for which the entries are to be returned.
	 * @return
	 */
	private ArrayList<Entry> getRelevantEntries(Position p) {
		long low, mid, hi;
		long readerPos, currKey, key = Zobrist.getPolyglotHashKey(p);
		ArrayList<Entry> entries = new ArrayList<>();
		ByteBuffer buff = ByteBuffer.allocateDirect(ENTRY_SIZE);
		try {
			// A simple binary search on the position hash values.
			low = 0;
			hi = bookStream.size();
			while (low < hi) {
				mid = (((hi + low)/2)/ENTRY_SIZE)*ENTRY_SIZE;
				bookStream.position(mid);
				bookStream.read(buff);
				currKey = buff.getLong();
				/* If our reader head falls onto the right entry, run it in both directions until it encounters entries with a different hash code
				 * to collect all the relevant moves for p. */
				if (currKey == key) {
					readerPos = mid;
					do {
						entries.add(new Entry(buff.getShort(), buff.getShort()));
						bookStream.position((readerPos -= ENTRY_SIZE));
						bookStream.read(buff);
					}
					while (buff.getLong() == key && readerPos >= 0);
					readerPos = mid + ENTRY_SIZE;
					if (bookStream.read(buff) == -1) {
						while (buff.getLong() == key)  {
							entries.add(new Entry(buff.getShort(), buff.getShort()));
							bookStream.position((readerPos += ENTRY_SIZE));
							if (bookStream.read(buff) == -1) break;
						}
					}
					return entries;
				}
				/* If not, we compare p's hash code to the hash code of the entry the reader head fell on. We have to consider that PolyGlot books
				 * use unsigned 64 bit integers for hashing. */
				else {
					if ((currKey | Long.MIN_VALUE) > (key | Long.MIN_VALUE))
						low = mid;
					else
						hi = mid;
				}
			}
			/* No matching entries have been found; we are out of book. If this method is called on ALTERNATIVE_BOOK and its useDefaultBook is set
			 * to true, we search the DEFAULT_BOOK, too. */
			return useDefaultBook ? DEFAULT_BOOK.getRelevantEntries(p) : null;
		}
		// Some IO error has occured.
		catch (IOException e) {
			return null;
		}
	}
	/**Parses a short in which moves are encoded in PolyGlot books and creates a Pure Algebraic Coordinate Notation string from it.
	 * 
	 * @param polyglotMove
	 * @return
	 * @throws IllegalArgumentException
	 */
	private static String polyglotMoveToPACN(Position p, short polyglotMove) throws IllegalArgumentException {
		String toFile, toRank, fromFile, fromRank, promPiece, pacn;
		toFile = "" + (char)((polyglotMove & 7) + 'a');
		toRank = "" + (int)(((polyglotMove >>> 3) & 7) + 1);
		fromFile = "" + (char)(((polyglotMove >>> 6) & 7) + 'a');
		fromRank = "" + (int)(((polyglotMove >>> 9) & 7) + 1);
		pacn = fromFile + fromRank + toFile + toRank;
		if (pacn.equals("e1h1")) {
			if (p.whiteKing == Square.E1.bitmap)
				return "e1g1";
		}
		else if (pacn.equals("e1a1")) {
			if (p.whiteKing == Square.E1.bitmap)
				return "e1c1";
		}
		else if (pacn.equals("e8h8")) {
			if (p.blackKing == Square.E8.bitmap)
				return "e8g8";
		}
		else if (pacn.equals("e8a8")) {
			if (p.blackKing == Square.E8.bitmap)
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
	/**Picks and returns an opening move for the Position from all the relevant entries found in the PolyGlot book based on the specified
	 * mathematical model for selection. If there have been no relevant entries found, it returns null.
	 * 
	 * @param p The position for which an opening move is sought.
	 * @param selection An enumeration of type {@link #Book.SelectionModel SelectionModel} that specifies the mathematical model to be applied when
	 * selecting the move.
	 * @return An opening move.
	 */
	public Move getMove(Position p, SelectionModel selection) {
		short max;
		long totalWeight;
		double randomDouble, weightSum;
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
					weightSum += (ent.weight/totalWeight);
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
		return p.parsePACN(polyglotMoveToPACN(p, e.move));
	}
}
