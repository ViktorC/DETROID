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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
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
import net.viktorc.detroid.framework.control.ControllerEngine;
import net.viktorc.detroid.framework.control.GameState;
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
import net.viktorc.detroid.framework.uci.SearchInformation;
import net.viktorc.detroid.framework.uci.SearchResults;
import net.viktorc.detroid.framework.uci.UCIEngine;

public final class MainController implements AutoCloseable, Observer {
	
	private static final String WHITE_SQR_STYLE_CLASS = "whiteSquare";
	private static final String BLACK_SQR_STYLE_CLASS = "blackSquare";
	private static final String SELECTED_SQR_STYLE_CLASS = "selectedSquare";
	private static final String PIECE_STYLE_CLASS = "piece";
	private static final long DEFAULT_TIME = 300000;
	private static final long TIMER_RESOLUTION = 100;
	
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
	private TableView<SearchData> searchStatView;
	
	private final Stage stage;
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
	
	public MainController(Stage stage, ControllerEngine controllerEngine, UCIEngine searchEngine) {
		this.stage = stage;
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
							dialog.show();
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
							dialog.show();
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
	private static String sqrIndToName(int ind) {
		int rank = ind/8;
		int file = ind%8;
		return "" + Character.toString((char) ('a' + file)) + (rank + 1);
	}
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
	private void addPiece(Piece piece, int row, int column) {
		int nodeInd = row*8 + column + 1; // The first child is not a square.
		int sqrInd = (7 - row)*8 + column;
		String sqr = sqrIndToName(sqrInd);
		Text chessPiece = new Text("" + piece.getCode());
		chessPiece.getStyleClass().add(PIECE_STYLE_CLASS);
		chessPiece.minWidth(Double.MAX_VALUE);
		chessPiece.minHeight(Double.MAX_VALUE);
		chessPiece.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				if (!usersTurn || controllerEngine.getGameState() != GameState.IN_PROGRESS)
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
	private void setMoveHistory() {
		String pgn = controllerEngine.toPGN();
		String moveList = pgn.substring(pgn.lastIndexOf(']') + 1).trim();
		moveList = moveList.replaceAll("\n", "");
		moveHistory.setText(moveList);
	}
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
	private Piece resolvePromotion() {
		Dialog<Piece> dialog = new PromotionDialog(stage, controllerEngine.isWhitesTurn());
		return dialog.showAndWait().get();
	}
	private void handleIllegalMove(String move) {
		Alert dialog = new ErrorAlert(stage, "Illegal engine move.",
				"The engine \"" + searchEngine.getName() + "\" proposed an illegal move (" + move + ").");
		dialog.showAndWait();
	}
	private synchronized void startSearch() {
		isReset = false;
		isSearching = true;
		searchEngine.getSearchInfo().addObserver(this);
		searchStats.clear();
		unmakeMove.setDisable(true);
		if (!gameOn) {
			gameOn = true;
			timer.schedule(task, TIMER_RESOLUTION, TIMER_RESOLUTION);
		}
		executor.submit(() -> {
			long timeLeft = controllerEngine.isWhitesTurn() ? whiteTime.get() : blackTime.get();
			long start = System.currentTimeMillis();
			SearchResults res = searchEngine.search(null, null, whiteTime.get(), blackTime.get(), timeControl.getWhiteInc(),
					timeControl.getBlackInc(), null, null, null, null, null, null);
			isSearching = false;
			searchEngine.getSearchInfo().deleteObserver(this);
			if (isReset)
				return;
			doTime = false;
			ponderMove = res.getSuggestedPonderMove();
			String bestMove = res.getBestMove();
			timeLeft -= (System.currentTimeMillis() - start);
			if (controllerEngine.isWhitesTurn())
				whiteTime.set(timeLeft);
			else
				blackTime.set(timeLeft);
			boolean isLegal = controllerEngine.getLegalMoves().contains(bestMove);
			Platform.runLater(() -> {
				if (isLegal)
					makeMove(bestMove);
				else
					handleIllegalMove(bestMove);
			});
		});
	}
	private synchronized void startPondering() {
		if (ponderMove == null)
			return;
		ponderHit = false;
		isReset = false;
		isPondering = true;
		searchEngine.getSearchInfo().addObserver(this);
		searchStats.clear();
		executor.submit(() -> {
			searchEngine.play(ponderMove);
			long timeLeft;
			long timeSpent;
			if (controllerEngine.isWhitesTurn()) {
				timeLeft = blackTime.get();
				timeSpent = whiteTime.get();
			} else {
				timeLeft = whiteTime.get();
				timeSpent = blackTime.get();
			}
			long start = System.currentTimeMillis();
			SearchResults res = searchEngine.search(null, true, whiteTime.get(), blackTime.get(), timeControl.getWhiteInc(),
					timeControl.getBlackInc(), null, null, null, null, null, null);
			searchEngine.getSearchInfo().deleteObserver(this);
			isPondering = false;
			if (isReset) {
				synchronized (MainController.this) {
					MainController.this.notify();
				}
				return;
			}
			// In case pondering is not allowed due to UCI settings or the search terminates early (near a mate).
			if (res == null || !ponderHit)
				return;
			doTime = false;
			long end = System.currentTimeMillis();
			timeSpent = end - start - (timeSpent - (controllerEngine.isWhitesTurn() ? blackTime.get() : whiteTime.get()));
			ponderMove = res.getSuggestedPonderMove();
			String bestMove = res.getBestMove();
			timeLeft -= timeSpent;
			if (controllerEngine.isWhitesTurn())
				whiteTime.set(timeLeft);
			else
				blackTime.set(timeLeft);
			boolean isLegal = controllerEngine.getLegalMoves().contains(bestMove);
			Platform.runLater(() -> {
				if (isLegal)
					makeMove(bestMove);
				else
					handleIllegalMove(bestMove);
			});
		});
	}
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
		if (usersTurn) {
			// Handle pondering.
			if (doPonder && isPondering) {
				if (move.equals(ponderMove)) {
					searchEngine.ponderhit();
					ponderHit = true;
				} else {
					isReset = true;
					searchEngine.stop();
					while (isPondering) {
						try {
							wait();
						} catch (InterruptedException e) { }
					}
				}
			}
		}
		usersTurn = !usersTurn;
		List<String> moveHistList = null;
		if (!ponderHit) {
			searchEngine.position(controllerEngine.getStartPosition());
			moveHistList = controllerEngine.getMoveHistory();
			for (String m : moveHistList)
				searchEngine.play(m);
		}
		if (controllerEngine.getGameState() != GameState.IN_PROGRESS) {
			isReset = true;
			searchEngine.stop();
			Alert dialog = new GameOverAlert(stage, controllerEngine.getGameState(), false);
			dialog.show();
		} else if (whiteTime.get() <= 0) {
			isReset = true;
			searchEngine.stop();
			whiteTime.set(0);
			controllerEngine.whiteForfeit();
			setTimeField(true);
			Alert dialog = new GameOverAlert(stage, controllerEngine.getGameState(), true);
			dialog.show();
		} else if (blackTime.get() <= 0) {
			isReset = true;
			searchEngine.stop();
			blackTime.set(0);
			controllerEngine.blackForfeit();
			setTimeField(false);
			Alert dialog = new GameOverAlert(stage, controllerEngine.getGameState(), true);
			dialog.show();
		} else {
			doTime = true;
			if (!usersTurn) {
				if (!ponderHit)
					startSearch();
			} else if (doPonder)
				startPondering();
		}
		if (usersTurn) {
			if (moveHistList.size() > 1)
				unmakeMove.setDisable(false);
		}
	}
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
			searchEngine.position(controllerEngine.getStartPosition());
			List<String> moveHistList = controllerEngine.getMoveHistory();
			for (String m : moveHistList)
				searchEngine.play(m);
			unmakeMove.setDisable(isUserWhite != controllerEngine.isWhitesTurn() || moveHistList.size() < 2);
		} else {
			if (!controllerEngine.position(position))
				throw new IllegalArgumentException();
			searchEngine.position(position);
			unmakeMove.setDisable(true);
			controllerEngine.setPlayers(isUserWhite ? "Player" : searchEngine.getName(),
					isUserWhite ? searchEngine.getName() : "Player");
			controllerEngine.setEvent("DETROID Chess GUI Game");
		}
		usersTurn = isUserWhite == controllerEngine.isWhitesTurn();
		legalMoves = controllerEngine.getLegalMoves();
		setBoard(controllerEngine.toFEN());
		setMoveHistory();
		setTimeField(true);
		setTimeField(false);
		searchStats.clear();
		if (controllerEngine.getGameState() != GameState.IN_PROGRESS) {
			Alert dialog = new GameOverAlert(stage, controllerEngine.getGameState(), false);
			dialog.show();
			return;
		}
		doTime = true;
		if (!usersTurn)
			startSearch();
		else if (doPonder)
			startPondering();
	}
	@FXML
	public void initialize() {
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
							} catch (InterruptedException e) { }
						}
					}
				}
				setPosition("startpos", false);
			}
		});
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
							} catch (InterruptedException e) { }
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
							} catch (InterruptedException e) { }
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
		copyFen.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
				content.put(DataFormat.PLAIN_TEXT, controllerEngine.toFEN());
				clipboard.setContent(content);
			}
		});
		copyPgn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
				content.put(DataFormat.PLAIN_TEXT, controllerEngine.toPGN());
				clipboard.setContent(content);
			}
		});
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
							} catch (InterruptedException e) { }
						}
					}
				}
				searchEngine.newGame();
				searchEngine.position(controllerEngine.getStartPosition());
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
				doTime = true;
			}
		});
		side.setOnAction(new EventHandler<ActionEvent>() {

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
				if (controllerEngine.isWhitesTurn()) {
					if (whiteTimes.size() > 0)
						whiteTime.set(whiteTimes.peek());
				} else {
					if (blackTimes.size() > 0)
						blackTime.set(blackTimes.peek());
				}
				isUserWhite = !isUserWhite;
				usersTurn = controllerEngine.isWhitesTurn() == isUserWhite;
				unmakeMove.setDisable(!usersTurn || controllerEngine.getMoveHistory().size() < 2);
				if (controllerEngine.getGameState() == GameState.IN_PROGRESS) {
					doTime = true;
					if (!usersTurn)
						startSearch();
				}
			}
		});
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
				if (!usersTurn) {
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
				Dialog<TimeControl> timeSettingsDialog = new TimeSettingsDialog(stage, timeControl);
				Optional<TimeControl> res = timeSettingsDialog.showAndWait();
				if (res.isPresent())
					timeControl = res.get();
				if (controllerEngine.getGameState() == GameState.IN_PROGRESS) {
					doTime = true;
					if (!usersTurn)
						startSearch();
				}
			}
		});
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
				if (!usersTurn) {
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
				Alert optionsDialog = new OptionsAlert(stage, searchEngine);
				optionsDialog.showAndWait();
				if (controllerEngine.getGameState() == GameState.IN_PROGRESS) {
					doTime = true;
					if (!usersTurn)
						startSearch();
				}
			}
		});
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
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Node n = new StackPane();
				String sqr = sqrIndToName((7 - i)*8 + j);
				boolean white = i%2 == 0 ? j%2 == 0 : j%2 == 1;
				n.getStyleClass().add(white ? WHITE_SQR_STYLE_CLASS : BLACK_SQR_STYLE_CLASS);
				n.setOnMouseClicked(new EventHandler<Event>() {

					@Override
					public void handle(Event event) {
						if (!usersTurn || controllerEngine.getGameState() != GameState.IN_PROGRESS)
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
		searchStats = FXCollections.observableArrayList();
		searchStatView.setItems(searchStats);
		moveHistory.setWrapText(true);
		isUserWhite = true;
		setPosition("startpos", false);
	}
	@Override
	public synchronized void update(Observable o, Object arg) {
		if (isReset || (!isPondering && !isSearching))
			return;
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
		SearchData stats = new SearchData("" + info.getDepth(), String.format("%.2f", (double) info.getTime()/1000),
				"" + info.getNodes(), "" + info.getCurrentMoveNumber(), String.join(" ", info.getPv()), score, "" + (info.getTime() == 0 ?
				0 : info.getNodes()/info.getTime()), String.format("%.2f", (double) searchEngine.getHashLoadPermill()/1000));
		Platform.runLater(() -> searchStats.add(stats));
	}
	@Override
	public void close() throws Exception {
		searchEngine.stop();
		timer.cancel();
		executor.shutdown();
	}
	
}
