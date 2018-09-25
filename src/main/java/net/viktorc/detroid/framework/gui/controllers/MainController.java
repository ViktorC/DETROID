package net.viktorc.detroid.framework.gui.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.gui.dialogs.GameOverAlert;
import net.viktorc.detroid.framework.gui.dialogs.InfoAlert;
import net.viktorc.detroid.framework.gui.dialogs.OptionsAlert;
import net.viktorc.detroid.framework.gui.dialogs.ConsoleAlert;
import net.viktorc.detroid.framework.gui.dialogs.ErrorAlert;
import net.viktorc.detroid.framework.gui.dialogs.PromotionDialog;
import net.viktorc.detroid.framework.gui.dialogs.TimeSettingsDialog;
import net.viktorc.detroid.framework.gui.models.Piece;
import net.viktorc.detroid.framework.gui.models.SearchData;
import net.viktorc.detroid.framework.gui.models.TimeControl;
import net.viktorc.detroid.framework.uci.ScoreType;
import net.viktorc.detroid.framework.uci.SearchInformation;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;
import net.viktorc.detroid.framework.validation.GameState;

/**
 * The controller class for the main view of the chess application.
 * 
 * @author Viktor
 *
 */
public final class MainController implements AutoCloseable, Observer {
	
	// CSS style definitions for dynamically generated nodes.
	private static final String WHITE_SQR_STYLE_CLASS = "whiteSquare";
	private static final String BLACK_SQR_STYLE_CLASS = "blackSquare";
	private static final String SELECTED_SQR_STYLE_CLASS = "selectedSquare";
	private static final String PIECE_STYLE_CLASS = "piece";
	// The default time control.
	private static final long DEFAULT_TIME = 300000;
	// How frequently the timer's are updated. Only the user's time is actually measured with such a low accuracy.
	private static final long TIMER_RESOLUTION = 100;
	// The number of data points from which on, symbols and grid lines are not drawn on the chart.
	private static final int CHART_SYMBOL_LIMIT = 10;
	private static final int GRID_LINE_LIMIT = 50;
	/* The maximum absolute y-value a data point on the chart can have. This is the value mate scores are assigned. 
	 * It's 33 because search scores are 16-bit integers and they are always displayed in 'pawns' instead of 
	 * centipawns ((2^15-1)/100 = 32.767). */
	private static final int MAX_ABS_Y = 33;
	
	@FXML
	private MenuItem reset;
	@FXML
	private MenuItem pasteFen;
	@FXML
	private MenuItem pastePgn;
	@FXML
	private MenuItem copyFen;
	@FXML
	private MenuItem copyPgn;
	@FXML
	private MenuItem unmakeMove;
	@FXML
	private RadioMenuItem side;
	@FXML
	private MenuItem timeSettings;
	@FXML
	private RadioMenuItem demo;
	@FXML
	private RadioMenuItem ponder;
	@FXML
	private MenuItem options;
	@FXML
	private MenuItem debugConsole;
	@FXML
	private MenuItem about;
	@FXML
	private GridPane board;
	@FXML
	private TextArea moveHistory;
	@FXML
	private TextField wTime;
	@FXML
	private TextField bTime;
	@FXML
	private LineChart<String, Number> graph;
	@FXML
	private TableView<SearchData> searchStatView;
	
	private final Stage stage;
	private final Object updateLock;
	private final Map<Long,Boolean> updates;
	private ObservableList<Data<String, Number>> dataPoints;
	private ObservableList<SearchData> searchStats;
	private ExecutorService executor;
	private ControllerEngine controllerEngine;
	private UCIEngine searchEngine;
	private Timer timer;
	private TimerTask task;
	private TimeControl timeControl;
	private AtomicLong whiteTime;
	private AtomicLong blackTime;
	private ConcurrentLinkedDeque<Long> whiteTimes;
	private ConcurrentLinkedDeque<Long> blackTimes;
	private volatile boolean doTime;
	private volatile List<String> legalMoves;
	private volatile List<String> legalDestinations;
	private volatile Integer selectedNodeInd;
	private volatile String selectedSource;
	private volatile String ponderMove;
	private volatile boolean isUserWhite;
	private volatile boolean usersTurn;
	private volatile boolean gameOn;
	private volatile boolean isReset;
	private volatile boolean doPonder;
	private volatile boolean isPondering;
	private volatile boolean ponderHit;
	private volatile boolean isSearching;
	private volatile boolean isDemo;
	
	/**
	 * Constructs an instance using the specified engines for game control and searching.
	 * 
	 * @param stage The current stage.
	 * @param controllerEngine The engine that controls the game play by enforcing the rules and storing the 
	 * positions and game states.
	 * @param searchEngine The engine responsible for searching the chess positions.
	 */
	public MainController(Stage stage, ControllerEngine controllerEngine, UCIEngine searchEngine) {
		this.stage = stage;
		updateLock = new Object();
		updates = new HashMap<>();
		executor = Executors.newSingleThreadExecutor();
		this.controllerEngine = controllerEngine;
		this.searchEngine = searchEngine;
		legalDestinations = new ArrayList<>();
		timeControl = new TimeControl(DEFAULT_TIME, DEFAULT_TIME, 0, 0);
		timer = new Timer();
		task = new TimerTask() {
			
			@Override
			public void run() {
				if (!doTime)
					return;
				if (MainController.this.controllerEngine.isWhitesTurn()) {
					if (whiteTime.addAndGet(-TIMER_RESOLUTION) <= 0) {
						doTime = false;
						whiteTime.set(0);
						controllerEngine.whiteForfeit();
						isReset = true;
						searchEngine.stop();
						Platform.runLater(() -> {
							if (selectedSource != null) {
								board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
								selectedNodeInd = null;
								selectedSource = null;
							}
							setTimeField(true);
							Alert dialog = new GameOverAlert(stage, controllerEngine.getGameState(), true);
							dialog.showAndWait();
						});
					} else
						Platform.runLater(() -> setTimeField(true));
				} else {
					if (blackTime.addAndGet(-TIMER_RESOLUTION) <= 0) {
						doTime = false;
						blackTime.set(0);
						controllerEngine.blackForfeit();
						isReset = true;
						searchEngine.stop();
						Platform.runLater(() -> {
							if (selectedSource != null) {
								board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
								selectedNodeInd = null;
								selectedSource = null;
							}
							setTimeField(false);
							Alert dialog = new GameOverAlert(stage, controllerEngine.getGameState(), true);
							dialog.showAndWait();
						});
					} else
						Platform.runLater(() -> setTimeField(false));
				}
			}
		};
		whiteTime = new AtomicLong();
		blackTime = new AtomicLong();
		whiteTimes = new ConcurrentLinkedDeque<>();
		blackTimes = new ConcurrentLinkedDeque<>();
	}
	/**
	 * Converts a square index on the GUI board into a string in coordinate notation.
	 * 
	 * @param ind The index of the square on the GUI chess board grid.
	 * @return The name of the chess square in coordinate notation.
	 */
	private static String sqrIndToName(int ind) {
		int rank = ind/8;
		int file = ind%8;
		return "" + Character.toString((char) ('a' + file)) + (rank + 1);
	}
	/**
	 * Sets the timers on the GUI.
	 * 
	 * @param white Whether white's or black's timer should be set.
	 */
	private void setTimeField(boolean white) {
		if (white)
			wTime.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(whiteTime.get()),
					TimeUnit.MILLISECONDS.toSeconds(whiteTime.get()) -
					TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(whiteTime.get()))));
		else
			bTime.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(blackTime.get()),
					TimeUnit.MILLISECONDS.toSeconds(blackTime.get()) -
					TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(blackTime.get()))));
	}
	/**
	 * Places a piece of the specified type to the specified square on the board.
	 * 
	 * @param piece The type of the piece.
	 * @param row The row into which it should be inserted.
	 * @param column The column into which it should be inserted.
	 */
	private void addPiece(Piece piece, int row, int column) {
		int nodeInd = row*8 + column + 1; // The first child is not a square.
		int sqrInd = (7 - row)*8 + column;
		String sqr = sqrIndToName(sqrInd);
		Text chessPiece = new Text("" + piece.getCode());
		chessPiece.getStyleClass().add(PIECE_STYLE_CLASS);
		chessPiece.minWidth(Double.MAX_VALUE);
		chessPiece.minHeight(Double.MAX_VALUE);
		// Set up the on mouse clicked event handler for pieces.
		chessPiece.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				if (!usersTurn || isDemo || controllerEngine.getGameState() != GameState.IN_PROGRESS)
					return;
				if (!gameOn) {
					gameOn = true;
					doTime = true;
					timer.schedule(task, TIMER_RESOLUTION, TIMER_RESOLUTION);
				}
				List<Node> children = board.getChildren();
				if (isLegal(sqr)) {
					if (selectedNodeInd != null)
						children.get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					selectedSource = sqr;
					selectedNodeInd = nodeInd;
					children.get(nodeInd).getStyleClass().add(SELECTED_SQR_STYLE_CLASS);
				} else if (selectedSource != null && legalDestinations.contains(sqr)) {
					String move = selectedSource + "" + sqr;
					if (!controllerEngine.getLegalMoves().contains(move))
						move += resolvePromotion().getFENNote();
					children.get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					makeMove(move);
				}
			}
		});
		board.add(chessPiece, column, row);
		GridPane.setHalignment(chessPiece, HPos.CENTER);
		GridPane.setValignment(chessPiece, VPos.CENTER);
	}
	/**
	 * Sets up the GUI chess board based on the specified FEN string.
	 * 
	 * @param fen The board state in FEN.
	 */
	private void setBoard(String fen) {
		board.getChildren().removeAll(board.lookupAll("." + PIECE_STYLE_CLASS));
		String board = fen.split(" ")[0].trim();
		String[] ranks = board.split("/");
		for (int i = 0; i < 8; i++) {
			String rank = ranks[i];
			int fileInd = 0;
			for (char c : rank.toCharArray()) {
				Piece piece = Piece.getByFENNote(c);
				if (piece != null)
					addPiece(piece, i, fileInd++);
				else
					fileInd += Character.getNumericValue(c);
			}
		}
	}
	/**
	 * Sets the move history text area according to the move history kept by the controller engine.
	 */
	private void setMoveHistory() {
		String pgn = controllerEngine.toPGN();
		String moveList = pgn.substring(pgn.lastIndexOf(']') + 1).trim();
		moveList = moveList.replaceAll("\n", "");
		moveHistory.setText("");
		moveHistory.appendText(moveList);
	}
	/**
	 * Sets the axes of the chart, assigns a new series to it, creates a new list of data points and 
	 * assigns them to the new series. Simply clearing the data points list is not sufficient due to 
	 * a bug that sometimes results in a line connecting the first data point to the horizontal line 
	 * at y = 0.
	 */
	private void resetChart() {
		Series<String, Number> series = new Series<>();
		series.setName("W" + System.lineSeparator() + '\u25b2' + System.lineSeparator() +
				'\u25bc' + System.lineSeparator() + "B");
		dataPoints = FXCollections.observableArrayList();
		series.setData(dataPoints);
		ObservableList<Series<String, Number>> graphData = FXCollections.observableArrayList();
		graphData.add(series);
		graph.setData(graphData);
		NumberAxis yAxis = (NumberAxis) graph.getYAxis();
		yAxis.setLowerBound(-MAX_ABS_Y);
		yAxis.setUpperBound(MAX_ABS_Y);
		yAxis.setAutoRanging(true);
	}
	/**
	 * Returns whether the specified square is a legal origin square in the current position for the 
	 * side to move.
	 * 
	 * @param sqr The square in coordinate notation.
	 * @return Whether it is the origin of at least one legal move in the current position.
	 */
	private boolean isLegal(String sqr) {
		boolean legal = false;
		for (String m : legalMoves) {
			if (m.startsWith(sqr)) {
				if (legal == false)
					legalDestinations.clear();
				legal = true;
				legalDestinations.add(m.substring(2, 4));
			}
		}
		return legal;
	}
	/**
	 * Displays the promotion dialog and returns the selected piece.
	 * 
	 * @return The piece to promote to.
	 */
	private Piece resolvePromotion() {
		Dialog<Piece> dialog = new PromotionDialog(stage, controllerEngine.isWhitesTurn());
		return dialog.showAndWait().get();
	}
	/**
	 * Displays an error alert with the details.
	 * 
	 * @param move The illegal move.
	 */
	private void handleIllegalMove(String move) {
		Alert dialog = new ErrorAlert(stage, "Illegal engine move.",
				"The engine \"" + searchEngine.getName() + "\" proposed an illegal move (" + move + ").");
		dialog.showAndWait();
	}
	/**
	 * Prompts the search engine to start searching the current position.
	 */
	private synchronized void startSearch() {
		isReset = false;
		isSearching = true;
		unmakeMove.setDisable(true);
		if (!gameOn) {
			gameOn = true;
			timer.schedule(task, TIMER_RESOLUTION, TIMER_RESOLUTION);
		}
		searchStats.clear();
		executor.submit(() -> {
			searchEngine.getSearchInfo().addObserver(this);
			long timeLeft = controllerEngine.isWhitesTurn() ? whiteTime.get() : blackTime.get();
			long start = System.currentTimeMillis();
			SearchResults res = searchEngine.search(null, null, whiteTime.get(), blackTime.get(),
					timeControl.getWhiteInc(), timeControl.getBlackInc(), null, null, null,
					null, null, null);
			timeLeft -= (System.currentTimeMillis() - start);
			synchronized (updateLock) {
				try {
					while (updates.values().contains(Boolean.FALSE))
						updateLock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			updates.clear();
			searchEngine.getSearchInfo().deleteObserver(this);
			isSearching = false;
			if (isReset)
				return;
			if (controllerEngine.isWhitesTurn())
				whiteTime.set(timeLeft);
			else
				blackTime.set(timeLeft);
			String bestMove = res.getBestMove();
			ponderMove = res.getSuggestedPonderMove().isPresent() ? res.getSuggestedPonderMove().get() : null;
			Double adjustedScore;
			if (res.getScore().isPresent()) {
				double score = res.getScore().get();
				ScoreType type = res.getScoreType().get();
				if (controllerEngine.isWhitesTurn())
					score = type == ScoreType.MATE ? score > 0 ? MAX_ABS_Y : -MAX_ABS_Y : score/100;
				else
					score = type == ScoreType.MATE ? score > 0 ? -MAX_ABS_Y : MAX_ABS_Y : -score/100;
				adjustedScore = Math.min(MAX_ABS_Y, Math.max(-MAX_ABS_Y, score));
			} else
				adjustedScore = null;
			boolean isLegal = controllerEngine.getLegalMoves().contains(bestMove);
			Platform.runLater(() -> {
				if (isLegal) {
					if (adjustedScore != null) {
						dataPoints.add(new Data<>(String.valueOf(controllerEngine.getMoveHistory().size()), adjustedScore));
						graph.setCreateSymbols(dataPoints.size() < CHART_SYMBOL_LIMIT);
						graph.setVerticalGridLinesVisible(dataPoints.size() < GRID_LINE_LIMIT);
					}
					makeMove(bestMove);
				}
				else
					handleIllegalMove(bestMove);
			});
		});
	}
	/**
	 * Prompts the search engine to start pondering on the position resulting from making the ponder move. 
	 * If there is no saved ponder move from the previous search, it immediately returns.
	 */
	private synchronized void startPondering() {
		if (ponderMove == null)
			return;
		ponderHit = false;
		isReset = false;
		isPondering = true;
		searchStats.clear();
		executor.submit(() -> {
			searchEngine.setPosition(controllerEngine.getStartPosition());
			for (String m : controllerEngine.getMoveHistory())
				searchEngine.play(m);
			searchEngine.play(ponderMove);
			long timeLeft;
			long timeSpent;
			if (controllerEngine.isWhitesTurn()) {
				timeLeft = blackTime.get();
				timeSpent = whiteTimes.peek();
			} else {
				timeLeft = whiteTime.get();
				timeSpent = blackTimes.peek();
			}
			searchEngine.getSearchInfo().addObserver(this);
			long start = System.currentTimeMillis();
			SearchResults res = searchEngine.search(null, true, whiteTime.get(), blackTime.get(),
					timeControl.getWhiteInc(), timeControl.getBlackInc(), null, null, null,
					null, null, null);
			long end = System.currentTimeMillis();
			synchronized (updateLock) {
				try {
					while (updates.values().contains(Boolean.FALSE))
						updateLock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			updates.clear();
			searchEngine.getSearchInfo().deleteObserver(this);
			isPondering = false;
			if (isReset) {
				synchronized (MainController.this) {
					MainController.this.notifyAll();
				}
				return;
			}
			// In case pondering is not allowed due to UCI settings or the search terminates early (near a mate).
			if (res == null || !ponderHit)
				return;
			doTime = false;
			timeSpent = end - start - (timeSpent - (controllerEngine.isWhitesTurn() ?
					blackTime.get() - timeControl.getBlackInc() :
					whiteTime.get() - timeControl.getWhiteInc()));
			timeLeft -= timeSpent;
			if (controllerEngine.isWhitesTurn())
				whiteTime.set(timeLeft);
			else
				blackTime.set(timeLeft);
			String bestMove = res.getBestMove();
			ponderMove = res.getSuggestedPonderMove().isPresent() ? res.getSuggestedPonderMove().get() : null;
			Double adjustedScore;
			if (res.getScore().isPresent()) {
				double score = res.getScore().get();
				ScoreType type = res.getScoreType().get();
				if (controllerEngine.isWhitesTurn())
					score = type == ScoreType.MATE ? score > 0 ? MAX_ABS_Y : -MAX_ABS_Y : score/100;
				else
					score = type == ScoreType.MATE ? score > 0 ? -MAX_ABS_Y : MAX_ABS_Y : -score/100;
				adjustedScore = Math.min(MAX_ABS_Y, Math.max(-MAX_ABS_Y, score));
			} else
				adjustedScore = null;
			boolean isLegal = controllerEngine.getLegalMoves().contains(bestMove);
			Platform.runLater(() -> {
				if (isLegal) {
					if (adjustedScore != null) {
						dataPoints.add(new Data<>(String.valueOf(controllerEngine.getMoveHistory().size()), adjustedScore));
						graph.setCreateSymbols(dataPoints.size() < CHART_SYMBOL_LIMIT);
						graph.setVerticalGridLinesVisible(dataPoints.size() < GRID_LINE_LIMIT);
					}
					makeMove(bestMove);
				} else
					handleIllegalMove(bestMove);
			});
		});
	}
	/**
	 * Prompts the controller engine to make the specified move, it updates the position of the search move if there 
	 * was no ponder hit, and it sets up the board based on the position stored by controller engine.
	 * 
	 * @param move The move to make.
	 */
	private synchronized void makeMove(String move) {
		ponderHit = false;
		doTime = false;
		if (controllerEngine.isWhitesTurn()) {
			whiteTime.addAndGet(timeControl.getWhiteInc());
			whiteTimes.addFirst(whiteTime.get());
			setTimeField(true);
		} else {
			blackTime.addAndGet(timeControl.getBlackInc());
			blackTimes.addFirst(blackTime.get());
			setTimeField(false);
		}
		selectedNodeInd = null;
		selectedSource = null;
		controllerEngine.play(move);
		setMoveHistory();
		setBoard(controllerEngine.toFEN());
		legalMoves = controllerEngine.getLegalMoves();
		// Handle pondering.
		if (usersTurn && !isDemo && doPonder && isPondering) {
			if (move.equals(ponderMove)) {
				ponderHit = true;
				searchEngine.ponderHit();
			} else {
				isReset = true;
				searchEngine.stop();
				try {
					while (isPondering)
						wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		usersTurn = !usersTurn;
		List<String> moveHistList = null;
		if (!ponderHit) {
			searchEngine.setPosition(controllerEngine.getStartPosition());
			moveHistList = controllerEngine.getMoveHistory();
			for (String m : moveHistList)
				searchEngine.play(m);
		}
		if (controllerEngine.getGameState() != GameState.IN_PROGRESS) {
			isReset = true;
			searchEngine.stop();
			(new GameOverAlert(stage, controllerEngine.getGameState(), false)).showAndWait();
		} else if (whiteTime.get() <= 0) {
			isReset = true;
			searchEngine.stop();
			whiteTime.set(0);
			controllerEngine.whiteForfeit();
			setTimeField(true);
			(new GameOverAlert(stage, controllerEngine.getGameState(), true)).showAndWait();
		} else if (blackTime.get() <= 0) {
			isReset = true;
			searchEngine.stop();
			blackTime.set(0);
			controllerEngine.blackForfeit();
			setTimeField(false);
			(new GameOverAlert(stage, controllerEngine.getGameState(), true)).showAndWait();
		} else {
			doTime = true;
			if (isDemo) {
				startSearch();
				return;
			} else if (!usersTurn) {
				if (!doPonder || !ponderHit)
					startSearch();
			} else if (doPonder)
				startPondering();
		}
		if (usersTurn && !isDemo && moveHistList != null && moveHistList.size() > 1)
			unmakeMove.setDisable(false);
	}
	/**
	 * Sets up the engines and the GUI based on the specified position.
	 * 
	 * @param position The position string.
	 * @param isPgn Whether the string is in PGN or FEN.
	 */
	private synchronized void setPosition(String position, boolean isPgn) {
		doTime = false;
		whiteTimes.clear();
		blackTimes.clear();
		whiteTime.set(timeControl.getWhiteTime());
		blackTime.set(timeControl.getBlackTime());
		whiteTimes.addFirst(whiteTime.get());
		blackTimes.addFirst(blackTime.get());
		controllerEngine.newGame();
		searchEngine.newGame();
		if (isPgn) {
			if (!controllerEngine.setGame(position))
				throw new IllegalArgumentException();
			searchEngine.setPosition(controllerEngine.getStartPosition());
			List<String> moveHistList = controllerEngine.getMoveHistory();
			for (String m : moveHistList)
				searchEngine.play(m);
			unmakeMove.setDisable(isUserWhite != controllerEngine.isWhitesTurn() || isDemo || moveHistList.size() < 2);
		} else {
			if (!controllerEngine.setPosition(position))
				throw new IllegalArgumentException();
			searchEngine.setPosition(position);
			unmakeMove.setDisable(true);
			controllerEngine.setPlayers(isUserWhite ? "Player" : searchEngine.getName(),
					isUserWhite ? searchEngine.getName() : "Player");
			controllerEngine.setEvent("DETROID Chess GUI Game");
		}
		usersTurn = isUserWhite == controllerEngine.isWhitesTurn();
		legalMoves = controllerEngine.getLegalMoves();
		if (selectedSource != null) {
			board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
			selectedNodeInd = null;
			selectedSource = null;
		}
		setBoard(controllerEngine.toFEN());
		setMoveHistory();
		setTimeField(true);
		setTimeField(false);
		resetChart();
		searchStats.clear();
		if (controllerEngine.getGameState() != GameState.IN_PROGRESS) {
			Alert dialog = new GameOverAlert(stage, controllerEngine.getGameState(), false);
			dialog.showAndWait();
			return;
		}
		doTime = true;
		if (!usersTurn || isDemo)
			startSearch();
		else if (doPonder)
			startPondering();
	}
	@FXML
	public void initialize() {
		// Reset menu item handler: resets the time controls and the game to the starting position.
		reset.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				isReset = true;
				searchEngine.stop();
				if (isPondering) {
					while (isPondering) {
						synchronized (MainController.this) {
							try {
								MainController.this.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
					}
				}
				setPosition("startpos", false);
			}
		});
		// Paste FEN from Clipboard menu item handler: sets up the position according to the FEN string in the clipboard.
		pasteFen.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				isReset = true;
				searchEngine.stop();
				if (isPondering) {
					while (isPondering) {
						synchronized (MainController.this) {
							try {
								MainController.this.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
					}
				}
				Clipboard clipboard = Clipboard.getSystemClipboard();
				String fen = clipboard.getString();
				try {
					setPosition(fen, false);
				} catch (Exception e) {
					Alert dialog = new ErrorAlert(stage, "FEN parsing error.",
							"The clipboard contents were not a valid FEN string.");
					dialog.show();
				}
			}
		});
		// Paste PGN from Clipboard menu item handler: sets up the position according to the PGN string in the clipboard.
		pastePgn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				isReset = true;
				searchEngine.stop();
				if (isPondering) {
					while (isPondering) {
						synchronized (MainController.this) {
							try {
								MainController.this.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
					}
				}
				Clipboard clipboard = Clipboard.getSystemClipboard();
				String pgn = clipboard.getString();
				try {
					setPosition(pgn, true);
				} catch (Exception e) {
					Alert dialog = new ErrorAlert(stage, "PGN parsing error.",
							"The clipboard contents were not a valid PGN string.");
					dialog.show();
				}
			}
		});
		// Copy FEN to Clipboard menu item handler: Copies the current position description in FEN to the clipboard.
		copyFen.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
				content.put(DataFormat.PLAIN_TEXT, controllerEngine.toFEN());
				clipboard.setContent(content);
			}
		});
		// Copy PGN to Clipboard menu item handler: Copies the current position description in PGN to the clipboard.
		copyPgn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
				content.put(DataFormat.PLAIN_TEXT, controllerEngine.toPGN());
				clipboard.setContent(content);
			}
		});
		// Unmake Move menu item handler: unmakes a whole move (only allowed if it is the user's turn).
		unmakeMove.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				doTime = false;
				controllerEngine.unplayLastMove();
				controllerEngine.unplayLastMove();
				if (isPondering) {
					isReset = true;
					searchEngine.stop();
					while (isPondering) {
						synchronized (MainController.this) {
							try {
								MainController.this.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
					}
				}
				searchEngine.newGame();
				searchEngine.setPosition(controllerEngine.getStartPosition());
				List<String> moveHistList = controllerEngine.getMoveHistory();
				for (String m : moveHistList)
					searchEngine.play(m);
				if (whiteTimes.size() >= 2)
					whiteTimes.poll();
				whiteTime.set(whiteTimes.peek());
				if (blackTimes.size() >= 2)
					blackTimes.poll();
				blackTime.set(blackTimes.peek());
				legalMoves = controllerEngine.getLegalMoves();
				if (selectedSource != null) {
					board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					selectedNodeInd = null;
					selectedSource = null;
				}
				unmakeMove.setDisable(moveHistList.size() < 2);
				if (!gameOn) {
					gameOn = true;
					timer.schedule(task, TIMER_RESOLUTION, TIMER_RESOLUTION);
				}
				setBoard(controllerEngine.toFEN());
				setMoveHistory();
				setTimeField(true);
				setTimeField(false);
				searchStats.clear();
				int numOfMoves = controllerEngine.getMoveHistory().size();
				List<Data<String, Number>> pointsToRemove = new ArrayList<>();
				for (Data<String, Number> point : dataPoints) {
					int x = Integer.parseInt(point.getXValue());
					if (x >= numOfMoves)
						pointsToRemove.add(point);
				}
				dataPoints.removeAll(pointsToRemove);
				graph.setCreateSymbols(dataPoints.size() < CHART_SYMBOL_LIMIT);
				graph.setVerticalGridLinesVisible(dataPoints.size() < GRID_LINE_LIMIT);
				doTime = true;
			}
		});
		// Play as White radio menu item handler: if it is selected the user plays as white, if not, as black.
		side.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				doTime = false;
				isReset = true;
				searchEngine.stop();
				if (isPondering) {
					while (isPondering) {
						synchronized (MainController.this) {
							try {
								MainController.this.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
					}
				}
				searchEngine.setPosition(controllerEngine.getStartPosition());
				for (String m : controllerEngine.getMoveHistory())
					searchEngine.play(m);
				if (selectedSource != null) {
					board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					selectedNodeInd = null;
					selectedSource = null;
				}
				if (controllerEngine.isWhitesTurn()) {
					if (whiteTimes.size() > 0) {
						whiteTime.set(whiteTimes.peek());
						setTimeField(true);
					}
				} else {
					if (blackTimes.size() > 0) {
						blackTime.set(blackTimes.peek());
						setTimeField(false);
					}
				}
				searchStats.clear();
				isUserWhite = !isUserWhite;
				usersTurn = controllerEngine.isWhitesTurn() == isUserWhite;
				unmakeMove.setDisable(!usersTurn || isDemo || controllerEngine.getMoveHistory().size() < 2);
				if (controllerEngine.getGameState() == GameState.IN_PROGRESS) {
					doTime = true;
					if (!usersTurn || isDemo)
						startSearch();
				}
			}
		});
		// Time Control Settings menu item handler: opens a dialog for setting the time control.
		timeSettings.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				doTime = false;
				isReset = true;
				searchEngine.stop();
				if (selectedSource != null) {
					board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					selectedNodeInd = null;
					selectedSource = null;
				}
				if (!usersTurn || isDemo) {
					if (controllerEngine.isWhitesTurn()) {
						if (whiteTimes.size() > 0) {
							whiteTime.set(whiteTimes.peek());
							setTimeField(true);
						}
					} else {
						if (blackTimes.size() > 0) {
							blackTime.set(blackTimes.peek());
							setTimeField(false);
						}
					}
				}
				searchStats.clear();
				Dialog<TimeControl> timeSettingsDialog = new TimeSettingsDialog(stage, timeControl);
				Optional<TimeControl> res = timeSettingsDialog.showAndWait();
				if (res.isPresent())
					timeControl = res.get();
				if (controllerEngine.getGameState() == GameState.IN_PROGRESS) {
					doTime = true;
					if (!usersTurn || isDemo)
						startSearch();
				}
			}
		});
		// Demo radio menu item handler: if selected the engine keeps playing against itself.
		demo.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				isDemo = demo.selectedProperty().get();
				if (isDemo) {
					if (usersTurn && controllerEngine.getGameState() == GameState.IN_PROGRESS) {
						doTime = false;
						isReset = true;
						searchEngine.stop();
						if (isPondering) {
							while (isPondering) {
								synchronized (MainController.this) {
									try {
										MainController.this.wait();
									} catch (InterruptedException e) {
										Thread.currentThread().interrupt();
										return;
									}
								}
							}
						}
						searchEngine.setPosition(controllerEngine.getStartPosition());
						for (String m : controllerEngine.getMoveHistory())
							searchEngine.play(m);
						if (selectedSource != null) {
							board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
							selectedNodeInd = null;
							selectedSource = null;
						}
						if (controllerEngine.isWhitesTurn()) {
							if (whiteTimes.size() > 0) {
								whiteTime.set(whiteTimes.peek());
								setTimeField(true);
							}
						} else {
							if (blackTimes.size() > 0) {
								blackTime.set(blackTimes.peek());
								setTimeField(false);
							}
						}
						searchStats.clear();
						doTime = true;
						startSearch();
					}
					ponder.setDisable(true);
					side.setDisable(true);
					unmakeMove.setDisable(true);
				} else {
					if (usersTurn) {
						isReset = true;
						searchEngine.stop();
						if (controllerEngine.isWhitesTurn()) {
							if (whiteTimes.size() > 0) {
								whiteTime.set(whiteTimes.peek());
								setTimeField(true);
							}
						} else {
							if (blackTimes.size() > 0) {
								blackTime.set(blackTimes.peek());
								setTimeField(false);
							}
						}
					}
					ponder.setDisable(false);
					side.setDisable(false);
					unmakeMove.setDisable(!usersTurn || controllerEngine.getMoveHistory().size() < 2);
				}
			}
		});
		// Ponder radio menu item handler: if selected the engine ponders while it is the user's turn.
		ponder.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				synchronized (MainController.this) {
					doPonder = ponder.selectedProperty().get();
					if (usersTurn) {
						if (doPonder)
							startPondering();
						else {
							ponderMove = null;
							isReset = true;
							searchEngine.stop();
						}
					}
				}
			}
		});
		/* Options menu item handler: opens a dialog that presents the UCI options of the search engine in the form of 
		 * interactive controls. */
		options.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				doTime = false;
				isReset = true;
				searchEngine.stop();
				if (selectedSource != null) {
					board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					selectedNodeInd = null;
					selectedSource = null;
				}
				if (!usersTurn || isDemo) {
					if (controllerEngine.isWhitesTurn()) {
						if (whiteTimes.size() > 0) {
							whiteTime.set(whiteTimes.peek());
							setTimeField(true);
						}
					} else {
						if (blackTimes.size() > 0) {
							blackTime.set(blackTimes.peek());
							setTimeField(false);
						}
					}
				}
				searchStats.clear();
				Alert optionsDialog = new OptionsAlert(stage, searchEngine);
				optionsDialog.showAndWait();
				if (controllerEngine.getGameState() == GameState.IN_PROGRESS) {
					doTime = true;
					if (!usersTurn || isDemo)
						startSearch();
				}
			}
		});
		/* Debug Console menu item handler: opens a non-modal dialog that contains a text area to which the search engine's 
		 * debug output is printed. */
		debugConsole.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				Alert console = new ConsoleAlert(stage, searchEngine.getDebugInfo());
				console.resultProperty().addListener((o, oldVal, newVal) -> {
					searchEngine.setDebugMode(false);
					debugConsole.setDisable(false);
				});
				console.setOnCloseRequest(new EventHandler<DialogEvent>() {
					
					@Override
					public void handle(DialogEvent event) {
						searchEngine.setDebugMode(false);
						debugConsole.setDisable(false);
					}
				});
				searchEngine.setDebugMode(true);
				debugConsole.setDisable(true);
				console.show();
			}
		});
		// About menu item handler: opens a simple dialog displaying the name of the search engine and its author.
		about.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				Alert info = new InfoAlert(stage, "About the chess search engine.", "Name: " + searchEngine.getName() +
						System.lineSeparator() + "Author: " + searchEngine.getAuthor());
				info.resultProperty().addListener((o, oldVal, newVal) -> {
					about.setDisable(false);
				});
				info.setOnCloseRequest(new EventHandler<DialogEvent>() {
					
					@Override
					public void handle(DialogEvent event) {
						about.setDisable(false);
					}
				});
				info.initModality(Modality.NONE);
				about.setDisable(true);
				info.show();
			}
		});
		// Set up the board and the squares with event handlers.
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Node n = new StackPane();
				String sqr = sqrIndToName((7 - i)*8 + j);
				boolean white = i%2 == 0 ? j%2 == 0 : j%2 == 1;
				n.getStyleClass().add(white ? WHITE_SQR_STYLE_CLASS : BLACK_SQR_STYLE_CLASS);
				n.setOnMouseClicked(new EventHandler<Event>() {

					@Override
					public void handle(Event event) {
						if (!usersTurn || isDemo || controllerEngine.getGameState() != GameState.IN_PROGRESS)
							return;
						if (selectedSource != null && legalDestinations.contains(sqr)) {
							String move = selectedSource + "" + sqr;
							if (!controllerEngine.getLegalMoves().contains(move))
								move += resolvePromotion().getFENNote();
							board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
							makeMove(move);
						}
					}
				});
				board.add(n, j, i);
			}
		}
		/* Make sure the text in the move history text area is wrapped and that it scrolls to the bottom as new 
		 * lines are added. */
		moveHistory.setWrapText(true);
		moveHistory.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				moveHistory.setScrollTop(Double.MAX_VALUE);
			}
		});
		// Set up the chart.
		resetChart();
		// Set up the search stats.
		searchStats = FXCollections.observableArrayList();
		searchStatView.setItems(searchStats);
		// Set up the initial position.
		isUserWhite = true;
		setPosition("startpos", false);
	}
	@Override
	public synchronized void update(Observable o, Object arg) {
		if (isReset || (!isPondering && !isSearching))
			return;
		long updated = System.nanoTime();
		updates.put(updated, Boolean.FALSE);
		SearchInformation info = (SearchInformation) o;
		String score;
		switch (info.getScoreType()) {
		case EXACT:
			score = String.format("%.2f", (double) info.getScore()/100);
			break;
		case LOWER_BOUND:
			score = String.format("%.2f+", (double) info.getScore()/100);
			break;
		case UPPER_BOUND:
			score = String.format("%.2f-", (double) info.getScore()/100);
			break;
		case MATE:
			score = info.getScore() < 0 ? "-M" + Math.abs(info.getScore()) : "M" + info.getScore();
			break;
		default:
			return;
		}
		SearchData stats = new SearchData(info.getDepth() + "/" + info.getSelectiveDepth(), String.format("%.2f", (double) info.getTime()/1000),
				"" + info.getNodes(), "" + info.getCurrentMoveNumber(), String.join(" ", info.getPv()), score, "" + (info.getTime() == 0 ?
				0 : info.getNodes()/info.getTime()), String.format("%.2f", (double) searchEngine.getHashLoadPermill()/1000));
		Platform.runLater(() -> {
			searchStats.add(stats);
			searchStatView.scrollTo(searchStats.size() - 1);
			updates.put(updated, Boolean.TRUE);
			synchronized (updateLock) {
				updateLock.notifyAll();
			}
		});
	}
	@Override
	public void close() throws Exception {
		searchEngine.stop();
		timer.cancel();
		executor.shutdown();
	}
	
}