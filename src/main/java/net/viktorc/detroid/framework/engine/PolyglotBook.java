package net.viktorc.detroid.framework.engine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import net.viktorc.detroid.framework.engine.Bitboard.Square;

/**
 * An implementation of the Book interface for PolyGlot opening books.
 *
 * @author Viktor
 */
public class PolyglotBook extends OpeningBook {

  // Polyglot entry size in bytes: U64 hash + U16 move + U16 weight + U32 learning
  private static final byte ENTRY_SIZE = 8 + 2 + 2 + 4;

  private ZobristKeyGenerator gen;

  /**
   * It instantiates a Book object on the opening book file specified by filePath; if the file cannot be accessed, an IOException is
   * thrown.
   *
   * @param filePath The path to the book.
   * @throws Exception If the book cannot be accessed.
   */
  public PolyglotBook(String filePath) throws Exception {
    super(filePath);
    gen = ZobristKeyGenerator.getInstance();
  }

  /**
   * It instantiates a PolyglotBook object on the opening book files specified by filePath and secondaryBookFilePath (as an alternative book
   * for when out of the main book); if the main opening book file cannot be accessed, an IOException is thrown, if the secondary opening
   * book file cannot be accessed, it is not set.
   *
   * @param filePath The file path to the primary book.
   * @param secondaryBookFilePath The file path to the secondary book.
   * @throws IOException If the books cannot be read.
   * @throws URISyntaxException If the file paths are illegal.
   */
  public PolyglotBook(String filePath, String secondaryBookFilePath) throws Exception {
    this(filePath);
    if (secondaryBookFilePath != null) {
      secondaryBook = new PolyglotBook(secondaryBookFilePath);
    }
  }

  private ArrayList<Entry> getRelevantEntries(Position pos) {
    long low, mid, hi, temp = -1;
    long readerPos, currKey, key = gen.generatePolyglotHashKey(pos);
    ArrayList<Entry> entries = new ArrayList<>();
    ByteBuffer buff = ByteBuffer.allocateDirect(ENTRY_SIZE);
    try {
      // A simple binary search on the position hash values.
      low = 0;
      hi = bookStream.size();
      while ((mid = (((hi + low) / 2) / ENTRY_SIZE) * ENTRY_SIZE) != temp) {
        if (Thread.currentThread().isInterrupted()) {
          return entries;
        }
        bookStream.position(mid);
        bookStream.read(buff);
        buff.clear();
        currKey = buff.getLong();
        /* If our reader head falls onto the right entry, run it in both directions until it encounters entries
         * with a different hash code to collect all the relevant moves for p. */
        if (currKey == key) {
          readerPos = mid;
          do {
            entries.add(new Entry(buff.getShort(), buff.getShort()));
            bookStream.position((readerPos -= ENTRY_SIZE));
            buff.clear();
            bookStream.read(buff);
            buff.clear();
            if (Thread.currentThread().isInterrupted()) {
              return entries;
            }
          }
          while (buff.getLong() == key && readerPos >= 0);
          readerPos = mid + ENTRY_SIZE;
          bookStream.position((readerPos));
          buff.clear();
          while (bookStream.read(buff) != -1) {
            buff.clear();
            if (buff.getLong() != key) {
              break;
            }
            entries.add(new Entry(buff.getShort(), buff.getShort()));
            bookStream.position((readerPos += ENTRY_SIZE));
            buff.clear();
            if (Thread.currentThread().isInterrupted()) {
              return entries;
            }
          }
          return entries;
        }
        /* If not, we compare p's hash code to the hash code of the entry the reader head fell on. We have to
         * consider that PolyGlot books use unsigned 64 bitboard integers for hashing. */
        else {
          if ((currKey + Long.MIN_VALUE) > (key + Long.MIN_VALUE)) {
            hi = mid;
          } else {
            low = mid;
          }
        }
        buff.clear();
        temp = mid;
      }
      /* No matching entries have been found; we are out of book. If this method is called on ALTERNATIVE_BOOK
       * and its useDefaultBook is set to true, we search the DEFAULT_BOOK, too. */
      return secondaryBook != null ? ((PolyglotBook) secondaryBook).getRelevantEntries(pos) : null;
    }
    // Some IO error has occured.
    catch (IOException e) {
      return null;
    }
  }

  private static String polyglotMoveToPACN(Position pos, short polyglotMove) throws IllegalArgumentException {
    String toFile = "" + (char) ((polyglotMove & 7) + 'a');
    String toRank = "" + (((polyglotMove >>> 3) & 7) + 1);
    String fromFile = "" + (char) (((polyglotMove >>> 6) & 7) + 'a');
    String fromRank = "" + (((polyglotMove >>> 9) & 7) + 1);
    String pacn = fromFile + fromRank + toFile + toRank;
    if (pacn.equals("e1h1")) {
      if (pos.getWhiteKing() == Square.E1.bitboard) {
        return "e1g1";
      }
    } else if (pacn.equals("e1a1")) {
      if (pos.getWhiteKing() == Square.E1.bitboard) {
        return "e1c1";
      }
    } else if (pacn.equals("e8h8")) {
      if (pos.getBlackKing() == Square.E8.bitboard) {
        return "e8g8";
      }
    } else if (pacn.equals("e8a8")) {
      if (pos.getBlackKing() == Square.E8.bitboard) {
        return "e8c8";
      }
    }
    String promPiece;
    switch (polyglotMove >>> 12) {
      case 0:
        promPiece = "";
        break;
      case 1:
        promPiece = "=n";
        break;
      case 2:
        promPiece = "=b";
        break;
      case 3:
        promPiece = "=r";
        break;
      case 4:
        promPiece = "=q";
        break;
      default:
        throw new IllegalArgumentException();
    }
    return pacn + promPiece;
  }

  @Override
  public Move getMove(Position pos, SelectionModel selection) throws Exception {
    short max;
    double totalWeight, randomDouble, weightSum;
    Entry e;
    ArrayList<Entry> relEntries = getRelevantEntries(pos);
    if (relEntries == null) {
      return null;
    }
    switch (selection) {
      case RANDOM: {
        Random rand = new Random(System.currentTimeMillis());
        e = relEntries.get(rand.nextInt(relEntries.size()));
      }
      break;
      case STOCHASTIC: {
        totalWeight = 0;
        for (Entry ent : relEntries) {
          totalWeight += ent.weight;
        }
        Random rand = new Random(System.currentTimeMillis());
        randomDouble = rand.nextDouble();
        weightSum = 0;
        e = null;
        totalWeight = Math.max(Double.MIN_VALUE, totalWeight);
        for (Entry ent : relEntries) {
          weightSum += ((double) ent.weight) / totalWeight;
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
      default:
        return null;
    }
    return e == null ? null : MoveStringUtils.parsePACN(pos, polyglotMoveToPACN(pos, e.move));
  }

  /**
   * A simple container class for Polyglot book entries. Only stores relevant information, i.e. move and weight.
   *
   * @author Viktor
   */
  private class Entry {

    final short move;
    final short weight;

    Entry(short move, short weight) {
      this.move = move;
      this.weight = weight;
    }
  }

}
