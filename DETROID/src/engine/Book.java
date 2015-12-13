package engine;

import util.*;

import java.nio.channels.*;
import java.nio.file.*;
import java.io.*;
import java.nio.*;

public class Book {
	
	private class Entry {
		
		short move;
		short weight;
		
		Entry(short move, short weight) {
			this.move = move;
			this.weight = weight;
		}
	}
	
	private final static byte ENTRY_SIZE = 8 + 2 + 2 + 4;
	private final static String DEFAULT_FILE_PATH = "book.bin";
	
	private static Book DEFAULT_BOOK;
	private static Book ALTERNATIVE_BOOK;
	
	private boolean useDefaultBook;
	private String filePath;
	private SeekableByteChannel bookStream;
	
	private Book(String filePath, boolean useDefaultBook) {
		try {
			bookStream = Files.newByteChannel(Paths.get(filePath), StandardOpenOption.READ);
			this.useDefaultBook = useDefaultBook;
			this.filePath = filePath;
		}
		catch (IOException e) {
			System.out.println("File not found: " + filePath);
		}
	}
	public Book getInstance() {
		if (ALTERNATIVE_BOOK != null)
			return ALTERNATIVE_BOOK;
		if (DEFAULT_BOOK == null)
			DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH, false);
		return DEFAULT_BOOK;
	}
	public Book getInstance(String filePath, boolean useDefaultBook)
		throws IllegalStateException {
		if (ALTERNATIVE_BOOK != null || DEFAULT_BOOK != null)
			throw new IllegalStateException();
		ALTERNATIVE_BOOK = new Book(filePath, useDefaultBook);
		if (useDefaultBook)
			DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH, false);
		return ALTERNATIVE_BOOK;
	}
	public List<Entry> relevantEntries(Position p) {
		int low, mid, hi;
		long readerPos, currKey, key = Zobrist.getPolyglotHashKey(p);
		List<Entry> entries = new Stack<>();
		ByteBuffer buff = ByteBuffer.allocateDirect(ENTRY_SIZE);
		low = 0;
		hi = bookStream.size();
		try {
			while (low < hi) {
				mid = (((hi + low)/2)/ENTRY_SIZE)*ENTRY_SIZE;
				bookStream.position(mid);
				bookStream.read(buff);
				currKey = buff.getLong();
				if (currKey == key) {
					readerPos = mid;
					do {
						entries.add(new Entry(buff.getShort(), buff.getShort()));
						bookStream.position((readerPos -= ENTRY_SIZE));
						bookStream.read(buff);
					}
					while (buff.getLong() == key);
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
				else {
					if (key >= 0 && currKey >= 0) {
						if (key > currKey)
							hi = mid;
						else
							low = mid;
					}
					else if (key < 0 && currKey < 0)
				}
			}
		}
		catch (IllegalArgumentException e) {
			
		}
	}
}
