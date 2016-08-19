package engine;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * An interface for reading and selecting moves from a chess opening book. It should allow for using an alternative book once out of the main book.
 * 
 * @author Viktor
 *
 */
abstract class Book implements Closeable {

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
	
	protected Path path;
	protected SeekableByteChannel bookStream;
	protected Book secondaryBook;
	
	/**
	 * It instantiates a Book object on the opening book file specified by filePath; if the file cannot be accessed,
	 * an IOException is thrown.
	 * 
	 * @param filePath
	 * @throws IOException
	 */
	protected Book(String filePath) throws IOException {
		path = Paths.get(filePath);
		bookStream = Files.newByteChannel(path, StandardOpenOption.READ);
	}
	/**
	 * Returns the path to the main book file this object has been instantiated on.
	 * 
	 * @return
	 */
	public String getPrimaryFilePath() {
		return path.toAbsolutePath().toString();
	}
	/**
	 * Returns the path to the secondary book file.
	 * 
	 * @return
	 */
	public String getSecondaryFilePath() {
		return secondaryBook == null ? null : secondaryBook.getPrimaryFilePath();
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
	abstract Move getMove(Position p, SelectionModel selection);
	@Override
	public void close() throws IOException {
		bookStream.close();
		if (secondaryBook != null)
			secondaryBook.close();
	}
}