package chess;

import util.List;
import util.Queue;

/**
 * A container class for search arguments.
 * 
 * @author Viktor
 *
 */
public class SearchArguments {

	private final List<Move> searchMoves;
	private final Boolean ponder;
	private final Long whiteTime;
	private final Long blackTime;
	private final Long whiteIncrement;
	private final Long blackIncrement;
	private final Integer movesToGo;
	private final Integer depth;
	private final Long nodes;
	private final Short mateDistance;
	private final Long searchTime;
	private final Boolean infinite;
	
	public SearchArguments(List<Move> searchMoves, Boolean ponder,
			Long whiteTime, Long blackTime, Long whiteIncrement,
			Long blackIncrement, Integer movesToGo, Integer depth, Long nodes,
			Short mateDistance, Long searchTime, Boolean infinite) {
		super();
		this.searchMoves = searchMoves;
		this.ponder = ponder;
		this.whiteTime = whiteTime;
		this.blackTime = blackTime;
		this.whiteIncrement = whiteIncrement;
		this.blackIncrement = blackIncrement;
		this.movesToGo = movesToGo;
		this.depth = depth;
		this.nodes = nodes;
		this.mateDistance = mateDistance;
		this.searchTime = searchTime;
		this.infinite = infinite;
	}
	public List<Move> getSearchMoves() {
		List<Move> list = new Queue<>();
		for (Move m : searchMoves) {
			list.add(m);
		}
		return list;
	}
	public Boolean getPonder() {
		return ponder;
	}
	public Long getWhiteTime() {
		return whiteTime;
	}
	public Long getBlackTime() {
		return blackTime;
	}
	public Long getWhiteIncrement() {
		return whiteIncrement;
	}
	public Long getBlackIncrement() {
		return blackIncrement;
	}
	public Integer getMovesToGo() {
		return movesToGo;
	}
	public Integer getDepth() {
		return depth;
	}
	public Long getNodes() {
		return nodes;
	}
	public Short getMateDistance() {
		return mateDistance;
	}
	public Long getSearchTime() {
		return searchTime;
	}
	public Boolean getInfinite() {
		return infinite;
	}
}
