package engine;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.*;
import java.nio.file.*;
import java.io.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import engine.Bitboard.Square;

/**
 * A class for reading and selecting moves from PolyGlot opening books. It allows for using an alternative book once out of the main book.
 * 
 * @author Viktor
 *
 */
class Book implements Closeable {
	
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
	public final static String DEFAULT_BOOK_FILE_PATH = "resources/default.bin";
	// Polyglot entry size in bytes: U64 hash + U16 move + U16 weight + U32 learning
	private final static byte ENTRY_SIZE = 8 + 2 + 2 + 4;
	
	private FileSystem fileSys;
	private SeekableByteChannel bookStream;
	private Book secondaryBook;
	private ZobristKeyGenerator gen;
	
	/**
	 * It instantiates a Book object on the opening book file specified by filePath; if the file cannot be accessed,
	 * an IOException is thrown.
	 * 
	 * @param filePath
	 * @throws IOException
	 */
	public Book(String filePath) throws IOException {
		Path realPath;
		String[] uriParts;
		Map<String, String> env;
		String uri;
		if (new File(filePath).isAbsolute())
			realPath = Paths.get(filePath);
		else {
			try {
				uri = ClassLoader.getSystemResource(filePath).toURI().toString();
				if (uri.startsWith("jar:file")) {
					uriParts = uri.split("!");
					env = new HashMap<>();
					fileSys = FileSystems.newFileSystem(URI.create(uriParts[0]), env);
					realPath = fileSys.getPath(uriParts[1]);
				}
				else
					realPath = Paths.get(ClassLoader.getSystemResource(filePath).toURI().getPath().substring(1));
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
		bookStream = Files.newByteChannel(realPath, StandardOpenOption.READ);
		gen = ZobristKeyGenerator.getInstance();
	}
	/**
	 * It instantiates a Book object on the default opening book; if the default book file cannot be accessed, an IOException is thrown.
	 * 
	 * @throws IOException
	 */
	public Book() throws IOException {
		this(DEFAULT_BOOK_FILE_PATH);
	}
	/**
	 * It instantiates a Book object on the opening book files specified by filePath and secondaryBookFilePath (as an alternative
	 * book for when out of the main book); if the main opening book file cannot be accessed, an IOException is thrown, if the secondary
	 * opening book file cannot be accessed, it is not set.
	 * 
	 * @param filePath
	 * @param secondaryBookFilePath
	 * @throws IOException
	 */
	public Book(String filePath, String secondaryBookFilePath) throws IOException {
		this(filePath);
		secondaryBook = new Book(secondaryBookFilePath);
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
				if (Thread.currentThread().isInterrupted())
					return entries;
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
						if (Thread.currentThread().isInterrupted())
							return entries;
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
						if (Thread.currentThread().isInterrupted())
							return entries;
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
	public void close() throws IOException {
		fileSys.close();
		bookStream.close();
		if (secondaryBook != null)
			secondaryBook.close();
	}
}
