package engine;

import java.nio.channels.*;
import java.nio.file.*;
import java.io.*;

public class Book {
	
	private static Book DEFAULT_BOOK;
	private static Book ALTERNATIVE_BOOK;
	
	private final static String DEFAULT_FILE_PATH = "book.bin";
	
	boolean useDefaultBook;
	private String filePath;
	private ByteChannel bookStream;
	
	private Book(String filePath, boolean useDefaultBook) {
		try {
			bookStream = Files.newByteChannel(Paths.get(filePath), StandardOpenOption.READ);
			this.useDefaultBook = useDefaultBook;
			this.filePath = filePath;
		}
		catch (IOException e) {
			
		}
	}
	public Book getInstance() {
		if (DEFAULT_BOOK == null)
			DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH, false);
		return DEFAULT_BOOK;
	}
	public Book getInstance(String filePath, boolean useDefaultBook) {
		if (ALTERNATIVE_BOOK == null)
			ALTERNATIVE_BOOK = new Book(filePath, useDefaultBook);
		if (useDefaultBook && DEFAULT_BOOK == null)
			DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH, false);
		return ALTERNATIVE_BOOK;
	}
}
