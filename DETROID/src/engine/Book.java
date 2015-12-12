package engine;

import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.io.*;

public class Book {
	
	private static Book DEFAULT_BOOK;
	private static Book ALTERNATIVE_BOOK;
	
	private final static String DEFAULT_FILE_PATH = "book.bin";
	
	private String filePath;
	private ByteChannel bookStream;
	
	private Book(String filePath) {
		try {
			bookStream = Files.newByteChannel(new File(filePath).toPath(), READ);
			this.filePath = filePath;
		}
		catch (IOException e) {
			
		}
	}
	public Book getInstance() {
		if (DEFAULT_BOOK == null)
			DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH);
		return DEFAULT_BOOK;
	}
	public Book getInstance(String filePath, boolean useDefaultBook) {
		if (ALTERNATIVE_BOOK != null && !ALTERNATIVE_BOOK.filePath.equals(filePath)) {
			close(ALTERNATIVE_BOOK);
			ALTERNATIVE_BOOK = new Book(filePath);
		}
		if (useDefaultBook) {
			if (DEFAULT_BOOK == null)
				DEFAULT_BOOK = new Book(DEFAULT_FILE_PATH);
		}
		else
			close(DEFAULT_BOOK);
		return ALTERNATIVE_BOOK;
	}
	private static void close(Book b) {
		b = null;
	}
}
